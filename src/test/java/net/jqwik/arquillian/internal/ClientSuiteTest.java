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

import java.util.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class ClientSuiteTest {
	@Example
	void startsTheSuiteOnceForAllProperties() throws Throwable {
		final RecordingAdaptor adaptor = new RecordingAdaptor();
		final ClientSuite suite = suiteBuilding(adaptor);

		assertThat(suite.adaptor()).isSameAs(adaptor);
		assertThat(suite.adaptor()).isSameAs(adaptor);
		assertThat(adaptor.getCalls()).containsExactly("beforeSuite");
	}

	@Example
	void finishEndsTheStartedSuite() throws Throwable {
		final RecordingAdaptor adaptor = new RecordingAdaptor();
		final ClientSuite suite = suiteBuilding(adaptor);
		suite.adaptor();

		suite.finish();
		suite.finish();

		assertThat(adaptor.getCalls()).containsExactly("beforeSuite", "afterSuite", "shutdown");
	}

	@Example
	void failureToEndIsReported() throws Throwable {
		final IllegalStateException stopFailure = new IllegalStateException("stop");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("afterSuite", stopFailure);
		final ClientSuite suite = suiteBuilding(adaptor);
		suite.adaptor();

		assertThatIllegalStateException().isThrownBy(suite::finish).withCause(stopFailure);
		assertThat(adaptor.getCalls()).endsWith("shutdown");
	}

	@Example
	void failedStartEndsWhatDidStart() {
		final IllegalStateException startFailure = new IllegalStateException("start");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("beforeSuite", startFailure);

		assertThatThrownBy(suiteBuilding(adaptor)::adaptor).isSameAs(startFailure);
		assertThat(adaptor.getCalls()).containsExactly("beforeSuite", "afterSuite", "shutdown");
	}

	@Example
	void failureToEndAFailedStartIsSuppressed() {
		final IllegalStateException startFailure = new IllegalStateException("start");
		final IllegalStateException stopFailure = new IllegalStateException("stop");
		final RecordingAdaptor adaptor = new RecordingAdaptor()
			.failing("beforeSuite", startFailure)
			.failing("afterSuite", stopFailure);

		assertThatThrownBy(suiteBuilding(adaptor)::adaptor).isSameAs(startFailure);
		assertThat(startFailure.getSuppressed()).containsExactly(stopFailure);
		assertThat(adaptor.getCalls()).endsWith("shutdown");
	}

	@Example
	void startErrorEndsWhatDidStart() {
		final NoClassDefFoundError missingAdapterClass = new NoClassDefFoundError("adapter");
		final RecordingAdaptor adaptor = new RecordingAdaptor().failing("beforeSuite", missingAdapterClass);

		assertThatThrownBy(suiteBuilding(adaptor)::adaptor).isSameAs(missingAdapterClass);
		assertThat(adaptor.getCalls()).containsExactly("beforeSuite", "afterSuite", "shutdown");
	}

	@Example
	void failedStartStaysFailedUntilTheSessionCloses() throws Throwable {
		final NoClassDefFoundError startFailure = new NoClassDefFoundError("adapter");
		final RecordingAdaptor restarted = new RecordingAdaptor();
		final Iterator<RecordingAdaptor> builds = Arrays.asList(
			new RecordingAdaptor().failing("beforeSuite", startFailure),
			restarted
		).iterator();
		final ClientSuite suite = new ClientSuite(builds::next);

		assertThatThrownBy(suite::adaptor).isSameAs(startFailure);
		assertThatThrownBy(suite::adaptor).isSameAs(startFailure);
		suite.finish();

		assertThat(suite.adaptor()).isSameAs(restarted);
	}

	private ClientSuite suiteBuilding(RecordingAdaptor adaptor) {
		return new ClientSuite(() -> adaptor);
	}
}
