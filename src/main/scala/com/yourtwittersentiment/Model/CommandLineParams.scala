package com.yourtwittersentiment.Model

/**
  * Created by siddartha on 11/29/17.
  */
case class CommandLineParams(
                               appName: String = "YourTwitterSentiment",
                               sparkMaster: String = "local[*]",
                               jobId: String = "yourtwittersentiment"
                             )
