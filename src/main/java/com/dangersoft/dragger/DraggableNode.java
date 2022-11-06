package com.dangersoft.dragger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;

public class DraggableNode extends AnchorPane {

	public static final String DEFAULT_CIRCLE_STYLE = "-fx-background-radius: 32;\n" + "-fx-border-color: black;\n"
			+ "-fx-border-radius: 32;" + "-fx-background-color: white;\n";
	public static final String DEFAULT_SQUARE_STYLE = "-fx-border-color: black;\n" + "-fx-background-color: white;\n";

	@FXML
	AnchorPane root_pane;

	@FXML
	AnchorPane left_link_handle;
	@FXML
	AnchorPane right_link_handle;

	private EventHandler<MouseEvent> mLinkHandleDragDetected;
	private EventHandler<DragEvent> mLinkHandleDragDropped;
	private EventHandler<DragEvent> mContextLinkDragOver;
	private EventHandler<DragEvent> mContextLinkDragDropped;
	private EventHandler<DragEvent> mContextDragOver;
	private EventHandler<DragEvent> mContextDragDropped;

	private NodeLink mDragLink = null;
	private AnchorPane right_pane = null;

	private Point2D mDragOffset = new Point2D(0.0, 0.0);

	private Label nodeLabel;

	private Slider slider;

	private static int labelCounter = 0;

	private boolean isCircle = false;

	private RootLayout layout;

	private List<DraggableNode> inGoingNodes = new ArrayList<DraggableNode>();
	private List<DraggableNode> outGoingNodes = new ArrayList<DraggableNode>();

	public List<DraggableNode> getInGoingNodes() {
		return inGoingNodes;
	}

	public List<DraggableNode> getOutGoingNodes() {
		return outGoingNodes;
	}

	public void removeFromInGoingNodes(DraggableNode node) {
		inGoingNodes.remove(node);
	}

	public void removeFromOutGoingNodes(DraggableNode node) {
		outGoingNodes.remove(node);
	}

	public void addToInGoingNodes(DraggableNode node) {
		inGoingNodes.add(node);
	}

	public void addToOutGoingNodes(DraggableNode node) {
		outGoingNodes.add(node);
	}

	public Label getLabel() {
		return nodeLabel;
	}

	public DraggableNode() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/DraggableNode.fxml"));

		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);

		try {
			fxmlLoader.load();

		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}

		setId(UUID.randomUUID().toString());

		setFocusTraversable(true);

		InvalidationListener updater = o -> {
			if (slider != null) {
				setLabel4Node(slider, false);
			}
		};

		this.prefHeightProperty().addListener(updater);
	}

	public DraggableNode(Slider slider, Label label, RootLayout root) {
		this();
		prefWidthProperty().bind(Bindings.multiply(slider.valueProperty(), 64.0 / 100.0));
		prefHeightProperty().bind(Bindings.multiply(slider.valueProperty(), 64.0 / 100.0));
		nodeLabel = label;
		this.slider = slider;
		this.layout = root;
	}

	public void change2Default() {
		getStyleClass().clear();
		if (isCircle) {
			String style = "-fx-background-radius: 32;\n" + "-fx-border-color: black;\n" + "-fx-border-radius: 32;";
			this.setStyle(style);
		} else {
			String style = "-fx-border-color: black;\n";
			this.setStyle(style);
		}
	}

	public void change2Marked() {
		if (isCircle) {
			String style = "-fx-background-radius: 32;\n" + "-fx-border-color: black;\n" + "-fx-border-radius: 32;"
					+ "-fx-background-color: green;\n";
			this.setStyle(style);
		} else {
			String style = "-fx-border-color: black;\n" + "-fx-background-color: green;\n";
			this.setStyle(style);
		}
	}

	/**
	 * Funktioniert leider nicht...
	 * 
	 * @param slider
	 */
	public void bindLabel2Node(Slider slider) {
		// das Label an die X- Ausrichtung binden
		nodeLabel.layoutXProperty().bindBidirectional(this.layoutXProperty());
		// das Label an die Y- Ausrichtung binden
		DoubleProperty delta = new SimpleDoubleProperty(0.0);
		delta.bind(Bindings.add(Bindings.add(this.layoutYProperty(), this.prefHeightProperty()),
				Bindings.multiply(slider.valueProperty(), 5.0 / 100.0)));
		nodeLabel.layoutYProperty().bindBidirectional(delta);
		nodeLabel.setText(nodeLabel.getText() + " " + ++labelCounter);
	}

	public void setLabel4Node(Slider slider, boolean update) {
		// das Label an die X- Ausrichtung binden
		nodeLabel.setLayoutX(this.layoutXProperty().get());
		// das Label an die Y- Ausrichtung binden
		DoubleProperty delta = new SimpleDoubleProperty(0.0);
		delta.bind(Bindings.add(Bindings.add(this.layoutYProperty(), this.prefHeightProperty()),
				Bindings.multiply(slider.valueProperty(), 5.0 / 100.0)));
		nodeLabel.setLayoutY(delta.get());
		if (update) {
			nodeLabel.setText(nodeLabel.getText() + " " + ++labelCounter);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@FXML
	private void initialize() {
		buildNodeDragHandlers();
		buildLinkDragHandlers();

		left_link_handle.setOnDragDetected(mLinkHandleDragDetected);
		right_link_handle.setOnDragDetected(mLinkHandleDragDetected);

		left_link_handle.setOnDragDropped(mLinkHandleDragDropped);
		right_link_handle.setOnDragDropped(mLinkHandleDragDropped);

		mDragLink = new NodeLink(RootLayout.getValueSlider(), layout);
		mDragLink.setVisible(false);

		parentProperty().addListener(new ChangeListener() {

			@Override
			public void changed(ObservableValue observable, Object oldValue, Object newValue) {
				right_pane = (AnchorPane) getParent();
			}
		});

//		this.setOnMouseClicked(new EventHandler<MouseEvent>() {
//
//			@Override
//			public void handle(MouseEvent e) {
//
//				// die Funktion stoesst eine Callback- Methode an
//				layout.changeSelection(DraggableNode.this);
//
//			}
//		});
	}

	public void buildNodeDragHandlers() {

		// drag detection for node dragging
		// TODO : eigens gebautes Label verschieben
		this.setOnDragDetected(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				getParent().setOnDragOver(null);
				getParent().setOnDragDropped(null);

				getParent().setOnDragOver(mContextDragOver);
				getParent().setOnDragDropped(mContextDragDropped);

				// begin drag ops
				mDragOffset = new Point2D(event.getX(), event.getY());

				relocateToPoint(new Point2D(event.getSceneX(), event.getSceneY()));

				ClipboardContent content = new ClipboardContent();
				DragContainer container = new DragContainer();

				container.addData("type", mType.toString());
				content.put(DragContainer.DragNode, container);

				startDragAndDrop(TransferMode.ANY).setContent(content);

				event.consume();
			}

		});

		mContextDragOver = new EventHandler<DragEvent>() {

			// dragover to handle node dragging in the right pane view
			@Override
			public void handle(DragEvent event) {

				event.acceptTransferModes(TransferMode.ANY);
				relocateToPoint(new Point2D(event.getSceneX(), event.getSceneY()));

				event.consume();
			}
		};

		// dragdrop for node dragging
		mContextDragDropped = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				getParent().setOnDragOver(null);
				getParent().setOnDragDropped(null);

				event.setDropCompleted(true);

				event.consume();
			}
		};

		// close button click (TODO : mit entfernen verbinden)
		// close_button.setOnMouseClicked(new EventHandler<MouseEvent>() {
		//
		// @Override
		// public void handle(MouseEvent event) {
		//
		// AnchorPane parent = (AnchorPane) self.getParent();
		// parent.getChildren().remove(self);
		// }
		// });
	}

	private void buildLinkDragHandlers() {

		mLinkHandleDragDetected = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				getParent().setOnDragOver(null);
				getParent().setOnDragDropped(null);

				getParent().setOnDragOver(mContextLinkDragOver);
				// TODO : check
				// getParent().setOnDragDropped(mLinkHandleDragDropped);
				getParent().setOnDragDropped(mContextLinkDragDropped);

				// Set up user-draggable link
				right_pane.getChildren().add(0, mDragLink);

				mDragLink.setVisible(false);

				Point2D p = new Point2D(getLayoutX() + (getWidth() / 2.0), getLayoutY() + (getHeight() / 2.0));

				mDragLink.setStart(p);

				// Drag content code
				ClipboardContent content = new ClipboardContent();
				DragContainer container = new DragContainer();

				AnchorPane link_handle = (AnchorPane) event.getSource();
				DraggableNode parent = (DraggableNode) link_handle.getParent().getParent().getParent();

				container.addData("source", getId());

				content.put(DragContainer.AddLink, container);

				parent.startDragAndDrop(TransferMode.ANY).setContent(content);

				event.consume();
			}
		};

		mLinkHandleDragDropped = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				getParent().setOnDragOver(null);
				getParent().setOnDragDropped(null);

				// get the drag data. If it's null, abort.
				// This isn't the drag event we're looking for.
				DragContainer container = (DragContainer) event.getDragboard().getContent(DragContainer.AddLink);

				if (container == null)
					return;

				// hide the draggable com.dangersoft.dragger.NodeLink and remove it from the right-hand
				// AnchorPane's children (erst entfernen, wenn es sich auch um
				// eine AddLink- Operation handelt, ergo container != null ist)
				mDragLink.setVisible(false);
				right_pane.getChildren().remove(0);

				ClipboardContent content = new ClipboardContent();

				container.addData("target", getId());

				content.put(DragContainer.AddLink, container);

				event.getDragboard().setContent(content);

				event.setDropCompleted(true);

				event.setDropCompleted(true);

				event.consume();
			}
		};

		mContextLinkDragOver = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {
				event.acceptTransferModes(TransferMode.ANY);

				// Relocate user-draggable link
				if (!mDragLink.isVisible())
					mDragLink.setVisible(true);

				mDragLink.setEnd(new Point2D(event.getX(), event.getY()));

				event.consume();

			}
		};

		mContextLinkDragDropped = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				getParent().setOnDragOver(null);
				getParent().setOnDragDropped(null);

				mDragLink.setVisible(false);
				right_pane.getChildren().remove(0);

				event.setDropCompleted(true);
				event.consume();
			}

		};
	}

	private DragIconType mType;

	public DragIconType getType() {
		return mType;
	}

	public void setCircle(boolean isCircle) {
		this.isCircle = isCircle;
	}

	public boolean isCircle() {
		return this.isCircle;
	}

	// TODO : eventuell entfernen
	public void setType(DragIconType type) {

		mType = type;

		change2Default();

		// TODO :
		switch (mType) {

		case blue:
			getStyleClass().add("icon-blue");
			break;

		case red:
			getStyleClass().add("icon-red");
			break;

		case green:
			getStyleClass().add("icon-green");
			break;

		case grey:
			getStyleClass().add("icon-grey");
			break;

		case purple:
			getStyleClass().add("icon-purple");
			break;

		case yellow:
			getStyleClass().add("icon-yellow");
			break;

		case black:
			getStyleClass().add("icon-black");
			break;

		default:
			break;
		}
	}

	public void relocateToPoint(Point2D p) {

		// relocates the object to a point that has been converted to
		// scene coordinates
		Point2D localCoords = getParent().sceneToLocal(p);

		relocate((int) (localCoords.getX() - mDragOffset.getX()), (int) (localCoords.getY() - mDragOffset.getY()));
		nodeLabel.relocate((int) (localCoords.getX() - mDragOffset.getX()), (int) (localCoords.getY()
				- mDragOffset.getY() + this.getPrefWidth() + 5.0 * slider.getValue() / 100.0));
	}

	public void relocateLabel(double x, double y) {
		nodeLabel.relocate(x, y + this.getPrefWidth() + 5.0 * slider.getValue() / 100.0);

	}

}
