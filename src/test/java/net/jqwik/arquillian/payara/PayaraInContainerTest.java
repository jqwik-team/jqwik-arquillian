/*
 * Copyright (c) 2025 jqwik team
 * Copyright (c) 2025 Adeptum AB and Adam Waldenberg
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package net.jqwik.arquillian.payara;

import jakarta.inject.*;
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
public class PayaraInContainerTest {
	@Inject
	private Greeter greeter;

	@Deployment
	public static WebArchive deployment() {
		return ShrinkWrap.create(WebArchive.class, "in-container.war")
			.addClass(Greeter.class)
			.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@Example
	void exampleRunsInsideTheContainer() {
		assertThat(ContainerExecution.isActive()).isTrue();
		assertThat(greeter.greet("jqwik")).isEqualTo("Hello, jqwik");
	}

	@Property(tries = 25)
	void propertyRunsAllTriesInsideTheContainer(@ForAll @AlphaChars @StringLength(min = 1, max = 20) String name) {
		assertThat(ContainerExecution.isActive()).isTrue();
		assertThat(greeter.greet(name)).isEqualTo("Hello, " + name);
	}
}
