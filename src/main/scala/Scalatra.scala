import javax.servlet.ServletContext
import org.scalatra._
import org.tinydvr.config.LiveConfiguration
import org.tinydvr.service.HelloWorldServlet

/**
 * http://www.scalatra.org/2.2/guides/deployment/configuration.html
 */
class Scalatra extends LifeCycle with LiveConfiguration {

  override def init(context: ServletContext) {

    // initialization

    // mount the servlets
    context.mount(new HelloWorldServlet(staticConfig), "/hello/*")
  }
}
