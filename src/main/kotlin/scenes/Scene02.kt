package scenes

import blob_tracker.Droplet
import blob_tracker.contours
import blob_tracker.loadVideoSource
import boofcv.alg.structure.SceneWorkingGraph.Camera
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.writer
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.launch
import org.openrndr.math.IntVector2
import org.openrndr.math.Polar
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import textgen.address
import tools.computeContours
import java.awt.GraphicsEnvironment
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.random.Random

fun Program.scene02() {

    var droplets = mutableMapOf<Int, Droplet>()

    val left = Rectangle(0.0, 0.0, width / 2.0, height * 1.0)
    val right = Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)

    var updateSecond: (contours: MutableMap<Int, Droplet>)->Unit by this.userProperties
    updateSecond = { dpls ->
        droplets = dpls
    }

    val anim = ZoomedAnimation(drawer.bounds)

    val rightView = viewBox(right) {

        val c = Camera2D()
        extend(c)
        extend {
            drawer.fill = ColorRGBa.BLACK
            drawer.stroke = ColorRGBa.GREEN
            for (d in droplets.values) {
                drawer.contour(d.contour)

                val p0 = d.bounds.center
                val p1 = Polar(
                    Double.uniform(0.0, 360.0, Random(0)),
                    d.bounds.width * 1.5).cartesian + p0

                drawer.lineSegment(p0, p1)
                val r = Rectangle.fromCenter(p1, 280.0, 80.0)
                drawer.stroke = null
                drawer.fill = ColorRGBa.GREEN
                drawer.rectangle(r)

                drawer.fill = ColorRGBa.BLACK
                drawer.writer {
                    box = r
                    newLine()
                    text(d.label)
                }
            }
        }
    }

    extend {
        //anim.updateAnimation()

        anim.rects = droplets.values.filter { it.isTracked  }.map { it.bounds }

        drawer.isolated {
            drawer.translate((-width / 4.0), 0.0)
            drawer.drawStyle.clip = left
            drawer.fill = null
            drawer.stroke = ColorRGBa.BLUE
            drawer.strokeWeight = 1.0

            val d = droplets.filter { it.value.bounds.center in left.offsetEdges(100.0) }


            d.values.forEach {
                it.draw(drawer, 1)
            }
        }

        rightView.draw()
        //drawer.translate(width / 2.0, 0.0)

    }

}

val secondScreenConnected by lazy { GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.size == 2 }
fun main() = application {
    configure {
        width = 1280
        height = 720
        if(secondScreenConnected) {
            position = IntVector2(1920, 0)
        }
    }
    program {

        var contours = listOf<ShapeContour>()

        val fromVideo = true
        val dry = viewBox(drawer.bounds).apply {
            if(fromVideo) loadVideoSource(drawer.bounds, fromWebcam)
        }

        val scene02 = viewBox(drawer.bounds).apply { scene02() }
        val update02: (contours: List<ShapeContour>) -> Unit by scene02.userProperties

        extend {
            dry.update()
            contours = computeContours(dry.result).filter { it.bounds.width in range && it.bounds.height in range }

            update02(contours)
            scene02.draw()
        }
    }
}