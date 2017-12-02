package com.yourtwittersentiment.KafkaProducer

import java.util.Properties

import org.apache.kafka.clients.producer.{ProducerRecord, KafkaProducer}

/**
  * Created by siddartha on 11/30/17.
  */
class KafkaSink(createProducer: () => KafkaProducer[String, String]) extends Serializable {

  lazy val producer = createProducer()

  def send(topic: String, value: String): Unit = producer.send(new ProducerRecord(topic, value))
}

object KafkaSink {
  def apply(config: Properties): KafkaSink = {
    val f = () => {
      val producer = new KafkaProducer[String, String](config)

      sys.addShutdownHook {
        producer.close()
      }

      producer
    }
    new KafkaSink(f)
  }
}
