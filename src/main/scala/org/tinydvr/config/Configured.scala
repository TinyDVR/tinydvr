package org.tinydvr.config

trait Configured extends DatabaseConfigured {
  protected def tinyDvrConfiguration: TinyDvrConfiguration
  override protected def databaseConfiguration = tinyDvrConfiguration.databaseConfiguration
}

trait DatabaseConfigured {
  protected def databaseConfiguration: DatabaseConnectionInfo
}

trait DatabaseConfigurationFromProperties extends DatabaseConfigured {
  override protected def databaseConfiguration: DatabaseConnectionInfo = DatabaseConfigurationSingleton.fromProperties
}

trait ConfigurationFromProperties extends Configured with DatabaseConfigurationFromProperties {
  protected lazy val tinyDvrConfiguration = new TinyDvrConfiguration(databaseConfiguration)
}