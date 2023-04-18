package scenes

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
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

    var firstTime = true

    var connectorTimer = 0.0
    var positionsTimer = 0.0
    var oldPositions = listOf<Vector2>()
    var currentPositions = listOf<Vector2>()
        set(value) {
            if(!firstTime) oldPositions = field
            connector = ShapeContour.fromPoints(value, false)
            field = value
        }

    var connector = ShapeContour.EMPTY

    fun switch(delay: Long) {
        ::connectorTimer.animate(1.0, 2000, Easing.CubicInOut).completed.listen {
            ::connectorTimer.animate(0.0, 1000, Easing.CubicInOut, delay).completed.listen {
                connector = ShapeContour.fromPoints(currentPositions.shuffled(), false)
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
    val sceneDuration = 60000L
    var sceneTimer = 0.0
    var current = 0

    fun scene() {
        ::sceneTimer.animate(1.0, sceneDuration).completed.listen {
            current = if(current == 2) 0 else current + 1
            sceneTimer = 0.0
            scene()
        }
    }

    init {
        scene()
    }
}