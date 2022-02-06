package com.example.imageeditor

import com.google.gson.JsonObject
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Point2D
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import java.awt.image.BufferedImage

open class DraggableNode<T>(private val nodeState: DataFormat, private val linkState: DataFormat) : AnchorPane() {
    @FXML
    lateinit var gridPane: GridPane

    @FXML
    lateinit var title: Label

    @FXML
    lateinit var linkInputLayout: VBox

    @FXML
    lateinit var linkOutputLayout: VBox

    @FXML
    lateinit var deleteButton: Button

    @FXML
    lateinit var contentContainer: Pane

    lateinit var inputs: Map<String, LinkInput<*>>

    var loader = FXMLLoader(HelloApplication::class.java.getResource("NodeBase.fxml"))
    var id: Int = -1
    private var offset = Point2D(0.0, 0.0)
    private var superParent: AnchorPane? = null
    var link = NodeLink(this)
    var value: T? = null
    val connectedLinks = mutableListOf<NodeLink<T>>()
    lateinit var type: NodeType

    private val dragDetectedHandler
        get() = EventHandler<MouseEvent> { event ->
            parent.onDragOver = dragOverHandler
            parent.onDragDropped = dragDroppedHandler

            offset = Point2D(event.x, event.y)

            val p = Point2D(event.sceneX, event.sceneY)
            val localCoords = parent.sceneToLocal(p)

            relocate((localCoords.x), (localCoords.y))

            val content = ClipboardContent()
            content[nodeState] = 1
            startDragAndDrop(*TransferMode.ANY).setContent(content)
            event.consume()
        }

    private val dragOverHandler = EventHandler<DragEvent> { event ->
        val p = Point2D(event.sceneX, event.sceneY)
        val localCoords = parent.sceneToLocal(p)

        relocate((localCoords.x - offset.x), (localCoords.y - offset.y))
        event.consume()
    }

    private val dragDroppedHandler = EventHandler<DragEvent> { event ->
        parent.onDragOver = null
        parent.onDragDropped = null
        event.isDropCompleted = true
        event.consume()
    }

    private val contextLinkDragOverHandler = EventHandler<DragEvent> { event ->
        event.acceptTransferModes(*TransferMode.ANY)
        if (!link.isVisible)
            link.isVisible = true
        link.setEnd(Point2D(event.x, event.y))
        event.consume()
    }

    private val contextLinkDragDroppedHandler = EventHandler<DragEvent> { event ->
        parent.onDragDropped = null
        parent.onDragOver = null
        link.isVisible = false

        superParent!!.children.removeAt(0)
        event.isDropCompleted = true
        event.consume()
    }

    val linkDragDetectedHandler = EventHandler<MouseEvent> { event ->
        if (!link.isConnected) {
            parent.onDragOver = contextLinkDragOverHandler
            parent.onDragDropped = contextLinkDragDroppedHandler

            link.isVisible = true
            link.bindStart(event.source as LinkOutput<*>)

            superParent!!.children.add(0, link)
            val content = ClipboardContent()
            content[linkState] = "link"
            startDragAndDrop(*TransferMode.ANY).setContent(content)
            event.consume()
        }
    }

    val linkDragDroppedHandler = EventHandler<DragEvent> { event ->
        parent.onDragOver = null
        parent.onDragDropped = null

        val linkDestination = event.source as LinkInput<T>
        val linkSource = event.gestureSource as DraggableNode<T>
        val connectedLink = linkSource.link


        if (connectedLink.valueProperty::class == linkDestination.valueProperty::class
            && !linkDestination.isConnected
            && linkSource != this
        ) {
            connectedLink.bindEnd(linkDestination)
            connectedLink.isConnected = true
            connectedLink.destination = linkDestination
            linkDestination.valueProperty.set(connectedLink.valueProperty.value)
            linkDestination.connectedLink = connectedLink
            connectedLinks.add(connectedLink)

            val content = ClipboardContent()

            content[linkState] = "link"
            startDragAndDrop(*TransferMode.ANY).setContent(content)
        } else {
            parent.onDragDropped = null
            parent.onDragOver = null
            connectedLink.isVisible = false
            linkSource.superParent!!.children.removeAt(0)
            event.isDropCompleted = true
        }
        event.consume()
    }

    fun addTarget(source: LinkOutput<T>, destination: LinkInput<T>) {
        link.isVisible = true
        link.bindStart(source)
        superParent!!.children.add(0, link)

        link.bindEnd(destination)
        link.isConnected = true
        link.destination = destination

        destination.valueProperty.set(link.valueProperty.value)
        destination.connectedLink = link
        connectedLinks.add(link)
    }

    fun removeLink(link: NodeLink<T>) {
        superParent!!.children.remove(link)
        link.isConnected = false
        link.unbindEnd()
        link.destination?.connectedLink = null
        link.destination?.valueProperty?.set(link.destination?.defaultValue)
        link.destination = null
    }

    fun serialize(): JsonObject {
        val result = JsonObject()

        result.addProperty("x", layoutX)
        result.addProperty("y", layoutY)
        result.addProperty("id", id)
        result.addProperty("type", type.toString())

        if (value is BufferedImage?) {
            if (value != null) {
                val image = SwingFXUtils.toFXImage(value as BufferedImage, null)
                val imageArray = writeImageToArray(image)
                var imageArrayToString = imageArray.toString()
                imageArrayToString = imageArrayToString.substring(1, imageArrayToString.lastIndex - 1)
                result.addProperty("value", imageArrayToString)
            } else result.addProperty("value", "null")
        } else if (value is Int || value is String || value is Float) {
            result.addProperty("value", value.toString())
        }

        if (link.isConnected) {
            val destinationInputId = link.destination!!.id
            val destinationNodeId = (link.destination!!.parent.parent.parent as DraggableNode<*>).id
            result.addProperty("connected", "true")
            result.addProperty("destinationNodeId", destinationNodeId)
            result.addProperty("destinationId", destinationInputId)
        }

        return result
    }

    fun writeImageToArray(image: Image): MutableList<Int> {
        val imageArray = mutableListOf<Int>()
        val width = image.width.toInt()
        val height = image.height.toInt()

        imageArray.add(width)
        imageArray.add(height)
        val reader = image.pixelReader;

        for (i in 0 until width) {
            for (j in 0 until height) {
                imageArray.add(reader.getArgb(i, j))
            }
        }

        return imageArray
    }

    fun delete() {
        (parent as AnchorPane).children.remove(this)
        for (link: NodeLink<T> in connectedLinks) {
            removeLink(link)
        }
        removeLink(link)
    }

    @FXML
    open fun initialize() {
        relocate(100.0, 100.0)
    }

    init {
        onDragDetected = dragDetectedHandler
        loader.setController(this)
        children.add(loader.load())

        deleteButton.setOnAction {
            (parent as AnchorPane).children.remove(this)
            for (link: NodeLink<T> in connectedLinks) {
                removeLink(link)
            }
            removeLink(link) }

        parentProperty().addListener { _, _, _ ->
            parent?.let {
                superParent = parent as AnchorPane
            }
        }

        link.setOnMouseClicked {
            removeLink(link)
        }
    }
}