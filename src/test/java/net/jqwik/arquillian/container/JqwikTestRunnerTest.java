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

import java.util.*;

import org.jboss.arquillian.test.spi.*;

import net.jqwik.api.*;
import net.jqwik.arquillian.internal.*;

import static org.assertj.core.api.Assertions.*;

class JqwikTestRunnerTest {
	@Example
	void launchesInsideAStartedSuite() {
		final RecordingAdaptor adaptor = new RecordingAdaptor();

		final TestResult result = JqwikTestRunner.executeInSuite(adaptor, () -> {
			adaptor.getCalls().add(ContainerExecution.isActive() ? "launch in container" : "launch");
			return TestResult.passed();
		});

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.PASSED);
		assertThat(adaptor.getCalls()).containsExactly("beforeSuite", "launch in container", "afterSuite", "shutdown");
	}

	@Example
	void endsTheSuiteWhenItFailedToStart() {
		final IllegalStateException startFailure = new IllegalStateException("start");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("beforeSuite", startFailure);

		final TestResult result = JqwikTestRunner.executeInSuite(adaptor, TestResult::passed);

		assertThat(result.getThrowable()).isSameAs(startFailure);
		assertThat(adaptor.getCalls()).containsExactly("beforeSuite", "afterSuite", "shutdown");
	}

	@Example
	void endsTheSuiteWhenTheLaunchThrows() {
		final IllegalArgumentException lookupFailure = new IllegalArgumentException("lookup");
		final RecordingAdaptor adaptor = new RecordingAdaptor();

		final TestResult result = JqwikTestRunner.executeInSuite(adaptor, () -> {
			throw lookupFailure;
		});

		assertThat(result.getThrowable()).isSameAs(lookupFailure);
		assertThat(adaptor.getCalls()).containsExactly("beforeSuite", "afterSuite", "shutdown");
	}

	@Example
	void endsTheSuiteWhenTheLaunchThrowsAnError() {
		final ServiceConfigurationError unloadableEngine = new ServiceConfigurationError("engine");
		final RecordingAdaptor adaptor = new RecordingAdaptor();

		final TestResult result = JqwikTestRunner.executeInSuite(adaptor, () -> {
			throw unloadableEngine;
		});

		assertThat(result.getThrowable()).isSameAs(unloadableEngine);
		assertThat(adaptor.getCalls()).containsExactly("beforeSuite", "afterSuite", "shutdown");
	}

	@Example
	void failureToEndFailsAPassedPropertyAndKeepsItsReport() {
		final IllegalStateException stopFailure = new IllegalStateException("stop");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("afterSuite", stopFailure);

		final TestResult result = JqwikTestRunner.executeInSuite(adaptor, () -> TestResult.passed("seed = 42"));

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
		assertThat(result.getThrowable()).isSameAs(stopFailure);
		assertThat(result.getDescription()).isEqualTo("seed = 42");
	}

	@Example
	void errorWhileEndingJoinsAFailedProperty() {
		final AssertionError falsified = new AssertionError("falsified");
		final NoClassDefFoundError stopFailure = new NoClassDefFoundError("observer");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("afterSuite", stopFailure);
		final TestResult falsifiedResult = TestResult.failed(falsified);

		final TestResult result = JqwikTestRunner.executeInSuite(adaptor, () -> falsifiedResult);

		assertThat(result).isSameAs(falsifiedResult);
		assertThat(falsified.getSuppressed()).containsExactly(stopFailure);
	}

	@Example
	void findsAPropertyDeclaredInASuperclass() throws NoSuchMethodException {
		assertThat(JqwikTestRunner.propertyMethod(Subclass.class, "inherited"))
			.isEqualTo(Superclass.class.getDeclaredMethod("inherited"));
	}

	@Example
	void findsAPropertyDeclaredAsInterfaceDefaultMethod() throws NoSuchMethodException {
		assertThat(JqwikTestRunner.propertyMethod(Subclass.class, "fromInterface"))
			.isEqualTo(WithDefaultMethod.class.getDeclaredMethod("fromInterface"));
	}

	@Example
	void findsTheOverridingMethodOnly() throws NoSuchMethodException {
		assertThat(JqwikTestRunner.propertyMethod(Subclass.class, "overridden"))
			.isEqualTo(Subclass.class.getDeclaredMethod("overridden"));
	}

	@Example
	void rejectsANameThatIsNotUnique() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> JqwikTestRunner.propertyMethod(Subclass.class, "overloaded"))
			.withMessageEndingWith("but found 2");
	}

	interface WithDefaultMethod {
		default void fromInterface() {
		}
	}

	static class Superclass {
		void inherited() {
		}

		void overridden() {
		}
	}

	static class Subclass extends Superclass implements WithDefaultMethod {
		@Override
		void overridden() {
		}

		void overloaded() {
		}

		void overloaded(int tries) {
		}
	}
}
