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

final class Proximity {
	/**
	 * jqwik runs its annotated lifecycle methods at -10. Staying below that deploys and enriches
	 * before the first user lifecycle method and cleans up after the last one.
	 */
	static final int OUTSIDE_USER_LIFECYCLE_METHODS = -15;

	private Proximity() {
	}
}
