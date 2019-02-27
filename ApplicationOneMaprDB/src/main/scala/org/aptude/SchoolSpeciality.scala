package org.aptude


import org.apache.spark.sql.functions.{col, concat_ws, max,explode}
import org.apache.spark.sql.{DataFrame, Row, SaveMode, SparkSession}
import com.mapr.db.spark.sql._

object SchoolSpeciality {

  var maxCount: Long = 0

  /**
    * Create a SparkSession Object
    * @return SparkSession object
    */
  def creatSparkSession(): SparkSession = {

    return SparkSession
      .builder()
      .appName("SchoolSpeciality")
      .master("local[1]")
      .getOrCreate()

  }

  /**
    * Function to read a file and create dataframe out of it
    * @param sparkSession SparkSession object
    * @param file input file to read
    * @return dataframe with content of the file
    */
  def readFile(sparkSession: SparkSession,file: String): DataFrame = {
    return sparkSession.read.option("multiline",true).json(file)
  }

  /**
    *
    * Function to read data from MaprDb
    * @param sparkSession Sparksession object
    * @param table        Table name to fetch data from Maprdb
    * @return
    */
  def readMaprDb(sparkSession: SparkSession,table: String): DataFrame = {
    return sparkSession.loadFromMapRDB(table)
  }

  /**
    * Function to write a dataframe content into file.
    * @param dataFrame input dataframe
    * @param path output directory to write the file
    * @param format format of output file
    */
  def writeToFile(dataFrame: DataFrame,path: String,format: String): Unit = {
    dataFrame.coalesce(1).write.mode(SaveMode.Append).format(format).save(path)
  }

  def writeToMaprDb(dataFrame: DataFrame,tableName: String): Unit ={
    dataFrame.write.option("Operation", "Insert").saveToMapRDB(tableName)
  }

  /**
    *
    * @param dataFrame Input dataframe for processing
    * @param level Level of processing: used to differentiate file
    * @param parent parentId list
    * @return returns catrgoryId as list for next cycle
    */
  def processData(dataFrame: DataFrame, level: Int,sparkSession: SparkSession,parent: List[Any]=null): List[Any] ={

    try {
      if(level==0){
        var resultDf= dataFrame.filter(col("parentId")===0)
        resultDf = resultDf.withColumn("_id",concat_ws("_",col("parentId"),col("categoryId")))
        resultDf.show(5)
        writeToMaprDb(resultDf, "/tables/category")
        return resultDf.select("categoryId").collect().map(_(0)).toList
      }else{
        var resultDf = dataFrame.filter(col("parentId").isin(parent: _*))
        resultDf = resultDf.withColumn("_id",concat_ws("_",col("parentId"),col("categoryId")))
        resultDf.show(5)
        writeToMaprDb(resultDf, "/tables/category")
        return resultDf.select("categoryId").collect().map(_(0)).toList
      }
    } catch {
      case unknown: Exception =>
        println("Error while processing level:: " + 0)
        unknown.printStackTrace()
        return null
    }
  }


  /**
    *
    * @param categoryDf input category dataframe for processing
    * @param sparkSession input sparkSession variable
    * @return list of categories id
    */
  def processCategoryData(categoryDf: DataFrame,sparkSession: SparkSession): Unit ={

    var i: Int =0
    var parentData = List[Any]()
    for(i <- 0 to 3){
      if(i==0){
        parentData = processData(categoryDf,0,sparkSession)
      }else{
        parentData = processData(categoryDf,i,sparkSession,parentData)
      }
    }
  }

  /**
    * Function to get list of category id's from MaprDb
    * @param sparkSession SParksession object
    * @return
    */
  def getCategoryIds(sparkSession: SparkSession,tableName:String): List[Any] ={
    val maprDf = readMaprDb(sparkSession,tableName)
    var categoryId = List[Any]()
    categoryId=maprDf.select("categoryId").collect().map(_(0)).toList

    return categoryId

  }

  /**
    * Function to get list of product id's available in MaprDb
    * @param sparkSession SparkSession Object
    * @return
    */
  def getProductIds(sparkSession: SparkSession,tableName: String): List[Any] ={
    val productDf = readMaprDb(sparkSession,tableName)
    var productIds = List[Any]()
    productIds=productDf.select("sku").collect().map(_(0)).toList

    return productIds
  }

  /**
    *
    * @param productDf product dataframe from processing
    * @param categoryIdList list of categories for finding product
    */
  def processProductData(productDf: DataFrame,categoryIdList: List[Any]): Unit ={
    val df1 = productDf.select(productDf("timestamp"),productDf("products"),explode(productDf("products")))
    val df2 = df1.select(df1("timestamp"),df1("products"),df1("col.sku"),explode(df1("col.categories")))
    val df3 = df2.select(df2("timestamp"),df2("products"),df2("sku"),df2("col.id").as("categoryId"))
    val df4 = df3.filter(col("categoryId").isin(categoryIdList: _*))
    val resultDf = df4.withColumn("_id",concat_ws("_",col("sku"),col("categoryId")))

    writeToMaprDb(resultDf, "/tables/product")
  }


  /**
    * Function to process product price information
    * @param priceDf intput price data as Dataframe
    * @param productIdList  list of product id
    */
  def processPriceData(priceDf: DataFrame,productIdList: List[Any]): Unit ={
    val df1 = priceDf.filter(col("sku").isin(productIdList: _*))
    val resultDf = df1.withColumn("_id",concat_ws("_",col("sku"),col("quantity"),col("listType"),col("originId")))

    writeToMaprDb(resultDf, "/tables/def-price")
  }


  /**
    * Main program execution starts here
    * @param args
    */
  def main(args: Array[String]): Unit = {
    val categoryFile = "/user/school_data/CategoryHier_20181211140246050.json"
    val productFIle="/user/school_data/Product_20181004131909180.json"
    val priceFile="/user/school_data/Price_MARKET_20181221004058547.json"

    val sparkSession = creatSparkSession()
    val categoryDf = readFile(sparkSession, categoryFile)
    var categoryIdList = List[Any]()
    processCategoryData(categoryDf,sparkSession)

    categoryIdList=getCategoryIds(sparkSession,"/tables/category")

    val productDf = readFile(sparkSession, productFIle)
    processProductData(productDf,categoryIdList)

    var productIdList = List[Any]()
    productIdList=getProductIds(sparkSession,"/tables/product")

    val priceDf = readFile(sparkSession, priceFile)
    processPriceData(priceDf,productIdList)

  }
}