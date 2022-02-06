module com.example.nodes {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires com.google.gson;
    requires javafx.swing;


    opens com.example.imageeditor to javafx.fxml;
    exports com.example.imageeditor;
}