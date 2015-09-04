package org.tinydvr.util

import org.slf4j.LoggerFactory
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Session
import org.tinydvr.config.DatabaseConnectionInfo

abstract class DatabaseConnection(db: DatabaseConnectionInfo) {

  private val logger = LoggerFactory.getLogger(getClass)

  protected def run[A](a: => A): A = {
    if (Session.hasCurrentSession) {
      a
    } else {
      val session = Session.create(db.getConnection, db.getAdapter)
      try {
        transaction(session)(a)
      } finally {
        try {
          session.cleanup
        } catch {
          case e: Exception => logger.error("Unable to cleanup Squeryl session", e)
        }
      }
    }
  }
}
