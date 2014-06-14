import javax.servlet.ServletContext
import org.scalatra._
import org.tinydvr.config.Configured
import org.tinydvr.service.servlets.HelloWorldServlet

/**
 * http://www.scalatra.org/2.2/guides/deployment/configuration.html
 */
class Scalatra extends LifeCycle with Configured {

  override def init(context: ServletContext) {

    // initialization

    // mount the servlets
    context.mount(new HelloWorldServlet(config), "/hello/*")
  }
}
