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
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.colormap.GrayscaleColormap
import org.openrndr.extra.fx.dither.ADither
import org.openrndr.extra.fx.dither.LumaHalftone
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.launch
import org.openrndr.math.IntVector2
import org.openrndr.math.Polar
import org.openrndr.math.mod
import org.openrndr.math.transforms.transform
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


    var updateSecond: (contours: MutableMap<Int, Droplet>)->Unit by this.userProperties
    updateSecond = { dpls ->
        droplets = dpls
    }

    val anim = ZoomedAnimation(drawer.bounds)
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

    val left = Rectangle(0.0, 0.0, width / 2.0, height * 1.0)
    val leftView = compose {
        layer {
            draw {
                drawer.clear(ColorRGBa.BLACK)
                val d = droplets.values

                anim.rects = d.filter { it.imageLoaded }.map { it.bounds }

                drawer.translate((-width / 4.0), 0.0)

                drawer.drawStyle.clip = left
                drawer.fill = null
                drawer.stroke = ColorRGBa.BLUE
                drawer.strokeWeight = 1.0
                d.forEach {
                    it.draw(drawer, 1)
                }
                drawer.defaults()
            }
            post(ADither()) {

            }
            post(GrayscaleColormap())
        }
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
                val title = "2/3 • THE ALL-KNOWING ORACLE"
                drawer.text(title.take((title.length * sceneAnimations.titleTimer).toInt()), 40.0, 50.0)

                drawer.fill = ColorRGBa.WHITE
                drawer.fontMap = fmItalic

                val text = "The oracle examines and interprets the droplets, inferring meaning for each of them."
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

    val right = Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)
    val rightView = viewBox(right) {

        val fm = loadFont("data/fonts/ia_writer.ttf", 8.0, contentScale = 4.0)

        val c = Camera2D()
        extend(c)
        extend {

            c.view = transform {
                translate(drawer.bounds.center)
                scale(3.0)
                translate(-anim.oldCenter.mix(anim.currentCenter, anim.moveAmt))
            }

            for ((index, d) in droplets.values.filter { it.imageLoaded && it.label != "" }.withIndex()) {

                val p0 = d.bounds.center
                val p1 = Polar(
                    Double.uniform(0.0, 360.0, Random(index)),
                    d.bounds.width * 1.8).cartesian + p0

                drawer.isolated {
                    drawer.stroke = ColorRGBa.RED

                    drawer.lineSegment(p0, p1)
                }

                drawer.stroke = ColorRGBa.RED
                d.draw(drawer, 1)

                drawer.isolated {

                    drawer.lineSegment(p0, p1)
                    val r = Rectangle.fromCenter(p1, 80.0, 11.0)
                    drawer.stroke = null
                    drawer.fill = ColorRGBa.RED
                    drawer.rectangle(r)

                    drawer.fontMap = fm
                    drawer.fill = ColorRGBa.BLACK
                    val time = sceneAnimations.textAnimation * 10.0
                    drawer.text(d.label.take((time).toInt() + 20).drop((time).toInt()), r.x + 5.0, r.y + 8.0)

                }

            }
        }
    }

    extend {
        sceneAnimations.updateAnimation()
        anim.updateAnimation()

        leftView.draw(drawer)
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