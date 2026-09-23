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

import net.jqwik.api.lifecycle.*;

public class ArquillianRegistrar implements RegistrarHook {
	@Override
	public void registerHooks(Registrar registrar) {
		registrar.register(ArquillianContainerHook.class, PropagationMode.NO_DESCENDANTS);
		registrar.register(ArquillianPropertyHook.class, PropagationMode.ALL_DESCENDANTS);
	}
}
