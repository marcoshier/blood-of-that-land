package blob_tracker

import org.openrndr.animatable.Animatable
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import tools.KLT
import tools.Loader
import tools.computeContours

class PlateFromPoints(val frame: Rectangle): Animatable() {

    val loader = Loader()
    val droplets = mutableMapOf<Int, Droplet>()

    val dropletAdded = Event<DropletAddedEvent>()

    var image = colorBuffer(frame.width.toInt(), frame.height.toInt())
        set(value) {
            field = value
            val range = 5.0..200.0

            val cc = computeContours(value)
            if(cc.isNotEmpty()) {
                contours = cc.filter { it.bounds.width in range && it.bounds.height in range}.map { it.close() }
                rects = contours.map { it.bounds }
            }
        }

    val tracker = KLT()

    var rects = listOf<Rectangle>()
    var trackPoints = listOf<TrackPoint>()
        set(value) {
            field = value

            //prune()
            val pointsToRects = rects.map { rect ->
                value.filter { p -> rect.contains(p.pos) }
            }

            if(droplets.isEmpty()) {
                println("empty")
                pointsToRects.forEachIndexed { index, points ->
                    val d = Droplet()
                    d.points = points.toSet()
                    droplets[index] = d
                }
            }

            val iter = pointsToRects.listIterator()
            while (iter.hasNext()) {
                val rectPoints = iter.next().toSet()
                val index = iter.nextIndex()
                val ids = rectPoints.map { it.id }

                val droplet = droplets.entries.firstOrNull {
                    it.value.currentIds.intersect(ids).isNotEmpty()
                }?.value

                if(droplet != null) {
                    droplet.points = rectPoints.toSet()
                } else {
                    droplets.getOrPut(index) {
                        val d = Droplet()
                        d.points = rectPoints
                        dropletAdded.trigger(DropletAddedEvent(index, d.label, d.c))
                        d
                    }
                }

            }

        }
    var contours = listOf<ShapeContour>()


    var dropTimer = 0.0
    var timer = 0.0
    fun prune() {
        if(!hasAnimations()) {
            //::dropTimer.cancel()
            ::dropTimer.animate(1.0, 4000).completed.listen {
                println("dropping")
                val inactive = tracker.getInactiveTracks(null)
                tracker.dropTracks { inactive.contains(it) }

                val keysToRemove = droplets.filter {
                    it.value.currentIds.intersect(trackPoints.map { it.id }).isEmpty() }.keys

                for(key in keysToRemove) {
                    droplets.remove(key)
                }

                tracker.spawnTracks()
            }
        }

    }

    fun update(cb: ColorBuffer) {
        image = cb
    }

    fun draw(drawer: Drawer) {
        updateAnimation()
        drawer.isolated {
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 0.1
            drawer.circles(trackPoints.map { it.pos }, 1.5)
        }

        droplets.forEach { it.value.draw(drawer) }


        drawer.isolated {
            drawer.fill = ColorRGBa.WHITE.opacify(0.5)
            drawer.stroke = null
            drawer.contours(contours)
        }


        drawer.fill = ColorRGBa.WHITE
        drawer.rectangle(0.0, 0.0, frame.width * timer, 5.0)
    }

    init {
        prune()
    }
}