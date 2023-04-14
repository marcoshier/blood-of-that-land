package film

import org.openrndr.application
import org.openrndr.draw.ColorBufferProxy
import org.openrndr.extra.fx.blend.Screen
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.internal.colorBufferLoader
import java.io.File
import kotlin.math.sin
import kotlin.math.tan

fun main() = application {
    configure {
        width = 1280
        height = 720
        hideWindowDecorations = false
    }
    program {

        val files = File("data/la_taranta_test/").listFiles()!!
            .filter { it.isFile }
            .map { it.absolutePath }

        val proxies = files.map {
            println("file:/$it")
            colorBufferLoader.loadFromUrl("file://$it")
        }

        extend(ScreenRecorder())
        extend {
            val t = ((tan(seconds * 0.01) * 0.5 + 0.5) * (files.size - 1)).toInt()

            if(seconds > 500.0) {
                application.exit()
            }

            proxies[t.coerceIn(0, proxies.size - 1)].colorBuffer?.let {
                drawer.imageFit(it, drawer.bounds)
            }

        }
    }
}