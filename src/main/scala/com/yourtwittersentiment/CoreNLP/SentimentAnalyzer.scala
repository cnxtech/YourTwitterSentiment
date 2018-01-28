package com.yourtwittersentiment.CoreNLP

import java.util.Properties

import com.yourtwittersentiment.Model.Sentiment._
import com.yourtwittersentiment.Model.{Classifier, Sentiment}
import com.yourtwittersentiment.Utils.{AppSettings, Util}
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import org.apache.spark.ml.PipelineModel
import scala.collection.convert.wrapAll._
/**
  * Created by siddartha on 11/29/17.
  */
object SentimentAnalyzer {

  lazy val pipeline: StanfordCoreNLP  = {
    val properties = new Properties()
    properties.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment")
    new StanfordCoreNLP(properties)
  }

  val model = PipelineModel.load(s"model/${AppSettings.classifierType.toString}")

  def getSentiment(tweet: String): Sentiment = Option(tweet) match {
    case Some(tweetText) if !tweetText.isEmpty => computeSentiment(tweetText)
    case _ => Sentiment.toSentiment(-1)
  }

  def computeSentiment(tweet: String): Sentiment = {
    val classifier = AppSettings.classifierType

    if(classifier == Classifier.CORENLP)
      computeCoreNLPSentiment(tweet)
    else
      computeClassifierSentiment(tweet)
  }

  def computeCoreNLPSentiment(tweet: String): Sentiment = {
    val (_, sentiment) = extractSentiments(tweet)
      .maxBy { case (sentence, _) => sentence.length }
    sentiment
  }

  def extractSentiments(text: String): List[(String, Sentiment)] = {
    val annotation: Annotation = pipeline.process(text)
    val sentences = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
    sentences
      .map(sentence => (sentence, sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])))
      .map { case (sentence, tree) => (sentence.toString, Sentiment.toSentiment(RNNCoreAnnotations.getPredictedClass(tree))) }
      .toList
  }

  def computeClassifierSentiment(tweet: String): Sentiment = {
    val test = Util.spark.createDataFrame(Seq(
      ("1", tweet)
    )).toDF("id", "text")

    val prediction = model.transform(test).select("predictedPolarity").head().getString(0).toInt
    Sentiment.toSentiment(prediction)
  }

}
