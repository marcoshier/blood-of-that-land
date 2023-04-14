package tools

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.events.Event


class Director: Animatable() {
    var disposeDummy = 0.0

    var videoSlider = 0.0
    var wordSlider = 0.0
    var captionSlider = 0.0

    val nextVid = Event<Unit>()
    val nextWord = Event<Unit>()
    val nextCaption = Event<Unit>()

    fun dispose(): PropertyAnimationKey<Double> {
        disposeDummy = 0.0
        ::disposeDummy.cancel()
        return ::disposeDummy.animate(1.0, 500)
    }

    fun queueCaption(time: Long)  {
        println("next caption")
        ::captionSlider.cancel()
        ::captionSlider.animate(1.0, 0, predelayInMs = time).completed.listen {
            nextCaption.trigger(Unit)
        }
    }

    fun queueVideo(time: Long)  {
        println("next vid")
        ::videoSlider.cancel()
        ::videoSlider.animate(1.0, 0, predelayInMs = time).completed.listen {
            nextVid.trigger(Unit)
        }
    }

    fun queueWord() {
        println("next word")
        ::wordSlider.cancel()
        ::wordSlider.animate(1.0, 10000).completed.listen {
            nextWord.trigger(Unit)
        }
    }

}