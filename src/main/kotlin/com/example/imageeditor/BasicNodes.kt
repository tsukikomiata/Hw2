package com.example.imageeditor

import javafx.beans.property.SimpleObjectProperty
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.SnapshotParameters
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.DataFormat
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Paint
import java.awt.image.BufferedImage

abstract class EditNode<T>(nodeState: DataFormat, linkState: DataFormat, private val validatorRegex: Regex
): DraggableNode<T>(nodeState, linkState) {
    lateinit var textField: TextField
    lateinit var output: LinkOutput<T>

    @FXML
    override fun initialize() {
        super.initialize()
        output = LinkOutput()
        output.onDragDetected = linkDragDetectedHandler
        linkOutputLayout.children.add(output)

        textField = TextField()
        textField.minWidth = contentContainer.prefWidth
        contentContainer.children.add(textField)

        textField.textProperty().addListener {
                _, _, new ->
            validatorRegex ?: return@addListener
            if (!new.matches(validatorRegex)) {
                output.onDragDetected = null
                textField.border = Border(
                    BorderStroke(
                        Paint.valueOf("FF0000"),
                        BorderStrokeStyle.SOLID,
                        CornerRadii(5.0),
                        BorderStroke.DEFAULT_WIDTHS)
                )
            }
            else {
                output.onDragDetected = linkDragDetectedHandler
                value = toValue(textField.text)
                link.valueProperty.set(value)
                textField.border = Border(
                    BorderStroke(
                        Paint.valueOf("000000"),
                        BorderStrokeStyle.SOLID,
                        CornerRadii(5.0),
                        BorderStroke.DEFAULT_WIDTHS)
                )
            }
        }
    }

    abstract fun toValue(text: String): T
}

abstract class ImageNodeBasic(nodeState: DataFormat, linkState: DataFormat
): DraggableNode<BufferedImage?>(nodeState, linkState) {
    lateinit var valueProperty: SimpleObjectProperty<BufferedImage?>
    lateinit var imageView: ImageView

    @FXML
    override fun initialize() {
        super.initialize()
        valueProperty = SimpleObjectProperty()

        valueProperty.addListener { _, _, newValue ->
            link.valueProperty.set(newValue)
            value = newValue
        }

        imageView = ImageView()
        imageView.fitWidthProperty().bind(contentContainer.widthProperty())
        imageView.fitHeightProperty().bind(contentContainer.heightProperty())
        imageView.setPreserveRatio(true)
        contentContainer.children.add(imageView)
    }
}

abstract class FilterNode(nodeState: DataFormat, linkState: DataFormat): ImageNodeBasic(nodeState, linkState) {
    lateinit var imageInput: LinkInput<BufferedImage?>
    lateinit var imageOutput: LinkOutput<BufferedImage?>

    @FXML
    override fun initialize() {
        super.initialize()
        contentContainer.prefWidth = 0.0
        linkOutputLayout.prefWidth = 120.0
        linkInputLayout.prefWidth = 120.0

        imageInput = LinkInput(null, 0)
        imageInput.valueProperty.addListener {
                _, _, newValue ->
            val filteredImage = filterImage(SwingFXUtils.toFXImage(newValue, null))
            valueProperty.value = SwingFXUtils.fromFXImage(filteredImage, null)
            link.valueProperty.value = SwingFXUtils.fromFXImage(filteredImage, null)
            imageView.image = filteredImage
        }
        imageInput.onDragDropped = linkDragDroppedHandler
        linkInputLayout.children.add(imageInput)

        imageOutput = LinkOutput()
        imageOutput.onDragDetected = linkDragDetectedHandler
        linkOutputLayout.children.add(imageOutput)
    }

    protected fun bindInputs() {
        for(input in inputs) {
            input.value.onDragDropped = linkDragDroppedHandler
            input.value.valueProperty.addListener {
                    _, _, _ ->
                val filteredImage = filterImage(SwingFXUtils.toFXImage(imageInput.valueProperty.value, null))
                valueProperty.value = SwingFXUtils.fromFXImage(filteredImage, null)
                link.valueProperty.value = SwingFXUtils.fromFXImage(filteredImage, null)
                imageView.image = filteredImage
            }
        }
    }

    protected fun addInputs() {
        inputs.forEach { entry ->
            linkInputLayout.children.add(Label(entry.key))
            linkInputLayout.spacing = 0.0
            linkInputLayout.children.add(entry.value)
        }
    }

    open fun filterImage(img: Image?): Image? {
        for (input in inputs) {
            if (input.value.valueProperty.value == null) return null
        }
        if (img == null) return null
        return filterFunction(img)
    }

    abstract fun filterFunction(img: Image): Image

    fun snapshotNodeRect(node: Node, rect: Rectangle2D): Image {
        val snapshotParams = SnapshotParameters()
        snapshotParams.viewport = rect

        val outImage = WritableImage(snapshotParams.viewport.width.toInt(), snapshotParams.viewport.height.toInt())
        node.snapshot(snapshotParams, outImage)

        return outImage
    }
}

