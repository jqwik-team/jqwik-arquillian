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
package net.jqwik.arquillian;

import java.lang.annotation.*;

import net.jqwik.api.lifecycle.*;
import net.jqwik.arquillian.internal.*;

/**
 * Runs the examples and properties of a container class under Arquillian.
 *
 * <p>A property that Arquillian runs in-container is executed there as a whole:
 * value generation, all tries and shrinking happen inside the container and only
 * the outcome travels back to the client.</p>
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AddLifecycleHook(ArquillianRegistrar.class)
public @interface JqwikArquillianSupport {
	/* Empty on purpose */
}
