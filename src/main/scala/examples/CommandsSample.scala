package examples

import org.scalatra._
import json.JacksonJsonSupport
import org.scalatra.commands._
import TodoStore.Todo
import org.json4s._
import org.scalatra.scalate.ScalateSupport
import scalaz._
import Scalaz._
import org.scalatra.validation.{ErrorCode, ValidationError}
import scala.util.control.Exception._

object TodosHandler extends CommandHandler {
  import CommandsSample._

  protected def handle: Handler = {
    case c: CreateTodoForm =>
      allCatch opt {
        TodoStore.save(Todo(~c.name.value, done = ~c.done.value)).successNel
      } getOrElse ValidationError("Unknown error").failNel

    case c: UpdateTodoForm =>
      allCatch opt {
        TodoStore.get(~c.id.value) map { todo =>
          TodoStore.save(todo.copy(name = ~c.name.value, done = c.done.value.getOrElse(todo.done))).successNel
        } getOrElse ValidationError("Not found", NotFound).failNel
      } getOrElse ValidationError("Unknown error", ServerError).failNel

    case c: ToggleDoneForm =>
      allCatch opt {
        TodoStore.get(~c.id.value) map { todo =>
          TodoStore.save(todo.copy(done = c.done.value.getOrElse(todo.done))).successNel
        } getOrElse ValidationError("Not found", NotFound).failNel
      } getOrElse ValidationError("Unknown error", ServerError).failNel
  }


}

object CommandsSample {

  case object ServerError extends ErrorCode

  abstract class TodoForm extends ModelCommand[Todo] with JsonCommand {
    protected implicit def jsonFormats: Formats = DefaultFormats
  }

  trait IdField { this: TodoForm =>
    val id: Field[Int] = bind[Int]("id").sourcedFrom(ValueSource.Path).required
  }

  trait DoneField { this: TodoForm =>
    val done: Field[Boolean] = bind[Boolean]("done").withDefaultValue(false)
  }

  trait NameField { this: TodoForm =>
    val name: Field[String] = bind[String]("name").notBlank.minLength(3)
  }

  trait TodoFields extends NameField with DoneField { self: TodoForm =>  }



  /**
   * A command that creates a todo item
   * Equivalent to
   * <pre>
   *   class CreateTodoForm {
   *     val name: Field[String] = bind[String]("name").notBlank.minLength(3)
   *     val done: Field[Boolean] = bind[Boolean]("done").withDefaultValue(false)
   *   }
   * </pre>
   *
   * @param jsonFormats the json formats
   */
  class CreateTodoForm(implicit jsonFormats: Formats) extends TodoForm with TodoFields

  /**
   * A command that updates a todo item
   * Equivalent to
   * <pre>
   *   class UpdateTodoForm {
   *     val id: Field[Int] = bind[Int]("id").sourcedFrom(ValueSource.Path).required
   *     val name: Field[String] = bind[String]("name").notBlank.minLength(3)
   *     val done: Field[Boolean] = bind[Boolean]("done").withDefaultValue(false)
   *   }
   * </pre>
   *
   * @param jsonFormats the json formats
   */
  class UpdateTodoForm(implicit jsonFormats: Formats) extends TodoForm with IdField with TodoFields

  /**
   * A command that toggles the done field a todo item
   * Equivalent to
   * <pre>
   *   class ToggleDoneForm {
   *     val id: Field[Int] = bind[Int]("id").sourcedFrom(ValueSource.Path).required
   *     val done: Field[Boolean] = bind[Boolean]("done").withDefaultValue(false)
   *   }
   * </pre>
   *
   * @param jsonFormats the json formats
   */
  class ToggleDoneForm(implicit jsonFormats: Formats) extends TodoForm with IdField with DoneField

}
class CommandsSample extends ScalatraServlet with TypedParamSupport with ScalateSupport with JacksonJsonParsing with JacksonJsonSupport {

  import CommandsSample._
  protected implicit val jsonFormats: Formats = DefaultFormats

  get("/") {
    contentType="text/html"
    ssp("/todos/index", "todos" -> TodoStore.all, "remaining" -> TodoStore.remaining)
  }

  post("/todos") {
    val cmd = command[CreateTodoForm]
    TodosHandler.execute(cmd).fold(
      errors => halt(400, errors),
      todo => redirect("/")
    )
  }

  get("/todos/:id") {
    params.getAs[Int]("id") map { id =>
      TodoStore.get(id) getOrElse halt(NotFound())
    } getOrElse halt(BadRequest())

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