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

import org.jboss.arquillian.test.spi.*;
import org.opentest4j.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

import static org.assertj.core.api.Assertions.*;

class RemotePropertyExecutionResultTest {
	@Example
	void passedBecomesSuccessful() {
		final PropertyExecutionResult result = RemotePropertyExecutionResult.from(TestResult.passed());

		assertThat(result.status()).isEqualTo(PropertyExecutionResult.Status.SUCCESSFUL);
		assertThat(result.throwable()).isEmpty();
	}

	@Example
	void failedKeepsTheTransportedThrowable() {
		final AssertionError falsified = new AssertionError("falsified");

		final PropertyExecutionResult result = RemotePropertyExecutionResult.from(TestResult.failed(falsified));

		assertThat(result.status()).isEqualTo(PropertyExecutionResult.Status.FAILED);
		assertThat(result.throwable()).containsSame(falsified);
	}

	@Example
	void failureWithoutThrowableReportsTheDescription() {
		final TestResult remote = TestResult.failed(null);
		remote.setDescription("lost in transport");

		final PropertyExecutionResult result = RemotePropertyExecutionResult.from(remote);

		assertThat(result.throwable()).get().isInstanceOf(AssertionError.class);
		assertThat(result.throwable().get()).hasMessage("lost in transport");
	}

	@Example
	void skippedBecomesAborted() {
		final PropertyExecutionResult result = RemotePropertyExecutionResult.from(TestResult.skipped("assumption"));

		assertThat(result.status()).isEqualTo(PropertyExecutionResult.Status.ABORTED);
		assertThat(result.throwable()).get().isInstanceOf(TestAbortedException.class);
		assertThat(result.throwable().get()).hasMessage("assumption");
	}

	@Example
	void mappingReplacesStatusAndThrowable() {
		final IllegalStateException cause = new IllegalStateException("cleanup failed");

		final PropertyExecutionResult mapped = RemotePropertyExecutionResult.from(TestResult.passed()).mapToFailed(cause);

		assertThat(mapped.status()).isEqualTo(PropertyExecutionResult.Status.FAILED);
		assertThat(mapped.throwable()).containsSame(cause);
	}
}
