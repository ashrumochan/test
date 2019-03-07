package org.tetra.log

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

class LoggingFactory(var logFileName: String) {
  val layout = new PatternLayout();
  val conversionPattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n";
  layout.setConversionPattern(conversionPattern);

  // creates console appender
  val consoleAppender = new ConsoleAppender();
  consoleAppender.setLayout(layout);
  consoleAppender.activateOptions();

  // creates file appender
  val fileAppender = new FileAppender();
  fileAppender.setFile(logFileName);
  fileAppender.setLayout(layout);
  fileAppender.activateOptions();

  // configures the root logger
  val rootLogger = Logger.getRootLogger();
  rootLogger.setLevel(Level.DEBUG);
  rootLogger.addAppender(consoleAppender);
  rootLogger.addAppender(fileAppender);

  val log = Logger.getLogger("MainLogging")

  def getLogger(): Logger = {
    return log;
  }

}
