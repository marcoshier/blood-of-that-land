package scenes

import blob_tracker.Droplet
import blob_tracker.Plate
import blob_tracker.loadVideoSource
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.Rectangle

val range = 10.0..250.0
val fromWebcam = false
val debug = true

fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {

        val dry = viewBox(drawer.bounds).apply { loadVideoSource(drawer.bounds, dry = true, fromWebcam)  }
        val video = viewBox(drawer.bounds).apply {loadVideoSource(drawer.bounds, dry = false, fromWebcam) }

        val plate = Plate(drawer.bounds)

        val scene01 = viewBox(drawer.bounds).apply { scene01() }
        val update01: (image: ColorBuffer, droplets: MutableMap<Int, Droplet>) -> Unit by scene01.userProperties

        val scene02 = viewBox(drawer.bounds).apply { scene02() }
        val update02: (droplets: MutableMap<Int, Droplet>) -> Unit by scene02.userProperties

        val scene03 = viewBox(drawer.bounds).apply { scene03() }
        val update03: (droplets: MutableMap<Int, Droplet>) -> Unit by scene03.userProperties


        val anim = Animator()

        extend {
            anim.updateAnimation()
            video.update()
            plate.update(video.result)

            when(anim.current) {
                0 -> {
                    update01(video.result, plate.droplets)
                    scene01.draw()
                }
                1 -> {
                    update02(plate.droplets)
                    scene02.draw()
                }
                2 -> {
                    update03(plate.droplets)
                    scene03.draw()
                }
            }


            if(debug) {
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.rectangle(0.0, 0.0, width * anim.sceneTimer, 10.0)
                drawer.text(anim.current.toString(), 5.0, 20.0)

                dry.update()
                val w = dry.result.bounds.width / 4.0
                val h = dry.result.bounds.height / 4.0
                drawer.image(dry.result, dry.result.bounds, Rectangle(0.0, height - h, w, h))
            }
        }
    }
}