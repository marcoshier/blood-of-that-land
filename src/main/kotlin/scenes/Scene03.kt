package scenes

import blob_tracker.Droplet
import blob_tracker.contours
import blob_tracker.loadVideoSource
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.IntVector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import tools.computeContours

fun Program.scene03() {

    var droplets = mutableMapOf<Int, Droplet>()

    val left = Rectangle(0.0, 0.0, width / 2.0, height * 1.0)
    val right = Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)

    var update02: (droplets: MutableMap<Int, Droplet>)->Unit by this.userProperties
    update02 = { dpls ->
        droplets = dpls
    }

    val anim = ZoomedAnimation(drawer.bounds)


    extend {
        anim.updateAnimation()
        val contours = droplets.contours

        anim.rects = droplets.values.filter { it.isTracked  }.map { it.bounds }

        drawer.isolated {
            drawer.drawStyle.clip = left
            drawer.stroke = ColorRGBa.RED

            drawer.contour(ShapeContour.fromPoints(contours.map { it.bounds.center }, false))

            drawer.fill = ColorRGBa.BLACK
            drawer.contours(contours)
        }

        drawer.translate(width / 2.0, 0.0)
        /*drawer.isolated {
            drawer.drawStyle.clip = right
            val scale = 3.0
            val w = right.width / scale
            val h = right.height / scale
            drawer.translate(
                anim.currentCenter!!.x.coerceIn(0.0, width - w),
                anim.currentCenter!!.y.coerceIn(0.0, height - h)
            )
            drawer.scale(scale)
            drawer.translate(-width / 2.0, -height / 2.0)
            drawer.fill = ColorRGBa.RED
            drawer.contours(contours.map { it.close() })
        }*/

    }

}
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

        val scene02 = viewBox(drawer.bounds).apply { scene03() }
        val update02: (contours: List<ShapeContour>) -> Unit by scene02.userProperties

        extend {
            dry.update()
            contours = computeContours(dry.result).filter { it.bounds.width in range && it.bounds.height in range }

            update02(contours)
            scene02.draw()
        }
    }
}