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

import org.junit.platform.launcher.*;

/**
 * Ends the Arquillian suite with the launcher session. A JVM shutdown hook is too late for that:
 * container adapters remove shutdown hooks of their own while they stop, which the JVM refuses
 * once it shuts down.
 */
public class ArquillianSuiteSessionListener implements LauncherSessionListener {
	@Override
	public void launcherSessionClosed(LauncherSession session) {
		final boolean nestedRunInsideTheContainer = ContainerExecution.isActive();
		if (!nestedRunInsideTheContainer) {
			Adaptors.finishClientSuite();
		}
	}
}
