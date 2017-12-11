package com.yourtwittersentiment

import java.util.Properties

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.yourtwittersentiment.CoreNLP.SentimentAnalyzer
import com.yourtwittersentiment.KafkaProducer.KafkaSink
import com.yourtwittersentiment.Model.{SentimentByTag, SentimentByUser, Sentiment, CommandLineParams}
import com.yourtwittersentiment.Utils.{SparkSessionFactory, AppSettings}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.{StateSpec, Seconds, StreamingContext, State}
import com.yourtwittersentiment.Utils.{ParamsParser, Util}
import twitter4j.Status
import scala.util.Random

/**
  * Created by siddartha on 11/29/17.
  */
object YourTwitterSentiment extends App {
  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("org").setLevel(Level.ERROR)

  val paramsOption: Option[CommandLineParams] = ParamsParser.paramsParser.parse(args, CommandLineParams())
  if (paramsOption.isDefined) {
    val params = paramsOption.get
    implicit val sparkSession = SparkSessionFactory.createSparkSession(params.sparkMaster, params.appName)

    Util.spark = sparkSession
    Util.sc = sparkSession.sparkContext

    val streamingContext = createStreamingContext(Util.sc)

    TwitterSentimentExtractor.run(streamingContext)
  }

  def createStreamingContext(sc: SparkContext): StreamingContext = {
    val streamingContext = new StreamingContext(sc, Seconds(AppSettings.twitterStreamInterval))
    streamingContext.checkpoint("sentimentcheckpoint")
    streamingContext
  }

  object TwitterSentimentExtractor {
    val latslong = List((47.923055, -99.2040184), (48.1513804, -97.6442523), (40.6284222, -76.3871768))

    def run(streamingContext: StreamingContext): Unit = {
      println("------------------------------------Initializing twitter stream------------------------------------")

      val twitterStream = TwitterUtils.createStream(streamingContext, Util.getTwitterAuth())

      val analyzedTweets = twitterStream.filter(filterTweets).map(analyzeSentimentForTweet).cache()

      val userSentimentDStream = analyzedTweets
        .map(getSentimentByUser)
        .map(new Gson().toJson(_))

      val tagSentimentDStream = analyzedTweets.flatMap(statusSentiment => {
        statusSentiment._1.getHashtagEntities.map(_.getText.toLowerCase).distinct.map(tag => ((statusSentiment._2, tag), 1L))
      })
        .reduceByKey(_ + _)
        .map(tagSentiCount => (tagSentiCount._1._2, (tagSentiCount._1._1, tagSentiCount._2)))
        .transform { rdd =>
          rdd.groupByKey()
        }
        .mapWithState(StateSpec.function(updateTagState _))

      val kafkaSink = Util.sc.broadcast(KafkaSink(Util.getKafkaProps()))

      userSentimentDStream.foreachRDD(rdd => {
        if (rdd.isEmpty()) {
          println("-------------------------------No Tweets With tag--------------------------------------------------")
        } else {
          val messageRdd = rdd.collect()
          val json = new Gson().toJson(messageRdd)
          println(json)
          kafkaSink.value.send(AppSettings.kafkaUserTopic, json)
        }
      })

      tagSentimentDStream.foreachRDD(rdd => {
        if (!rdd.isEmpty()) {
          val top20 = rdd.takeOrdered(20)(Ordering.by[SentimentByTag, Long](_.count).reverse)
          val gson = new Gson()
          println(gson.toJson(top20))
          kafkaSink.value.send(AppSettings.kafkaTagTopic, gson.toJson(top20))

//                    top20.foreach(message => {
//            println("tag: " + gson.toJson(message))
//            kafkaSink.value.send(AppSettings.kafkaTagTopic, gson.toJson(message))
//          })
        } else {
          println("-------------------------------No Tweets With tag--------------------------------------------------")
        }
      })

      println("------------------------------------Initialization complete------------------------------------")

      streamingContext.start()
      streamingContext.awaitTermination()
    }

    def filterTweets(tweet: Status): Boolean = {
      val tags = tweet.getText.split(" ").filter(_.startsWith("#")).map(_.toLowerCase)
      //tags.contains("#thursdaythoughts") && tweet.getLang == "en"
      tweet.getLang == "en"
    }

    def analyzeSentimentForTweet(tweet: Status): (Status, Sentiment.Sentiment) = {
      val sentiment = SentimentAnalyzer.getSentiment(tweet.getText.replaceAll("\n", ""))
      (tweet, sentiment)
    }

    def getSentimentByUser(tweetSentiment: (Status, Sentiment.Sentiment)): SentimentByUser = {
      val tweet = tweetSentiment._1
      val sentiment = tweetSentiment._2
      val geoLoc = tweet.getGeoLocation
      var lat = 0.0
      var lng = 0.0

      if (geoLoc == null) {
        val loc = latslong(Random.nextInt(latslong.size))
        lat = loc._1
        lng = loc._2
      } else {
        lat = geoLoc.getLatitude
        lng = geoLoc.getLongitude
      }
      val sentimentByUser = SentimentByUser(tweet.getUser.getScreenName,
        tweet.getUser.getOriginalProfileImageURL,
        tweet.getText,
        lat,
        lng,
        sentiment.toString
      )
      sentimentByUser
    }

    def extractSentimentByTag(tags: (String, (Iterable[(Sentiment.Sentiment, Long)], Long))): SentimentByTag = {
      val tag = tags._1
      val countsBySenti = tags._2._1.toIndexedSeq
      val tagCount = tags._2._2

      val sentiPos: (Sentiment.Sentiment, Long) = countsBySenti
        .find(it => it._1 == Sentiment.POSITIVE)
        .getOrElse((Sentiment.POSITIVE, 0L))

      val sentiNeg: (Sentiment.Sentiment, Long) = countsBySenti
        .find(it => it._1 == Sentiment.NEGATIVE)
        .getOrElse((Sentiment.NEGATIVE, 0L))

      val sentiNet: (Sentiment.Sentiment, Long) = countsBySenti
        .find(it => it._1 == Sentiment.NEUTRAL)
        .getOrElse((Sentiment.NEUTRAL, 0L))

      SentimentByTag(tag,
        tagCount,
        sentiPos._2,
        sentiNeg._2,
        sentiNet._2)
    }

    def updateTagState(tag: String,
                       newTagSentiments: Option[Iterable[(Sentiment.Sentiment, Long)]],
                       currentState: State[SentimentByTag]): SentimentByTag = {

      val newCountsBySenti = newTagSentiments.getOrElse(Seq[(Sentiment.Sentiment, Long)]()).toIndexedSeq

      val newSentiPos: (Sentiment.Sentiment, Long) = newCountsBySenti
        .find(it => it._1 == Sentiment.POSITIVE)
        .getOrElse((Sentiment.POSITIVE, 0L))

      val newSentiNeg: (Sentiment.Sentiment, Long) = newCountsBySenti
        .find(it => it._1 == Sentiment.NEGATIVE)
        .getOrElse((Sentiment.NEGATIVE, 0L))

      val newSentiNet: (Sentiment.Sentiment, Long) = newCountsBySenti
        .find(it => it._1 == Sentiment.NEUTRAL)
        .getOrElse((Sentiment.NEUTRAL, 0L))

      val existingState = currentState.getOption().getOrElse(SentimentByTag(tag, 0, 0, 0, 0))

      val newState = SentimentByTag(tag,
        existingState.count + 1,
        existingState.positiveSentimentCount + newSentiPos._2,
        existingState.negativeSentimentCount + newSentiNeg._2,
        existingState.neutralSentimentCount + newSentiNet._2)

      currentState.update(newState)
      newState
    }
  }

}
