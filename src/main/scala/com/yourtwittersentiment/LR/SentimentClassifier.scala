package com.yourtwittersentiment.LR

import com.johnsnowlabs.nlp.{Finisher, DocumentAssembler}
import com.johnsnowlabs.nlp.annotators.spell.norvig.NorvigSweetingApproach
import com.johnsnowlabs.nlp.annotators.{Lemmatizer, Stemmer, Normalizer, RegexTokenizer}
import com.johnsnowlabs.nlp.annotators.sbd.pragmatic.SentenceDetectorModel
import com.yourtwittersentiment.Model.{Sentiment, Classifier}
import com.yourtwittersentiment.Utils.{AppSettings, Util}
import org.apache.spark.ml.{PipelineModel, PipelineStage, Pipeline}
import org.apache.spark.ml.classification.{MultilayerPerceptronClassifier, NaiveBayes}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.sql._

/**
  * Created by siddartha on 12/25/17.
  */
object SentimentClassifier {
  def run(): Unit = {

    val sentenceDF = readTrainingData()

    val labelIndexer = new StringIndexer()
      .setInputCol("polarity")
      .setOutputCol("label")
      .fit(sentenceDF)

    val intermediate = getTextProcessingStages(2000)

    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedPolarity")
      .setLabels(labelIndexer.labels)


    val Array(trainingData, testData) = sentenceDF.randomSplit(Array(0.8,
      0.2))

    val classifier = new NaiveBayes()

    val stages = labelIndexer +: intermediate :+ classifier :+ labelConverter

    val pipeline = new Pipeline()
      .setStages(stages)

    val model = pipeline.fit(trainingData)

    val predictions = model.transform(testData)

    predictions.show(100)

    evaluateModel(predictions)
    saveModel(model)
  }

  def saveModel(model: PipelineModel): Unit = {
    val classifierType = AppSettings.classifierType.toString
    model.write.overwrite().save(s"model/$classifierType")
  }

  def trainPerceptron(): Unit = {
    val vocabSize = 1000
    val sentenceDF = readTrainingData()

    val labelIndexer = new StringIndexer()
      .setInputCol("polarity")
      .setOutputCol("label")
      .fit(sentenceDF)

    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedPolarity")
      .setLabels(labelIndexer.labels)

    val intermediate = getTextProcessingStages(vocabSize)

    val Array(trainingData, testData) = sentenceDF.randomSplit(Array(0.8,
      0.2))

    val stages = labelIndexer +: intermediate

    val pipeline = new Pipeline()
      .setStages(stages)

    val featureTransofromModel = pipeline.fit(trainingData)

    val train = featureTransofromModel.transform(trainingData).select("polarity", "text", "label", "features")
    val test = featureTransofromModel.transform(testData).select("polarity", "text", "label", "features")

    train.show(20)

    val layers = Array[Int](vocabSize, (vocabSize / 2).toInt, (vocabSize / 2).toInt, 2)

    val trainer = new MultilayerPerceptronClassifier()
      .setLayers(layers)
      .setBlockSize(128)
      .setSeed(1234L)
      .setMaxIter(50)

    val model = trainer.fit(train)

    val predictions = model.transform(test)

    val predictionsFinal = labelConverter.transform(predictions)

    predictionsFinal.show(100)
    evaluateModel(predictionsFinal)
    model.write.overwrite().save(s"model/${AppSettings.classifierType.toString}")
  }


  def getTextProcessingStages(vocabSize: Int): Array[_ <: PipelineStage] = {

    val documentAssembler = new DocumentAssembler()
      .setInputCol("text")

    val sentenceDetector = new SentenceDetectorModel()
      .setInputCols(Array("document"))
      .setOutputCol("sentence")

    val tokenizer = new RegexTokenizer()
      .setInputCols(Array("sentence"))
      .setOutputCol("token")

    val normalizer = new Normalizer()
      .setInputCols(Array("token"))
      .setOutputCol("normalized")

    val spellChecker = new NorvigSweetingApproach()
      .setInputCols(Array("normalized"))
      .setOutputCol("spell")

    val stemmer = new Stemmer()
      .setInputCols(Array("spell"))
      .setOutputCol("stem")

    val finisher = new Finisher()
      .setInputCols(Array("spell"))
      .setOutputCols(Array("processed"))
      .setIncludeKeys(true)
      .setCleanAnnotations(false)
      .setOutputAsArray(true)


    val stopwords: Array[String] = Util.sc.textFile("data/english.txt")
      .flatMap(_.stripMargin.split("\\s+"))
      .collect ++ Array("rt")

    val filterer = new StopWordsRemover()
      .setStopWords(stopwords)
      .setCaseSensitive(false)
      .setInputCol("processed")
      .setOutputCol("finished")

    val countVectorizer = new CountVectorizer()
      .setInputCol("finished")
      .setOutputCol("features")
      .setVocabSize(vocabSize)

    val hashingtf = new HashingTF()
      .setInputCol("finished")
      .setOutputCol("tf")
      .setNumFeatures(vocabSize)

    val idf = new IDF()
      .setInputCol("tf")
      .setOutputCol("features")

    Array(
      documentAssembler,
      sentenceDetector,
      tokenizer,
      normalizer,
      spellChecker,
//      stemmer,
      finisher,
      filterer,
      countVectorizer)
    //          hashingtf,
    //          idf,)
  }

  def readTrainingData(): DataFrame = {
    val sqlContext = Util.spark.sqlContext
    import sqlContext.implicits._
    val inputText = Util.sc.textFile(AppSettings.trainingDataSet)

    val sentenceDF = inputText
      .map(sentence => {
        val words = sentence.split(",")
        (words(0), words(5))
      }).toDF("polarity", "text")
    sentenceDF
  }

  def evaluateModel(predictions: DataFrame): Unit = {

    val testtp = predictions.filter("polarity == predictedPolarity").count * 1.0
    val predCount = predictions.count() * 1.0
    println("Test accuracy-------- " + (testtp / predCount) * 100)

    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("label")
      .setPredictionCol("prediction")
      .setMetricName("accuracy")

    val accuracy = evaluator.evaluate(predictions)
    println("Test Error = " + (1.0 - accuracy))
  }
}
