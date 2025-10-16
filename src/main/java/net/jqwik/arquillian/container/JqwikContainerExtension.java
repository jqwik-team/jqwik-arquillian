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
package net.jqwik.arquillian.container;

import org.jboss.arquillian.container.test.spi.client.deployment.*;
import org.jboss.arquillian.core.spi.*;

public class JqwikContainerExtension implements LoadableExtension {
	@Override
	public void register(ExtensionBuilder builder) {
		builder.service(AuxiliaryArchiveAppender.class, JqwikDeploymentAppender.class);
	}
}
