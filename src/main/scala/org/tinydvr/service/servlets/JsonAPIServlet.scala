package org.tinydvr.service.servlets

import org.scalatra._
import org.slf4j.LoggerFactory
import net.liftweb.json._
import net.liftweb.json.Extraction.decompose

/**
 * Adds some useful functionality on top of the ScalatraServlet get and post functions
 */
trait JsonAPIServlet extends ScalatraServlet {
  val CONTENT_TYPE_JSON = "application/json"
  private val logger = LoggerFactory.getLogger(getClass)
  private implicit val formats = net.liftweb.json.DefaultFormats

  override def get(transformers: RouteTransformer*)(body: => Any): Route = {
    super.get(transformers: _*)(withJson { body })
  }

  override def post(transformers: RouteTransformer*)(body: => Any): Route = {
    super.get(transformers: _*)(withJson { body })
  }

  def okResponse(result: AnyRef, msg: Option[String] = Some("OK")): Any = {
    jsonResponse(200, msg, Some(result))
  }

  def halt400(msg: Option[String] = Some("Bad Request")): Nothing = {
    halt(400, body = jsonResponse(400, msg, None))
  }

  def halt401(msg: Option[String] = Some("Unauthorized")): Nothing = {
    halt(401, body = jsonResponse(401, msg, None))
  }

  def halt500(msg: Option[String] = Some("Internal Server Error")): Nothing = {
    halt(500, body = jsonResponse(500, msg, None))
  }


  protected def jsonResponse(status: Int, msg: Option[String], result: Option[AnyRef] = None): Any = {
    contentType = CONTENT_TYPE_JSON
    compact(render(decompose(wrapResponse(status, msg, result))))
  }

  protected def wrapResponse(status: Int, msg: Option[String], result: Option[AnyRef] = None): JsonAPIResponse = {
    JsonAPIResponse(
      status,
      msg,
      result
    )
  }

  protected def withJson[T <: Any](body: => T): Any = {
    try {
      val res = body
      okResponse(res.asInstanceOf[AnyRef])
    } catch {
      case e: Exception => {
        logger.error("Uncaught Exception in JsonServlet", e)
        halt500()
      }
    }
  }

}

//
// Types
//

case class JsonAPIResponse(status: Int, message: Option[String], data: Option[Any])
