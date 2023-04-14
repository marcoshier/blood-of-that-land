import org.openrndr.application
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideoDevice

fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {
        val devices = VideoPlayerFFMPEG.listDeviceNames()
        println(devices)
        val video = loadVideoDevice(devices[0]).apply { play() }
        println(video.width)
        println(devices)
        extend {
            video.draw(drawer)

        }
    }
}