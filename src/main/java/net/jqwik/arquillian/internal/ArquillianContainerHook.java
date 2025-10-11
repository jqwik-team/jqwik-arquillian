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

import net.jqwik.api.lifecycle.*;

public class ArquillianContainerHook implements BeforeContainerHook, AfterContainerHook {
	@Override
	public void beforeContainer(ContainerLifecycleContext context) throws Throwable {
		Adaptors.current().beforeClass(testClass(context), LifecycleMethodExecutor.NO_OP);
	}

	@Override
	public void afterContainer(ContainerLifecycleContext context) throws Throwable {
		Adaptors.current().afterClass(testClass(context), LifecycleMethodExecutor.NO_OP);
	}

	@Override
	public int beforeContainerProximity() {
		return Proximity.OUTSIDE_USER_LIFECYCLE_METHODS;
	}

	@Override
	public int afterContainerProximity() {
		return Proximity.OUTSIDE_USER_LIFECYCLE_METHODS;
	}

	private Class<?> testClass(ContainerLifecycleContext context) {
		return context.optionalContainerClass()
			.orElseThrow(() -> new IllegalStateException("Arquillian support needs a container class: " + context.label()));
	}
}
