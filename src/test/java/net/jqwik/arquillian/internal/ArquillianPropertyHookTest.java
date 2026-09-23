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

import java.lang.reflect.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.execution.lifecycle.*;

import static org.assertj.core.api.Assertions.*;

class ArquillianPropertyHookTest {
	private final ArquillianPropertyHook hook = new ArquillianPropertyHook();
	private final PropertyLifecycleContext context = contextOf(this);

	@Example
	void passedPropertyRunsBetweenBeforeAndAfter() throws Throwable {
		final RecordingAdaptor adaptor = new RecordingAdaptor();

		final PropertyExecutionResult result = hook.aroundProperty(adaptor, context, () -> {
			adaptor.getCalls().add("property");
			return PlainExecutionResult.successful();
		});

		assertThat(result.status()).isEqualTo(PropertyExecutionResult.Status.SUCCESSFUL);
		assertThat(adaptor.getCalls()).containsExactly("before", "test", "property", "after");
	}

	@Example
	void afterRunsWhenBeforeFails() {
		final NoClassDefFoundError enrichmentFailure = new NoClassDefFoundError("enricher");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("before", enrichmentFailure);

		assertThatThrownBy(() -> hook.aroundProperty(adaptor, context, PlainExecutionResult::successful)).isSameAs(enrichmentFailure);
		assertThat(adaptor.getCalls()).containsExactly("before", "after");
	}

	@Example
	void afterFailureJoinsTheBeforeFailure() {
		final IllegalStateException enrichmentFailure = new IllegalStateException("enrichment");
		final IllegalStateException cleanupFailure = new IllegalStateException("cleanup");
		final RecordingAdaptor adaptor = new RecordingAdaptor()
			.failing("before", enrichmentFailure)
			.failing("after", cleanupFailure);

		assertThatThrownBy(() -> hook.aroundProperty(adaptor, context, PlainExecutionResult::successful)).isSameAs(enrichmentFailure);
		assertThat(enrichmentFailure.getSuppressed()).containsExactly(cleanupFailure);
	}

	@Example
	void afterRunsWhenArquillianCannotRunTheProperty() {
		final IllegalStateException protocolFailure = new IllegalStateException("protocol");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("test", protocolFailure);

		assertThatThrownBy(() -> hook.aroundProperty(adaptor, context, PlainExecutionResult::successful)).isSameAs(protocolFailure);
		assertThat(adaptor.getCalls()).containsExactly("before", "test", "after");
	}

	@Example
	void afterFailureFailsAPassedProperty() throws Throwable {
		final IllegalStateException cleanupFailure = new IllegalStateException("cleanup");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("after", cleanupFailure);

		final PropertyExecutionResult result = hook.aroundProperty(adaptor, context, PlainExecutionResult::successful);

		assertThat(result.status()).isEqualTo(PropertyExecutionResult.Status.FAILED);
		assertThat(result.throwable()).containsSame(cleanupFailure);
	}

	@Example
	void afterFailureJoinsAFailedProperty() throws Throwable {
		final AssertionError falsified = new AssertionError("falsified");
		final IllegalStateException cleanupFailure = new IllegalStateException("cleanup");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("after", cleanupFailure);
		final PropertyExecutionResult falsifiedResult = PlainExecutionResult.failed(falsified, "42");

		final PropertyExecutionResult result = hook.aroundProperty(adaptor, context, () -> falsifiedResult);

		assertThat(result).isSameAs(falsifiedResult);
		assertThat(falsified.getSuppressed()).containsExactly(cleanupFailure);
	}

	@Example
	void errorFromAnAfterObserverFailsAPassedProperty() throws Throwable {
		final AssertionError dataSetMismatch = new AssertionError("data set");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("after", dataSetMismatch);

		final PropertyExecutionResult result = hook.aroundProperty(adaptor, context, PlainExecutionResult::successful);

		assertThat(result.status()).isEqualTo(PropertyExecutionResult.Status.FAILED);
		assertThat(result.throwable()).containsSame(dataSetMismatch);
	}

	@Example
	void errorFromAfterJoinsTheBeforeFailure() {
		final IllegalStateException enrichmentFailure = new IllegalStateException("enrichment");
		final NoClassDefFoundError cleanupError = new NoClassDefFoundError("observer");
		final RecordingAdaptor adaptor = new RecordingAdaptor()
			.failing("before", enrichmentFailure)
			.failing("after", cleanupError);

		assertThatThrownBy(() -> hook.aroundProperty(adaptor, context, PlainExecutionResult::successful)).isSameAs(enrichmentFailure);
		assertThat(enrichmentFailure.getSuppressed()).containsExactly(cleanupError);
	}

	private static PropertyLifecycleContext contextOf(Object testInstance) {
		final Method targetMethod = ArquillianPropertyHookTest.class.getDeclaredMethods()[0];
		return (PropertyLifecycleContext) Proxy.newProxyInstance(PropertyLifecycleContext.class.getClassLoader(),
			new Class<?>[] {PropertyLifecycleContext.class}, (proxy, method, arguments) -> {
				switch (method.getName()) {
					case "testInstance":
						return testInstance;
					case "targetMethod":
						return targetMethod;
					case "toString":
						return "property lifecycle context";
					default:
						throw new UnsupportedOperationException(method.getName());
				}
			});
	}
}
