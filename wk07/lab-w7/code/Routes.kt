import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

data class Task(val id: Int, val title: String)

// small helper to detect htmx requests
fun ApplicationCall.isHtmx() = request.headers["HX-Request"] == "true"

// renderer placeholder
fun render(tpl: String, model: Map<String, Any?>): String =
    "<!-- render $tpl with $model -->"

fun Route.inlineEditRoutes(repo: TaskRepo) {

    // show edit form
    get("/tasks/{id}/edit") {
        val id = call.parameters["id"]!!.toInt()
        val t = repo.get(id)

        // use key "task" so it matches {{ task.* }} in Pebble
        val html = render("tasks/edit.peb", mapOf("task" to t, "error" to null))

        if (call.isHtmx()) {
            call.respondText(html, ContentType.Text.Html)
        } else {
            call.respondText("Full page not implemented")
        }
    }

    // handle save
    post("/tasks/{id}/edit") {
        val id = call.parameters["id"]!!.toInt()
        val params = call.receiveParameters()
        val title = (params["title"] ?: "").trim()
        val t = repo.get(id)

        if (title.isBlank()) {
            val html = render("tasks/edit.peb", mapOf("task" to t, "error" to "Title is required"))
            return@post call.respondText(html, ContentType.Text.Html)
        }

        val updated = repo.update(id, title)

        // re-render the normal view of this task
        val viewHtml = render("tasks/view.peb", mapOf("task" to updated))

        // status message, announced to screen readers
        val status = """
        <div id="status"
             hx-swap-oob="true"
             role="status"
             aria-live="polite">
          Saved “$title”.
        </div>
        """.trimIndent()

        // send both the updated task and the status message
        call.respondText(viewHtml + status, ContentType.Text.Html)
    }
}

interface TaskRepo {
    fun get(id: Int): Task
    fun update(id: Int, title: String): Task
}


