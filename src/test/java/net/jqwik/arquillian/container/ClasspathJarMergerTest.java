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
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import org.jboss.shrinkwrap.api.*;
import org.jboss.shrinkwrap.api.asset.*;
import org.jboss.shrinkwrap.api.exporter.*;
import org.jboss.shrinkwrap.api.spec.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class ClasspathJarMergerTest {
	private static final String SERVICE = "META-INF/services/example.Service";

	@Example
	void concatenatesServiceFilesOfTheSameName() throws IOException {
		final JavaArchive merged = ClasspathJarMerger.merge("merged.jar", List.of(
			jar("first", SERVICE, "example.FirstProvider"),
			jar("second", SERVICE, "example.SecondProvider")
		));

		assertThat(textOf(merged, SERVICE)).contains("example.FirstProvider", "example.SecondProvider");
	}

	@Example
	void keepsTheFirstOfDuplicateResources() throws IOException {
		final JavaArchive merged = ClasspathJarMerger.merge("merged.jar", List.of(
			jar("first", "example/shared.txt", "first"),
			jar("second", "example/shared.txt", "second")
		));

		assertThat(textOf(merged, "example/shared.txt")).isEqualTo("first");
	}

	@Example
	void dropsEntriesThatOnlyMakeSenseInTheOriginalJar() throws IOException {
		final JavaArchive merged = ClasspathJarMerger.merge("merged.jar", List.of(
			jar("modular", "module-info.class", "descriptor"),
			jar("multi-release", "META-INF/versions/9/module-info.class", "descriptor"),
			jar("signed", "META-INF/SIGNER.SF", "signature")
		));

		assertThat(merged.getContent().values()).noneMatch(node -> node.getAsset() != null);
	}

	private File jar(String name, String path, String content) throws IOException {
		final File jar = Files.createTempFile(name, ".jar").toFile();
		jar.deleteOnExit();
		ShrinkWrap.create(JavaArchive.class, jar.getName())
			.add(new StringAsset(content), path)
			.as(ZipExporter.class).exportTo(jar, true);
		return jar;
	}

	private String textOf(JavaArchive archive, String path) throws IOException {
		try (InputStream content = archive.get(path).getAsset().openStream()) {
			return new String(content.readAllBytes(), StandardCharsets.UTF_8).strip();
		}
	}
}
