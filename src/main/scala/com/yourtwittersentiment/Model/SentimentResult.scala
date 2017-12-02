package com.yourtwittersentiment.Model

/**
  * Created by siddartha on 11/30/17.
  */
case class SentimentByUser(
                        userName: String,
                        imageUrl: String,
                        tweet: String,
                        latitude: Double,
                        longitude: Double,
                        sentiment: String
                        )

case class SentimentByTag(
                            tag: String,
                            count: Long,
                            positiveSentimentCount: Long,
                            negativeSentimentCount: Long,
                            neutralSentimentCount: Long
                          )

