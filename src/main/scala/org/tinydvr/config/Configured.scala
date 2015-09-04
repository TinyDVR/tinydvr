package org.tinydvr.config

trait Configured {
  protected val staticConfig: StaticConfiguration
}

trait LiveConfiguration extends Configured {
  override protected val staticConfig: StaticConfiguration = StaticConfigurationSingleton.fromProperties
}