package textgen

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

val address = "http://127.0.0.1:5000/infer"

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        val sequences = listOf(
            "I'm neutral about it",
            "i hate it",
            "i love it"
        )
        val json = JsonObject()
        val array = JsonArray().also { ja -> sequences.map { ja.add(it) } }
        json.add("Sequences", array)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(address))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
            .build()

        val client = HttpClient.newHttpClient()

        val response = withContext(Dispatchers.IO) {
            client.send(request, BodyHandlers.ofString())
        }

        println(response.statusCode())
        println(response.body())

        extend {
            drawer.clear(ColorRGBa.PINK)
        }
    }
}
