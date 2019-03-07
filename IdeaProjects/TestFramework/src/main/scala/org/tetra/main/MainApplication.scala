package org.tetra.main

import org.tetra.config.{ApplicationConfiguration, SparkSessionFactory}
import org.tetra.data.DataSource
import org.tetra.log.LoggingFactory
import org.tetra.validate.Validator

object MainApplication {

  def main(args: Array[String]): Unit = {
    val configFileName = args(0)
    val config = new ApplicationConfiguration().getConfigInfo(configFileName)
    val log = new LoggingFactory(config.getString("log.fileName")).getLogger()
    log.info("Job Executed by: "+config.getString("user.name"))
    log.info("Initiating Spark Session")
    val sparkSession = new SparkSessionFactory().getInstance()
    val validator = new Validator()
    val sourceDataFrame = new DataSource().getSourceData(sparkSession, config, log)

    if (config.getBoolean("test.nullCheck")) {
      log.info("Null Check Initiated")
      if (validator.nullCheck(sourceDataFrame, log)) {
        log.info("Null Check Succeeded")
      } else {
        log.error("Null Check Failed")
      }
    }

    if (config.getBoolean("input.duplicateCheck")) {
      log.info("Duplicate Check Initiated")
      if (validator.duplicateCheck(sourceDataFrame, config, log)) {
        log.info("Duplicate Check Succeeded")
      } else {
        log.error("Duplicate Check Failed")
      }
    }

    if (config.getBoolean("test.absoluteDuplicateCheck")) {
      log.info("Absolute Duplicate Check Initiated")
      if (validator.absoluteDuplicateCheck(sourceDataFrame, config, log)) {
        log.info("Absolute Duplicate Check Succeeded")
      } else {
        log.error("Duplicate Check Failed")
      }
    }

  }
}
