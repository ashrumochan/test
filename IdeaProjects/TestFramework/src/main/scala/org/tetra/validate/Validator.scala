package org.tetra.validate

import com.typesafe.config.Config
import org.apache.spark.sql.{DataFrame, Row, SaveMode, SparkSession}
import org.apache.spark.sql.functions.col
import org.apache.log4j.Logger

class Validator {

  def nullCheck(sourceDataFrame: DataFrame, log: Logger): Boolean = {

    var testState = true
    val cols = sourceDataFrame.columns.toSeq
    for (x <- cols) {
      if (sourceDataFrame.count == sourceDataFrame.where(sourceDataFrame.col(x).isNull).count()) {
        if (testState)
          testState = true
      } else {
        log.error("Null Check Failed for: " + x)
      }
    }
    return testState
  }

  def duplicateCheck(sourceDataFrame: DataFrame, config: Config, log: Logger): Boolean = {
    val cols = config.getString("test.duplicateCheckColumns").split(",").map(_.trim)
    val resultDf = sourceDataFrame.groupBy(cols map col: _*).count().filter("count>1")
    var testState = false
    if (resultDf.count() > 0) {
      writeToFile(resultDf, config.getString("test.duplicateCheckFileName"), "csv")
      log.info("Tables has Duplicate records, Placed at: " + config.getString("test.duplicateCheckFileName"))
      testState = false
    } else {
      testState = true
    }
    return testState
  }

  def absoluteDuplicateCheck(sourceDataFrame: DataFrame, config: Config, log: Logger): Boolean = {
    val cols = sourceDataFrame.columns.toSeq
    val resultDf = sourceDataFrame.groupBy(cols map col: _*).count().filter("count>1")
    var testState = false
    if (resultDf.count() > 0) {
      writeToFile(resultDf, config.getString("test.absoluteDuplicateCheckFileName"), "csv")
      log.error("Tables has Absolute Duplicate records, Placed at: " + config.getString("test.absoluteDuplicateCheckFileName"))
      testState = false
    } else {
      testState = true
    }
    return testState
  }


  /**
    * Function to write a dataframe content into file.
    *
    * @param dataFrame input dataframe
    * @param path      output directory to write the file
    * @param format    format of output file
    */
  def writeToFile(dataFrame: DataFrame, path: String, format: String): Unit = {
    dataFrame.write.mode(SaveMode.Append).format(format).save(path)
  }


}