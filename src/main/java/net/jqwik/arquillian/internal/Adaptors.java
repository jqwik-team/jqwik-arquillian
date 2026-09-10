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
package net.jqwik.arquillian.internal;

import java.util.*;

import org.jboss.arquillian.test.spi.*;

/**
 * The client side adaptor lives for the whole launcher session, like the Arquillian suite it stands for.
 * It is deliberately not kept in a jqwik store: a property running in an embedded container
 * starts a nested engine run that shares, and clears, the store repository of the client run.
 */
final class Adaptors {
	private static TestRunnerAdaptor client;
	private static Exception suiteStartFailure;

	private Adaptors() {
	}

	static TestRunnerAdaptor current() throws Exception {
		final Optional<TestRunnerAdaptor> inContainer = ContainerExecution.adaptor();
		return inContainer.isPresent() ? inContainer.get() : client();
	}

	private static synchronized TestRunnerAdaptor client() throws Exception {
		// A container cannot be started twice, and a retry would bury the reason the first start failed
		if (suiteStartFailure != null) {
			throw suiteStartFailure;
		}
		if (client == null) {
			client = startSuite();
		}
		return client;
	}

	private static TestRunnerAdaptor startSuite() throws Exception {
		try {
			final TestRunnerAdaptor adaptor = TestRunnerAdaptorBuilder.build();
			adaptor.beforeSuite();
			return adaptor;
		} catch (Exception e) {
			suiteStartFailure = e;
			throw e;
		}
	}

	static synchronized void finishClientSuite() {
		if (client == null) {
			return;
		}
		final TestRunnerAdaptor finished = client;
		client = null;
		try {
			finished.afterSuite();
		} catch (Exception e) {
			throw new IllegalStateException("Arquillian suite did not shut down cleanly", e);
		} finally {
			finished.shutdown();
		}
	}
}
