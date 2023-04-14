import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    oliveProgram {

        val positions = drawer.bounds.scatter(20.0)
        val circles = positions.map {
            Circle(it, 45.0).contour
        }

        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.fill = null
            Random.resetState()


            for(johnny in circles) {
                val y = Double.uniform(0.0, 1.0, Random.rnd)
                val t = y * seconds
                val c = johnny.sub(t, t + 0.2)

                drawer.contour(c)
            }


        }
    }
}