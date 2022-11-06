package com.dangersoft.dragger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.DataFormat;
import javafx.util.Pair;

public class DragContainer implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final DataFormat AddNode = new DataFormat("application.com.dangersoft.dragger.DragIcon.add");

    private final List<Pair<String, Object>> mDataPairs = new ArrayList<Pair<String, Object>>();

    public static final DataFormat Binding = new DataFormat("com.buddyware.treefrog.filesystem.view.FileSystemBinding");

    public static final DataFormat Node = new DataFormat("com.buddyware.treefrog.filesystem.view.FileSystemNode");

    public static final DataFormat DragNode = new DataFormat("application.com.dangersoft.dragger.DraggableNode.drag");

    public static final DataFormat AddLink = new DataFormat("application.com.dangersoft.dragger.NodeLink.add");

    public static final DataFormat HotArea = new DataFormat("application.HotArea.add");

    public static final DataFormat KillHotArea = new DataFormat("application.KillHotArea.add");

    public DragContainer() {
    }

    public void addData(String key, Object value) {
        mDataPairs.add(new Pair<String, Object>(key, value));
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key) {

        for (Pair<String, Object> data : mDataPairs) {

            if (data.getKey().equals(key))
                return (T) data.getValue();

        }

        return null;
    }

    public List<Pair<String, Object>> getData() {
        return mDataPairs;
    }
}
