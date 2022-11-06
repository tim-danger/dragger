package com.dangersoft.dragger;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.layout.AnchorPane;

public class DragIcon extends AnchorPane {

	public DragIcon() {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/DragIcon.fxml"));

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

	private boolean isCircle = false;

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

	public void setType(DragIconType type) {

		mType = type;

		getStyleClass().clear();
		if (isCircle) {
			String style = "-fx-background-radius: 32;\n" + "-fx-border-color: black;\n" + "-fx-border-radius: 32;";
			this.setStyle(style);
		} else {
			String style = "-fx-border-color: black;\n";
			this.setStyle(style);
		}

		// TODO : remove or change
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

		Point2D localCoords = getParent().sceneToLocal(p);

		relocate((int) (localCoords.getX() - (getBoundsInLocal().getWidth() / 2)),
				(int) (localCoords.getY() - (getBoundsInLocal().getHeight() / 2)));
	}

}
