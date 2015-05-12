package de.cgawron.godrive.model;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

import de.cgawron.go.sgf.Node;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.RootNode;

public class GameInfo {
	private static final Logger logger = Logger.getLogger(GameInfo.class.getName());

	Map<String, Object> properties = new HashMap<String, Object>();

	int rootId;
	int id;
	String fileId;

	public GameInfo(String fileId, RootNode rootNode) {
		this.fileId = fileId;
		Node root = rootNode.getRoot();
		logger.info("root: " + root);
		this.rootId = root.getId();
		this.id = rootId;
		for (Property.Key key : root.keySet()) {
			logger.info("Key: " + key);
			properties.put(key.getUserFriendlyName(), root.get(key).getValue().toString());
		}
	}
}
