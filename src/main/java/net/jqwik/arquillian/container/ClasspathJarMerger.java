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

import java.io.*;
import java.util.*;
import java.util.regex.*;

import lombok.*;
import org.jboss.shrinkwrap.api.*;
import org.jboss.shrinkwrap.api.asset.*;
import org.jboss.shrinkwrap.api.importer.*;
import org.jboss.shrinkwrap.api.spec.*;

/**
 * Merges class path entries into one archive. Service files of the same name are concatenated,
 * because jqwik wires itself through many of them and a plain merge keeps only the first.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class ClasspathJarMerger {
	private static final String SERVICES = "/META-INF/services/";
	private static final Pattern SIGNATURE_FILE = Pattern.compile("/META-INF/[^/]+\\.(SF|DSA|RSA|EC)");

	private final JavaArchive merged;
	private final Map<ArchivePath, ByteArrayOutputStream> services = new LinkedHashMap<>();

	static JavaArchive merge(String archiveName, Collection<File> classpathEntries) {
		final ClasspathJarMerger merger = new ClasspathJarMerger(ShrinkWrap.create(JavaArchive.class, archiveName));
		classpathEntries.forEach(merger::add);
		return merger.withConcatenatedServices();
	}

	private void add(File classpathEntry) {
		contentOf(classpathEntry).getContent().forEach((path, node) -> {
			if (node.getAsset() == null || isBoundToItsOriginalJar(path)) {
				return;
			}
			if (path.get().startsWith(SERVICES)) {
				appendProviders(services.computeIfAbsent(path, p -> new ByteArrayOutputStream()), node.getAsset());
			} else if (!merged.contains(path)) {
				merged.add(node.getAsset(), path);
			}
		});
	}

	private JavaArchive withConcatenatedServices() {
		services.forEach((path, providers) -> merged.add(new ByteArrayAsset(providers.toByteArray()), path));
		return merged;
	}

	private void appendProviders(ByteArrayOutputStream providers, Asset serviceFile) {
		final byte[] content = new ByteArrayAsset(serviceFile.openStream()).getSource();
		providers.write(content, 0, content.length);
		providers.write('\n');
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
			|| SIGNATURE_FILE.matcher(name).matches();
	}
}
