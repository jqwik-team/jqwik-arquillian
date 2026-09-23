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
import java.util.*;

import lombok.*;
import org.jboss.arquillian.test.spi.*;
import org.jboss.arquillian.test.spi.event.suite.*;

/**
 * Records the lifecycle calls it receives and fails the ones it is told to, with a runtime exception or an error.
 * Like Arquillian for a property that runs as client, it runs the test method itself.
 */
public class RecordingAdaptor implements TestRunnerAdaptor {
	@Getter
	private final List<String> calls = new ArrayList<>();
	private final Map<String, Throwable> failures = new HashMap<>();

	public RecordingAdaptor failing(String call, RuntimeException failure) {
		failures.put(call, failure);
		return this;
	}

	public RecordingAdaptor failing(String call, Error failure) {
		failures.put(call, failure);
		return this;
	}

	@Override
	public void beforeSuite() {
		record("beforeSuite");
	}

	@Override
	public void afterSuite() {
		record("afterSuite");
	}

	@Override
	public void beforeClass(Class<?> testClass, LifecycleMethodExecutor executor) {
		record("beforeClass");
	}

	@Override
	public void afterClass(Class<?> testClass, LifecycleMethodExecutor executor) {
		record("afterClass");
	}

	@Override
	public void before(Object testInstance, Method testMethod, LifecycleMethodExecutor executor) {
		record("before");
	}

	@Override
	public void after(Object testInstance, Method testMethod, LifecycleMethodExecutor executor) {
		record("after");
	}

	@Override
	public TestResult test(TestMethodExecutor testMethodExecutor) {
		record("test");
		try {
			testMethodExecutor.invoke();
			return TestResult.passed();
		} catch (Throwable t) {
			return TestResult.failed(t);
		}
	}

	@Override
	public <T extends TestLifecycleEvent> void fireCustomLifecycle(T event) {
		record("fireCustomLifecycle");
	}

	@Override
	public void shutdown() {
		record("shutdown");
	}

	private void record(String call) {
		calls.add(call);
		final Throwable failure = failures.get(call);
		if (failure instanceof Error) {
			throw (Error) failure;
		}
		if (failure != null) {
			throw (RuntimeException) failure;
		}
	}
}
