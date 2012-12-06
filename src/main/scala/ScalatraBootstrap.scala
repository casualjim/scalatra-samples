import examples._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context mount (new CommandsSample, "/todos")
    context mount (new AtmosphereChat, "/chat")
    context mount (new JsonSample, "/json")
    context mount (new HelloScalatra, "/")
  }
}
