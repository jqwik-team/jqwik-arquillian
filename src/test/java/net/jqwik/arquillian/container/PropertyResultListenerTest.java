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
import java.util.function.*;
import java.util.stream.*;

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
	private static final List<TestResult.Status> BY_SEVERITY =
		List.of(TestResult.Status.PASSED, TestResult.Status.SKIPPED, TestResult.Status.FAILED);
	private static final Outcome NOTHING_EXECUTED =
		new Outcome(TestResult.Status.FAILED, t -> assertThat(t).isInstanceOf(IllegalStateException.class));

	private final PropertyResultListener listener = new PropertyResultListener();

	@Example
	void failureWithoutThrowableBecomesAnAssertionError() {
		listener.executionFinished(PROPERTY, TestExecutionResult.failed(null));

		assertThat(listener.result().getThrowable()).isInstanceOf(AssertionError.class);
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
	void classFailureAfterAPassedPropertyFailsIt() {
		final RuntimeException cleanup = new RuntimeException("cleanup");

		listener.executionFinished(PROPERTY, TestExecutionResult.successful());
		listener.executionFinished(CLASS, TestExecutionResult.failed(cleanup));

		final TestResult result = listener.result();

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
		assertThat(result.getThrowable()).isSameAs(cleanup);
	}

	@Example
	void nothingExecutedFails() {
		listener.executionFinished(ENGINE, TestExecutionResult.successful());

		final TestResult result = listener.result();

		assertThat(result.getStatus()).isEqualTo(TestResult.Status.FAILED);
		assertThat(result.getThrowable()).isInstanceOf(IllegalStateException.class);
	}

	@Property
	void resultIsTheFirstOfTheMostSevereOutcomes(@ForAll("events") List<Event> events) {
		final Outcome expected = events.stream().map(Event::outcome).flatMap(Optional::stream)
			.max(Comparator.comparing(outcome -> BY_SEVERITY.indexOf(outcome.status())))
			.orElse(NOTHING_EXECUTED);

		final TestResult result = resultAfter(events);

		assertThat(result.getStatus()).isEqualTo(expected.status());
		expected.throwable().accept(result.getThrowable());
	}

	@Property
	void reportEntriesTravelInTheDescriptionInOrder(@ForAll("events") List<Event> events) {
		final String report = events.stream().filter(Reported.class::isInstance).map(event -> ((Reported) event).value())
			.collect(Collectors.joining(System.lineSeparator()));

		assertThat(resultAfter(events).getDescription()).isEqualTo(report);
	}

	@Provide
	Arbitrary<List<Event>> events() {
		final Arbitrary<TestIdentifier> nodes = Arbitraries.of(ENGINE, CLASS, PROPERTY);
		final Arbitrary<String> texts = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(5);
		final Arbitrary<TestExecutionResult> results = Arbitraries.oneOf(
			Arbitraries.just(TestExecutionResult.successful()),
			texts.map(TestAbortedException::new).injectNull(0.3).map(TestExecutionResult::aborted),
			texts.map(AssertionError::new).injectNull(0.3).map(TestExecutionResult::failed)
		);
		return Arbitraries.<Event>oneOf(
			Combinators.combine(nodes, results).as(Finished::new),
			Combinators.combine(nodes, texts).as(Skipped::new),
			Combinators.combine(nodes, texts).as(Reported::new)
		).list().ofMaxSize(8);
	}

	private static TestResult resultAfter(List<Event> events) {
		final PropertyResultListener listener = new PropertyResultListener();
		events.forEach(event -> event.sendTo(listener));
		return listener.result();
	}

	private static TestIdentifier identifier(String name, TestDescriptor.Type type) {
		return TestIdentifier.from(new AbstractTestDescriptor(ENGINE_ID.append("node", name), name) {
			@Override
			public Type getType() {
				return type;
			}
		});
	}

	private record Outcome(TestResult.Status status, Consumer<Throwable> throwable) {
	}

	private sealed interface Event {
		void sendTo(PropertyResultListener listener);

		Optional<Outcome> outcome();
	}

	private record Finished(TestIdentifier node, TestExecutionResult result) implements Event {
		@Override
		public void sendTo(PropertyResultListener listener) {
			listener.executionFinished(node, result);
		}

		@Override
		public Optional<Outcome> outcome() {
			final Throwable carried = result.getThrowable().orElse(null);
			return switch (result.getStatus()) {
				case SUCCESSFUL -> node.isTest()
					? Optional.of(new Outcome(TestResult.Status.PASSED, t -> assertThat(t).isNull()))
					: Optional.empty();
				case ABORTED -> Optional.of(new Outcome(TestResult.Status.SKIPPED, t -> assertThat(t).isSameAs(carried)));
				case FAILED -> Optional.of(new Outcome(TestResult.Status.FAILED, t -> {
					if (carried != null) {
						assertThat(t).isSameAs(carried);
					} else {
						assertThat(t).isInstanceOf(AssertionError.class);
					}
				}));
			};
		}

		@Override
		public String toString() {
			return node.getDisplayName() + " " + result.getStatus() + result.getThrowable().map(t -> " " + t).orElse("");
		}
	}

	private record Skipped(TestIdentifier node, String reason) implements Event {
		@Override
		public void sendTo(PropertyResultListener listener) {
			listener.executionSkipped(node, reason);
		}

		@Override
		public Optional<Outcome> outcome() {
			return Optional.of(new Outcome(TestResult.Status.SKIPPED,
				t -> assertThat(t).isInstanceOf(TestAbortedException.class).hasMessage(reason)));
		}

		@Override
		public String toString() {
			return node.getDisplayName() + " skipped: " + reason;
		}
	}

	private record Reported(TestIdentifier node, String value) implements Event {
		@Override
		public void sendTo(PropertyResultListener listener) {
			listener.reportingEntryPublished(node, ReportEntry.from("key", value));
		}

		@Override
		public Optional<Outcome> outcome() {
			return Optional.empty();
		}

		@Override
		public String toString() {
			return node.getDisplayName() + " reported " + value;
		}
	}
}
