package film

import tools.Loader
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.FontImageMap
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.fx.grain.FilmGrain
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.uniform
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.writer
import tools.Director
import kotlin.math.floor

fun main() = application {
    configure {
        width = 1920
        height = 1080
        hideWindowDecorations = true
    }
    program {
        val loader = Loader().apply {
            loadText()
            loadVideos()
        }
        val director = Director()

        val font = loadFont("data/fonts/Roboto-Italic.ttf", 40.0)

        var grabWord = false
        var currentTextIndex = 0
        var currentCaption = ""
        director.nextCaption.listen {
            grabWord = Random.bool(0.5)
            currentTextIndex += if(currentTextIndex >= loader.allText.length - 100) 0 else 80

            val word = if (grabWord) " " + loader.labels.random() else ""
            currentCaption = loader.allText.substring(currentTextIndex..currentTextIndex + 80) + word
            director.queueCaption(Double.uniform(5000.0, 15000.0).toLong())
        }

        val cb = colorBuffer(width, height)
        var currentVideoIndex = 0
        var currentVideo = loadVideo(loader.allVideos[currentVideoIndex].path, PlayMode.VIDEO)
        director.nextVid.listen {
            currentVideo.newFrame.listeners.clear()
            currentVideo.pause()
            //currentVideo.dispose()
            currentVideoIndex++
            currentVideo = loadVideo(loader.allVideos[currentVideoIndex].path, PlayMode.VIDEO)
            currentVideo.play()
            currentVideo.newFrame.listeners.add {
                it.frame.copyTo(cb, sourceRectangle = it.frame.bounds.toInt(), targetRectangle = IntRectangle(0, height, width, -height))
            }

            director.queueVideo(Double.uniform(5000.0, 15000.0).toLong())

        }


        val comp = compose {
            layer {
                draw {
                    drawer.clear(ColorRGBa.GRAY)
                    drawer.image(cb)
                }
                post(ColorCorrection()) {
                    brightness = -0.125
                    contrast = 1.2
                }
                post(FilmGrain()) {
                    time = seconds
                }
            }
            layer {
                draw {
                    drawer.strokeWeight = 1.5
                    drawer.fill = ColorRGBa.BLACK
                    drawer.fontMap = font

                    drawer.writer {
                        box = Rectangle(width / 2.0 - (textWidth(font, currentCaption) / 2.0), height - 150.0, width * 1.0, 150.0)
                        text(currentCaption)
                    }
                }
                post(ApproximateGaussianBlur())
            }
            layer {
                draw {
                    drawer.strokeWeight = 1.5
                    drawer.fill = ColorRGBa.WHITE
                    drawer.fontMap = font

                    drawer.writer {
                        box = Rectangle(width / 2.0 - (textWidth(font, currentCaption) / 2.0), height - 150.0, width * 1.0, 150.0)
                        if(!grabWord) {
                            text(currentCaption)
                        } else {
                            val split = currentCaption.split(" ")
                            text(currentCaption.removeSuffix(split.last()))
                            drawer.fill = ColorRGBa.BLUE
                            text(split.last())
                        }
                    }
                }
            }
        }



        director.queueCaption(0)
        director.queueVideo(0)
        //director.queueWord()

        //extend(ScreenRecorder())
        extend {
            director.updateAnimation()
            if(director.disposeDummy == 1.0) {
                currentVideo.draw(drawer, blind = true)
            }

            comp.draw(drawer)

            drawer.stroke = null
            drawer.fill = if(grabWord) ColorRGBa.BLUE else ColorRGBa.GRAY
            drawer.rectangle(80.0, 80.0, 10.0, 10.0)
        }
    }
}

fun textWidth(fm: FontImageMap, text: String): Double {
    return text.fold(0.0) { a, b -> a.plus(fm.characterWidth(b)) }
}

