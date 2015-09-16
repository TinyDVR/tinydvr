package org.tinydvr.util

import akka.actor._

object Singletons {

  // note: ActorSystem is a heavyweight structure... so create one per logical application.
  lazy val actorSystem = ActorSystem("tinydvr")
}
