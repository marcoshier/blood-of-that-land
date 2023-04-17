package scenes

import blob_tracker.Droplet
import blob_tracker.contours
import blob_tracker.loadVideoSource
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.color.LumaMap
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

fun Program.scene01() {

    var dry = colorBuffer(width, height)
    var droplets = mutableMapOf<Int, Droplet>()

    var update01: (image: ColorBuffer, contours: MutableMap<Int, Droplet>)->Unit by this.userProperties
    update01 = { img, dpls ->
        dry = img
        droplets = dpls
    }

    val left = Rectangle(0.0, 0.0, width / 2.0, height * 1.0)
    val right = Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)


    val anim = ZoomedAnimation(drawer.bounds)

    val dt = LumaMap().apply {
        foreground = ColorRGBa.RED
        background = ColorRGBa.BLACK
    }
    val cb = colorBuffer(width, height)


    extend {
        drawer.clear(ColorRGBa.BLACK)
        //anim.updateAnimation()
        val contours = droplets.contours

        anim.contours = contours

        dt.apply(dry, cb)
        drawer.imageFit(cb, left)

        drawer.translate(width / 2.0, 0.0)
        drawer.isolated {
            val scale = 3.0
            val w = right.width / scale
            val h = right.height / scale
            val xy = Vector2(
                anim.currentCenter!!.x.coerceIn(0.0, width - w),
                anim.currentCenter!!.y.coerceIn(0.0, height - h)
            )


            drawer.drawStyle.clip = right
            drawer.translate(xy)
            drawer.scale(scale)
            drawer.translate(-width / 2.0, -height / 2.0)
            drawer.fill = null
            drawer.stroke = ColorRGBa.BLUE
            drawer.strokeWeight = 1.0
            droplets.filter { it.value.bounds.center in right.offsetEdges(100.0) }.forEach {
                it.value.draw(drawer, 0)
            }

        }


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
            if(fromVideo) loadVideoSource(drawer.bounds, dry = false, fromWebcam)
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