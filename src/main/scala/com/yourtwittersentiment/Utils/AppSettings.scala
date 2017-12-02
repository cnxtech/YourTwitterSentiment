package com.yourtwittersentiment.Utils
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by siddartha on 11/29/17.
  */
object AppSettings {
  private val config: Config = ConfigFactory.load("application.conf")

  val twitterAccessToken = config.getString("TWITTER_ACCESS_TOKEN_KEY")
  val twitterAccessTokenSecret = config.getString("TWITTER_ACCESS_TOKEN_SECRET")
  val twitterConsumer = config.getString("TWITTER_CONSUMER_KEY")
  val twitterConsumerSecret = config.getString("TWITTER_CONSUMER_SECRET")

  //val runService = config.getBoolean("RUN_AS_SERVICE")

  val twitterStreamInterval = config.getInt("TWITTER_INTERVAL")

  val kafkaUserTopic = config.getString("kafkaSink.topic.user")
  val kafkaTagTopic = config.getString("kafkaSink.topic.tag")
  val kafkaServers = config.getString("kafkaSink.bootstrap.servers")
  val acks = config.getString("kafkaSink.acks")
  val kafkaBufferMemory = config.getString("kafkaSink.buffer.memory")
  val kafkaBlockOnBufferFull = config.getString("kafkaSink.block.on.buffer.full")
  val kafkaRetries = config.getString("kafkaSink.retries")
  val kafkaBackoff = config.getString("kafkaSink.retry.backoff.ms")



}
