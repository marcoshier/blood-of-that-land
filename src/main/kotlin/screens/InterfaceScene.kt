package screens

import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.extra.keyframer.Keyframer
import org.openrndr.extra.viewbox.viewBox

fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {

        val interface01 = viewBox(drawer.bounds).apply { interface01() }

        class Director: Animatable() {
            var timer01 = 0.0
            var timer02 = 0.0

            fun sceneTimer() {
                ::timer01.animate(1.0, 20000).completed.listen {
                    timer02 = 0.0
                    ::timer02.animate(1.0, 20000).completed.listen {
                        timer01 = 0.0
                        sceneTimer()
                    }
                }
            }

            init {
                sceneTimer()
            }
        }
        val director = Director()

        extend {
            director.updateAnimation()

            if(director.timer01 != 1.0) {
                interface01.draw()
            }

            drawer.rectangle(0.0, 0.0, width * director.timer01, 20.0)
            drawer.rectangle(0.0, 20.0, width * director.timer02, 20.0)
        }
    }
}