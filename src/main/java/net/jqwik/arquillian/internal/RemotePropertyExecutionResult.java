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

import net.jqwik.api.lifecycle.*;

/**
 * Outcome of a property that ran inside the container. Seed, samples and counts stay there;
 * they reach the client as part of the transported report.
 */
final class RemotePropertyExecutionResult implements PropertyExecutionResult {
	private final Status status;
	private final Throwable throwable;

	private RemotePropertyExecutionResult(Status status, Throwable throwable) {
		this.status = status;
		this.throwable = throwable;
	}

	static PropertyExecutionResult from(TestResult remoteResult) {
		switch (remoteResult.getStatus()) {
			case PASSED:
				return new RemotePropertyExecutionResult(Status.SUCCESSFUL, null);
			case SKIPPED:
				return new RemotePropertyExecutionResult(Status.ABORTED, remoteResult.getThrowable());
			default:
				return new RemotePropertyExecutionResult(Status.FAILED, failureOf(remoteResult));
		}
	}

	private static Throwable failureOf(TestResult remoteResult) {
		final Throwable transported = remoteResult.getThrowable();
		return transported != null ? transported : new AssertionError(remoteResult.getDescription());
	}

	@Override
	public Optional<String> seed() {
		return Optional.empty();
	}

	@Override
	public Optional<List<Object>> falsifiedParameters() {
		return Optional.empty();
	}

	@Override
	public Status status() {
		return status;
	}

	@Override
	public Optional<Throwable> throwable() {
		return Optional.ofNullable(throwable);
	}

	@Override
	public int countChecks() {
		return 0;
	}

	@Override
	public int countTries() {
		return 0;
	}

	@Override
	public Optional<FalsifiedSample> originalSample() {
		return Optional.empty();
	}

	@Override
	public Optional<ShrunkFalsifiedSample> shrunkSample() {
		return Optional.empty();
	}

	@Override
	public PropertyExecutionResult mapTo(Status newStatus, Throwable newThrowable) {
		return new RemotePropertyExecutionResult(newStatus, newThrowable);
	}
}
