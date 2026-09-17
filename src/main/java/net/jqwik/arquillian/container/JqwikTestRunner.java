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
import org.junit.platform.commons.support.*;
import org.junit.platform.engine.discovery.*;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;

import net.jqwik.arquillian.internal.*;
import net.jqwik.engine.*;

/**
 * Runs one example or property, with all its tries and shrinking, inside the container.
 */
public class JqwikTestRunner implements TestRunner {
	@Override
	public TestResult execute(Class<?> testClass, String methodName) {
		final long start = System.currentTimeMillis();
		final TestResult result = executeGuarded(testClass, methodName);
		return result.setStart(start).setEnd(System.currentTimeMillis());
	}

	private TestResult executeGuarded(Class<?> testClass, String methodName) {
		try {
			return executeInSuite(TestRunnerAdaptorBuilder.build(), testClass, methodName);
		} catch (Exception | LinkageError e) {
			return TestResult.failed(e);
		}
	}

	private TestResult executeInSuite(TestRunnerAdaptor adaptor, Class<?> testClass, String methodName) throws Exception {
		try {
			adaptor.beforeSuite();
			final TestResult result = ContainerExecution.call(adaptor, () -> launch(testClass, methodName));
			adaptor.afterSuite();
			return result;
		} finally {
			adaptor.shutdown();
		}
	}

	private TestResult launch(Class<?> testClass, String methodName) {
		final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
			.selectors(DiscoverySelectors.selectMethod(testClass, propertyMethod(testClass, methodName)))
			.filters(EngineFilter.includeEngines(JqwikTestEngine.ENGINE_ID))
			.build();
		final PropertyResultListener listener = new PropertyResultListener();
		LauncherFactory.create().execute(request, listener);
		return listener.result();
	}

	static Method propertyMethod(Class<?> testClass, String methodName) {
		final List<Method> candidates = ReflectionSupport.findMethods(testClass,
			method -> method.getName().equals(methodName) && !method.isSynthetic(), HierarchyTraversalMode.BOTTOM_UP);
		if (candidates.size() != 1) {
			throw new IllegalArgumentException("Arquillian addresses a property by name only. Expected exactly one method "
				+ testClass.getName() + "#" + methodName + " but found " + candidates.size());
		}
		return candidates.get(0);
	}
}
