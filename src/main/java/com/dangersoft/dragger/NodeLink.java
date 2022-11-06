package com.dangersoft.dragger;

import java.io.IOException;
import java.util.UUID;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

/**
 * 
 * @author Tim Erdmannsdoerfer
 *
 */
public class NodeLink extends AnchorPane {

    @FXML
    Line node_link;

    private static final double arrowWidth = 8;
    private static final double arrowLength = 15;
    private SimpleDoubleProperty scaleFactor = new SimpleDoubleProperty(100.0);

    // brauchen wir, damit wir den Listener installieren können
    private Polygon arrowHead = new Polygon();

    private DraggableNode outGoingNode;
    private DraggableNode inGoingNode;

    public DraggableNode getOutGoingNode() {
        return outGoingNode;
    }

    public DraggableNode getInGoingNode() {
        return inGoingNode;
    }

    public NodeLink() {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/NodeLink.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();

        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        setId(UUID.randomUUID().toString());

        getChildren().addAll(arrowHead);

        InvalidationListener updater = o -> {
            double ex = getEndX();
            double ey = getEndY();
            double sx = getStartX();
            double sy = getStartY();

            if (ex == sx && ey == sy) {

            } else {

                double factor = scaleFactor.get() * arrowLength / Math.hypot(sx - ex, sy - ey);
                double factorO = scaleFactor.get() * arrowWidth / Math.hypot(sx - ex, sy - ey);

                // part in direction of main line
                double dx = (sx - ex) * factor;
                double dy = (sy - ey) * factor;

                // part ortogonal to main line
                double ox = (sx - ex) * factorO;
                double oy = (sy - ey) * factorO;

                arrowHead.getPoints().clear();
                arrowHead.getPoints().addAll(new Double[] { ex, ey, ex + dx - oy, ey + dy + ox, ex + dx + oy, ey + dy
                        - ox });

            }
        };

        // add updater to properties
        startXProperty().addListener(updater);
        startYProperty().addListener(updater);
        endXProperty().addListener(updater);
        endYProperty().addListener(updater);
        node_link.strokeWidthProperty().addListener(updater);
        updater.invalidated(null);
    }

    public NodeLink(Slider slider, RootLayout layout) {
        this();
        // aendert sich der Wert des Sliders, dann soll sich die Breite der
        // Linie
        // ebenfalls mitaendern
        scaleFactor.bind(Bindings.divide(slider.valueProperty(), 100.0));
        node_link.strokeWidthProperty().bind(Bindings.divide(slider.valueProperty(), 100.0));
    }

    /**
     * Problem : Click-Events werden zwar richtig installiert (ueber Sysout
     * geprueft), feuern dann aber fuer den falschen Knoten. Daher werden die
     * falschen Pfeile selektiert. Dieses Verhalten ist sehr problematisch und
     * vermutlich ein erneuter JavaFX- Bug. Aergerlicherweise muesste man
     * vermutlich den Listener einfach ausbauen und Pfeile bspw. ueber die Tab-
     * Taste von einem Knoten aus ans
     * 
     * @param node
     * @param layout
     * @param sourceID
     * @param targetID
     */
    @Deprecated
    public void installClickListener(NodeLink node, RootLayout layout, String sourceID, String targetID) {

        node.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent e) {

                // die Funktion stoesst eine Callback- Methode an
                layout.changeSelection(sourceID, targetID);

            }
        });
    }

    @FXML
    private void initialize() {

    }

    public void setStart(Point2D startPoint) {

        node_link.setStartX(startPoint.getX());
        node_link.setStartY(startPoint.getY());
    }

    public void setEnd(Point2D endPoint) {

        node_link.setEndX(endPoint.getX());
        node_link.setEndY(endPoint.getY());
    }

    public void bindEnds(DraggableNode source, DraggableNode target) {
        node_link.startXProperty().bind(Bindings.add(source.layoutXProperty(), Bindings.divide(source
                .prefWidthProperty(), 2.0)));

        node_link.startYProperty().bind(Bindings.add(source.layoutYProperty(), Bindings.divide(source
                .prefWidthProperty(), 2.0)));

        // Abstand der y- Koordinaten
        DoubleBinding deltaY = (DoubleBinding) Bindings.add(target.layoutYProperty(), Bindings.divide(target
                .prefWidthProperty(), 2.0)).subtract(Bindings.add(source.layoutYProperty(), Bindings.divide(source
                        .prefWidthProperty(), 2.0)));

        // Abstand der x- Koordinaten
        DoubleBinding deltaX = (DoubleBinding) Bindings.add(target.layoutXProperty(), Bindings.divide(target
                .prefWidthProperty(), 2.0)).subtract(Bindings.add(source.layoutXProperty(), Bindings.divide(source
                        .prefWidthProperty(), 2.0)));

        // Ausrichtung der x- Koordinaten
        DoubleBinding direction = Bindings.when(deltaX.greaterThanOrEqualTo(0.0D)).then(1.0D).otherwise(-1.0D);

        // Steigung (fuer die Winkelberechnung noetig)
        DoubleBinding steigung = (DoubleBinding) Bindings.divide(deltaY, deltaX);

        // Absolutbetrag der Steigung
        DoubleBinding betragSteigung = abs(steigung);

        DoubleBinding Alpha = atan(steigung);
        DoubleBinding cos = cos(Alpha);
        DoubleBinding sin = sin(Alpha);

        DoubleBinding Radius = Bindings.add(Bindings.divide(target.prefWidthProperty(), 2.0), 0.0D);

        // handelt es sich um ein Viereck, muessen der Winkel und der Radius
        // angepasst werden
        if (!target.isCircle()) {
            DoubleBinding AlphaAbs = abs(Alpha);
            Alpha = (DoubleBinding) Bindings.when(betragSteigung.greaterThan(1.0D)).then(Bindings.subtract(Math.PI / 2,
                    AlphaAbs)).otherwise(Alpha);
            Radius = (DoubleBinding) Bindings.divide(Radius, cos(Alpha));
        }

        // dieser Ansatz geht auf die preferred width statt auf die width. Diese
        // wird
        // aber auch durch den Schieberegler veraendert
        node_link.endXProperty().bind(Bindings.add(target.layoutXProperty(), Bindings.divide(target.prefWidthProperty(),
                2.0)).subtract(Bindings.multiply(direction, Bindings.multiply(cos, Radius))));

        node_link.endYProperty().bind(Bindings.add(target.layoutYProperty(), Bindings.divide(target.prefWidthProperty(),
                2.0)).subtract(Bindings.multiply(direction, Bindings.multiply(sin, Radius))));

        // Source und Target setzen
        outGoingNode = target;
        inGoingNode = source;
        target.addToInGoingNodes(inGoingNode);
        source.addToOutGoingNodes(outGoingNode);

    }

    public void change2Default() {
        node_link.setStroke(Color.BLACK);
        arrowHead.setFill(Color.BLACK);
    }

    public void change2Marked() {
        node_link.setStroke(Color.GREEN);
        arrowHead.setFill(Color.GREEN);
    }

    // start/end properties

    public final void setStartX(double value) {
        node_link.setStartX(value);
    }

    public final double getStartX() {
        return node_link.getStartX();
    }

    public final DoubleProperty startXProperty() {
        return node_link.startXProperty();
    }

    public final void setStartY(double value) {
        node_link.setStartY(value);
    }

    public final double getStartY() {
        return node_link.getStartY();
    }

    public final DoubleProperty startYProperty() {
        return node_link.startYProperty();
    }

    public final void setEndX(double value) {
        node_link.setEndX(value);
    }

    public final double getEndX() {
        return node_link.getEndX();
    }

    public final DoubleProperty endXProperty() {
        return node_link.endXProperty();
    }

    public final void setEndY(double value) {
        node_link.setEndY(value);
    }

    public final double getEndY() {
        return node_link.getEndY();
    }

    public final DoubleProperty endYProperty() {
        return node_link.endYProperty();
    }

    /**
     * Binding for {@link java.lang.Math#atan(double)}
     *
     * @param a
     *            the value whose arc tangent is to be returned.
     * @return the arc tangent of the argument.
     */
    public DoubleBinding atan(final ObservableDoubleValue a) {
        return Bindings.createDoubleBinding(() -> Math.atan(a.get()), a);
    }

    /**
     * Binding for {@link java.lang.Math#cos(double)}
     *
     * @param a
     *            an angle, in radians.
     * @return the cosine of the argument.
     */
    public DoubleBinding cos(final ObservableDoubleValue a) {
        return Bindings.createDoubleBinding(() -> Math.cos(a.get()), a);
    }

    /**
     * Binding for {@link java.lang.Math#sin(double)}
     *
     * @param a
     *            an angle, in radians.
     * @return the sine of the argument.
     */
    public DoubleBinding sin(final ObservableDoubleValue a) {
        return Bindings.createDoubleBinding(() -> Math.sin(a.get()), a);
    }

    /**
     * Binding for {@link java.lang.Math#abs(double)}
     *
     * @param a
     *            the argument whose absolute value is to be determined
     * @return the absolute value of the argument.
     */
    public DoubleBinding abs(final ObservableDoubleValue a) {
        return Bindings.createDoubleBinding(() -> Math.abs(a.get()), a);
    }

}
