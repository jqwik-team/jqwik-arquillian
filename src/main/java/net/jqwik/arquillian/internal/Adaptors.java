/*
 * Copyright (c) 2025-2026 jqwik team
 * Copyright (c) 2025-2026 Adeptum AB and Adam Waldenberg
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package net.jqwik.arquillian.internal;

import java.util.*;

import lombok.*;
import org.jboss.arquillian.test.spi.*;

/**
 * The client side adaptor lives for the whole launcher session, like the Arquillian suite it stands for.
 * It is deliberately not kept in a jqwik store: a property running in an embedded container
 * starts a nested engine run that shares, and clears, the store repository of the client run.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Adaptors {
	private static final ClientSuite CLIENT_SUITE = new ClientSuite(TestRunnerAdaptorBuilder::build);

	static TestRunnerAdaptor current() throws Throwable {
		final Optional<TestRunnerAdaptor> inContainer = ContainerExecution.adaptor();
		return inContainer.isPresent() ? inContainer.get() : CLIENT_SUITE.adaptor();
	}

	static void finishClientSuite() {
		CLIENT_SUITE.finish();
	}
}
