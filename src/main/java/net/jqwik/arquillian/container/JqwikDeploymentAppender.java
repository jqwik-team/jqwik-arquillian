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

import java.io.*;
import java.net.*;
import java.util.*;

import org.jboss.arquillian.container.test.spi.*;
import org.jboss.arquillian.container.test.spi.client.deployment.*;
import org.jboss.shrinkwrap.api.*;

import net.jqwik.arquillian.*;
import net.jqwik.arquillian.internal.*;

/**
 * Ships jqwik, the JUnit Platform and this library to the container.
 */
public class JqwikDeploymentAppender extends CachedAuxilliaryArchiveAppender {
	private static final List<String> REQUIRED_LIBRARIES = Arrays.asList(
		"net.jqwik.api.Property",
		"net.jqwik.engine.JqwikTestEngine",
		"org.junit.platform.commons.JUnitException",
		"org.junit.platform.engine.TestEngine",
		"org.junit.platform.launcher.Launcher",
		"org.opentest4j.AssertionFailedError",
		"org.apiguardian.api.API"
	);

	private static final List<String> OPTIONAL_LIBRARIES = Arrays.asList(
		"net.jqwik.time.api.Dates",
		"net.jqwik.web.api.Web"
	);

	@Override
	protected Archive<?> buildArchive() {
		return ClasspathJarMerger.merge("arquillian-jqwik.jar", libraries())
			.addClass(JqwikArquillianSupport.class)
			.addPackages(true, ContainerExecution.class.getPackage(), JqwikTestRunner.class.getPackage())
			.addAsServiceProvider(TestRunner.class, JqwikTestRunner.class);
	}

	private Set<File> libraries() {
		final Set<File> libraries = new LinkedHashSet<>();
		REQUIRED_LIBRARIES.forEach(marker -> libraries.add(locationOf(required(marker))));
		OPTIONAL_LIBRARIES.forEach(marker -> find(marker).map(this::locationOf).ifPresent(libraries::add));
		return libraries;
	}

	private Class<?> required(String className) {
		return find(className).orElseThrow(() -> new IllegalStateException(className + " is not on the class path"));
	}

	private Optional<Class<?>> find(String className) {
		try {
			return Optional.of(Class.forName(className, false, JqwikDeploymentAppender.class.getClassLoader()));
		} catch (ClassNotFoundException e) {
			return Optional.empty();
		}
	}

	private File locationOf(Class<?> marker) {
		try {
			return new File(marker.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Cannot locate the library of " + marker.getName(), e);
		}
	}
}
