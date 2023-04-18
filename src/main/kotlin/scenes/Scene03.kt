package scenes

import blob_tracker.Droplet
import blob_tracker.contours
import blob_tracker.loadVideoSource
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.draw.writer
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.IntVector2
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import tools.computeContours

fun Program.scene03() {

    var droplets = mutableMapOf<Int, Droplet>()
    var contours = listOf<ShapeContour>()

    val left = Rectangle(0.0, 0.0, width / 2.0, height * 1.0)
    val right = Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)

    val zoomedAnim = ZoomedAnimation(drawer.bounds)
    val connectAnim = ConnectAnimation(Rectangle(width / 4.0, 0.0, height * 1.0, height * 1.0))

    var updateThird: (droplets: MutableMap<Int, Droplet>)->Unit by this.userProperties
    updateThird = { dpls ->
        droplets = dpls
        contours = droplets.map { it.value.contour }
        val initialPositions = contours.map { it.bounds.center }
        connectAnim.apply {
            oldPositions = initialPositions
            currentPositions = initialPositions
            firstTime = false
            switch(2000L)
        }
    }

    val rightView = viewBox(right) {

        val grid = drawer.bounds.grid(5, 7).flatten()

        extend {
            droplets.forEach { it.value.update() }

            for((i, droplet) in droplets.filter { it.value.imageLoaded }) {
                val image = droplet.cb
                val r = grid[i]
                val imageRect = r.sub(0.0, 0.0, 1.0, 0.8)
                val textRect = r.sub(0.0, 0.8, 1.0, 0.2)
                drawer.image(image,
                    image.bounds,
                    imageRect
                )
                drawer.fill = ColorRGBa.WHITE
                drawer.writer {
                    box= textRect
                    newLine()
                    text(droplet.label)
                }
            }

        }

    }

    extend {
        zoomedAnim.updateAnimation()
        connectAnim.updateAnimation()


        if(connectAnim.currentPositions.isNotEmpty()) {
            drawer.isolated {
                drawer.translate(-width / 4.0, 0.0)
                drawer.drawStyle.clip = left
                drawer.stroke = ColorRGBa.RED

                drawer.contour(connectAnim.connector.sub(0.0, connectAnim.connectorTimer))

                drawer.fill = ColorRGBa.BLACK
                contours.forEachIndexed { i, it ->
                    drawer.pushTransforms()
                    drawer.contour(it)
                    drawer.popTransforms()
                }
            }
        }

        rightView.draw()


    }

}
fun main() = application {
    configure {
        width = 1280
        height = 720
        if(secondScreenConnected) {
            position = IntVector2(1920, 0)
        }
    }
    program {

        var contours = listOf<ShapeContour>()

        val fromVideo = true
        val dry = viewBox(drawer.bounds).apply {
            if(fromVideo) loadVideoSource(drawer.bounds, fromWebcam)
        }

        val scene02 = viewBox(drawer.bounds).apply { scene03() }
        val update02: (contours: List<ShapeContour>) -> Unit by scene02.userProperties

        extend {
            dry.update()
            contours = computeContours(dry.result).filter { it.bounds.width in range && it.bounds.height in range }

            update02(contours)
            scene02.draw()
        }
    }
}