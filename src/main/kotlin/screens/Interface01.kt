package screens

import blob_tracker.*
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.*
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.fx.color.LumaMap
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.writer
import scenes.fromWebcam
import tools.ColorMoreThan
import kotlin.random.Random

fun main() = application {
    configure {
        width = 1280
        height = 720
        hideWindowDecorations = true
        windowAlwaysOnTop = true
    }
    program {

        val interf = viewBox(drawer.bounds).apply { interface01() }

        extend {

            interf.draw()

        }
    }
}

fun Program.interface01(fromVideo: Boolean = true) {

        val sourceFrame = drawer.bounds

        val dry = viewBox(sourceFrame).apply {
            if(fromVideo) loadVideoSource(sourceFrame, fromWebcam)
        }

        val source = viewBox(sourceFrame) {
                extend(Post()) {
                    val threshold = ColorMoreThan().apply {
                        foreground = rgb(0.3587, 0.0, 0.4933)
                    }
                    val colorcorr = ColorCorrection().apply {
                        brightness = -0.342
                        contrast = -0.043
                        saturation = 0.106
                        hueShift = -3.354
                        gamma = 2.112
                    }
                    post { input, output ->
                        val int = intermediate[0]
                        colorcorr.apply(input, int)
                        threshold.apply(int, output)
                    }
                }
                extend {
                    drawer.clear(ColorRGBa.WHITE)
                    drawer.shadeStyle = shadeStyle {
                        fragmentTransform = """
                                vec2 texCoord = c_boundsPosition.xy;
                                texCoord.y = 1.0 - texCoord.y;
                                vec2 size = textureSize(p_image, 0);
                                texCoord.x /= size.x/size.y;
                                texCoord.x += 0.18;
                                x_fill = texture(p_image, texCoord);
                            """
                        parameter("image", dry.result)
                    }

                    drawer.stroke = null
                    val shape = Circle(width / 2.0, height / 2.0, 270.0).shape
                    drawer.shape(shape)
                    //drawer.image(dry.result)
                }
            }

        var seed = 0
        var seed1 = 0

        class Animator: Animatable() {
            var backdrop = 0.0
            var tgtTimer = 0.0

            fun flash() {
                backdrop = 1.0
                ::backdrop.cancel()
                ::backdrop.animate(0.0, 2000, Easing.CubicOut)
            }

            fun timer() {
                ::tgtTimer.animate(1.0, 2000).completed.listen {
                    seed1++
                    timer()
                }
            }

            init {
                timer()
            }
        }
        val anim = Animator()

        val space = Plate(source.result.bounds)

        val labelsSoFar = mutableListOf<String>()
        var lastDroplet = DropletAddedEvent(0, "", ColorRGBa.TRANSPARENT)
        space.dropletAdded.listen {
                seed++
                labelsSoFar.add(it.path)
                anim.flash()
                lastDroplet = it
        }

        val tgt = Rectangle(width - 250.0, 30.0, 220.0, 400.0)
        val fx = LumaMap().apply {
            background = ColorRGBa.BLACK
            foreground = ColorRGBa.RED
        }
        val cb = colorBuffer(tgt.width.toInt(), tgt.height.toInt())

        extend {
            anim.updateAnimation()

            dry.update()
            source.update()
            space.update(source.result)
            space.updateAnimation()

            drawer.clear(ColorRGBa.BLACK)

            space.run {
                drawer.isolated {
                    drawer.fill = ColorRGBa.WHITE
                    drawer.stroke = null
                    drawer.strokeWeight = 0.1
                    drawer.circles(trackPoints.map { it.pos }, 1.5)
                }

                space.droplets.forEach { it.value.draw(drawer) }
            }

            if(labelsSoFar.isNotEmpty()) {
                writer {
                    drawer.stroke = null
                    box = Rectangle(30.0, 30.0, 300.0, 1000.0)
                    for(d in space.droplets.toSortedMap()) {
                        newLine()
                        drawer.fill = ColorRGBa.WHITE
                        text("${d.key} - ${d.value.label}")
                    }
/*
                    newLine()
                    drawer.fill = lastDroplet.c
                    text(lastDroplet.label)*/
                }
            }

            drawer.isolated {
                drawer.fill = ColorRGBa.WHITE.shade(anim.backdrop)
                drawer.stroke = ColorRGBa.WHITE
                val r = Rectangle(30.0, height - 130.0, 100.0, 100.0)
                drawer.rectangle(r)
                drawer.stroke = ColorRGBa.BLACK
                drawer.fill = null
                for(i in 0 until 6) {
                    val radius = (r.width / 2.0) * ((i + 1) / 6.0) + (1.0 - anim.backdrop) * 12.0
                    val sub0 = Double.uniform(0.0, 0.5, Random(seed + i))
                    val offset = Double.uniform(0.0, 1.5, Random(seed + i))
                    drawer.contour(Circle(
                        r.center,
                        radius).contour.sub(-sub0 + offset, sub0 + offset))
                }
                drawer.stroke = ColorRGBa.WHITE
                drawer.rectangle(r)
            }


            drawer.isolated {
                val src = tgt.randomInRectangle(source.result.bounds, Random(seed1))
                fx.apply(dry.result, cb)
                drawer.image(dry.result, src, tgt)
                drawer.fill = null
                drawer.strokeWeight = 0.4
                drawer.stroke = ColorRGBa.WHITE
                drawer.rectangle(tgt)
            }
        }
}

fun Rectangle.randomInRectangle(other: Rectangle, rnd: Random = Random.Default): Rectangle {
    val newX = Double.uniform(x, other.width - x, rnd)
    val newY = Double.uniform(y, other.height - y, rnd)
    return Rectangle(newX, newY, width, height)
}