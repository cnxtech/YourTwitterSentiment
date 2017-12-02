package com.yourtwittersentiment.Utils

import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

/**
  * Created by siddartha on 11/29/17.
  */
object SparkSessionFactory {
  def createSparkSession(
                          sparkMaster: String,
                          appName: String
                        ): SparkSession = {

    val conf = new SparkConf()
      .setAppName(appName)

    conf.setMaster(sparkMaster)

    conf.set("spark.scheduler.mode", "FAIR")

    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    conf.set("spark.sql.shuffle.partitions", "50")
    val sparkSession = SparkSession.builder
      .config(conf)
      .getOrCreate()

    sparkSession
  }
}
