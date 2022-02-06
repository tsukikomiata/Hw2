package com.example.imageeditor

import javafx.beans.binding.Bindings
import javafx.beans.binding.When
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Point2D
import javafx.scene.layout.AnchorPane
import javafx.scene.shape.CubicCurve

class NodeLink<T>(private val source: DraggableNode<T>) : AnchorPane() {
    @FXML
    lateinit var link: CubicCurve

    private val offsetX = SimpleDoubleProperty()
    private val offsetY = SimpleDoubleProperty()
    private val offsetDirX1 = SimpleDoubleProperty()
    private val offsetDirX2 = SimpleDoubleProperty()
    private val offsetDirY1 = SimpleDoubleProperty()
    private val offsetDirY2 = SimpleDoubleProperty()
    val valueProperty = SimpleObjectProperty<T>()

    var isConnected: Boolean = false

    var destination: LinkInput<T>? = null

    @FXML
    fun initialize() {
        offsetX.set(100.0)
        offsetY.set(50.0)

        valueProperty.addListener {
                _, _, newValue ->
            destination?.valueProperty?.set(newValue)
        }

        offsetDirX1.bind(
            When(link.startXProperty().greaterThan(link.endXProperty())).then(-1.0).otherwise(1.0)
        )

        offsetDirX2.bind(
            When(link.startXProperty().greaterThan(link.endXProperty())).then(1.0).otherwise(-1.0)
        )

        link.controlX1Property().bind(Bindings.add(link.startXProperty(), offsetX.multiply(offsetDirX1)))
        link.controlX2Property().bind(Bindings.add(link.endXProperty(), offsetX.multiply(offsetDirX2)))
        link.controlY1Property().bind(Bindings.add(link.startYProperty(), offsetY.multiply(offsetDirY1)))
        link.controlY2Property().bind(Bindings.add(link.endYProperty(), offsetY.multiply(offsetDirY2)))
    }

    fun setStart(point: Point2D) {
        link.startX = point.x
        link.startY = point.y
    }

    fun setEnd(point: Point2D) {
        link.endX = point.x
        link.endY = point.y
    }

    fun <T> bindStart(source: LinkOutput<T>) {
        bindLayoutProperty(source, link.startXProperty(), link.startYProperty())
    }

    fun <T> bindEnd(source: LinkInput<T>) {
        bindLayoutProperty(source, link.endXProperty(), link.endYProperty())
    }

    fun unbindEnd() {
        link.endXProperty().unbind()
        link.endYProperty().unbind()
        setEnd(Point2D(link.startX, link.startY))
    }

    fun bindLayoutProperty(source: AnchorPane, propertyX: DoubleProperty, propertyY: DoubleProperty) {
        val currentBindingX =
            Bindings.add(source.layoutXProperty(), source.prefWidth / 2.0)
                .add(source.parent.layoutXProperty())
                .add(source.parent.parent.layoutXProperty())
                .add(source.parent.parent.parent.layoutXProperty())

        val currentBindingY =
            Bindings.add(source.layoutYProperty(), source.prefHeight / 2.0)
                .add(source.parent.layoutYProperty())
                .add(source.parent.parent.layoutYProperty())
                .add(source.parent.parent.parent.layoutYProperty())

        propertyX.bind(currentBindingX)
        propertyY.bind(currentBindingY)
    }

    init {
        val fxmlLoader = FXMLLoader(
            javaClass.getResource("NodeLink.fxml")
        )
        fxmlLoader.setRoot(this)
        fxmlLoader.setController(this)
        fxmlLoader.load<Any>()
    }
}