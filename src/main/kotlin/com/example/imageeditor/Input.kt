package com.example.imageeditor

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle

class LinkInput<T>(initialValue: T?, id: Int): AnchorPane() {
    val id = id
    val valueProperty = SimpleObjectProperty(initialValue)
    var defaultValue: T? = initialValue
    var connectedLink: NodeLink<T>? = null
    val isConnected: Boolean
        get() = connectedLink != null

    init {
        setPrefSize(20.0, 20.0)
        val circle = Circle(10.0, Color.ORANGE)
        circle.translateY = 10.0
        children.add(circle)
    }
}