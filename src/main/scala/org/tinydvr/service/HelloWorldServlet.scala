package org.tinydvr.service

import org.tinydvr.config.StaticConfiguration

class HelloWorldServlet(config: StaticConfiguration) extends JsonAPIServlet {

  get("/") {
    HelloWorldResponse("Yay!")
  }
}

case class HelloWorldResponse(msg: String)
