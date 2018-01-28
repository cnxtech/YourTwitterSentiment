package com.yourtwittersentiment.LR

import com.yourtwittersentiment.CoreNLP.SentimentAnalyzer
import com.yourtwittersentiment.Model.CommandLineParams
import com.yourtwittersentiment.Utils.{Util, SparkSessionFactory, ParamsParser}
import com.yourtwittersentiment.Model.Classifier
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame}
import org.apache.spark.ml.feature._
import org.apache.spark.sql.functions.col
import org.apache.spark.ml.classification._

/**
  * Created by siddartha on 12/24/17.
  */
object TrainLR extends App {
  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("org").setLevel(Level.ERROR)

  val paramsOption: Option[CommandLineParams] = ParamsParser.paramsParser.parse(args, CommandLineParams())

  if (paramsOption.isDefined) {
    val params = paramsOption.get
    implicit val sparkSession = SparkSessionFactory.createSparkSession(params.sparkMaster, params.appName)

    Util.spark = sparkSession
    Util.sc = sparkSession.sparkContext

    SentimentClassifier.trainPerceptron()
    //println(SentimentAnalyzer.computeClassifierSentiment("I am happy"))
  }

  object TrainClassifier {
    def run(classifierType: Classifier.Value): Unit = {

            val sentenceDF = SentimentClassifier.readTrainingData()

            val labelIndexer = new StringIndexer()
              .setInputCol("polarity")
              .setOutputCol("label")
              .fit(sentenceDF)

            val indexedDF = labelIndexer.transform(sentenceDF)

            val inputData = produceFeatures(indexedDF)

            val Array(trainingData, testData) = inputData.randomSplit(Array(0.8,
              0.2))

            var classifier = new GBTClassifier()
              .setImpurity("entropy")
              .setMaxIter(30)

            val model = classifier.fit(trainingData)

            val predictions = model.transform(testData)

            val labelConverter = new IndexToString()
              .setInputCol("prediction")
              .setOutputCol("predictedPolarity")
              .setLabels(labelIndexer.labels)

            val predictionsFinal = labelConverter.transform(predictions)


            predictionsFinal.show(100)

            SentimentClassifier.evaluateModel(predictionsFinal)
    }
  }

    def produceFeatures(indexedDF: DataFrame): DataFrame = {

      val tokenizer = new RegexTokenizer()
        .setGaps(false)
        .setPattern("\\p{L}+")
        .setInputCol("text")
        .setOutputCol("words")

      val wordsDF = tokenizer.transform(indexedDF)

      val stopwords: Array[String] = Util.sc.textFile("data/english.txt")
        .flatMap(_.stripMargin.split("\\s+"))
        .collect ++ Array("rt")

      val filterer = new StopWordsRemover()
        .setStopWords(stopwords)
        .setCaseSensitive(false)
        .setInputCol("words")
        .setOutputCol("filtered")

      val noStopWordsDF = filterer.transform(wordsDF)


      val countVectorizer = new CountVectorizer()
        .setInputCol("filtered")
        .setOutputCol("features")

      val countVectorizerModel = countVectorizer.fit(noStopWordsDF)

      val countVectorizerDF = countVectorizerModel.transform(noStopWordsDF)

      val inputData = countVectorizerDF
        .select("polarity", "label", "features")
        .withColumn("label", col("label").cast("double"))
      inputData
    }
}
