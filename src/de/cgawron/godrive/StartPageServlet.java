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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Logger;

import de.cgawron.godrive.model.State;

/**
 * Servlet to check that the current user is authorized and to serve the start
 * page for DrEdit.
 *
 * @author vicfryzel@google.com (Vic Fryzel)
 * @author nivco@google.com (Nicolas Garnier)
 * @author jbd@google.com (Burcu Dogan)
 */
@SuppressWarnings("serial")
public class StartPageServlet extends GoDriveServlet {

	private static final Logger logger = Logger.getLogger(StartPageServlet.class.getName());

	/**
	 * Ensure that the user is authorized, and setup the required values for
	 * index.jsp.
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {

		StringBuffer reqUrl = req.getRequestURL();
		String queryString = req.getQueryString();
		if (queryString != null) {
			reqUrl.append("?").append(queryString);
		}
		logger.info("StartPageServlet: " + reqUrl);

		// handle OAuth2 callback
		if (false && handleCallbackIfRequired(req, resp))
			return;

		// Deserialize the state in order to specify some values to the DrEdit
		// JavaScript client below.
		String stateParam = req.getParameter("state");

		// Making sure that we have user credentials
		if (false && loginIfRequired(req, resp, stateParam))
			return;

		if (stateParam != null) {
			State state = new State(stateParam);
			if (state.ids != null && state.ids.size() > 0) {
				resp.sendRedirect("#/view/" + state.ids.toArray()[0]);
				return;
			} else if (state.folderId != null) {
				resp.sendRedirect("#/create/" + state.folderId);
				return;
			}
		}
		req.getRequestDispatcher("/index.html").forward(req, resp);
	}

}
