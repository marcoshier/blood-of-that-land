package blob_tracker

import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.viewbox.ViewBox
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import tools.ColorMoreThan
import tools.Loader

fun main() = application {
    configure {
        width = 2120
        height = 1080
        windowAlwaysOnTop = true
    }
    program {

        val gui = GUI()

        val sourceFrame = Rectangle(0.0, 0.0, 960.0, 540.0)
        val dry = viewBox(sourceFrame).apply { loadDropletsVideo(sourceFrame, dry = false) }

        val source = viewBox(sourceFrame) {
            extend(Post()) {
                val threshold = ColorMoreThan().addTo(gui)
                val colorcorr = ColorCorrection().addTo(gui)
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

        val target = viewBox(sourceFrame) {
            val space = Plate(source.result.bounds)
            space.dropletAdded.listen {
                println("added")
            }
            extend {
                space.update(source.result)
                space.draw(drawer)
            }
        }

        extend(gui)
        mouse.apply {
            buttonDown.listeners.reverse()
            buttonUp.listeners.reverse()
            dragged.listeners.reverse()
        }
        extend {
            drawer.translate(200.0, 0.0)
            dry.draw()
            drawer.translate(960.0, 0.0)
            source.draw()
            drawer.defaults()
            drawer.translate(200.0, 540.0)
            target.draw()

        }
    }
}

fun Program.loadDropletsWebcam(sourceFrame: Rectangle) {
        val cb = colorBuffer(this.width, this.height)
        val devices = VideoPlayerFFMPEG.listDeviceNames()
        val video = loadVideoDevice(devices[0], PlayMode.VIDEO).apply {
            play()
            newFrame.listen {
                it.frame.copyTo(cb,
                    sourceRectangle = it.frame.bounds.flippedVertically().toInt(),
                    targetRectangle = sourceFrame.toInt()
                )
            }
        }


        extend {
            drawer.clear(ColorRGBa.WHITE)
            video.draw(drawer, blind = true)

            drawer.translate(drawer.bounds.center)
            drawer.scale(1.1, 1.3)
            drawer.translate(-drawer.bounds.center)
            drawer.image(cb)

        }
}

fun Program.loadDropletsVideo(sourceFrame: Rectangle, dry: Boolean = false) {
    val cb = colorBuffer(this.width, this.height)
    val video = loadVideo("offline-data/oil_on_water.mp4", PlayMode.VIDEO).apply {
        play()
        seek(duration / 3.0)
        ended.listen {
            restart()
            seek(15.0)
        }
        newFrame.listen {
            it.frame.copyTo(cb,
                sourceRectangle = it.frame.bounds.flippedVertically().toInt(),
                targetRectangle = sourceFrame.toInt()
            )
        }
    }

    if(!dry) {
        extend(Post()) {
            val threshold = ColorMoreThan().apply {
                background = ColorRGBa.BLACK
                foreground = rgb(0.0921, 0.6333, 0.0)
            }
            val colorcorr = ColorCorrection().apply {
                brightness = 0.18
                contrast = 1.0
                saturation = 1.0
                hueShift = -112.91
                gamma = 0.776
            }
            post { input, output ->
                val int = intermediate[0]
                colorcorr.apply(input, int)
                threshold.apply(int, output)
            }
        }
    }
    extend {
        drawer.clear(ColorRGBa.WHITE)
        video.draw(drawer, blind = true)

        drawer.translate(drawer.bounds.center)
        drawer.scale(0.99)
        drawer.translate(-drawer.bounds.center)
        drawer.image(cb)

    }
}
