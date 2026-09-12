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
import java.net.*;

import jakarta.inject.*;
import org.assertj.core.api.*;
import org.jboss.arquillian.container.test.api.*;
import org.jboss.shrinkwrap.api.*;
import org.jboss.shrinkwrap.api.asset.*;
import org.jboss.shrinkwrap.api.spec.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.arquillian.*;
import net.jqwik.arquillian.internal.*;

import static org.assertj.core.api.Assertions.*;

@JqwikArquillianSupport
public class PayaraManagedTest {
	private static final String SET_BY_THE_SERVER_JVM_ONLY = "com.sun.aas.instanceRoot";

	@Inject
	private Greeter greeter;

	@Deployment
	public static WebArchive deployment() throws URISyntaxException {
		return ShrinkWrap.create(WebArchive.class, "managed.war")
			.addClass(Greeter.class)
			.addAsLibrary(jarThatHolds(Assertions.class))
			.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	// A server in a JVM of its own sees nothing of the test class path, so the archive carries what the test needs
	private static File jarThatHolds(Class<?> type) throws URISyntaxException {
		return new File(type.getProtectionDomain().getCodeSource().getLocation().toURI());
	}

	@Example
	void exampleRunsInTheServerJvm() {
		assertThat(ContainerExecution.isActive()).isTrue();
		assertThat(System.getProperty(SET_BY_THE_SERVER_JVM_ONLY)).isNotNull();
		assertThat(greeter.greet("jqwik")).isEqualTo("Hello, jqwik");
	}

	@Property(tries = 25)
	void propertyRunsAllTriesInTheServerJvm(@ForAll @AlphaChars @StringLength(min = 1, max = 20) String name) {
		assertThat(ContainerExecution.isActive()).isTrue();
		assertThat(System.getProperty(SET_BY_THE_SERVER_JVM_ONLY)).isNotNull();
		assertThat(greeter.greet(name)).isEqualTo("Hello, " + name);
	}

	@Example
	@RunAsClient
	void clientJvmIsNotTheServerJvm() {
		assertThat(ContainerExecution.isActive()).isFalse();
		assertThat(System.getProperty(SET_BY_THE_SERVER_JVM_ONLY)).isNull();
	}
}
