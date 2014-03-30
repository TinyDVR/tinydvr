package org.tinydvr.config

trait Configured {

  protected lazy val config = ConfigurationLoader.load()

}
