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

        val imagePaths = mutableListOf("brindisi_65001.png", "brindisi_65027.png")

        suspend fun getResponse(paths: MutableList<String>): String {

            val json = JsonObject()
            val array = JsonArray().also { for(path in paths) { it.add(path) } }
            json.add("Sequences", array)


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
                    val new = getResponse(imagePaths)
                    println(new)
                    yield()
                }
            }
        }

        extend {

            drawer.fill = ColorRGBa.WHITE
            writer {
                box = drawer.bounds.offsetEdges(-30.0)
                newLine()
                text(imagePaths.joinToString())
            }

        }
    }
}