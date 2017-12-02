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
import org.apache.spark.streaming.{Seconds, StreamingContext}
import com.yourtwittersentiment.Utils.{ParamsParser, Util}
import twitter4j.Status

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
    streamingContext
  }

  object TwitterSentimentExtractor {
    def run(streamingContext: StreamingContext): Unit = {
      println("------------------------------------Initializing twitter stream------------------------------------")

      val twitterStream = TwitterUtils.createStream(streamingContext, Util.getTwitterAuth())

      val analyzedTweets = twitterStream.filter(filterTweets).map(analyzeSentimentForTweet).cache()

      val userSentimentDStream = analyzedTweets
        .map(getSentimentByUser)
        .map(new Gson().toJson(_))

      val tagCountDStream = analyzedTweets.flatMap(_._1.getHashtagEntities.map(_.getText).map(tag => (tag, 1L)))

      val tagSentimentDStream = analyzedTweets.flatMap(statusSentiment => {
        statusSentiment._1.getHashtagEntities.map(_.getText).map(tag => ((statusSentiment._2, tag), 1L))
      })
        .reduceByKey(_ + _)
        .map(tagSentiCount => (tagSentiCount._1._2, (tagSentiCount._1._1, tagSentiCount._2)))
        .transform { rdd =>
          rdd.groupByKey()
        }
        .join(tagCountDStream)
        .map(extractSentimentByTag)
        .map(new Gson().toJson(_))

      val kafkaSink = Util.sc.broadcast(KafkaSink(Util.getKafkaProps()))

      userSentimentDStream.foreachRDD(rdd => {
        if (rdd.count() == 0) {
          println("-------------------------------No Tweets With tag--------------------------------------------------")
        } else {
          val messageRdd = rdd.map(new Gson().toJson(_))
          messageRdd.foreach(message => kafkaSink.value.send(AppSettings.kafkaUserTopic, message))
        }
      })

      tagSentimentDStream.foreachRDD(rdd => {
        if (rdd.count() > 0) {
          rdd.foreach(message => {
            println("tag: " + message)
            kafkaSink.value.send(AppSettings.kafkaTagTopic, message)
          })
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
      tweet.getGeoLocation != null
    }

    def analyzeSentimentForTweet(tweet: Status): (Status, Sentiment.Sentiment) = {
      val sentiment = SentimentAnalyzer.getSentiment(tweet.getText.replaceAll("\n", ""))
      (tweet, sentiment)
    }

    def getSentimentByUser(tweetSentiment: (Status, Sentiment.Sentiment)): SentimentByUser = {
      val tweet = tweetSentiment._1
      val sentiment = tweetSentiment._2
      val sentimentByUser = SentimentByUser(tweet.getUser.getScreenName,
        tweet.getUser.getOriginalProfileImageURL,
        tweet.getText,
        tweet.getGeoLocation.getLatitude,
        tweet.getGeoLocation.getLongitude,
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
  }

}
