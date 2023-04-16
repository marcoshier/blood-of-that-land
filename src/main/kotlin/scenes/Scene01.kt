package scenes

import blob_tracker.loadDropletsVideo
import blob_tracker.loadDropletsWebcam
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.color.LumaMap
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import tools.computeContours

fun Program.scene01() {

    var dry = colorBuffer(width, height)
    var update01: (image: ColorBuffer)->Unit by this.userProperties
    update01 = {
        dry = it
    }

    val left = Rectangle(0.0, 0.0, width / 2.0, height * 1.0)
    val right = Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)


    var contours = listOf<ShapeContour>()

    val anim = ZoomedAnimation(drawer.bounds, contours)

    val dt = LumaMap().apply {
        foreground = ColorRGBa.RED
        background = ColorRGBa.BLACK
    }
    val cb = colorBuffer(width, height)

    val dt2 = LumaMap().apply {
        foreground = ColorRGBa.BLACK
        background = ColorRGBa.RED.shade(0.4)
    }
    val cb2 = colorBuffer(width, height)

    val rt = renderTarget(right.width.toInt(), right.height.toInt()) {
        colorBuffer()
        depthBuffer()
    }

    extend {
        anim.updateAnimation()

        dt.apply(dry, cb)
        drawer.imageFit(cb, left)


        drawer.isolatedWithTarget(rt) {
            drawer.clear(ColorRGBa.BLACK)
            val w = right.width / 3.0
            val h = right.height / 3.0
            val src = Rectangle.fromCenter(
                Vector2(
                    anim.currentCenter!!.x.coerceIn(w / 2.0, width - w / 2.0),
                    anim.currentCenter!!.y.coerceIn(h / 2.0, height - h / 2.0),
                ), w, h)
            dt2.apply(dry, cb2)
            drawer.image(cb2, src, Rectangle(0.0, 0.0, width * 2.0, height * 1.0))
        }

        drawer.translate(width / 2.0, 0.0)
        drawer.image(rt.colorBuffer(0))

        contours = computeContours(rt.colorBuffer(0))
        for((index, contour) in contours.withIndex()) {
            drawer.stroke = ColorRGBa.GREEN
            drawer.contour(contour)
            drawer.fill = ColorRGBa.GREEN
            drawer.text(index.toString(), contour.bounds.center)
        }
        drawer.contours(contours)

    }

}

fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {

        val fromVideo = true
        val dry = viewBox(drawer.bounds).apply {
            if(fromVideo) loadDropletsVideo(drawer.bounds, dry = false) else loadDropletsWebcam(drawer.bounds)
        }

        val vb = viewBox(drawer.bounds).apply { scene01() }
        val update01: (image: ColorBuffer) -> Unit by vb.userProperties

        extend {
            dry.update()
            update01(dry.result)
            vb.draw()
        }
    }
}