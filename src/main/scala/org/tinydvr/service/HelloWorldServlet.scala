package org.tinydvr.service

class HelloWorldServlet() extends JsonAPIServlet {

  get("/") {
    HelloWorldResponse("Yay!")
  }
}

case class HelloWorldResponse(msg: String)
