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
package net.jqwik.arquillian.internal;

import java.util.*;

import org.jboss.arquillian.test.spi.*;

/**
 * Ends what was begun even when beginning or running it failed, like Arquillian's JUnit integrations
 * run their after callbacks. A failure while ending never hides the failure that came first.
 *
 * <p>Errors count as failures too: Arquillian hands an observer's {@code AssertionError} or
 * {@code NoClassDefFoundError} through unchanged. Only an {@code OutOfMemoryError} ends the run
 * right away, as it does in jqwik.</p>
 */
public final class ArquillianLifecycle {
	private ArquillianLifecycle() {
	}

	public static void endSuite(TestRunnerAdaptor adaptor) throws Exception {
		try {
			adaptor.afterSuite();
		} catch (Throwable t) {
			cleanUpAfter(t, adaptor::shutdown);
			throw t;
		}
		adaptor.shutdown();
	}

	public static Optional<Throwable> failureOf(Step step) {
		try {
			step.run();
			return Optional.empty();
		} catch (OutOfMemoryError e) {
			throw e;
		} catch (Throwable t) {
			return Optional.of(t);
		}
	}

	static void cleanUpAfter(Throwable failure, Step cleanup) {
		failureOf(cleanup).filter(cleanupFailure -> cleanupFailure != failure).ifPresent(failure::addSuppressed);
	}

	@FunctionalInterface
	public interface Step {
		void run() throws Exception;
	}
}
