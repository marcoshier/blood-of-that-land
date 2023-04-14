package sketches

import org.openrndr.application
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.Rectangle
import screens.interface01
import tools.Director


fun main() = application {
    configure {
        width = 1920
        height = 540
        //position = IntVector2(0, 0)
        hideWindowDecorations = false
    }
    program {
        val director = Director()

        val film = viewBox(Rectangle(0.0, 0.0, width / 2.0, height * 1.0)).apply { filmWithImages() }
        val nextImage: () -> Unit by film.userProperties

        director.nextVid.listen {
            nextImage()
            director.queueVideo(5000)
        }


        val interf = viewBox(Rectangle(width / 2.0, 0.0, width / 2.0, height * 1.0)).apply { interface01() }
        /*        val nextWord: (word: String) -> Unit by interf.userProperties

                director.nextWord.listen {
                    nextWord(server.getNextSentenceDebug())
                }*/


        extend {
            director.updateAnimation()

            interf.draw()
            drawer.defaults()
            film.draw()

        }
    }
}