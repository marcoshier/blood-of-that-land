package scenes

import blob_tracker.Droplet
import blob_tracker.contours
import blob_tracker.loadVideoSource
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.fx.color.LumaMap
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import kotlin.math.sin

fun Program.scene01() {

    var dry = colorBuffer(width, height)
    var droplets = mutableMapOf<Int, Droplet>()

    var updateFirst: (image: ColorBuffer, contours: MutableMap<Int, Droplet>)->Unit by this.userProperties
    updateFirst = { img, dpls ->
        dry = img
        droplets = dpls
    }

    val left = Rectangle(0.0, 0.0, width / 2.0, height * 1.0)
    val dt = LumaMap().apply {
        foreground = ColorRGBa.RED
        background = ColorRGBa.BLACK
    }
    val cb = colorBuffer(width, height)


    val anim = ZoomedAnimation(drawer.bounds)

    val right = Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)
    val rightView = viewBox(right) {

        val c = Camera2D()

        extend(c)
        extend {
            //anim.currentCenter!! +
            c.view = transform {
                translate(drawer.bounds.center)
                scale(2.0)
                translate(-anim.currentCenter!!)
            }

            drawer.fill = null
            drawer.stroke = ColorRGBa.BLUE
            drawer.strokeWeight = 1.0
            droplets.forEach {
                it.value.draw(drawer, 0)
            }

            drawer.circle(320.0, height / 2.0, 50.0)
        }
    }


    extend {
        anim.rects = droplets.values.filter { it.imageLoaded }.map { it.bounds }

        drawer.clear(ColorRGBa.BLACK)
        anim.updateAnimation()

        dt.apply(dry, cb)
        drawer.imageFit(cb, left)

        rightView.draw()
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
            if(fromVideo) loadVideoSource(drawer.bounds, fromWebcam)
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