package org.tinydvr.config

trait Configured {

  // TODO: make sure this only loads once per jvm instance.
  protected lazy val config = ConfigurationLoader.load()

}
