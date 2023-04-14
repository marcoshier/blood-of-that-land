package textgen

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.launch
import org.openrndr.writer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main() = application {
    configure {  }
    program {

        var text = mutableListOf("Salento is a land of incredible wonders, but, in the current climate, it will drown in its own")

        suspend fun getResponse(): String {

            val char = text.last().run {
                if(contains(".")) substringAfterLast(".")
                else substringAfterLast(",")
            }

            val json = JsonObject()
            val array = JsonArray().also { it.add(char) }
            json.add("Sequences", array)
            println(json.toString())

            val request = HttpRequest.newBuilder()
                .uri(URI.create(address))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build()

            val client = HttpClient.newHttpClient()

            val resp = withContext(Dispatchers.IO) {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            }
            return resp.body()
        }

        keyboard.keyUp.listen {
            if(it.name == "f") {
                launch {
                    val new = getResponse()
                    text.add(new)
                    yield()
                }
            }
        }

        extend {

            drawer.fill = ColorRGBa.WHITE
            writer {
                box = drawer.bounds.offsetEdges(-30.0)
                newLine()
                text(text.joinToString())
            }

        }
    }
}