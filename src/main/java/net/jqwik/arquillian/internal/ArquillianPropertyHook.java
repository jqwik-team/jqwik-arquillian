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

import org.jboss.arquillian.test.spi.*;

import net.jqwik.api.lifecycle.*;

public class ArquillianPropertyHook implements AroundPropertyHook {
	private static final String REMOTE_REPORT_KEY = "arquillian";

	@Override
	public PropertyExecutionResult aroundProperty(PropertyLifecycleContext context, PropertyExecutor property) throws Throwable {
		return aroundProperty(Adaptors.current(), context, property);
	}

	@Override
	public int aroundPropertyProximity() {
		return Proximity.OUTSIDE_USER_LIFECYCLE_METHODS;
	}

	PropertyExecutionResult aroundProperty(TestRunnerAdaptor adaptor, PropertyLifecycleContext context, PropertyExecutor property)
		throws Throwable {

		final PropertyExecutionResult result;
		try {
			adaptor.before(context.testInstance(), context.targetMethod(), LifecycleMethodExecutor.NO_OP);
			result = ContainerExecution.isActive() ? property.execute() : runThroughArquillian(adaptor, context, property);
		} catch (Throwable failure) {
			ArquillianLifecycle.cleanUpAfter(failure, () -> after(adaptor, context));
			throw failure;
		}
		return ArquillianLifecycle.failureOf(() -> after(adaptor, context))
			.map(afterFailure -> withAfterFailure(result, afterFailure))
			.orElse(result);
	}

	private void after(TestRunnerAdaptor adaptor, PropertyLifecycleContext context) throws Exception {
		adaptor.after(context.testInstance(), context.targetMethod(), LifecycleMethodExecutor.NO_OP);
	}

	private PropertyExecutionResult withAfterFailure(PropertyExecutionResult result, Throwable afterFailure) {
		if (result.status() != PropertyExecutionResult.Status.FAILED || !result.throwable().isPresent()) {
			return result.mapToFailed(afterFailure);
		}
		result.throwable().get().addSuppressed(afterFailure);
		return result;
	}

	private PropertyExecutionResult runThroughArquillian(TestRunnerAdaptor adaptor, PropertyLifecycleContext context,
		PropertyExecutor property) throws Exception {

		final PropertyMethodExecutor executor = new PropertyMethodExecutor(context, property);
		final TestResult remoteResult = adaptor.test(executor);
		return executor.localResult().orElseGet(() -> fromContainer(remoteResult, context));
	}

	private PropertyExecutionResult fromContainer(TestResult remoteResult, PropertyLifecycleContext context) {
		final String report = RemotePropertyExecutionResult.reportOf(remoteResult);
		if (!report.isEmpty()) {
			context.reporter().publishValue(REMOTE_REPORT_KEY, report);
		}
		return RemotePropertyExecutionResult.from(remoteResult);
	}
}
