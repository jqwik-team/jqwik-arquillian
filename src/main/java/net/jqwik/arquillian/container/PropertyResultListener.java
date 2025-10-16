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
package net.jqwik.arquillian.container;

import java.util.*;

import org.jboss.arquillian.test.spi.*;
import org.junit.platform.engine.*;
import org.junit.platform.engine.reporting.*;
import org.junit.platform.launcher.*;

/**
 * Collects what has to travel back to the client. jqwik publishes seed, original and shrunk
 * sample as report entries, not as part of the failure, so they are carried in the description.
 */
final class PropertyResultListener implements TestExecutionListener {
	private final StringJoiner report = new StringJoiner(System.lineSeparator());
	private TestResult result;

	TestResult result() {
		final TestResult collected = result != null ? result : TestResult.failed(new IllegalStateException("No property was executed"));
		collected.addDescription(report.toString());
		return collected;
	}

	@Override
	public void executionFinished(TestIdentifier identifier, TestExecutionResult executionResult) {
		final boolean failedOutsideTheProperty = executionResult.getStatus() != TestExecutionResult.Status.SUCCESSFUL;
		if (identifier.isTest() || (failedOutsideTheProperty && result == null)) {
			result = toTestResult(executionResult);
		}
	}

	@Override
	public void executionSkipped(TestIdentifier identifier, String reason) {
		if (identifier.isTest()) {
			result = TestResult.skipped(reason);
		}
	}

	@Override
	public void reportingEntryPublished(TestIdentifier identifier, ReportEntry entry) {
		entry.getKeyValuePairs().values().forEach(report::add);
	}

	private TestResult toTestResult(TestExecutionResult executionResult) {
		switch (executionResult.getStatus()) {
			case SUCCESSFUL:
				return TestResult.passed();
			case ABORTED:
				return TestResult.skipped(executionResult.getThrowable().orElse(null));
			default:
				return TestResult.failed(executionResult.getThrowable()
					.orElseGet(() -> new AssertionError("Property failed without a throwable")));
		}
	}
}
