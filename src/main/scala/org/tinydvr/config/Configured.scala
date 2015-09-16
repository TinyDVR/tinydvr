package org.tinydvr.config

trait Configured {
  protected val staticConfig: StaticConfiguration
  protected lazy val dynamicConfig = new DynamicConfiguration(staticConfig)
}

trait LiveConfiguration extends Configured {
  override protected val staticConfig: StaticConfiguration = StaticConfigurationSingleton.fromProperties
}