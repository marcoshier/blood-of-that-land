package sketches

import org.openrndr.Program
import org.openrndr.application
import org.openrndr.draw.ColorBufferProxy
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.internal.colorBufferLoader
import tools.Director
import java.io.File

fun Program.filmWithImages() {

    val labelsScores = File("data/labels-scores.csv").readLines()
    val imageToScores = labelsScores.drop(1).map {
        val l = it.split(",")
        val image = l[0]
        val scores = l.drop(1).map { it.toDouble() }
        image to scores
    }

    var i = 0

    val path = "file:///home/marco/PycharmProjects/resnet-50-test/dataset/"
    var proxy = colorBufferLoader.loadFromUrl(path + imageToScores[i].first.dropLast(7) + "/" + imageToScores[i].first)

    var nextImage : ()->Unit by this.userProperties
    nextImage = {
        i++
        val name = imageToScores[i].first
        println(path + name.dropLast(4) + "/" + name)
        proxy = colorBufferLoader.loadFromUrl(path + name.dropLast(7) + "/" + name)
    }
    extend {
        if(proxy.state == ColorBufferProxy.State.LOADED) {
            drawer.imageFit(proxy.colorBuffer!!, drawer.bounds, fitMethod = FitMethod.Cover)
        }
    }
}

fun main() = application {
    configure {
        width = 1920
        height = 1080
    }
    program {
        val director = Director()

        val filmTest = viewBox(drawer.bounds).apply { filmWithImages() }
        val nextImage: () -> Unit by filmTest.userProperties

        director.nextVid.listen {
            nextImage()
            director.queueVideo(5000)
        }
        extend {
            director.updateAnimation()

            filmTest.draw()
        }
    }
}