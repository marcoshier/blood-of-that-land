package tools

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBufferProxy
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.internal.colorBufferLoader
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File

fun main() = application {
    configure {
        width = 1440
        height = 800
    }
    program {

        val labelsScores = File("data/labels-scores.csv").readLines()
        val labels = labelsScores[0].split("/").drop(1).map { it.split(",")[0] }
        val imageToScores = labelsScores.drop(1).map {
            val l = it.split(",")
            val image = l[0]
            val scores = l.drop(1).map { it.toDouble() }
            image to scores
        }

        val positions = csvReader().readAllWithHeader(File("data/umap-2d.csv")).map {
            Vector2(it["0"]!!.toDouble(), it["1"]!!.toDouble())
        }
        val mapped = positions.map(positions.bounds, Rectangle(400.0, 0.0, 800.0, 800.0))
        val kd = mapped.kdTree()

        var last = 0

        val path = "file:///home/marco/PycharmProjects/resnet-50-test/dataset/"
        var proxy = colorBufferLoader.loadFromUrl(path + name.dropLast(4) + "/" + name)

        var scoresToLabels = listOf<Pair<String, Double>>()

        extend {
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = null

            drawer.circles(mapped, 2.0)

            kd.findNearest(mouse.position)?.let {

                drawer.fill = ColorRGBa.BLUE
                drawer.stroke = ColorRGBa.BLUE
                drawer.strokeWeight = 0.3
                drawer.circle(it, 2.0)

                drawer.lineSegment(it, mouse.position)

                val i = mapped.indexOf(it)
                if(i != last && i in imageToScores.indices) {
                    val name = imageToScores[i].first
                    proxy = colorBufferLoader.loadFromUrl(path + name.dropLast(7) + "/" + name)

                    val scores = imageToScores[i].second
                    scoresToLabels = (labels zip scores).sortedByDescending { (_, s) -> s }.take(20)

                    last = i
                }
            }

            if(proxy.state == ColorBufferProxy.State.LOADED) {
                drawer.imageFit(proxy.colorBuffer!!, Rectangle(0.0, 0.0, 400.0, height * 1.0), fitMethod = FitMethod.Contain)
            }

            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            scoresToLabels.forEachIndexed { index, (label, score) ->
                val y = index * 25.0 + 180.0
                drawer.text(label, 1250.0, y - 10.0)
                drawer.rectangle(1250.0, y, 400.0 * score,  2.0)
            }


        }
    }
}