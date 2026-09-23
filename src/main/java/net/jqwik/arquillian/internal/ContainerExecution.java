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
import java.util.concurrent.*;

import org.jboss.arquillian.test.spi.*;

/**
 * Marks the thread that runs a property inside the container. An embedded container
 * shares the JVM, and possibly the class loader, with the client, so neither a system
 * property nor a plain static can tell the two sides apart.
 */
public final class ContainerExecution {
	private static final ThreadLocal<TestRunnerAdaptor> ADAPTOR = new ThreadLocal<>();

	private ContainerExecution() {
	}

	public static boolean isActive() {
		return adaptor().isPresent();
	}

	static Optional<TestRunnerAdaptor> adaptor() {
		return Optional.ofNullable(ADAPTOR.get());
	}

	public static <T> T call(TestRunnerAdaptor adaptor, Callable<T> inContainer) throws Exception {
		ADAPTOR.set(adaptor);
		try {
			return inContainer.call();
		} finally {
			ADAPTOR.remove();
		}
	}
}
