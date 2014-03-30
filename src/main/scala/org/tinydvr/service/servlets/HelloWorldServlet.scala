package org.tinydvr.service.servlets

import org.tinydvr.config.Configuration

class HelloWorldServlet(config: Configuration) extends JsonAPIServlet {

  get("/") {
    HelloWorldResponse("Yay!")
  }
}

case class HelloWorldResponse(msg: String)
