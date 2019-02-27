package org.aptude

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, explode, udf}
import com.mapr.db.spark.sql._


object FirstTaskMaprDb {

  def writeToMaprDb(dataFrame: DataFrame,tableName: String): Unit ={
    dataFrame.write.option("Operation", "Insert").saveToMapRDB(tableName)
  }

  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .master("local")
      .appName("FirstApplication")
      .getOrCreate()

    print("Hello Test Application")

    val df = spark.read.option("multiLine",true).json("/input/Product_20181004111036135.json")

    val df1 = df.select(df("timestamp"),explode(df("products")).as("Products"))

    val df2 = df1.select(df1("timestamp"),df1("Products.sku"),df1("Products"),explode(df1("Products.categories")).as("categories"))

    val getConcatenated = udf( (first: String, second: String, third: String) => { first +"|" +second +"|"+third } )

    val df3 = df2.withColumn("_id",getConcatenated(col("sku"),col("categories.type"), col("categories.assignmentId")))
      .select("_id","sku","categories.type","categories.assignmentId","timestamp")

    writeToMaprDb(df3, "/tables/firstTask")
  }

}
