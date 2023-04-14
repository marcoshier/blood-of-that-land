package sketches

import tools.Loader
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.shape.IntRectangle
import tools.Director

fun Program.film01() {

    val loader = Loader()
    val videos = loader.loadVideos()

    val cb = colorBuffer(width, height)

    var currentIndex = 0
    var currentVideo = videos[currentIndex].run { VideoPlayerFFMPEG.fromFile(path, PlayMode.VIDEO) }

    var nextVid : ()->Unit by this.userProperties
    nextVid = {
        currentVideo.newFrame.listeners.clear()

        currentVideo.pause()
        currentIndex++
        currentVideo = videos[currentIndex].run {
            VideoPlayerFFMPEG.fromFile(path, PlayMode.VIDEO)
        }.also{
            it.play()
            it.newFrame.listeners.add { fe ->
                fe.frame.copyTo(cb, sourceRectangle = fe.frame.bounds.toInt(),
                    targetRectangle = IntRectangle(0, 1080, 1920, -1080)
                )
            }}
    }

    extend {
        currentVideo.draw(drawer, blind = true)

        drawer.image(cb)
    }
}

fun main() = application {
    configure {
        width = 1920
        height = 1080
    }
    program {
        val director = Director()

        val filmTest = viewBox(drawer.bounds).apply { film01() }
        val nextVid: () -> Unit by filmTest.userProperties

        nextVid()
        director.nextVid.listen {
            nextVid()
            director.queueVideo(5000)
        }
        extend {
            director.updateAnimation()
            filmTest.draw()
        }
    }
}