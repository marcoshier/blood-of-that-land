package scenes

import blob_tracker.Droplet
import blob_tracker.loadVideoSource
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blend.DestinationAtop
import org.openrndr.extra.fx.blend.Normal
import org.openrndr.extra.fx.color.LumaMap
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.random.Random

fun Program.scene01() {

    var dry = colorBuffer(width, height)
    var droplets = mutableMapOf<Int, Droplet>()

    var updateFirst: (image: ColorBuffer, contours: MutableMap<Int, Droplet>)->Unit by this.userProperties
    updateFirst = { img, dpls ->
        dry = img
        droplets = dpls
    }

    val sceneAnimations = object: Animatable() {
        var titleTimer = 0.0
        var textTimer = 0.0
        var backdrop = 0.0
        var backdropTimer = 0.0

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

    val left = Rectangle(0.0, 0.0, width / 2.0, height * 1.0)
    val leftView = compose {
        layer {
            draw {
                drawer.imageFit(dry, left)
            }
            post(LumaMap()) {
                foreground = ColorRGBa.RED
                background = ColorRGBa.WHITE
            }
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
                drawer.stroke = ColorRGBa.RED.shade(0.2)
                drawer.strokeWeight = 300.0
                drawer.circle(left.center, 550.0)

                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.rectangle(30.0, 30.0, 290.0 * sceneAnimations.titleTimer, 28.0)

                drawer.fontMap = fm
                drawer.fill = ColorRGBa.BLACK
                val title = "1 • THE ALL-SEEING ORACLE"
                drawer.text(title.take((title.length * sceneAnimations.titleTimer).toInt()), 40.0, 50.0)

                drawer.fill = ColorRGBa.WHITE
                drawer.fontMap = fmItalic

                val text = "The oracle watches the plate, searching for meaning, within the scattered traces of the past, hidden in the oil droplets..."
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

    val zoomAnimation = ZoomedAnimation(drawer.bounds)

    val c = Camera2D()
    class Compositions {
        var isLoaded = false
            set(value) {
                field = value
                comps = droplets.map {
                    compose {
                        layer {
                            layer {
                                draw {
                                    drawer.view = c.view
                                    c.view = transform {
                                        translate(drawer.bounds.center)
                                        scale(3.0)
                                        translate(-zoomAnimation.oldCenter.mix(zoomAnimation.currentCenter, zoomAnimation.moveAmt))
                                    }

                                    it.value.draw(drawer)
                                }
                            }
                        }
                        layer {
                            blend(Normal()) {
                                clip = true
                            }
                            draw {
                                drawer.defaults()
                                drawer.imageFit(it.value.cb, drawer.bounds)
                            }
                        }

                    }
                }
            }
        var comps = listOf<Composite>()
    }
    val compositions = Compositions()

    val right = Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)
    val rightView = viewBox(right) {


        extend(c)
        extend {

            compositions.comps.forEach {
                it.draw(drawer)
            }

        }
    }

    extend {
        zoomAnimation.rects = droplets.values.filter { it.imageLoaded }.map { it.bounds }

        sceneAnimations.updateAnimation()
        zoomAnimation.updateAnimation()

        leftView.draw(drawer)

        if(!compositions.isLoaded && droplets.any { it.value.imageLoaded }) {
            compositions.isLoaded = true
        } else {
            droplets.map { it.value.update() }
        }

        if(compositions.isLoaded) {
            rightView.draw()
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