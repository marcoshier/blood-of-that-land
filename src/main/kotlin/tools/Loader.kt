package tools

import org.openrndr.draw.ColorBufferProxy
import org.openrndr.internal.colorBufferLoader
import java.io.File


class Loader {

    val imagesPath = "/home/marco/PycharmProjects/resnet-50-test/dataset/"

    val proxies = mutableListOf<ColorBufferProxy>()
    var labels = File("data/labels-only.csv").readLines().map {
        val s = it.split(",")
        val name = s[0]
        val label = s[1].replace("\"", "")

        proxies.add(colorBufferLoader.loadFromUrl("file://${imagesPath + name.dropLast(7) + "/" + name}"))
        label
    }.toMutableList()

    val allText = loadText()
    fun loadText(): String {
        val textPath = "data/text_test.txt"
        return File(textPath).readText()
    }

    val allImages = loadImages()
    fun loadImages(): List<File> {
        val images = File(imagesPath).walk().filter { it.isFile }
        return images.toList()
    }

    val allVideos = loadVideos()
    fun loadVideos(): List<File> {
        val videoPath = "/home/marco/Desktop/Archive_segmented/Videos/"
        val videos = File(videoPath).walk().filter { it.isFile }
        return videos.toList()
    }

    val labelsCopy = labels
    fun next(): String {
        val copy = labelsCopy
        return if(labels.isNotEmpty()) {
            val r = copy.random()
            labelsCopy.remove(r)
            r
        } else {
            labels = labelsCopy
            println("finished!")
            ""
        }
    }
}
