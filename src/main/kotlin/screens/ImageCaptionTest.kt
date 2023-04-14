package screens

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.launch
import textgen.address
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main() = application {
    configure { }
    program {

        val client = HttpClient.newHttpClient()

        class VitGPT2Server {

            var image = colorBuffer(width, height)
            var currentCaption = ""
            var currentPath = ""
                set(value) {
                    field = value
                    launch {
                        currentCaption = getResponse(value)
                        yield()
                    }
                    image = loadImage(value)
                }

            suspend fun getResponse(imagePath: String): String {

                val json = JsonObject()
                val array = JsonArray().also { it.add(imagePath) }
                json.add("Sequences", array)

                val request = HttpRequest.newBuilder()
                    .uri(URI.create(address))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build()

                return withContext(Dispatchers.IO) {
                    client.send(request, HttpResponse.BodyHandlers.ofString())
                }.body()
            }

        }

        val server = VitGPT2Server()

        server.currentPath = "http://0.0.0.0:8000/IdeaProjects/blood-of-the-land/data/images/" + "cheeta.jpg"

        extend {

            drawer.imageFit(server.image, drawer.bounds)
            drawer.translate(30.0, height - 80.0)
            drawer.text(server.currentCaption)

        }
    }
}
