package scenes

import blob_tracker.loadDropletsVideo
import blob_tracker.loadDropletsWebcam
import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.ShapeContour
import tools.computeContours

val range = 10.0..100.0

fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {

        val debug = true
        val fromVideo = true
        val dry = viewBox(drawer.bounds).apply { if(fromVideo) loadDropletsVideo(drawer.bounds, dry = false) else loadDropletsWebcam(drawer.bounds) }

        var contours = listOf<ShapeContour>()

        val scene01 = viewBox(drawer.bounds).apply { scene01() }
        val update01: (image: ColorBuffer) -> Unit by scene01.userProperties

        val scene02 = viewBox(drawer.bounds).apply { scene02() }
        val update02: (contours: List<ShapeContour>) -> Unit by scene02.userProperties

        var current = 0

        class Animator: Animatable() {
            var sceneTimer = 0.0

            fun scene() {
                ::sceneTimer.animate(1.0, 10000).completed.listen {
                    current = if(current == 2) 0 else current + 1
                    sceneTimer = 0.0
                    scene()
                }
            }

            init {
                //scene()
            }
        }
        val anim = Animator()

        extend {
            anim.updateAnimation()
            dry.update()
            contours = computeContours(dry.result).filter { it.bounds.width in range && it.bounds.height in range }

            when(current) {
                0 -> {
                    update01(dry.result)
                    scene01.draw()
                }
                1 -> {
                    update02(contours)
                    scene02.draw()
                }
            }


            if(debug) {
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.rectangle(0.0, 0.0, width * anim.sceneTimer, 10.0)
                drawer.text(current.toString(), 5.0, 20.0)
            }
        }
    }
}