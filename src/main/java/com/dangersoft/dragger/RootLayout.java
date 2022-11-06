package com.dangersoft.dragger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class RootLayout extends AnchorPane {

	@FXML
	SplitPane base_pane;
	@FXML
	AnchorPane right_pane;
	@FXML
	VBox left_pane;

	private EventHandler<DragEvent> mIconDragOverRoot = null;
	private EventHandler<DragEvent> mIconDragDropped = null;
	private EventHandler<DragEvent> mIconDragOverRightPane = null;
	private EventHandler<MouseEvent> mHotAreaDragDetected = null;
	private EventHandler<DragEvent> mHotAreaDragOverRightPane = null;
	private EventHandler<MouseEvent> mHotAreaMoveDetected = null;
	private EventHandler<DragEvent> mHotAreaMoveOverRightPane = null;
	private EventHandler<DragEvent> mHotAreaDropOverRightPane = null;
	private EventHandler<DragEvent> mHotAreaMoveDropped = null;
	private Rectangle hotArea = new Rectangle();
	private List<DraggableNode> draggableNodes = new ArrayList<DraggableNode>();
	private final static Slider valueSlider = new Slider(0, 100, 100);

	// currently selected node
	private DraggableNode selectedNode;

	// currently selected node-links
	private NodeLink selectedLink;

	private Map<String, NodeLink> id2Arrow = new HashMap<String, NodeLink>();

	public static Slider getValueSlider() {
		return valueSlider;
	}

	private DragIcon mDragOverIcon = null;

	private Point2D dragOffsetRelative2HotArea = null;

	private Point2D[] offsets = null;

	public RootLayout() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/RootLayout.fxml"));

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

		// Add one icon that will be used for the drag-drop process
		// This is added as a child to the root AnchorPane so it can be
		// visible on both sides of the split pane.
		mDragOverIcon = new DragIcon();

		mDragOverIcon.setVisible(false);
		mDragOverIcon.setOpacity(0.65);
		getChildren().add(mDragOverIcon);

		// populate left pane with multiple colored icons for testing
		for (int i = 0; i < 6; i++) {

			DragIcon icn = new DragIcon();

			if (i % 2 == 0) {
				icn.setCircle(true);
			}
			addDragDetection(icn);

			icn.setType(DragIconType.values()[i]);
			left_pane.getChildren().add(icn);
		}

		left_pane.getChildren().add(valueSlider);

		addDragDetectionHotArea();
		buildDragHandlers();
		hotArea.setVisible(false);
		hotArea.setFill(Color.TRANSPARENT);
		hotArea.setStroke(Color.BLACK);
		right_pane.getChildren().add(hotArea);
		getStyleClass().clear();
	}

	public void changeSelection(DraggableNode node) {
		right_pane.requestFocus();
		// zuerst bisherige Auswahl loeschen
		change2Default();
		selectedNode = node;
		selectedLink = null;
		selectedNode.change2Marked();
	}

	public void changeSelection(String sourceID, String targetID) {
		right_pane.requestFocus();
		// zuerst bisherige Auswahl loeschen
		change2Default();
		selectedLink = id2Arrow.get(sourceID + "_" + targetID);
		selectedNode = null;
		selectedLink.change2Marked();
	}

	private void change2Default() {
		if (selectedNode != null) {
			selectedNode.change2Default();
		}

		if (selectedLink != null) {
			selectedLink.change2Default();
		}
	}

	private void moveToNextNode() {
		if (selectedNode != null) {
			selectedNode.change2Default();
			if (selectedNode.getOutGoingNodes().size() > 0) {
				// selektiere den Pfeil
				changeSelection(selectedNode.getId(), selectedNode.getOutGoingNodes().get(0).getId());
			}
			selectedNode = null;
		} else if (selectedLink != null) {
			selectedLink.change2Default();
			if (selectedLink.getOutGoingNode() != null) {
				// die Target- Node selektieren
				changeSelection(selectedLink.getOutGoingNode());
			}
			selectedLink = null;
		}
	}

	private void moveDownToNextArrow() {
		if (selectedLink == null) {
			return;
		} else {
			// waehle den naechsten Pfeil
			DraggableNode source = selectedLink.getInGoingNode();
			DraggableNode target = selectedLink.getOutGoingNode();
			int indexOfCurrentNode = source.getOutGoingNodes().indexOf(target);
			if (indexOfCurrentNode > -1 && indexOfCurrentNode < source.getOutGoingNodes().size()) {
				// Index befindet sich zwischen 0 und der Listengroe�e
				int newIndex = ++indexOfCurrentNode;
				newIndex = newIndex == source.getOutGoingNodes().size() ? 0 : newIndex;
				DraggableNode newTarget = source.getOutGoingNodes().get(newIndex);
				// der neue Pfeil wird dann entsprechend markiert
				changeSelection(source.getId(), newTarget.getId());
			} else {
				return;
			}
		}

	}

	private void moveUpToNextArrow() {
		if (selectedLink == null) {
			return;
		} else {
			// waehle den naechsten Pfeil
			DraggableNode source = selectedLink.getInGoingNode();
			DraggableNode target = selectedLink.getOutGoingNode();
			int indexOfCurrentNode = source.getOutGoingNodes().indexOf(target);
			if (indexOfCurrentNode >= -1 && indexOfCurrentNode < source.getOutGoingNodes().size()) {
				// Index befindet sich zwischen 0 und der Listengroe�e
				int newIndex = --indexOfCurrentNode;
				newIndex = newIndex == -1 ? source.getOutGoingNodes().size() - 1 : newIndex;
				DraggableNode newTarget = source.getOutGoingNodes().get(newIndex);
				// der neue Pfeil wird dann entsprechend markiert
				changeSelection(source.getId(), newTarget.getId());
			} else {
				return;
			}
		}

	}

	public void removeCurrent() {
		if (selectedNode != null) {
			deleteNode(selectedNode);
			selectedNode = null;
		} else if (selectedLink != null) {

			// eingehender Knoten
			DraggableNode inGoing = selectedLink.getInGoingNode();
			DraggableNode outGoing = selectedLink.getOutGoingNode();

			// Schluessel aus der Hashmap
			String key = inGoing.getId() + "_" + outGoing.getId();

			// die Nodes anpassen
			inGoing.removeFromOutGoingNodes(outGoing);
			outGoing.removeFromInGoingNodes(inGoing);

			// entferne den Eintrag in der Map
			id2Arrow.remove(key);

			right_pane.getChildren().remove(selectedLink);
			selectedLink = null;
		} else if (draggableNodes != null && draggableNodes.size() > 0) {
			for (DraggableNode node : draggableNodes) {
				deleteNode(node);
			}

			// den default- Handler wiederherstellen
			// die HotArea verschwinden lassen
			hotArea.setVisible(false);
			// Default- Status wieder herstellen
			right_pane.setOnDragDetected(mHotAreaDragDetected);
			right_pane.setOnDragOver(mIconDragOverRightPane);
			right_pane.setOnDragDropped(mIconDragDropped);
		}
	}

	/**
	 * Das Loeschen eines Knoten
	 * 
	 * @param node2BeDeleted
	 */
	private void deleteNode(DraggableNode node2BeDeleted) {
		// alle eingehenden Pfeile loeschen
		for (DraggableNode node : node2BeDeleted.getInGoingNodes()) {
			String key = node.getId() + "_" + node2BeDeleted.getId();
			NodeLink link = id2Arrow.get(key);
			if (link != null) {
				right_pane.getChildren().remove(link);
			}
			id2Arrow.remove(key);
			// loesche die selected node bei den outgoing nodes der ingoing
			// nodes
			node.removeFromOutGoingNodes(node2BeDeleted);
		}
		// alle ausgehenden Pfeile loeschen
		for (DraggableNode node : node2BeDeleted.getOutGoingNodes()) {
			String key = node2BeDeleted.getId() + "_" + node.getId();
			NodeLink link = id2Arrow.get(node2BeDeleted.getId() + "_" + node.getId());
			if (link != null) {
				right_pane.getChildren().remove(link);
			}
			id2Arrow.remove(key);
			// loesche die selected node bei den ingoing nodes der outgoing
			// nodes
			node.removeFromInGoingNodes(node2BeDeleted);
		}
		right_pane.getChildren().remove(node2BeDeleted.getLabel());
		right_pane.getChildren().remove(node2BeDeleted);
	}

	private void buildDragHandlers() {

		// drag over transition to move widget form left pane to right pane
		mIconDragOverRoot = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				Point2D p = right_pane.sceneToLocal(event.getSceneX(), event.getSceneY());

				if (!right_pane.boundsInLocalProperty().get().contains(p)) {
					mDragOverIcon.relocateToPoint(new Point2D(event.getSceneX(), event.getSceneY()));
					return;
				}

				event.consume();
			}
		};

		mIconDragOverRightPane = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				event.acceptTransferModes(TransferMode.ANY);

				mDragOverIcon.relocateToPoint(new Point2D(event.getSceneX(), event.getSceneY()));

				event.consume();
			}
		};

		mHotAreaDragOverRightPane = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				event.acceptTransferModes(TransferMode.ANY);

				// aufgrund eines Bugs muss hier ein Relocate erfolgen
				hotArea.relocate(hotArea.getX() + 0.5, hotArea.getY() + 0.5);

				Point2D forHotArea = right_pane
						.sceneToLocal(new Point2D(event.getSceneX() + 0.5, event.getSceneY() + 0.5));

				hotArea.setWidth(forHotArea.getX() - hotArea.getX());
				hotArea.setHeight(forHotArea.getY() - hotArea.getY());

				event.consume();

			}

		};

		mHotAreaMoveOverRightPane = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				event.acceptTransferModes(TransferMode.ANY);

				Point2D forHotArea = right_pane.sceneToLocal(new Point2D(event.getSceneX(), event.getSceneY()));
				Point2D delta = new Point2D(forHotArea.getX() - dragOffsetRelative2HotArea.getX() - hotArea.getX(),
						forHotArea.getY() - dragOffsetRelative2HotArea.getY() - hotArea.getY());

				hotArea.relocate(forHotArea.getX() - dragOffsetRelative2HotArea.getX() + 0.5,
						forHotArea.getY() - dragOffsetRelative2HotArea.getY() + 0.5);

				// System.out.println(delta);
				for (int i = 0; i < draggableNodes.size(); i++) {
					draggableNodes.get(i).relocate(offsets[i].getX() + delta.getX(), offsets[i].getY() + delta.getY());
					draggableNodes.get(i).relocateLabel(offsets[i].getX() + delta.getX(),
							offsets[i].getY() + delta.getY());
				}

				event.consume();

			}

		};

		mIconDragDropped = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				DragContainer container = (DragContainer) event.getDragboard().getContent(DragContainer.AddNode);

				container.addData("scene_coords", new Point2D(event.getSceneX(), event.getSceneY()));

				ClipboardContent content = new ClipboardContent();
				content.put(DragContainer.AddNode, container);

				event.getDragboard().setContent(content);
				event.setDropCompleted(true);
			}
		};

		mHotAreaDropOverRightPane = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {
				// den Listener entsprechend �ndern
				right_pane.setOnDragDetected(mHotAreaMoveDetected);
				event.setDropCompleted(true);
			}
		};

		mHotAreaMoveDropped = new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				// die HotArea verschwinden lassen
				hotArea.setVisible(false);
				// Default- Status wieder herstellen
				right_pane.setOnDragDetected(mHotAreaDragDetected);
				right_pane.setOnDragOver(mIconDragOverRightPane);
				right_pane.setOnDragDropped(mIconDragDropped);
				event.setDropCompleted(true);
			}
		};

		this.setOnDragDone(new EventHandler<DragEvent>() {

			@Override
			public void handle(DragEvent event) {

				right_pane.removeEventHandler(DragEvent.DRAG_OVER, mIconDragOverRightPane);
				right_pane.removeEventHandler(DragEvent.DRAG_DROPPED, mIconDragDropped);
				right_pane.removeEventHandler(DragEvent.DRAG_DROPPED, mHotAreaMoveDropped);
				right_pane.removeEventHandler(DragEvent.DRAG_OVER, mHotAreaDragOverRightPane);
				right_pane.removeEventFilter(DragEvent.DRAG_OVER, mHotAreaMoveOverRightPane);
				base_pane.removeEventHandler(DragEvent.DRAG_OVER, mIconDragOverRoot);

				mDragOverIcon.setVisible(false);

				DragContainer container = (DragContainer) event.getDragboard().getContent(DragContainer.AddNode);

				if (container != null) {
					if (container.getValue("scene_coords") != null) {

						Label nodeLabel = new Label("Label");

						DraggableNode node = new DraggableNode(valueSlider, nodeLabel, RootLayout.this);

						node.setCircle(mDragOverIcon.isCircle());
						node.setType(DragIconType.valueOf(container.getValue("type")));
						right_pane.getChildren().addAll(node, nodeLabel);

						Point2D cursorPoint = container.getValue("scene_coords");

						// TODO : rausnehmen
						System.out.println("Pref Height " + node.getPrefHeight());
						System.out.println("Pref Width " + node.getPrefWidth());
						node.relocateToPoint(new Point2D(cursorPoint.getX() - node.getPrefWidth() / 2,
								cursorPoint.getY() - node.getPrefHeight() / 2));

						node.setLabel4Node(valueSlider, true);
					}
				}

				container = (DragContainer) event.getDragboard().getContent(DragContainer.DragNode);

				if (container != null) {
					if (container.getValue("type") != null)
						System.out.println("Moved node " + container.getValue("type"));
				}

				// AddLink drag operation
				container = (DragContainer) event.getDragboard().getContent(DragContainer.AddLink);

				if (container != null) {

					// TODO : den Fall debuggen, dass ein Quadrat (evtl. auch
					// Kreis) kurz vor dem
					// Rand steht und von links eine Verbindung eingezeichnet
					// werden soll. Der Pfeil
					// verschwindet nicht, bleibt einfach stehen

					// bind the ends of our link to the nodes whose id's are
					// stored in the drag container
					String sourceId = container.getValue("source");
					String targetId = container.getValue("target");
					System.out.println(container.getData());

					if (sourceId != null && targetId != null) {

						// System.out.println(container.getData());
						NodeLink link = new NodeLink(valueSlider, RootLayout.this);

						// add our link at the top of the rendering order so
						// it's rendered first
						right_pane.getChildren().add(0, link);

						DraggableNode source = null;
						DraggableNode target = null;

						for (Node n : right_pane.getChildren()) {

							if (n.getId() == null)
								continue;

							if (n.getId().equals(sourceId))
								source = (DraggableNode) n;

							if (n.getId().equals(targetId))
								target = (DraggableNode) n;

						}

						if (source != null && target != null) {
							link.bindEnds(source, target);
							id2Arrow.put(source.getId() + "_" + target.getId(), link);
							// link.installClickListener(link, com.dangersoft.dragger.RootLayout.this,
							// sourceId, targetId);
						}
					}
				}

				container = (DragContainer) event.getDragboard().getContent(DragContainer.HotArea);

				if (container != null) {
					draggableNodes.clear();
					boolean nodeContained = false;
					for (Node n : right_pane.getChildren()) {
						if (n instanceof DraggableNode) {
							DraggableNode candidate = (DraggableNode) n;
							if (hotArea.contains(candidate.getLayoutX() + candidate.getWidth() / 2,
									candidate.getLayoutY() + candidate.getWidth() / 2)) {
								System.out.println(candidate + " contained");
								draggableNodes.add(candidate);
								nodeContained = true;
							}
						}
					}

					if (!nodeContained) {
						hotArea.setVisible(false);
						// Default- Status wieder herstellen
						// TODO : in Funktion auslagern
						right_pane.setOnDragDetected(mHotAreaDragDetected);
						right_pane.setOnDragOver(mIconDragOverRightPane);
						right_pane.setOnDragDropped(mIconDragDropped);
					}
				}

				container = (DragContainer) event.getDragboard().getContent(DragContainer.KillHotArea);

				if (container != null) {

				}

				event.consume();
			}
		});
	}

	private void addDragDetection(DragIcon dragIcon) {

		dragIcon.setOnDragDetected(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				// set the other drag event handles on their respective objects
				base_pane.setOnDragOver(mIconDragOverRoot);
				right_pane.setOnDragOver(mIconDragOverRightPane);
				right_pane.setOnDragDropped(mIconDragDropped);

				// get a reference to the clicked com.dangersoft.dragger.DragIcon object
				DragIcon icn = (DragIcon) event.getSource();

				// set the circle property
				mDragOverIcon.setCircle(icn.isCircle());

				// begin drag ops
				mDragOverIcon.setType(icn.getType());
				mDragOverIcon.relocateToPoint(new Point2D(event.getSceneX(), event.getSceneY()));

				ClipboardContent content = new ClipboardContent();
				DragContainer container = new DragContainer();

				container.addData("type", mDragOverIcon.getType().toString());
				content.put(DragContainer.AddNode, container);

				mDragOverIcon.startDragAndDrop(TransferMode.ANY).setContent(content);
				mDragOverIcon.setVisible(true);
				mDragOverIcon.setMouseTransparent(true);
				event.consume();
			}
		});
	}

	private void addDragDetectionHotArea() {

		mHotAreaDragDetected = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				right_pane.setOnDragOver(null);
				right_pane.setOnDragDropped(null);

				// Listener erst nach dem Setzen der H�he etc. registrieren
				right_pane.setOnDragOver(mHotAreaDragOverRightPane);
				right_pane.setOnDragDropped(mHotAreaDropOverRightPane);

				// Koordinaten zun�chst erst mal holen
				Point2D start = right_pane.sceneToLocal(new Point2D(event.getSceneX(), event.getSceneY()));

				System.out.println(start);
				hotArea.setVisible(true);
				hotArea.setX(start.getX() + 0.5);
				hotArea.setY(start.getY() + 0.5);
				// System.out.println("Hot- Area: " + hotArea.getX() + ", " +
				// hotArea.getY());
				hotArea.setWidth(0.0);
				hotArea.setHeight(0.0);

				ClipboardContent content = new ClipboardContent();
				DragContainer container = new DragContainer();

				// Start- com.dangersoft.dragger.Koordinate in den Container h�ngen
				container.addData("container", start.toString());
				content.put(DragContainer.HotArea, container);

				startDragAndDrop(TransferMode.ANY).setContent(content);

				event.consume();

			}

		};

		mHotAreaMoveDetected = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				right_pane.setOnDragOver(null);
				right_pane.setOnDragDropped(null);

				right_pane.setOnDragOver(mHotAreaMoveOverRightPane);
				right_pane.setOnDragDropped(mHotAreaMoveDropped);

				// Koordinaten zun�chst erst mal holen
				Point2D start = right_pane.sceneToLocal(new Point2D(event.getSceneX(), event.getSceneY()));
				dragOffsetRelative2HotArea = new Point2D(start.getX() - hotArea.getX(), start.getY() - hotArea.getY());

				// Offsets aller Koordinaten bestimmen
				offsets = calcOffsets();

				System.out.println("Hot Area drag: " + start);

				ClipboardContent content = new ClipboardContent();
				DragContainer container = new DragContainer();

				// hier darf nichts reingehängt werden
				container.addData("container", start.toString());
				content.put(DragContainer.KillHotArea, container);

				// geht Beides
				/* right_pane. */startDragAndDrop(TransferMode.ANY).setContent(content);

				event.consume();
			}

			private Point2D[] calcOffsets() {
				Point2D[] offsets = new Point2D[draggableNodes.size()];
				int count = 0;
				for (DraggableNode node : draggableNodes) {
					offsets[count++] = new Point2D(node.getLayoutX(), node.getLayoutY());
				}
				return offsets;
			}
		};

		right_pane.setOnDragDetected(mHotAreaDragDetected);

		this.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				switch (event.getCode()) {
				case DELETE:
					removeCurrent();
					break;
				case TAB:
					moveToNextNode();
					break;
				case DOWN:
					moveDownToNextArrow();
					break;
				case UP:
					moveUpToNextArrow();
					break;
				default:
					break;
				}
			}
		});

		// this.setOnKeyReleased(new EventHandler<KeyEvent>() {
		// @Override
		// public void handle(KeyEvent event) {
		// switch (event.getCode()) {
		// case DOWN:
		// moveToNextArrow();
		// break;
		// default:
		// break;
		// }
		// }
		// });
	}

}
