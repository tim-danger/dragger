package com.dangersoft.dragger;

import java.io.IOException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;

public class NodeLabel extends AnchorPane {

    @FXML
    Label node_label;

    private static int labelCounter = 0;

    public NodeLabel() {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/NodeLabel.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();

        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    private void initialize() {

    }

    public void setLabel(Slider slider, DraggableNode node) {
        DoubleProperty delta = new SimpleDoubleProperty(0.0);
        delta.bind(Bindings.add(Bindings.add(node.layoutYProperty(), node.prefWidthProperty()), Bindings.multiply(slider
                .valueProperty(), 5.0 / 100.0)));

        // das Label an die X- Ausrichtung binden
        node_label.layoutXProperty().set(node.layoutXProperty().get());
        // das Label an die Y- Ausrichtung binden
        node_label.layoutYProperty().set(delta.get());
        node_label.setText(node_label.getText() + " " + ++labelCounter);
    }

    /**
     * Das Binding verliert sich leider beim Drag. Keine Ahnung, warum
     * 
     * @param slider
     * @param node
     */
    public void bindLabel(Slider slider, DraggableNode node) {

        // TODO : Labels manuell verschieben, das Vorgehen funktioniert leider
        // nicht. Die Engine uebernimmt irgendwann die Kontrolle und die Labels
        // werden in Y- Richtung nicht mehr richtig angepasst
        final DoubleProperty delta = new SimpleDoubleProperty(0.0);
        delta.bind(Bindings.add(Bindings.add(node.layoutYProperty(), node.heightProperty()), Bindings.multiply(slider
                .valueProperty(), 5.0 / 100.0)));

        // das Label an die X- Ausrichtung binden
        this.layoutXProperty().bindBidirectional(node.layoutXProperty());
        // das Label an die Y- Ausrichtung binden
        this.layoutYProperty().bindBidirectional(delta);
        node_label.setText(node_label.getText() + " " + ++labelCounter);
    }

}
