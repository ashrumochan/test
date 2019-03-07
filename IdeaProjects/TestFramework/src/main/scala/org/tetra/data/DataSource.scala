package org.tetra.data

import com.typesafe.config.Config
import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, SparkSession}

class DataSource {

  def getSourceData(sparkSession: SparkSession, config: Config, log: Logger): DataFrame = {
    var sourceData: DataFrame = null

    if (config.getBoolean("input.isDirectHive")) {
      log.info("Hive Direct Data Source")
      sourceData = sparkSession.sql(config.getString("input.query"))
    } else if (config.getBoolean("input.isFile")) {
      sourceData = sparkSession.read.format("csv")
        .option("header", config.getBoolean("input.fileWithSchema").toString())
        .load(config.getString("input.fileName"))
    } else {
      log.info("JDBC Data Source")
      sourceData = sparkSession.read.format("jdbc")
        .option("url", config.getString("input.databaseUrl"))
        .option("driver", config.getString("input.driverName"))
        .option("dbtable", config.getString("input.query"))
        .option("user", config.getString("input.databaseUserName"))
        .option("password", config.getString("input.databasePassword"))
        .load()
    }

    return sourceData
  }

}
