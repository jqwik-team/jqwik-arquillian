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

public class ArquillianPropertyHook implements AroundPropertyHook {
	private static final String REMOTE_REPORT_KEY = "arquillian";

	@Override
	public PropertyExecutionResult aroundProperty(PropertyLifecycleContext context, PropertyExecutor property) throws Throwable {
		final TestRunnerAdaptor adaptor = Adaptors.current();
		adaptor.before(context.testInstance(), context.targetMethod(), LifecycleMethodExecutor.NO_OP);
		try {
			return ContainerExecution.isActive() ? property.execute() : runThroughArquillian(adaptor, context, property);
		} finally {
			adaptor.after(context.testInstance(), context.targetMethod(), LifecycleMethodExecutor.NO_OP);
		}
	}

	@Override
	public int aroundPropertyProximity() {
		return Proximity.OUTSIDE_USER_LIFECYCLE_METHODS;
	}

	private PropertyExecutionResult runThroughArquillian(TestRunnerAdaptor adaptor, PropertyLifecycleContext context,
		PropertyExecutor property) throws Exception {

		final PropertyMethodExecutor executor = new PropertyMethodExecutor(context, property);
		final TestResult remoteResult = adaptor.test(executor);
		return executor.localResult().orElseGet(() -> fromContainer(remoteResult, context));
	}

	private PropertyExecutionResult fromContainer(TestResult remoteResult, PropertyLifecycleContext context) {
		final String report = remoteResult.getDescription();
		if (report != null && !report.isBlank()) {
			context.reporter().publishValue(REMOTE_REPORT_KEY, report);
		}
		return RemotePropertyExecutionResult.from(remoteResult);
	}
}
