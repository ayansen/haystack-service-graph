package com.expedia.www.haystack.commons.kstreams.app

import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig, Topology}

/*
 *
 *     Copyright 2018 Expedia, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *
 */

import org.slf4j.LoggerFactory

import scala.util.Try

/**
  * 
  * @param topologySupplier
  * @param streamsConfig
  * @param consumerTopicName
  */
class StreamsFactory(topologySupplier: Supplier[Topology], streamsConfig: StreamsConfig, consumerTopicName: Option[String]) {

  require(topologySupplier != null, "streamsBuilder is required")
  require(streamsConfig != null, "streamsConfig is required")

  def this(streamsSupplier: Supplier[Topology], streamsConfig: StreamsConfig) = this(streamsSupplier, streamsConfig, None)

  private val LOGGER = LoggerFactory.getLogger(classOf[StreamsFactory])

  /**
    *
    * @param listener
    * @return
    */
  def create(listener: StateChangeListener): ManagedService = {
    checkConsumerTopic()

    val streams = new KafkaStreams(topologySupplier.get(), streamsConfig)
    streams.setStateListener(listener)
    streams.setUncaughtExceptionHandler(listener)
    streams.cleanUp()
    new ManagedKafkaStreams(streams)
  }

  private def checkConsumerTopic(): Unit = {
    if (consumerTopicName.nonEmpty) {
      val topicName = consumerTopicName.get
      LOGGER.info(s"checking for the consumer topic $topicName")
      val adminClient = AdminClient.create(getBootstrapProperties)
      try {
        val present = adminClient.listTopics().names().get().contains(topicName)
        if (!present) {
          throw new TopicNotPresentException(topicName,
            s"Topic '$topicName' is configured as a consumer and it is not present")
        }
      }
      finally {
        Try(adminClient.close(5, TimeUnit.SECONDS))
      }
    }
  }

  private def getBootstrapProperties: Properties = {
    val properties = new Properties()
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
      streamsConfig.getList(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
    properties
  }

  /**
    * Custom RuntimeException that represents required Kafka topic not present
    * @param topic Name of the topic that is missing
    * @param message Reason why the exception was raised
    */
  class TopicNotPresentException(topic: String, message: String) extends RuntimeException(message) {
  }
}

