package nge.test

import org.bytedeco.javacv.*
import org.bytedeco.javacv.Frame
import org.bytedeco.opencv.opencv_core.*
import org.bytedeco.opencv.global.opencv_imgproc.*
import javax.swing.*
import java.awt.*
import java.io.File
import javax.imageio.ImageIO
import java.io.BufferedReader
import java.io.InputStreamReader


var grabber: OpenCVFrameGrabber? = null
var isRunning = false
var lastCapturedFrame: Frame? = null // Stocker la dernière image capturée

fun createAndShowGUI() {
    val frame = JFrame("VIN Recognition App")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(640, 480)

    val panel = ImagePanel("/home/hanane/IdeaProjects/test2/src/main/resources/2.png")
    panel.layout = null

    val vinLabel = JLabel("VIN: ")
    vinLabel.foreground = Color.RED
    vinLabel.font = vinLabel.font.deriveFont(20f)
    vinLabel.setBounds(80, 430, 480, 30)
    panel.add(vinLabel)

    val startButton = JButton("Start Camera")
    startButton.setBounds(10, 10, 150, 50)
    panel.add(startButton)

    val stopButton = JButton("Stop Camera")
    stopButton.setBounds(180, 10, 150, 50)
    panel.add(stopButton)

    val predictButton = JButton("Predict VIN")
    predictButton.setBounds(350, 10, 150, 50)
    panel.add(predictButton)

    val label = JLabel()
    label.setBounds(80, 60, 480, 360)
    panel.add(label)

    frame.contentPane = panel
    frame.isVisible = true

    startButton.addActionListener {
        if (!isRunning) {
            Thread { startCamera(label) }.start()
        }
    }

    stopButton.addActionListener {

        stopCamera()
    }

    predictButton.addActionListener {
        // Lorsque le bouton prédiction est cliqué, capturer la dernière image
        Thread {
            val imagePath = captureLastImage()  // Capture la dernière image enregistrée
            //stopCamera()
            if (imagePath != null) {
                val vin = callPythonScript(imagePath)
                //SwingUtilities.invokeLater { vinLabel.text = "VIN: $vin" }
            }
        }.start()
    }
}

fun startCamera(label: JLabel) {
    grabber = OpenCVFrameGrabber(0)
    isRunning = true

    try {
        grabber?.start()
        val converter = Java2DFrameConverter()

        while (isRunning) {
            val frame = grabber?.grab() ?: break
            lastCapturedFrame = frame // Sauvegarder la dernière image capturée
            //stopCamera()
            val image = converter.convert(frame) ?: continue
            SwingUtilities.invokeLater {
                label.icon = ImageIcon(image)
                label.repaint()
            }
            Thread.sleep(100) // Réduit la charge CPU, fréquence de capture plus lente
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun stopCamera() {
    isRunning = false
    grabber?.stop()
    grabber?.release()
    grabber = null
}

fun captureLastImage(): String? {
    // Si grabber n'a pas démarré ou si aucune image n'a été capturée, retournez null
    if (lastCapturedFrame == null) {
        println("Erreur: Aucune image capturée.")
        return null
    }

    val imagePath = "/home/hanane/captured_image.jpg"
    try {
        val converter = Java2DFrameConverter()
        val image = converter.convert(lastCapturedFrame)  // Utiliser la dernière image capturée
        val file = File(imagePath)
        ImageIO.write(image, "jpg", file)
        return imagePath
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun callPythonScript(imagePath: String): String {
    val pythonScriptPath = "/home/python_scripts/vin_recognition.py"

    if (!File(pythonScriptPath).exists()) {
        println("Erreur: Script Python introuvable !")
        return "No VIN found"
    }

    return try {
        val processBuilder = ProcessBuilder("python3", pythonScriptPath, imagePath)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        println("Script output: $output")
        output.trim()
    } catch (e: Exception) {
        e.printStackTrace()
        "No VIN found"
    }
}

fun main() {
    SwingUtilities.invokeLater(::createAndShowGUI)
}

class ImagePanel(imagePath: String) : JPanel() {
    private val backgroundImage: Image = ImageIO.read(File(imagePath))
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.drawImage(backgroundImage, 0, 0, width, height, this)
    }
}
