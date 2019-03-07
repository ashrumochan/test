package org.tetra.config

import org.apache.spark.sql.SparkSession

class SparkSessionFactory {

  private var spark: SparkSession = null

  def getInstance(): SparkSession = {
    if (spark == null) {
      spark = SparkSession
        .builder()
        .appName("Data Validation")
        .config("hive.exec.dynamic.partition", "true")
        .config("hive.exec.dynamic.partition.mode", "nonstrict")
        .config("hive.warehouse.data.skipTrash", "true")
        .enableHiveSupport()
        .getOrCreate
    }
    spark
  }
}