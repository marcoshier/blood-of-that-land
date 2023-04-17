package scenes

import org.openrndr.animatable.Animatable
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour

class ZoomedAnimation(val bounds: Rectangle): Animatable() {
    var timerRight = 0.0
    var contours: List<ShapeContour> = listOf()
    var currentCenter: Vector2? = bounds.center
        set(value) {
            field = value ?: bounds.center
        }

    fun tick() {
        ::timerRight.animate(1.0, 2000).completed.listen {
            currentCenter = contours.shuffled().map { it.bounds }.firstOrNull { it.width in range }?.center
            tick()
        }
    }

    init {
        tick()
    }
}


class Animator(): Animatable() {
    var sceneTimer = 0.0
    var current = 0

    fun scene() {
        ::sceneTimer.animate(1.0, 10000).completed.listen {
            current = if(current == 2) 0 else current + 1
            sceneTimer = 0.0
            scene()
        }
    }

    init {
        //scene()
    }
}