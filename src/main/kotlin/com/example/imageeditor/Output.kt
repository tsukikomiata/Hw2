package com.example.imageeditor

import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle

class LinkOutput<T>: AnchorPane() {
    init {
        setPrefSize(20.0, 20.0)
        val circle = Circle(10.0, Color.BLUE)
        circle.translateX = 25.0
        circle.translateY = 10.0
        children.add(circle)
    }
}