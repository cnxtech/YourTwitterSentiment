package com.yourtwittersentiment.Utils

import com.yourtwittersentiment.Model.CommandLineParams

/**
  * Created by siddartha on 11/29/17.
  */
object ParamsParser {
    val paramsParser = new scopt.OptionParser[CommandLineParams]("yourtwittersentiment") {
      head("yourtwittersentiment")

      opt[String]("sparkMaster")
        .text("sparkMaster")
        .action((sm, config) => config.copy(sparkMaster = sm))

      opt[String]("jobId")
        .text("Job identifier")
        .action((id, config) => config.copy(jobId = id))

      help("help").text("prints this help text")

    }
}
