package com.expedia.www.haystack.commons.kstreams.app

import com.codahale.metrics.JmxReporter
import com.expedia.www.haystack.commons.logger.LoggerUtils
import com.expedia.www.haystack.commons.metrics.MetricsSupport
import org.slf4j.LoggerFactory

trait Main extends MetricsSupport {

  def main(args: Array[String]): Unit = {
    //create an instance of the application
    val app = new Application(createStreamsRunner())

    //start the application
    app.start()

    //add a shutdown hook
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = app.stop()
    })
  }

  def createStreamsRunner(): StreamsRunner
}

/**
  * This is the main application class. This controls the application
  * start and shutdown actions
  *
  * @param topologyRunner instance of TopologyRunner to start and stop
  */
class Application(topologyRunner: StreamsRunner) extends MetricsSupport {

  private val LOGGER = LoggerFactory.getLogger(classOf[Application])
  private val jmxReporter: JmxReporter = JmxReporter.forRegistry(metricRegistry).build()

  def start(): Unit = {
    //start JMX reporter for metricRegistry
    jmxReporter.start()

    //start the topology
    topologyRunner.start()
  }

  def stop(): Unit = {
    LOGGER.info("Shutting down topology")
    topologyRunner.close()

    LOGGER.info("Shutting down jmxReporter")
    jmxReporter.close()

    LOGGER.info("Shutting down logger. Bye!")
    LoggerUtils.shutdownLogger()
  }
}

