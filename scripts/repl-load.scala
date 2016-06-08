println("Creating common access object \"tinydvr\"...")
import org.joda.time._
import org.tinydvr.config._
import org.tinydvr.db._
import org.tinydvr.jobs._
import org.tinydvr.schedulesdirect.api._
import org.tinydvr.schedulesdirect.api.Implicits._
import org.tinydvr.service.TinyDVRAPI
import org.tinydvr.util._


val tinydvr = new Object with
  Configured with
  ConfigurationFromProperties with
  SchedulesDirectAPI with
  TinyDVRAPI with
  TinyDVRDB with
  VariableReplacer {
  def config = tinyDvrConfiguration
}