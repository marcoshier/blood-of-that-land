package blob_tracker

import boofcv.abst.tracker.PointTrack
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.color.spaces.ColorOKHSVa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.bounds

class TrackPoint(val pos: Vector2, val id: Int): PointTrack()

class Droplet {

    var isTracked = true

    var label = ""
    var currentIds = listOf<Int>()
        set(value) {
            isTracked = value.intersect(field).isNotEmpty()
            field = value
        }
    var bounds = Rectangle.EMPTY
    var points = setOf<TrackPoint>()
        set(value) {
            bounds = value.map { it.pos }.bounds
            currentIds = value.map { it.id }
            field = value
        }
    var contour = ShapeContour.EMPTY

    val c = ColorRGBa.RED
    fun draw(drawer: Drawer) {
        if(points.isNotEmpty()) {
            drawer.stroke = c
            drawer.strokeWeight = 1.0
            drawer.fill = null
            drawer.contour(contour)

            drawer.stroke = null
            drawer.fill = c
            drawer.circles(points.map { it.pos }, 0.7)

            drawer.text(label, bounds.corner - Vector2(10.0, 14.0))
        }
    }

}