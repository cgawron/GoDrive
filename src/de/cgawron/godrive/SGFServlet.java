/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package de.cgawron.godrive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;

import javax.ejb.EJB;
//import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.cgawron.go.Point;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Node;
import de.cgawron.go.sgf.Property;
import de.cgawron.go.sgf.Property.Key;
import de.cgawron.godrive.model.ClientFile;
import de.cgawron.godrive.model.GameInfo;
import de.cgawron.godrive.model.NodeInfo;

/**
 * Servlet providing a small API for the DrEdit JavaScript client to use in
 * manipulating files. Each operation (GET, POST, PUT) issues requests to the
 * Google Drive API.
 *
 * @author vicfryzel@google.com (Vic Fryzel)
 */
@SuppressWarnings("serial")
public class SGFServlet extends GoDriveServlet {
	private static final String MIMETYPE_SGF = "application/x-go-sgf";

	private static final Logger logger = Logger.getLogger(SGFServlet.class.getName());

	private static final String TEST_SGF = "/WEB-INF/test.sgf";
	private static final String KEY_SGF_TREE = "SGF_TREE";

	@EJB
	private UpdateService updateService;

	/**
	 * Given a {@code file_id} URI parameter, return a JSON representation of
	 * the given file.
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		String fileId = req.getParameter("file_id");
		logger.info("path: " + req.getRequestURI());
		logger.info("query: " + req.getQueryString());
		logger.info("file_id: " + fileId);
		Drive service = getDriveService(getCredential(req, resp));
		
		if (fileId == null) {
			sendError(resp, 400, "The `file_id` URI parameter must be specified.");
			return;
		}

		if (req.getRequestURI().endsWith("sgf")) {
			try {
				resp.setContentType("application/x-go-sgf");
				File file = null;
				try {
					file = service.files().get(fileId).execute();
				} catch (GoogleJsonResponseException e) {
					if (e.getStatusCode() == 401) {
						// The user has revoked our token or it is otherwise
						// bad.
						// Delete the local copy so that their next page load
						// will
						// recover.
						deleteCredential(req, resp);
						sendGoogleJsonResponseError(resp, e);
						return;
					}
					else {
						sendError(resp, 404, "File not found");
						return;
					}
				}

				String content = downloadFileContent(service, file);
				sendJson(resp, new ClientFile(file, content));
			} catch (Exception e) {
				logger.log(Level.SEVERE, "failed to save gameTree", e);
				sendError(resp, 500, e.getLocalizedMessage());
			}
		}
	}

	/**
	 * Create a new file given a JSON representation, and return the JSON
	 * representation of the created file.
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Drive service = getDriveService(getCredential(req, resp));
		logger.info("path: " + req.getRequestURI());
		logger.info("query: " + req.getQueryString());
		final String fileId = req.getParameter("file_id");

		if (fileId == null) {
			sendError(resp, 400, "The `file_id` URI parameter must be specified.");
			return;
		}

		final File file = service.files().get(fileId).execute();

		GameTree gameTree = null;
		gameTree = (GameTree) req.getSession().getAttribute(KEY_SGF_TREE + fileId);
		if (gameTree == null) {
			String content = downloadFileContent(service, file);
			if (content == null || content.length() == 0) {
				logger.info("Empty file!");
				gameTree = new GameTree();
			}

			try {
				logger.info("Content: " + content);
				gameTree = new GameTree(new StringReader(content));
			} catch (Exception e) {
				throw new RuntimeException("failed to parse SGF", e);
			}
		}
		else {
			logger.info("used cached GameTree");
		}

		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		NodeInfo nodeInfo = gson.fromJson(req.getReader(), NodeInfo.class);
		logger.info("doPost: nodeInfo=" + nodeInfo.id);
		Node node = gameTree.getNode(nodeInfo.id);
		for (String keyString : nodeInfo.properties.keySet()) {
			Key key = new Key(keyString);
			if (node.contains(key)) {
				node.remove(key);
			}
			node.put(key, Property.createProperty(key, nodeInfo.properties.get(keyString)));
		}

		req.getSession().setAttribute(KEY_SGF_TREE + file.getId(), gameTree);
		sendJson(resp, new NodeInfo(node));

		updateService.submitUpdateRequest(service, file, gameTree);
	}

	/**
	 * Update a file given a JSON representation, and return the JSON
	 * representation of the created file.
	 */
	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		final Drive service = getDriveService(getCredential(req, resp));

		logger.info("path: " + req.getRequestURI());
		logger.info("query: " + req.getQueryString());
		if (req.getRequestURI().endsWith("info")) {
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.create();
			GameInfo other = gson.fromJson(req.getReader(), GameInfo.class);

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			GameTree gameTree = new GameTree();
			gameTree.getRoot().put(new Property.SimpleText(Property.APPLICATION,
					"SGF Editor (https://godrive.cgawron.de/)"));
			gameTree.save(stream);
			String sgfContent = stream.toString();

			File file = new File();
			file.setTitle("new game");
			file.setDescription("game created by SGF Editor");
			file.setMimeType(MIMETYPE_SGF);

			file = service.files().insert(file,
					ByteArrayContent.fromString(MIMETYPE_SGF, sgfContent))
					.execute();

			req.getSession().setAttribute(KEY_SGF_TREE + file.getId(), gameTree);
			sendJson(resp, new ClientFile(file, sgfContent));
		}
		else {
			Point point = new Point(req.getParameter("point"));
			logger.info("doPut: point=" + point);

			final String fileId = req.getParameter("file_id");

			if (fileId == null) {
				sendError(resp, 400, "The `file_id` URI parameter must be specified.");
				return;
			}

			final File file = service.files().get(fileId).execute();

			GameTree gameTree = null;
			gameTree = (GameTree) req.getSession().getAttribute(KEY_SGF_TREE + fileId);
			if (gameTree == null) {
				String content = downloadFileContent(service, file);
				if (content == null || content.length() == 0) {
					logger.info("Empty file!");
					gameTree = new GameTree();
				}

				try {
					logger.info("Content: " + content);
					gameTree = new GameTree(new StringReader(content));
				} catch (Exception e) {
					throw new RuntimeException("failed to parse SGF", e);
				}
			}
			else {
				logger.info("used cached GameTree");
			}

			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.create();
			NodeInfo nodeInfo = gson.fromJson(req.getReader(), NodeInfo.class);
			logger.info("doPut: nodeInfo=" + nodeInfo.id);
			Node node = gameTree.getNode(nodeInfo.id);
			Node child = gameTree.appendNode(node);
			child.move(point);

			req.getSession().setAttribute(KEY_SGF_TREE + file.getId(), gameTree);
			sendJson(resp, new NodeInfo(child));

			updateService.submitUpdateRequest(service, file, gameTree);
		}
	}

	/**
	 * Download the content of the given file.
	 *
	 * @param service
	 *            Drive service to use for downloading.
	 * @param file
	 *            File metadata object whose content to download.
	 * @return String representation of file content. String is returned here
	 *         because this app is setup for text/plain files.
	 * @throws IOException
	 *             Thrown if the request fails for whatever reason.
	 */
	private String downloadFileContent(Drive service, File file)
			throws IOException {
		GenericUrl url = new GenericUrl(file.getDownloadUrl());
		HttpResponse response = service.getRequestFactory().buildGetRequest(url)
				.execute();
		try {
			return new Scanner(response.getContent()).useDelimiter("\\A").next();
		} catch (java.util.NoSuchElementException e) {
			return "";
		}
	}

}
