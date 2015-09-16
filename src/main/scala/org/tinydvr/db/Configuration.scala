package org.tinydvr.db

import org.squeryl.KeyedEntity
import org.squeryl.annotations._

class Configuration extends KeyedEntity[String] {
  def id = key

  @Column(name = "key") // the configuration key
  var key: String = _

  @Column(name = "value") // the configuration value
  var value: String = _

}
