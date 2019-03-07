package org.tetra.config
import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

class ApplicationConfiguration {


  def getConfigInfo(fileName: String): Config = {

    val configFile = new File(fileName)

    val fileConfig = ConfigFactory.parseFile(configFile)
    val config = ConfigFactory.load(fileConfig)
    config

  }

}