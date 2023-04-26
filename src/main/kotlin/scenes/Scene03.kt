package scenes

import blob_tracker.Droplet
import blob_tracker.contours
import blob_tracker.loadVideoSource
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.IntVector2
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import tools.computeContours
import kotlin.random.Random

fun Program.scene03() {

    var droplets = mutableMapOf<Int, Droplet>()
    var contours = listOf<ShapeContour>()

    val zoomedAnim = ZoomedAnimation(drawer.bounds)
    val connectAnim = ConnectAnimation(Rectangle(width / 4.0, 0.0, height * 1.0, height * 1.0))
    val sceneAnimations = object: Animatable() {
        var titleTimer = 0.0
        var textTimer = 0.0
        var backdrop = 0.0
        var backdropTimer = 0.0
        var textAnimation = 0.0

        fun animateText() {
            ::textAnimation.animate(1.0, 3000).completed.listen {
                textAnimation = 0.0
                animateText()
            }
        }

        fun flash(delay: Long) {
            ::backdrop.cancel()
            ::backdropTimer.animate(1.0, 0, predelayInMs = delay).completed.listen {
                backdrop = 1.0
                ::backdrop.animate(0.0, 2000, Easing.CubicOut).completed.listen {
                    flash(Random.nextLong(3000, 5000))
                }
            }
        }
    }
    sceneAnimations.animateText()

    var updateThird: (droplets: MutableMap<Int, Droplet>)->Unit by this.userProperties
    updateThird = { dpls ->
        droplets = dpls
        contours = droplets.map { it.value.contour }
        connectAnim.apply {
            val keys = droplets.map { it.key }
            oldOrder = keys
            currentOrder = keys
            currentDroplets = droplets
            switch(2000L)
        }
    }

    val left = Rectangle(0.0, 0.0, width / 2.0, height * 1.0)
    val leftView = compose {
        layer {
            draw {
                drawer.isolated {
                    drawer.translate(-width / 4.0, 0.0)
                    drawer.drawStyle.clip = left
                    drawer.stroke = ColorRGBa.WHITE

                    drawer.contour(connectAnim.connector.sub(0.0, connectAnim.connectorTimer))

                    drawer.fill = ColorRGBa.BLACK
                    contours.forEachIndexed { i, it ->
                        drawer.pushTransforms()
                        drawer.contour(it)
                        drawer.popTransforms()
                    }
                }

            }
        }
        layer {
            layer {
                val fm = loadFont("data/fonts/ia_writer.ttf", 24.0, contentScale = 2.0)
                val fmItalic = loadFont("data/fonts/ia_writer_italic.ttf", 18.0, contentScale = 4.0)
                val bottomRect = Rectangle(30.0, height - 80.0, left.width - 60.0, 60.0)

                sceneAnimations.apply {
                    flash(2000)
                    ::titleTimer.animate(1.0, 3000, Easing.CubicInOut)
                    ::textTimer.animate(1.0, 8000)
                }

                draw {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.RED.shade(0.4)
                    drawer.strokeWeight = 300.0
                    drawer.circle(left.center, 550.0)


                    drawer.fill = ColorRGBa.WHITE
                    drawer.stroke = null
                    drawer.rectangle(30.0, 30.0, 330.0 * sceneAnimations.titleTimer, 28.0)

                    drawer.fontMap = fm
                    drawer.fill = ColorRGBa.BLACK
                    val title = "3/3 • THE ALL-READING ORACLE"
                    drawer.text(title.take((title.length * sceneAnimations.titleTimer).toInt()), 40.0, 50.0)

                    drawer.fill = ColorRGBa.WHITE
                    drawer.fontMap = fmItalic

                    val text = "The oracle reads geographies and attempts to connect the traces of meaning into new narratives, for its final reading."
                    drawer.writer {
                        box = bottomRect
                        newLine()
                        text(text.take((text.length * sceneAnimations.textTimer).toInt()))
                    }

                    drawer.isolated {
                        drawer.fill = ColorRGBa.WHITE.opacify(sceneAnimations.backdrop)
                        drawer.stroke = null
                        val r = Rectangle(left.width - 60.0, 30.0, 30.0, 30.0)
                        drawer.rectangle(r)
                    }

                }
            }
        }
    }

    val right = Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)
    val rightView = viewBox(right) {

        val grid = Rectangle(0.0, 0.0, width * 1.0, height * 2.0).offsetEdges(-20.0).grid(4, 12).flatten()
        val poemRect = Rectangle(50.0, height - 50.0 - 300.0, width - 100.0, 300.0)
        extend {
            droplets.forEach { it.value.update() }

            drawer.isolated { drawer.rectangle(poemRect) }

            for((i, j) in connectAnim.oldOrder zip connectAnim.currentOrder) {
                val droplet = droplets[j]
                if(droplet?.label != "") {
                    val r = Rectangle.fromCenter(grid[i].center.mix(grid[j].center, connectAnim.orderFader), grid[0].width, grid[0].height)
                    val image = droplet!!.cb
                    val imageRect = r.sub(0.0, 0.0, 1.0, 0.7)
                    val textRect = r.sub(0.0, 0.8, 1.0, 1.0)
                    drawer.imageFit(image, imageRect )
                    drawer.fill = ColorRGBa.WHITE

                    val time = sceneAnimations.textAnimation * 10.0
                    val text = droplet.label.take((time).toInt() + 20).drop((time).toInt())

                    drawer.text(text, textRect.x + 3.0, textRect.y + 3.0)

                    drawer.fill = ColorRGBa.BLACK
                    drawer.text(droplet.label, poemRect.x + 10.0, poemRect.y + (16.0 * j) + 20.0)
                }

            }

        }

    }

    extend {
        sceneAnimations.updateAnimation()
        zoomedAnim.updateAnimation()
        connectAnim.updateAnimation()

        if(connectAnim.currentPositions.isNotEmpty()) {
            leftView.draw(drawer)
        }
        rightView.draw()


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