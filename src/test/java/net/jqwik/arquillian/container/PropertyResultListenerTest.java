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
package net.jqwik.arquillian.container;

import org.jboss.arquillian.test.spi.*;
import org.junit.platform.engine.*;
import org.junit.platform.engine.reporting.*;
import org.junit.platform.engine.support.descriptor.*;
import org.junit.platform.launcher.*;
import org.opentest4j.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class PropertyResultListenerTest {
	private static final UniqueId ENGINE_ID = UniqueId.forEngine("jqwik");
	private static final TestIdentifier ENGINE = TestIdentifier.from(new EngineDescriptor(ENGINE_ID, "jqwik"));
	private static final TestIdentifier CLASS = identifier("class", TestDescriptor.Type.CONTAINER);
	private static final TestIdentifier PROPERTY = identifier("property", TestDescriptor.Type.TEST);

	private final PropertyResultListener listener = new PropertyResultListener();

	@Example
	void passedPropertyPassesWithAnEmptyDescription() {
		listener.executionFinished(PROPERTY, TestExecutionResult.successful());
		listener.executionFinished(CLASS, TestExecutionResult.successful());
		listener.executionFinished(ENGINE, TestExecutionResult.successful());

		final TestResult result = listener.result();

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.PASSED);
		assertThat(result.getDescription()).isEmpty();
	}

	@Example
	void failedPropertyKeepsItsThrowable() {
		final AssertionError falsified = new AssertionError("falsified");

		listener.executionFinished(PROPERTY, TestExecutionResult.failed(falsified));

		final TestResult result = listener.result();

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
		assertThat(result.getThrowable()).isSameAs(falsified);
	}

	@Example
	void failureWithoutThrowableBecomesAnAssertionError() {
		listener.executionFinished(PROPERTY, TestExecutionResult.failed(null));

		assertThat(listener.result().getThrowable()).isInstanceOf(AssertionError.class);
	}

	@Example
	void abortedPropertyIsSkippedWithItsThrowable() {
		final TestAbortedException assumption = new TestAbortedException("assumption");

		listener.executionFinished(PROPERTY, TestExecutionResult.aborted(assumption));

		final TestResult result = listener.result();

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.SKIPPED);
		assertThat(result.getThrowable()).isSameAs(assumption);
	}

	@Example
	void skippedPropertyCarriesTheReasonAsThrowable() {
		listener.executionSkipped(PROPERTY, "disabled");

		final TestResult result = listener.result();

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.SKIPPED);
		assertThat(result.getThrowable()).isInstanceOf(TestAbortedException.class).hasMessage("disabled");
		assertThat(result.getDescription()).isEmpty();
	}

	@Example
	void skippedClassSkipsTheProperty() {
		listener.executionSkipped(CLASS, "disabled");
		listener.executionFinished(ENGINE, TestExecutionResult.successful());

		assertThat(listener.result().getStatus()).isEqualTo(TestResult.Status.SKIPPED);
	}

	@Example
	void classFailureBeforeThePropertyIsKept() {
		final RuntimeException deployment = new RuntimeException("deployment");

		listener.executionFinished(CLASS, TestExecutionResult.failed(deployment));
		listener.executionFinished(ENGINE, TestExecutionResult.failed(new RuntimeException("engine")));

		assertThat(listener.result().getThrowable()).isSameAs(deployment);
	}

	@Example
	void classFailureAfterAPassedPropertyFailsIt() {
		final RuntimeException cleanup = new RuntimeException("cleanup");

		listener.executionFinished(PROPERTY, TestExecutionResult.successful());
		listener.executionFinished(CLASS, TestExecutionResult.failed(cleanup));

		final TestResult result = listener.result();

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
		assertThat(result.getThrowable()).isSameAs(cleanup);
	}

	@Example
	void failedPropertyOutweighsALaterClassFailure() {
		final AssertionError falsified = new AssertionError("falsified");

		listener.executionFinished(PROPERTY, TestExecutionResult.failed(falsified));
		listener.executionFinished(CLASS, TestExecutionResult.failed(new RuntimeException("cleanup")));

		assertThat(listener.result().getThrowable()).isSameAs(falsified);
	}

	@Example
	void reportEntriesTravelInTheDescriptionInOrder() {
		listener.reportingEntryPublished(PROPERTY, ReportEntry.from("seed", "42"));
		listener.reportingEntryPublished(PROPERTY, ReportEntry.from("sample", "[a]"));
		listener.executionFinished(PROPERTY, TestExecutionResult.successful());

		assertThat(listener.result().getDescription()).isEqualTo("42" + System.lineSeparator() + "[a]");
	}

	@Example
	void nothingExecutedFails() {
		listener.executionFinished(ENGINE, TestExecutionResult.successful());

		final TestResult result = listener.result();

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
		assertThat(result.getThrowable()).isInstanceOf(IllegalStateException.class);
	}

	private static TestIdentifier identifier(String name, TestDescriptor.Type type) {
		return TestIdentifier.from(new AbstractTestDescriptor(ENGINE_ID.append("node", name), name) {
			@Override
			public Type getType() {
				return type;
			}
		});
	}
}
