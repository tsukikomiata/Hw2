package com.example.imageeditor

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.image.WritableImage
import javafx.scene.input.DataFormat
import javafx.scene.layout.AnchorPane
import javafx.stage.FileChooser
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class HelloController {
    private var curId = 0
    private val nodesOnScene = mutableListOf<DraggableNode<*>>()

    @FXML
    private lateinit var finalImage: FinalImageNode

    @FXML
    private lateinit var inputImage: InputImageNode

    @FXML
    lateinit var anchorPane: AnchorPane

    fun saveScene() {
        var nodesOnStage = mutableListOf<DraggableNode<*>>()
        anchorPane.children.forEach {
            if (it is DraggableNode<*>) {
                nodesOnStage.add(it)
            }
        }

        val nodesToJson = mutableListOf<JsonObject>()
        nodesOnStage.forEach {
            nodesToJson.add(it.serialize())
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(nodesToJson)

        val extensionFilter = FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(extensionFilter)

        var file = fileChooser.showSaveDialog(null)

        if (file.nameWithoutExtension == file.name) {
            file = File(file.parentFile, file.nameWithoutExtension + ".json")
        }
        file.writeText(json)
    }

    fun loadScene() {
        val extensionFilter = FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(extensionFilter)

        val file = fileChooser.showOpenDialog(null)

        if (file != null) {
            val jsonString = file.readText()
            val gson = GsonBuilder().create()
            val loadedNodes = gson.fromJson(jsonString, mutableListOf<Map<*, *>>()::class.java)

            clearScene()
            val createdNodes = mutableListOf<DraggableNode<*>>()

            // Добавление загруженных нод на сцену без связей
            loadedNodes.forEach {
                val type = it["type"] as String
                curId = (it["id"] as Double).toInt()
                createNode(type)
                val createdNode = anchorPane.children.last() as DraggableNode<*>
                createdNodes.add(createdNode)

                val x = it["x"] as Double
                val y = it["y"] as Double
                createdNode.relocate(x, y)

                if ((type == "INPUT_IMAGE" || type == "IMAGE") && (it["value"] != "null")) {
                    val value = it["value"] as String
                    val image = readImgFromByteArray(value)
                    (createdNode as ImageNode).imageView.image = image
                    createdNode.valueProperty.set(SwingFXUtils.fromFXImage(image, null))
                } else if ((type == "FLOAT" || type == "INT" || type == "STRING") && (it["value"] != "null")) {
                    val value = it["value"] as String
                    (createdNode as EditNode<*>).textField.text = value
                }
            }

            // Проход по нодам еще раз для добавления связей
            loadedNodes.forEach {
                if (it["connected"] == "true") {
                    val node = findNodeById(createdNodes, (it["id"] as Double).toInt())
                    val destinationNode = findNodeById(createdNodes, (it["destinationNodeId"] as Double).toInt())

                    if (node != null && destinationNode != null) {
                        var linkInput = findLinkInputById(destinationNode, (it["destinationId"] as Double).toInt())

                        when (node.type) {
                            NodeType.SCALE -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as ScaleNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.ADD_IMAGE -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as AddImageNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.ADD_TEXT -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as AddTextNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.BRIGHTNESS -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as BrightnessNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.FLOAT -> {
                                linkInput = linkInput as LinkInput<Float>
                                (node as FloatNode).addTarget(node.output, linkInput)
                            }
                            NodeType.INT -> {
                                linkInput = linkInput as LinkInput<Int>
                                (node as IntNode).addTarget(node.output, linkInput)
                            }
                            NodeType.GRAY_FILTER -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as GrayFilterNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.IMAGE -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as ImageNode).addTarget(node.output, linkInput)
                            }
                            NodeType.INVERT_FILTER -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as InvertFilterNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.SEPIA -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as SepiaNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.STRING -> {
                                linkInput = linkInput as LinkInput<String>
                                (node as StringNode).addTarget(node.output, linkInput)
                            }
                            NodeType.MOVE -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as MoveNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.ROTATE -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as RotateNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.BLUR -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as BlurFilterNode).addTarget(node.imageOutput, linkInput)
                            }
                            NodeType.INPUT_IMAGE -> {
                                linkInput = linkInput as LinkInput<BufferedImage?>
                                (node as InputImageNode).addTarget(node.output, linkInput)
                            }
                            else -> {

                            }
                        }
                    }
                }
            }
        }
    }

    fun findNodeById(nodes: MutableList<DraggableNode<*>>, id: Int): DraggableNode<*>? {
        nodes.forEach {
            if (it.id == id)
                return it
        }
        return null
    }

    fun findLinkInputById(node: DraggableNode<*>, inputId: Int): LinkInput<*>? {
        node.linkInputLayout.children.forEach {
            if (it is LinkInput<*>)
                if ((it as LinkInput<*>).id == inputId)
                    return it
        }
        return null
    }

    fun readImgFromByteArray(byteString: String): WritableImage {
        val byteArray = byteString.split(", ")
        val width = byteArray[0].toInt()
        val height = byteArray[1].toInt()
        var iterator = 1

        val image = WritableImage(width, height)
        val writer = image.pixelWriter;

        for (i in 0 until width) {
            for (j in 0 until height) {
                iterator += 1
                writer.setArgb(i, j, byteArray[iterator].toInt())
            }
        }

        return image
    }

    @FXML
    fun initialize() {
        addInputImageNode()
        addFinalImageNode()
    }

    fun clearScene() {
        nodesOnScene.clear()
        anchorPane.children.forEach {
            if (it is DraggableNode<*>)
                nodesOnScene.add(it)
        }

        nodesOnScene.forEach {
            it.delete()
        }
    }

    private val nodeState = DataFormat("nodeState")
    private val linkState = DataFormat("linkState")

    private fun addInputImageNode() {
        inputImage = InputImageNode(nodeState, linkState)
        inputImage.id = curId++
        anchorPane.children.add(inputImage)
    }

    private fun addFinalImageNode() {
        finalImage = FinalImageNode(nodeState, linkState)
        finalImage.id = curId++
        anchorPane.children.add(finalImage)
    }

    @FXML
    private fun addImageNode() {
        val imageNode = ImageNode(nodeState, linkState)
        imageNode.id = curId++
        anchorPane.children.add(imageNode)
    }

    @FXML
    private fun addAddImageNode() {
        val addImageNode = AddImageNode(nodeState, linkState)
        addImageNode.id = curId++
        anchorPane.children.add(addImageNode)
    }

    @FXML
    private fun addSepiaNode() {
        val sepiaNode = SepiaNode(nodeState, linkState)
        sepiaNode.id = curId++
        anchorPane.children.add(sepiaNode)
    }

    @FXML
    private fun addGrayFilterNode() {
        val grayFilterNode = GrayFilterNode(nodeState, linkState)
        grayFilterNode.id = curId++
        anchorPane.children.add(grayFilterNode)
    }

    @FXML
    private fun addIntNode() {
        val intNode = IntNode(nodeState, linkState)
        intNode.id = curId++
        anchorPane.children.add(intNode)
    }

    @FXML
    private fun addAddTextNode() {
        val addTextNode = AddTextNode(nodeState, linkState)
        addTextNode.id = curId++
        anchorPane.children.add(addTextNode)
    }

    @FXML
    private fun addBlurNode() {
        val blurFilterNode = BlurFilterNode(nodeState, linkState)
        blurFilterNode.id = curId++
        anchorPane.children.add(blurFilterNode)
    }

    @FXML
    private fun addBrightnessNode() {
        val brightnessNode = BrightnessNode(nodeState, linkState)
        brightnessNode.id = curId++
        anchorPane.children.add(brightnessNode)
    }

    @FXML
    private fun addFloatNode() {
        val floatNode = FloatNode(nodeState, linkState)
        floatNode.id = curId++
        anchorPane.children.add(floatNode)
    }

    @FXML
    private fun addInvertFilterNode() {
        val invertFilterNode = InvertFilterNode(nodeState, linkState)
        invertFilterNode.id = curId++
        anchorPane.children.add(invertFilterNode)
    }

    @FXML
    private fun addStringNode() {
        val stringNode = StringNode(nodeState, linkState)
        stringNode.id = curId++
        anchorPane.children.add(stringNode)
    }

    @FXML
    private fun addMoveNode() {
        val MoveNode = MoveNode(nodeState, linkState)
        MoveNode.id = curId++
        anchorPane.children.add(MoveNode)
    }

    @FXML
    private fun addRotateNode() {
        val RotateNode = RotateNode(nodeState, linkState)
        RotateNode.id = curId++
        anchorPane.children.add(RotateNode)
    }

    @FXML
    private fun addScaleNode() {
        val ScaleNode = ScaleNode(nodeState, linkState)
        ScaleNode.id = curId++
        anchorPane.children.add(ScaleNode)
    }

    fun createNode(type: String) {
        when (type) {
            "SCALE" -> addScaleNode()
            "ADD_IMAGE" -> addAddImageNode()
            "ADD_TEXT" -> addAddTextNode()
            "BRIGHTNESS" -> addBrightnessNode()
            "FLOAT" -> addFloatNode()
            "INT" -> addIntNode()
            "GRAY_FILTER" -> addGrayFilterNode()
            "IMAGE" -> addImageNode()
            "INVERT_FILTER" -> addInvertFilterNode()
            "SEPIA" -> addSepiaNode()
            "STRING" -> addStringNode()
            "MOVE" -> addMoveNode()
            "ROTATE" -> addRotateNode()
            "BLUR" -> addBlurNode()
            "INPUT_IMAGE" -> addInputImageNode()
            "FINAL_IMAGE" -> addFinalImageNode()
            else -> print("Unknown type")
        }
    }

    @FXML
    private fun saveImage() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("Png image *.png", "*.png"))
        var file = fileChooser.showSaveDialog(null)

        if (file.nameWithoutExtension == file.name) {
            file = File(file.parentFile, file.nameWithoutExtension + ".png")
        }

        try {
            ImageIO.write(finalImage.valueProperty.value!!, "png", file)
        } catch (ex: IOException) {
            print(ex)
        }
    }
}
