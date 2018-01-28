package com.yourtwittersentiment.Model

/**
  * Created by siddartha on 12/25/17.
  */
object Classifier extends Enumeration {
    type Model = Value
    val NAIVE_BAYES, RANDOM_FOREST, MLPC, CORENLP = Value

  def toClassifier(ctype: String): Classifier.Value = {
    ctype match {
      case "NaiveBayes" => NAIVE_BAYES
      case "Mlpc" => MLPC
      case "RandomForest" => RANDOM_FOREST
      case "CoreNlp" => CORENLP
    }
  }

   override def toString() =
      this match {
        case NAIVE_BAYES => "NaiveBayes"
        case MLPC=> "Mlpc"
        case RANDOM_FOREST => "RandomForest"
        case CORENLP => "CoreNlp"
      }
}
