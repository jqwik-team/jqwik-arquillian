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

import java.net.*;
import java.net.http.*;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.test.api.*;
import org.jboss.shrinkwrap.api.*;
import org.jboss.shrinkwrap.api.asset.*;
import org.jboss.shrinkwrap.api.spec.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.arquillian.*;
import net.jqwik.arquillian.internal.*;

import static org.assertj.core.api.Assertions.*;

@JqwikArquillianSupport
public class PayaraRunAsClientTest {
	private static final HttpClient HTTP = HttpClient.newHttpClient();

	@ArquillianResource
	private URL deployment;

	@Deployment
	public static WebArchive deployment() {
		return ShrinkWrap.create(WebArchive.class, "run-as-client.war")
			.addClasses(Greeter.class, GreeterServlet.class)
			.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@Example
	@RunAsClient
	void exampleRunsOnTheClient() throws Exception {
		assertThat(ContainerExecution.isActive()).isFalse();
		assertThat(greetingOverHttp("jqwik")).isEqualTo("Hello, jqwik");
	}

	@Property(tries = 25)
	@RunAsClient
	void propertyRunsAllTriesOnTheClient(@ForAll @AlphaChars @StringLength(min = 1, max = 20) String name) throws Exception {
		assertThat(ContainerExecution.isActive()).isFalse();
		assertThat(greetingOverHttp(name)).isEqualTo("Hello, " + name);
	}

	@Example
	void exampleWithoutRunAsClientStillRunsInsideTheContainer() {
		assertThat(ContainerExecution.isActive()).isTrue();
	}

	private String greetingOverHttp(String name) throws Exception {
		final URI greeting = deployment.toURI().resolve("greet?" + GreeterServlet.NAME_PARAMETER + "=" + name);
		return HTTP.send(HttpRequest.newBuilder(greeting).build(), HttpResponse.BodyHandlers.ofString()).body();
	}
}
