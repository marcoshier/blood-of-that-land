package scenes

import blob_tracker.Droplet
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.events.Event
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour

class ZoomedAnimation(val bounds: Rectangle): Animatable() {
    var timerRight = 0.0
    var rects: List<Rectangle> = listOf()
    var moveAmt = 0.0
    var oldCenter = Vector2.ZERO
    var currentCenter = Vector2.ZERO
        set(value) {
            oldCenter = field
            move()
            field = value
        }

    fun move() {
        ::moveAmt.animate(1.0, 2000, Easing.CubicInOut).completed.listen {
            oldCenter = currentCenter
            moveAmt = 0.0
        }
    }

    fun tick() {
        ::timerRight.animate(1.0, 5000).completed.listen {
            if(rects.isNotEmpty()) {
                currentCenter = rects.random().center
                println(currentCenter)
            }
            tick()
        }
    }

    init {
        tick()
    }
}

class ConnectAnimation(val frame: Rectangle): Animatable() {
    var orderFader = 0.0
    var oldOrder = listOf<Int>()

    var currentOrder = listOf<Int>()
        set(value) {
            oldOrder = field
            field = value
            println(value)
        }
    var connectorTimer = 0.0
    var currentDroplets = mutableMapOf<Int, Droplet>()
        set(value) {
            field = value
            currentPositions = field.map { it.value.bounds.center }
        }
    var currentPositions = listOf<Vector2>()
        set(value) {
            connector = ShapeContour.fromPoints(value, false)
            field = value
        }

    var connector = ShapeContour.EMPTY

    fun switch(delay: Long) {
        orderFader = 0.0
        ::connectorTimer.animate(1.0, 2000, Easing.CubicInOut).completed.listen {
            ::connectorTimer.animate(0.0, 1000, Easing.CubicInOut, delay).completed.listen {
                val shuffled = currentDroplets.map { it.key to it.value }.shuffled()
                currentOrder = shuffled.map { it.first }
                orderFader = 0.0
                ::orderFader.animate(1.0, 1000, Easing.CubicInOut)

                val positions = shuffled.map { it.second.bounds.center }
                connector = ShapeContour.fromPoints(positions, false)
                switch(5000)
            }
        }
    }


    /*fun switch(delay: Long) {
        currentPositions = List(currentPositions.size) { Vector2.uniform(frame.offsetEdges(200.0)) }
        ::positionsTimer.animate(1.0, 1000, Easing.CubicInOut, delay).completed.listen {
            positionsTimer = 0.0
            switch(2000L)
        }
    }
*/
}

class SceneChanger: Animatable() {

    val startVideo = Event<Unit>()
    val prepareVideo = Event<Unit>()

    val sceneDuration = 60000L
    var sceneTimer = 0.0
    var tvTimer = 0.0
    var current = 0

    fun scene() {
        ::sceneTimer.animate(1.0, sceneDuration).completed.listen {

            current += 1
            sceneTimer = 0.0

            if(current == 2) {
                prepareVideo.trigger(Unit)
            }
            if(current == 3) {
                startVideo.trigger(Unit)
                ::tvTimer.animate(1.0, 60000).completed.listen {
                    current = 0
                    tvTimer = 0.0
                    scene()
                }
            }
            if(current < 3) {
                scene()
            }
        }
    }

    init {
        scene()
    }
}