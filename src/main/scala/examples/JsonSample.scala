package examples

import org.scalatra._
import org.scalatra.json._
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._
import collection.mutable
import org.json4s._

object TodoStore {
  
  private[this] val idProvider = new AtomicInteger(0)


  case class Todo(name: String, id: Int = idProvider.incrementAndGet, done: Boolean = false)
  

  private[this] val store: mutable.ConcurrentMap[Int, Todo] = new ConcurrentHashMap[Int, Todo].asScala
  
  def save(todo: Todo): this.type = {
    store += todo.id -> todo
    this
  }
 
  def get(id: Int): Option[Todo] = store get id
 
  def findByName(name: String): Option[Todo] = store find {
    case (_, candidate) => candidate.name equalsIgnoreCase name
  } map (_._2)
 
  def delete(id: Int): this.type = {
    store -= id
    this
  }

  def all: List[Todo] = store.values.toList

  def remaining: List[Todo] = store.values.filterNot(_.done).toList

  def apply(id: Int) = store(id)

  save(Todo("Show up for meetup", done = true))
  save(Todo("Show hello world", done = true))
  save(Todo("Show splats", done = true))
  save(Todo("Show JSON support"))
  save(Todo("Show Commands"))
  save(Todo("Show atmosphere support"))
  save(Todo("Negotiate lasting world peace", done = false))
  save(Todo("End world hunger once and for all", done = false))
  save(Todo("Negotiate lasting world peace", done = false))
  save(Todo("Prepare for zombie attack", done = false))

}

class JsonSample extends ScalatraServlet with TypedParamSupport with JacksonJsonSupport with JValueResult {
  import TodoStore.Todo

  protected implicit val jsonFormats = DefaultFormats

  override def defaultFormat = 'json

  get("/") {
    TodoStore.all
  }

  get("/:id") {
    params.getAs[Int]("id") flatMap TodoStore.get getOrElse halt(NotFound())
  }

  post("/") {
    TodoStore.save(parsedBody.extract[Todo])
  }

  put("/:id") {
    params.getAs[Int]("id") flatMap TodoStore.get map { todo => 
      val toSave = todo.copy(name = (parsedBody \ "name").extractOrElse(""), done = (parsedBody \ "done").extractOrElse(todo.done))
      TodoStore save toSave
      toSave
    } getOrElse halt(NotFound())
  }

  delete("/:id") {
    params.getAs[Int]("id") map { id =>
      TodoStore delete id
      NoContent()
    } getOrElse halt(BadRequest("This needs and id!"))
  }


  notFound {
    // remove content type in case it was set through an action
    contentType = null
    serveStaticResource() getOrElse resourceNotFound()
  }
}
