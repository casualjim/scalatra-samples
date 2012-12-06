package examples

import org.scalatra._
import scalate.ScalateSupport

class HelloScalatra extends ScalatraServlet with ScalateSupport {

  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Say <a href="hello-scalate">hello to Scalate</a>.
      </body>
    </html>
  }

  get("/hello/:name") {
    <html>
      <body>
        <h1>Hello, {params("name")}</h1>
      </body>
    </html> 
  }

  get("/join/*") {
    "Join " + multiParams("splat").mkString(" ")
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }
}
