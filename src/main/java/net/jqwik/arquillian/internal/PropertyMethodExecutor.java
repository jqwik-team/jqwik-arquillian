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

import java.lang.reflect.*;
import java.util.*;

import org.jboss.arquillian.test.spi.*;

import net.jqwik.api.lifecycle.*;

/**
 * Arquillian only invokes this executor when the property runs as client. A property that runs
 * in-container is started there by name, and the executor is never invoked.
 */
final class PropertyMethodExecutor implements TestMethodExecutor {
	private final PropertyLifecycleContext context;
	private final PropertyExecutor property;
	private PropertyExecutionResult localResult;

	PropertyMethodExecutor(PropertyLifecycleContext context, PropertyExecutor property) {
		this.context = context;
		this.property = property;
	}

	Optional<PropertyExecutionResult> localResult() {
		return Optional.ofNullable(localResult);
	}

	@Override
	public String getMethodName() {
		return getMethod().getName();
	}

	@Override
	public Method getMethod() {
		return context.targetMethod();
	}

	@Override
	public Object getInstance() {
		return context.testInstance();
	}

	@Override
	public void invoke(Object... ignoredBecauseJqwikGeneratesTheParameters) {
		localResult = property.execute();
	}
}
