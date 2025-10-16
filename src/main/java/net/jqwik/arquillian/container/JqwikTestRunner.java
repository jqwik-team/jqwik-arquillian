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

import java.lang.reflect.*;
import java.util.*;

import org.jboss.arquillian.container.test.spi.*;
import org.jboss.arquillian.test.spi.*;
import org.junit.platform.engine.discovery.*;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;

import net.jqwik.arquillian.internal.*;

/**
 * Runs one example or property, with all its tries and shrinking, inside the container.
 */
public class JqwikTestRunner implements TestRunner {
	private static final String JQWIK_ENGINE_ID = "jqwik";

	@Override
	public TestResult execute(Class<?> testClass, String methodName) {
		final long start = System.currentTimeMillis();
		final TestResult result = executeGuarded(testClass, methodName);
		return result.setStart(start).setEnd(System.currentTimeMillis());
	}

	private TestResult executeGuarded(Class<?> testClass, String methodName) {
		try {
			final TestRunnerAdaptor adaptor = TestRunnerAdaptorBuilder.build();
			try {
				adaptor.beforeSuite();
				final TestResult result = ContainerExecution.call(adaptor, () -> launch(testClass, methodName));
				adaptor.afterSuite();
				return result;
			} finally {
				adaptor.shutdown();
			}
		} catch (Exception | LinkageError e) {
			return TestResult.failed(e);
		}
	}

	private TestResult launch(Class<?> testClass, String methodName) {
		final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
			.selectors(DiscoverySelectors.selectMethod(testClass, propertyMethod(testClass, methodName)))
			.filters(EngineFilter.includeEngines(JQWIK_ENGINE_ID))
			.build();
		final PropertyResultListener listener = new PropertyResultListener();
		LauncherFactory.create().execute(request, listener);
		return listener.result();
	}

	private Method propertyMethod(Class<?> testClass, String methodName) {
		final List<Method> candidates = new ArrayList<>();
		for (Class<?> type = testClass; type != null && candidates.isEmpty(); type = type.getSuperclass()) {
			Arrays.stream(type.getDeclaredMethods())
				.filter(method -> method.getName().equals(methodName) && !method.isSynthetic())
				.forEach(candidates::add);
		}
		if (candidates.size() != 1) {
			throw new IllegalArgumentException("Arquillian addresses a property by name only. Expected exactly one method "
				+ testClass.getName() + "#" + methodName + " but found " + candidates.size());
		}
		return candidates.get(0);
	}
}
