package com.yourtwittersentiment.Model

/**
  * Created by siddartha on 11/29/17.
  */
object Sentiment extends Enumeration {
  type Sentiment = Value
  val POSITIVE, NEGATIVE, NEUTRAL = Value

  def toSentiment(sentiment: Int): Sentiment = sentiment match {
    case x if x == 0 || x == 1 => Sentiment.NEGATIVE
    case 2 => Sentiment.NEUTRAL
    case x if x == 3 || x == 4 => Sentiment.POSITIVE
  }

  override def toString() =
    this match {
      case POSITIVE => "Positive"
      case NEUTRAL=> "Neutral"
      case NEGATIVE => "Negative"
    }

}