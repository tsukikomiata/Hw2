package com.example.imageeditor

import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.geometry.Rectangle2D
import javafx.scene.control.Button
import javafx.scene.control.Separator
import javafx.scene.effect.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.DataFormat
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.awt.Color
import java.awt.Font
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.floor

enum class NodeType {
    ADD_IMAGE, ADD_TEXT, BRIGHTNESS, FLOAT, INT, GRAY_FILTER, IMAGE, INVERT_FILTER, SEPIA, STRING, MOVE, ROTATE, SCALE,
    BLUR, INPUT_IMAGE, FINAL_IMAGE
}

open class FloatNode(nodeState: DataFormat, linkState: DataFormat,
): EditNode<Float>(nodeState, linkState, Regex("[+-]?([0-9]*[.])?[0-9]+")) {
    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Float"
        type = NodeType.FLOAT
        value = 0.0f
        link.valueProperty.set(value)
        textField.text = "0.0"
    }

    override fun toValue(text: String): Float = text.toFloat()
}

class IntNode(nodeState: DataFormat, linkState: DataFormat,
): EditNode<Int>(nodeState, linkState, Regex("^[+-]?\\d+\$")) {
    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Int"
        type = NodeType.INT
        value = 0
        link.valueProperty.set(value)
        textField.text = "0"
    }

    override fun toValue(text: String): Int = text.toInt()
}

class StringNode(nodeState: DataFormat, linkState: DataFormat,
): EditNode<String>(nodeState, linkState, Regex("^[\\s\\S]*")) {
    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "String"
        type = NodeType.STRING
        value = ""
        link.valueProperty.set(value)
        textField.text = ""
    }

    override fun toValue(text: String): String = text
}

open class ImageNode(nodeState: DataFormat, linkState: DataFormat): ImageNodeBasic(nodeState, linkState) {
    lateinit var output: LinkOutput<BufferedImage?>

    @FXML
    override fun initialize() {
        super.initialize()

        title.text = "Image"
        type = NodeType.IMAGE
        val openButton = Button("Open image")
        openButton.setOnAction {
            val img = importImage()
            valueProperty.set(SwingFXUtils.fromFXImage(img, null))
            imageView.image = img
            children.remove(openButton)
        }

        val sep = Separator()
        sep.opacity = 0.0
        gridPane.add(openButton, 1, 4)
        gridPane.add(sep, 1, 5)

        output = LinkOutput()
        output.onDragDetected = linkDragDetectedHandler
        linkOutputLayout.children.add(output)
    }

    private fun importImage(): Image {
        val fileChooser = FileChooser()

        val extensionFilters = listOf(
            FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"),
            FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.jpg"),
            FileChooser.ExtensionFilter("JPEG files (*.jpeg)", "*.jpeg")
        )
        fileChooser.extensionFilters.addAll(extensionFilters)

        val file = fileChooser.showOpenDialog(scene.window as Stage)
        val importedImage = SwingFXUtils.toFXImage(ImageIO.read(file.inputStream()), null)

        return importedImage
    }
}

class InputImageNode(nodeState: DataFormat, linkState: DataFormat): ImageNode(nodeState, linkState) {
    @FXML
    override fun initialize() {
        super.initialize()
        relocate(100.0, 400.0)
        title.text = "Input Image"
        type = NodeType.INPUT_IMAGE
        gridPane.children.remove(deleteButton)
    }
}

class FinalImageNode(nodeState: DataFormat, linkState: DataFormat): ImageNodeBasic(nodeState, linkState) {
    lateinit var imageInput: LinkInput<BufferedImage?>

    @FXML
    override fun initialize() {
        super.initialize()
        relocate(1200.0, 400.0)
        title.text = "Final Image"
        type = NodeType.FINAL_IMAGE
        gridPane.children.remove(deleteButton)

        imageInput = LinkInput(null, 1)
        imageInput.onDragDropped = linkDragDroppedHandler
        imageInput.valueProperty.addListener {
                _, _, newValue ->
            valueProperty.value = newValue
            imageView.image = SwingFXUtils.toFXImage(newValue, null)
        }
        linkInputLayout.children.add(imageInput)
    }
}

class AddTextNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState) {
    lateinit var xInput: LinkInput<Int?>
    lateinit var yInput: LinkInput<Int?>
    lateinit var textInput: LinkInput<String?>

    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "AddText"
        type = NodeType.ADD_TEXT

        xInput = LinkInput(null, 1)
        yInput = LinkInput(null, 2)
        textInput = LinkInput(null, 3)

        inputs = mapOf(
            Pair("x", xInput),
            Pair("y", yInput),
            Pair("Text", textInput)
        )

        addInputs()
        bindInputs()
    }

    override fun filterFunction(img: Image): Image {
        val bufferedImage = SwingFXUtils.fromFXImage(img, null)
        val font = Font("Arial", Font.BOLD, 100)
        val graphics = bufferedImage.graphics
        graphics.font = font
        graphics.color = Color.BLACK
        graphics.drawString(
            textInput.valueProperty.value!!,
            xInput.valueProperty.value!!,
            yInput.valueProperty.value!!
        )

        return SwingFXUtils.toFXImage(bufferedImage, null)
    }
}

class AddImageNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState) {
    lateinit var xInput: LinkInput<Int?>
    lateinit var yInput: LinkInput<Int?>
    lateinit var addingImageInput: LinkInput<BufferedImage?>

    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Add Image"
        type = NodeType.ADD_IMAGE

        xInput = LinkInput(null, 1)
        yInput = LinkInput(null, 2)
        addingImageInput = LinkInput(null, 3)

        inputs = mapOf(
            Pair("Img2", addingImageInput),
            Pair("x", xInput),
            Pair("y", yInput)
        )

        addInputs()
        bindInputs()
    }

    override fun filterFunction(img: Image): Image {
        val bufferedImage = SwingFXUtils.fromFXImage(img, null)
        val font = Font("Arial", Font.BOLD, 100)
        val graphics = bufferedImage.graphics
        graphics.font = font
        graphics.color = Color.BLACK
        graphics.drawImage(
            addingImageInput.valueProperty.value!!,
            xInput.valueProperty.value!!,
            yInput.valueProperty.value!!,
            null
        )

        return SwingFXUtils.toFXImage(bufferedImage, null)
    }
}

class GrayFilterNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState) {
    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Gray"
        type = NodeType.GRAY_FILTER
        inputs = mapOf()
    }

    override fun filterFunction(img: Image): Image {
        val grayFilter = ColorAdjust()
        grayFilter.saturation = -1.0
        val imageViewGray = ImageView(img)
        imageViewGray.effect = grayFilter

        return snapshotNodeRect(imageViewGray, Rectangle2D(0.0, 0.0, img.width, img.height))
    }
}

class BrightnessNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState) {
    lateinit var brightnessInput: LinkInput<Float?>

    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Brightness"
        type = NodeType.BRIGHTNESS
        brightnessInput = LinkInput(null, 1)

        inputs = mapOf(
            Pair("Level", brightnessInput)
        )
        addInputs()
        bindInputs()
    }

    override fun filterFunction(img: Image): Image {
        val brightFilter = ColorAdjust()
        brightFilter.brightness = brightnessInput.valueProperty.value!!.toDouble()
        val imageV = ImageView(img)
        imageV.effect = brightFilter

        return snapshotNodeRect(imageV, Rectangle2D(0.0, 0.0, img.width, img.height))
    }
}

class BlurFilterNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState) {
    private lateinit var IntensityInput: LinkInput<Int?>

    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Blur"
        type = NodeType.BLUR
        IntensityInput = LinkInput(null, 1)

        inputs = mapOf(
            Pair("Intens", IntensityInput)
        )

        addInputs()
        bindInputs()
    }

    override fun filterFunction(img: Image): Image {
        val color = ColorInput()
        color.paint = javafx.scene.paint.Color.WHITE
        color.width = Double.MAX_VALUE
        color.height = Double.MAX_VALUE

        val blur = GaussianBlur()
        blur.radius = IntensityInput.valueProperty.value!!.toDouble()

        val imageV = ImageView(img)
        imageV.effect = blur

        return snapshotNodeRect(imageV, Rectangle2D(0.0, 0.0, img.width, img.height))
    }
}

class InvertFilterNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState) {
    @FXML
    override fun initialize() {
        super.initialize()
        inputs = mapOf()
        title.text = "InvertFilter"
        type = NodeType.INVERT_FILTER
    }

    override fun filterFunction(img: Image): Image {
        val bufferedImage = SwingFXUtils.fromFXImage(img, null)

        for (x in 0 until bufferedImage.width) {
            for (y in 0 until bufferedImage.height) {
                val rgba = bufferedImage.getRGB(x, y)
                val color = Color(rgba, true)
                val newColor = Color(
                    255 - color.red,
                    255 - color.green,
                    255 - color.blue
                )
                bufferedImage.setRGB(x, y, newColor.rgb)
            }
        }

        return SwingFXUtils.toFXImage(bufferedImage, null)
    }
}

class SepiaNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState) {
    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Sepia"
        type = NodeType.SEPIA
        inputs = mapOf()
    }

    override fun filterFunction(img: Image): Image {
        val sepiaTone = SepiaTone()
        sepiaTone.level = 0.5
        val imageV = ImageView(img)
        imageV.effect = sepiaTone

        return snapshotNodeRect(imageV, Rectangle2D(0.0, 0.0, img.width, img.height))
    }
}

class MoveNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState) {
    lateinit var xInput: LinkInput<Float?>
    lateinit var yInput: LinkInput<Float?>

    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Move"
        type = NodeType.MOVE

        xInput = LinkInput(null, 1)
        yInput = LinkInput(null, 2)

        inputs = mapOf(
            Pair("x", xInput),
            Pair("y", yInput)
        )

        addInputs()
        bindInputs()
    }

    override fun filterFunction(img: Image): Image {
        val moveEffect = DisplacementMap()
        moveEffect.offsetX = xInput.valueProperty.value!!.toDouble()
        moveEffect.offsetY = yInput.valueProperty.value!!.toDouble()
        val imageV = ImageView(img)
        imageV.effect = moveEffect

        return snapshotNodeRect(imageV, Rectangle2D(0.0, 0.0, img.width, img.height))
    }
}

class RotateNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState){
    lateinit var angleInput: LinkInput<Float?>

    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Rotate"
        type = NodeType.ROTATE

        angleInput = LinkInput(null, 1)

        inputs = mapOf(
            Pair("Ang", angleInput)
        )

        addInputs()
        bindInputs()
    }

    override fun filterFunction(img: Image): Image {
        val imageV = ImageView(img)
        imageV.rotate = Math.toDegrees(angleInput.valueProperty.value!!.toDouble())

        return snapshotNodeRect(imageV, Rectangle2D(imageV.boundsInParent.minX, imageV.boundsInParent.minY,
            imageV.boundsInParent.width, imageV.boundsInParent.height))
    }
}

class ScaleNode(nodeState: DataFormat, linkState: DataFormat): FilterNode(nodeState, linkState) {
    lateinit var xInput: LinkInput<Float?>
    lateinit var yInput: LinkInput<Float?>

    @FXML
    override fun initialize() {
        super.initialize()
        title.text = "Scale"
        type = NodeType.SCALE

        xInput = LinkInput(null, 1)
        yInput = LinkInput(null, 2)

        inputs = mapOf(
            Pair("x", xInput),
            Pair("y", yInput)
        )

        addInputs()
        bindInputs()
    }

    override fun filterFunction(img: Image): Image {
        val bufferedImage = SwingFXUtils.fromFXImage(img, null)
        val xScale = xInput.valueProperty.value!!
        val yScale = yInput.valueProperty.value!!

        var scaledImage = BufferedImage(
            floor(bufferedImage.width * xScale).toInt(),
            floor(bufferedImage.height * yScale).toInt(),
            BufferedImage.TYPE_INT_ARGB
        )

        val affineTransform = AffineTransform.getScaleInstance(xScale.toDouble(), yScale.toDouble())
        val scaleOp = AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BICUBIC)
        scaledImage = scaleOp.filter(bufferedImage, scaledImage)

        return SwingFXUtils.toFXImage(scaledImage, null)
    }
}