/*
 * Copyright (c) 2026 jqwik team
 * Copyright (c) 2026 Adeptum AB and Adam Waldenberg
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package net.jqwik.arquillian.payara;

import java.io.*;
import java.nio.charset.*;

import jakarta.inject.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/greet")
public class GreeterServlet extends HttpServlet {
	static final String NAME_PARAMETER = "name";

	@Inject
	private Greeter greeter;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().print(greeter.greet(request.getParameter(NAME_PARAMETER)));
	}
}
