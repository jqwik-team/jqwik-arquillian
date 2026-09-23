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

import java.util.function.*;

import org.jboss.arquillian.test.spi.*;

/**
 * The Arquillian suite on the client, started by the first Arquillian test class and ended with the launcher session.
 */
final class ClientSuite {
	private final Supplier<TestRunnerAdaptor> adaptors;
	private TestRunnerAdaptor adaptor;
	private Throwable startFailure;

	ClientSuite(Supplier<TestRunnerAdaptor> adaptors) {
		this.adaptors = adaptors;
	}

	synchronized TestRunnerAdaptor adaptor() throws Throwable {
		// Every class and property would otherwise retry a start that already failed, and bury the reason it failed first
		if (startFailure != null) {
			throw startFailure;
		}
		if (adaptor == null) {
			try {
				adaptor = started(adaptors.get());
			} catch (Throwable t) {
				startFailure = t;
				throw t;
			}
		}
		return adaptor;
	}

	synchronized void finish() {
		// The next launcher session in this JVM gets a start attempt of its own
		startFailure = null;
		if (adaptor == null) {
			return;
		}
		final TestRunnerAdaptor finished = adaptor;
		adaptor = null;
		try {
			ArquillianLifecycle.endSuite(finished);
		} catch (Exception e) {
			throw new IllegalStateException("Arquillian suite did not shut down cleanly", e);
		}
	}

	private TestRunnerAdaptor started(TestRunnerAdaptor starting) throws Exception {
		try {
			starting.beforeSuite();
			return starting;
		} catch (Throwable t) {
			// Stops the containers that did start, a managed server for instance, instead of leaving them running
			ArquillianLifecycle.cleanUpAfter(t, () -> ArquillianLifecycle.endSuite(starting));
			throw t;
		}
	}
}
