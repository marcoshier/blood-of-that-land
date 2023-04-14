package film

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blend.*
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.internal.colorBufferLoader
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

fun main() = application {
    configure {
        width = 1280
        height = 720
        windowAlwaysOnTop = true
    }

    oliveProgram {
        val files = File("data/la_taranta_test/").listFiles()!!
            .filter { it.isFile }
            .map { it.absolutePath }

        val proxies = files.map {
            println("file:/$it")
            colorBufferLoader.loadFromUrl("file://$it")
        }

        val oilAndWater = loadVideo("data/oil_on_water.mp4", PlayMode.VIDEO)
        oilAndWater.play()
        oilAndWater.seek(oilAndWater.duration / 2.0)

        val cb = colorBuffer(width, height)
        oilAndWater.newFrame.listen {
            it.frame.copyTo(cb,
                sourceRectangle = it.frame.bounds.toInt(),
                targetRectangle = IntRectangle(0, height, width, -height))
            cb.shadow.download()
        }

        val gui = GUI()

        val c = compose {
            layer {
                draw {
                    val t = ((sin(seconds * 0.01) * 0.5 + 0.5) * (files.size - 1)).toInt()

                    proxies[t.coerceIn(0, proxies.size - 1)].colorBuffer?.let {
                        drawer.imageFit(it, drawer.bounds)
                    }
                }
            }
            layer {
                draw {
                    blend(Multiply())
                    oilAndWater.draw(drawer, blind = true)

                    drawer.image(cb)
                    fun f(v: Vector2): Double {
                        val iv = v.toInt()
                        val d = if (iv.x >= 0 && iv.y >= 0 && iv.x < cb.width && iv.y < cb.height) cb.shadow[iv.x, iv.y].luminance else 0.0
                        return cos(d * PI * 18.0)
                    }

                    val contours = findContours(::f, drawer.bounds.offsetEdges(32.0), 4.0)

                    drawer.fill = null
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.contours(contours)
                }

                post(ColorCorrection()).addTo(gui)
            }
        }

        extend(gui)
        extend {

            c.draw(drawer)
        }
    }
}