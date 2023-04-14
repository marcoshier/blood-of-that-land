package blob_tracker

import org.openrndr.animatable.Animatable
import org.openrndr.boofcv.binding.toGrayF32
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import scenes.range
import tools.KLT
import tools.Loader
import tools.computeContours
import tools.wordList

class DropletAddedEvent(val index: Int, val label: String, val c: ColorRGBa)
class Plate(val frame: Rectangle): Animatable() {

    val tracker = KLT()
    val droplets = mutableMapOf<Int, Droplet>()

    val dropletAdded = Event<DropletAddedEvent>()

    var image = colorBuffer(frame.width.toInt(), frame.height.toInt())
        set(value) {
            field = value

            val cc = computeContours(value)
            if(cc.isNotEmpty()) {
                contours = cc.filter { it.bounds.width in range && it.bounds.height in range}
            }
        }

    var contours = listOf<ShapeContour>()
        set(value) {
            field = value
            // prune()
            tracker.process(image.toGrayF32())
            prune()
            tracker.spawnTracks()

            trackPoints = tracker.getActiveTracks(null)
                .map { TrackPoint(Vector2(it.pixel.x, it.pixel.y), it.featureId.toInt()) }
                .toMutableList()

        }

    var trackPoints = mutableListOf<TrackPoint>()
        set(value) {
            if(field != value) {
                val pointsToContours = contours.map { contour ->
                    value.filter { p -> contour.bounds.contains(p.pos) } to contour
                }

                if(droplets.isEmpty()) {
                    pointsToContours.forEachIndexed { index, (poc, c) ->
                        putDroplet(index, poc.toSet(), c)
                    }
                }

                val iter = pointsToContours.listIterator()
                while (iter.hasNext()) {
                    val next = iter.next()
                    val rectPoints = next.first.toSet()
                    val c = next.second

                    val index = iter.nextIndex()
                    val ids = rectPoints.map { it.id }


                    val droplet = droplets.entries.firstOrNull {
                        it.value.currentIds.intersect(ids).isNotEmpty()
                    }?.value

                    if(droplet != null) {
                        droplet.points = rectPoints.toSet()
                        droplet.contour = c
                    } else {
                        droplets.getOrElse(index) {
                            putDroplet(index, rectPoints, c)
                        }
                    }

                }
            }
            field = value
        }


    private fun putDroplet(index: Int, pts: Set<TrackPoint>, c: ShapeContour) {
        val d = Droplet().apply {
            label = wordList.random()
            points = pts
            contour = c
        }
        droplets[index] = d
        dropletAdded.trigger(DropletAddedEvent(index, d.label,  d.c))
    }

    var timer = 0.0
    private fun prune() {
        if(!hasAnimations()) {
            timer = 0.0
            cancel()
            ::timer.animate(1.0, 2000).completed.listen {

                val outliers = droplets.filter {
                    it.value.currentIds.intersect(trackPoints.map { it.id }).isEmpty() || !it.value.isTracked
                }

                droplets.values.removeAll(outliers.values)
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


        drawer.isolated {
            drawer.stroke = ColorRGBa.RED
            drawer.contours(contours)
        }

        droplets.values.forEach { it.draw(drawer) }


        drawer.fill = ColorRGBa.WHITE
        drawer.rectangle(0.0, 0.0, frame.width * timer, 5.0)
    }
}