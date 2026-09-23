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

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class ArquillianLifecycleTest {
	@Example
	void endSuiteShutsDownWhenAfterSuiteFails() {
		final IllegalStateException stopFailure = new IllegalStateException("stop");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("afterSuite", stopFailure);

		assertThatThrownBy(() -> ArquillianLifecycle.endSuite(adaptor)).isSameAs(stopFailure);
		assertThat(adaptor.getCalls()).containsExactly("afterSuite", "shutdown");
	}

	@Example
	void failedShutdownJoinsTheAfterSuiteFailure() {
		final IllegalStateException stopFailure = new IllegalStateException("stop");
		final IllegalStateException shutdownFailure = new IllegalStateException("shutdown");
		final RecordingAdaptor adaptor = new RecordingAdaptor()
			.failing("afterSuite", stopFailure)
			.failing("shutdown", shutdownFailure);

		assertThatThrownBy(() -> ArquillianLifecycle.endSuite(adaptor)).isSameAs(stopFailure);
		assertThat(stopFailure.getSuppressed()).containsExactly(shutdownFailure);
	}

	@Example
	void outOfMemoryEndsTheRunInsteadOfBecomingAFailure() {
		final OutOfMemoryError outOfMemory = new OutOfMemoryError("heap");

		assertThatThrownBy(() -> ArquillianLifecycle.failureOf(() -> {
			throw outOfMemory;
		})).isSameAs(outOfMemory);
	}

	@Example
	void failureOfHandsBackWhatTheStepThrew() {
		final IllegalStateException stepFailure = new IllegalStateException("step");

		assertThat(ArquillianLifecycle.failureOf(() -> {
			throw stepFailure;
		})).containsSame(stepFailure);
		assertThat(ArquillianLifecycle.failureOf(() -> { })).isEmpty();
	}

	@Example
	void failureOfHandsBackErrorsToo() {
		final AssertionError observerError = new AssertionError("observer");

		assertThat(ArquillianLifecycle.failureOf(() -> {
			throw observerError;
		})).containsSame(observerError);
	}

	@Example
	void cleanUpAfterAddsTheCleanupFailureAsSuppressed() {
		final IllegalStateException failure = new IllegalStateException("first");
		final IllegalStateException cleanupFailure = new IllegalStateException("cleanup");

		ArquillianLifecycle.cleanUpAfter(failure, () -> {
			throw cleanupFailure;
		});

		assertThat(failure.getSuppressed()).containsExactly(cleanupFailure);
	}

	@Example
	void cleanUpAfterNeverSuppressesAFailureIntoItself() {
		final IllegalStateException failure = new IllegalStateException("rethrown by the cleanup");

		ArquillianLifecycle.cleanUpAfter(failure, () -> {
			throw failure;
		});

		assertThat(failure.getSuppressed()).isEmpty();
	}
}
