println("Creating common access object \"tinydvr\"...")
import org.joda.time._
import org.tinydvr.config._
import org.tinydvr.db._
import org.tinydvr.jobs._
import org.tinydvr.util._

val tinydvr = new Object with LiveConfiguration with SchedulesDirectAPI with TinyDVRDB with VariableReplacer {
  def getConfiguration: StaticConfiguration = staticConfig
}