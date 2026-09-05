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
import java.util.*;

import org.jboss.shrinkwrap.api.*;
import org.jboss.shrinkwrap.api.asset.*;
import org.jboss.shrinkwrap.api.importer.*;
import org.jboss.shrinkwrap.api.spec.*;

/**
 * Merges class path entries into one archive. Service files of the same name are concatenated,
 * because jqwik wires itself through many of them and a plain merge keeps only the first.
 */
final class ClasspathJarMerger {
	private static final String SERVICES = "/META-INF/services/";

	private final JavaArchive merged;
	private final Map<ArchivePath, StringBuilder> services = new LinkedHashMap<>();

	private ClasspathJarMerger(String archiveName) {
		merged = ShrinkWrap.create(JavaArchive.class, archiveName);
	}

	static JavaArchive merge(String archiveName, Collection<File> classpathEntries) {
		final ClasspathJarMerger merger = new ClasspathJarMerger(archiveName);
		classpathEntries.forEach(merger::add);
		merger.services.forEach((path, providers) -> merger.merged.add(new StringAsset(providers.toString()), path));
		return merger.merged;
	}

	private void add(File classpathEntry) {
		contentOf(classpathEntry).getContent().forEach((path, node) -> {
			if (node.getAsset() == null || isBoundToItsOriginalJar(path)) {
				return;
			}
			if (path.get().startsWith(SERVICES)) {
				services.computeIfAbsent(path, p -> new StringBuilder()).append(textOf(node)).append('\n');
			} else if (!merged.contains(path)) {
				merged.add(node.getAsset(), path);
			}
		});
	}

	private JavaArchive contentOf(File classpathEntry) {
		return classpathEntry.isDirectory()
			? ShrinkWrap.create(ExplodedImporter.class).importDirectory(classpathEntry).as(JavaArchive.class)
			: ShrinkWrap.create(ZipImporter.class).importFrom(classpathEntry).as(JavaArchive.class);
	}

	private boolean isBoundToItsOriginalJar(ArchivePath path) {
		final String name = path.get();
		return name.endsWith("module-info.class")
			|| name.equals("/META-INF/MANIFEST.MF")
			|| name.startsWith("/META-INF/versions/")
			|| name.matches("/META-INF/[^/]+\\.(SF|DSA|RSA|EC)");
	}

	private String textOf(Node node) {
		try (InputStream content = node.getAsset().openStream()) {
			final ByteArrayOutputStream text = new ByteArrayOutputStream();
			final byte[] buffer = new byte[8192];
			for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
				text.write(buffer, 0, read);
			}
			return new String(text.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Unreadable service file " + node.getPath().get(), e);
		}
	}
}
