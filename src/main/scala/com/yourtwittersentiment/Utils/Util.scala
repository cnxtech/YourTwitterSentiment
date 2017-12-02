package com.yourtwittersentiment.Utils

import java.util.Properties

import org.apache.log4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import twitter4j.TwitterFactory
import twitter4j.auth.Authorization
import twitter4j.conf.ConfigurationBuilder

import scala.io.Source

/**
  * Created by siddartha on 11/29/17.
  */
object Util {
  val logger = Logger.getLogger(this.getClass)

  var spark: SparkSession = null
  var sc: SparkContext = null

  val config: Properties = new Properties()
  config.put("bootstrap.servers", AppSettings.kafkaServers)
  config.put("acks", AppSettings.acks)
  config.put("retries", AppSettings.kafkaRetries)
  config.put("buffer.memory", AppSettings.kafkaBufferMemory)
  config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  def getTwitterAuth(): Some[Authorization] ={

    val configurationBuilder = new ConfigurationBuilder()
    configurationBuilder.setOAuthConsumerKey(AppSettings.twitterConsumer)
    configurationBuilder.setOAuthConsumerSecret(AppSettings.twitterConsumerSecret)
    configurationBuilder.setOAuthAccessToken(AppSettings.twitterAccessToken)
    configurationBuilder.setOAuthAccessTokenSecret(AppSettings.twitterAccessTokenSecret)
    Some(new TwitterFactory(configurationBuilder.build()).getInstance().getAuthorization)
  }

  def loadFileAsList(fileName: String) : List[String] = {
      Source.fromInputStream(getClass.getResourceAsStream("/" + fileName)).getLines().toList
  }

  def getKafkaProps(): Properties = {
    config
  }
}
