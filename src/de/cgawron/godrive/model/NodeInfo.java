package de.cgawron.godrive.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.MarkupModel;
import de.cgawron.go.sgf.MarkupModel.Markup;
import de.cgawron.go.sgf.Node;
import de.cgawron.go.sgf.Property;

public class NodeInfo {
	private static final Logger logger = Logger.getLogger(NodeInfo.class.getName());

	/**
	 * @ToDo add GameInfo
	 * @ToDo add atari, ko & color information to enable client side (= faster)
	 *       play
	 * @ToDo add numer of captured stones
	 */

	public Map<String, Object> properties = new HashMap<String, Object>();

	public int parent = -1;
	public int id = -1;
	public int endOfLine = -1;
	public String turn;
	public List<Integer> children = new ArrayList<Integer>();
	public List<Integer> siblings = new ArrayList<Integer>();
	public Map<String, Object> board = new HashMap<String, Object>();
	public Map<String, Object> markup = new HashMap<String, Object>();

	public NodeInfo(Node node) {
		logger.info("node: " + node);

		this.id = node.getId();
		this.turn = node.turn() == BoardType.BLACK ? "B" : "W";

		Node parentNode = node.getParent();
		if (parentNode != null)
			parent = parentNode.getId();

		for (Node child : node.getChildren()) {
			children.add(child.getId());
		}

		Node eol = node;
		while (eol.getChildren().size() > 0)
			eol = eol.getChildren().get(0);
		this.endOfLine = eol.getId();

		for (Node sibling : node.getSiblings()) {
			siblings.add(sibling.getId());
		}

		Goban goban = node.getGoban();
		int boardSize = goban.getBoardSize();
		for (Point p : Point.all(boardSize)) {
			if (goban instanceof MarkupModel) {
				MarkupModel mm = (MarkupModel) goban;
				Markup m = mm.getMarkup(p);
				// if (m != null)
				markup.put(p.sgfString(), mm.getMarkup(p));
				BoardType bt = goban.getStone(p);
				if (bt != BoardType.EMPTY)
					board.put(p.sgfString(), bt);
			} else {
				BoardType bt = goban.getStone(p);
				if (bt != BoardType.EMPTY)
					board.put(p.sgfString(), bt);
			}
		}

		for (Property.Key key : node.keySet()) {
			logger.info("Key: " + key);
			properties.put(key.getUserFriendlyName(), node.get(key).getValue().toString());
		}
	}
}
