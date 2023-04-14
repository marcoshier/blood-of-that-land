package scenes

import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.shape.ShapeContour

fun Program.scene02() {

    var contours = listOf<ShapeContour>()

    var update02: (contours: List<ShapeContour>)->Unit by this.userProperties
    update02 = {
        contours = it
    }

    extend {

        drawer.stroke = ColorRGBa.GREEN
        drawer.contours(contours)

    }

}