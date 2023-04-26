package scenes

import blob_tracker.Droplet
import blob_tracker.Plate
import blob_tracker.loadVideoSource
import com.google.gson.Gson
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import org.openrndr.Fullscreen
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.launch
import org.openrndr.shape.Rectangle
import tools.ColorMoreThan
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

val range = 10.0..250.0
val fromWebcam = true
val debug = false


fun main() = application {
    configure {
        if(debug) {
            width = 1280
            height = 720
        } else
        {
            width = 1280
            height = 720
            fullscreen = Fullscreen.SET_DISPLAY_MODE
            windowAlwaysOnTop = true
            hideCursor=true
            hideWindowDecorations=true

        }
    }
    program {

        val dry = viewBox(drawer.bounds).apply { loadVideoSource(drawer.bounds, fromWebcam)  }
        val video = viewBox(drawer.bounds).apply { wet() }
        val treat: (img: ColorBuffer) -> Unit by video.userProperties

        val plate = Plate(drawer.bounds)

        val scene01 = viewBox(drawer.bounds).apply { scene01() }
        val updateFirst: (image: ColorBuffer, droplets: MutableMap<Int, Droplet>) -> Unit by scene01.userProperties

        val scene02 = viewBox(drawer.bounds).apply { scene02() }
        val updateSecond: (droplets: MutableMap<Int, Droplet>) -> Unit by scene02.userProperties












        var shouldUpdate = 0
        val scene03 = viewBox(drawer.bounds).apply { scene03() }
        val updateThird: (droplets: MutableMap<Int, Droplet>) -> Unit by scene03.userProperties


        val socket = DatagramSocket()
        val address = java.net.InetSocketAddress(InetAddress.getByName("192.168.42.2"), 9002)

        fun send(state:String) {
            val baos = ByteArrayOutputStream(1024)
            val oos = ObjectOutputStream(baos)
            oos.writeUnshared(state)
            val data = baos.toByteArray()
            val p = DatagramPacket(data, data.size, address)
            socket.send(p)
        }

        val sceneChanger = SceneChanger()
        sceneChanger.prepareVideo.listen {
            println("sending video paths")
            launch {
                val filePaths = plate.droplets.filter { it.value.imageLoaded }.map { it.value.file + "|" + it.value.label}
                send(filePaths.joinToString(","))
            }
        }
        sceneChanger.startVideo.listen {
            launch {
                send("START")
            }
        }

        var switch = false

        extend {
            sceneChanger.updateAnimation()

            dry.update()
            treat(dry.result)
            video.update()
            if(shouldUpdate == 0) {
                plate.update(video.result)
            }


            drawer.isolated {
                when(sceneChanger.current) {
                    0 -> {
                        updateFirst(video.result, plate.droplets)
                        scene01.draw()
                    }
                    1 -> {
                        updateSecond(plate.droplets)
                        scene02.draw()
                    }
                    2 -> {
                        if(shouldUpdate == 0) {
                            updateThird(plate.droplets)
                            shouldUpdate++
                        }
                        scene03.draw()
                    }
                }
            }

            if(sceneChanger.current == 1 || sceneChanger.current == 2) {
                val unlabeled = plate.droplets.filter { it.value.imageLoaded && it.value.label == "" }
                // remove the switch?
                if(unlabeled.isNotEmpty() && !switch) {
                    launch {
                        val filePaths = unlabeled.map {
                            println("getting label for ${it.value.file}")
                            it.key to it.value.file
                        }
                        val ls = plate.server.getLabels(filePaths.map { it.second })
                        val array = Gson().fromJson(ls, Array<String>::class.java)

                        filePaths.take(11).forEach {
                            plate.droplets[it.first]?.label = array[it.first]
                        }

                    }
                    switch =true
                }
            }


            if(debug) {
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.rectangle(0.0, 0.0, width * sceneChanger.sceneTimer, 10.0)
                drawer.text(sceneChanger.current.toString(), 5.0, 20.0)

                dry.update()
                val w = dry.result.bounds.width / 4.0
                val h = dry.result.bounds.height / 4.0
                drawer.image(dry.result, dry.result.bounds, Rectangle(0.0, height - h, w, h))
                drawer.image(video.result, dry.result.bounds, Rectangle(w, height - h, w, h))
            }
        }
    }
}

fun Program.wet() {

    var cb = colorBuffer(width, height)

    var treat: (img: ColorBuffer)->Unit by this.userProperties
    treat = { img ->
        cb = img
    }

    extend(Post()) {
        val threshold = ColorMoreThan().apply {
            background = ColorRGBa.BLACK
            foreground = rgb(0.0921, 0.6333, 0.0)
        }
        val colorcorr = ColorCorrection().apply {
            brightness = 0.354
            contrast = 1.0
            saturation = 1.0
            hueShift = -112.91
            gamma = 0.776
        }
        post { input, output ->
            val int = intermediate[0]
            colorcorr.apply(input, int)
            threshold.apply(int, output)
        }
    }
    extend {
        drawer.image(cb)
    }
}