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

import java.util.function.*;

import org.jboss.arquillian.test.spi.*;
import org.opentest4j.*;

import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.execution.lifecycle.*;

/**
 * Outcome of a property that ran inside the container. Seed, samples and counts stay there;
 * they reach the client as part of the transported report.
 *
 * <p>The engine only reports results of its own type and warns about any other implementation
 * on every property, hence the dependency on an engine class instead of an own implementation.</p>
 */
final class RemotePropertyExecutionResult {
	private static final String SEED_STAYS_IN_CONTAINER = null;

	private RemotePropertyExecutionResult() {
	}

	static PropertyExecutionResult from(TestResult remoteResult) {
		switch (remoteResult.getStatus()) {
			case PASSED:
				return PlainExecutionResult.successful();
			case SKIPPED:
				return PlainExecutionResult.aborted(reasonOf(remoteResult, TestAbortedException::new), SEED_STAYS_IN_CONTAINER);
			default:
				return PlainExecutionResult.failed(reasonOf(remoteResult, AssertionError::new), SEED_STAYS_IN_CONTAINER);
		}
	}

	private static Throwable reasonOf(TestResult remoteResult, Function<String, Throwable> describedReason) {
		final Throwable transported = remoteResult.getThrowable();
		return transported != null ? transported : describedReason.apply(remoteResult.getDescription());
	}
}
