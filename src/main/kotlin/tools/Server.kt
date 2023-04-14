package tools

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openrndr.events.Event
import org.openrndr.extra.gui.addTo
import textgen.address
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

val wordList by lazy { csvReader().readAll(File("offline-data/labels-only.csv")).map { it[1].split(",")[0] } }

class Server() {

    val respReceived = Event<String>()

    val client = HttpClient.newHttpClient()
    var textSoFar = ""
        set(value) {
            //respReceived.trigger(value)
            field += value
        }

    fun getResponseDebug(text: String) {
        //val vocab = "abcdefghijklmnopqrstuvwyxz            "
        //val resp = (0..80).map { vocab.random() }

        textSoFar += text
    }

    val text = File("data/text_test.txt").readText()
    var currentRange = 0
    fun getNextSentenceDebug(): String {
        val newRange = currentRange + 50
        currentRange = newRange
        return text.substring(currentRange..newRange)
    }

    suspend fun getResponse(text: String) {
        val char = text.run {
            if(contains(".")) substringAfterLast(".")
            else substringAfterLast(",")
        } // CHECK

        val json = JsonObject()
        val array = JsonArray().also { it.add(char) }
        json.add("Sequences", array)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(address))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
            .build()

        val resp = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }.body()

        textSoFar = resp
    }
}
