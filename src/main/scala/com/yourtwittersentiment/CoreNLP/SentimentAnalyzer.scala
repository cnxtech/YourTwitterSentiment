package com.yourtwittersentiment.CoreNLP

import java.util.Properties

import com.yourtwittersentiment.Model.Sentiment.Sentiment
import com.yourtwittersentiment.Model.Sentiment
import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
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

  def getSentiment(tweet: String): Sentiment = Option(tweet) match {
    case Some(tweetText) if !tweetText.isEmpty => computeSentiment(tweetText)
    case _ => Sentiment.toSentiment(-1)
  }

  def computeSentiment(tweet: String): Sentiment = {
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

}
