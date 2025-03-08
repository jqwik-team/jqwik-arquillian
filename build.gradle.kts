plugins {
	id("java-library")
	id("maven-publish")
}

val githubProjectName = "jqwik-team"
val artifactId = "jqwik-arquillian"
val moduleGroupId = "net.jqwik"
val jqwikVersion = "1.9.2"
val arquillianVersion = "1.9.1.Final"
val shrinkwrapVersion = "1.2.6"
val assertJVersion = "3.27.0"
val jqwikArquillianVersion = "0.1.0-SNAPSHOT"

group = moduleGroupId
version = jqwikArquillianVersion
description = "Jqwik Arquillian support module"

repositories {
	mavenCentral()
}

tasks.jar {
	archiveBaseName.set(artifactId)
	archiveVersion.set(jqwikArquillianVersion)
	manifest {
		attributes("Automatic-Module-Name" to "net.jqwik.arquillian")
	}
}

java {
	withJavadocJar()
	withSourcesJar()
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

tasks.compileJava {
	options.release = 11
	options.encoding = "UTF-8"
}

tasks.compileTestJava {
	options.compilerArgs.add("-parameters")
	options.encoding = "UTF-8"
}

tasks.test {
	useJUnitPlatform {
		includeEngines("jqwik")
	}
	testLogging {
		events("passed", "skipped", "failed")
		showStandardStreams = true
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
}

dependencies {
	api(platform("org.jboss.arquillian:arquillian-bom:${arquillianVersion}"))
	api("net.jqwik:jqwik:${jqwikVersion}")
	api("org.jboss.arquillian.test:arquillian-test-api")
	api("org.jboss.arquillian.container:arquillian-container-test-api")

	implementation("org.jboss.arquillian.test:arquillian-test-spi")
	implementation("org.jboss.arquillian.container:arquillian-container-test-spi")
	implementation("org.jboss.arquillian.core:arquillian-core-impl-base")
	implementation("org.jboss.arquillian.test:arquillian-test-impl-base")
	implementation("org.jboss.arquillian.container:arquillian-container-impl-base")
	implementation("org.jboss.arquillian.container:arquillian-container-test-impl-base")
	implementation("org.jboss.shrinkwrap:shrinkwrap-impl-base:${shrinkwrapVersion}")

	testImplementation("org.assertj:assertj-core:${assertJVersion}")
}

publishing {
	publications {
		create<MavenPublication>("jqwikArquillian") {
			groupId = moduleGroupId
			artifactId = artifactId
			from(components["java"])
			pom {
				groupId = moduleGroupId
				name = artifactId
				description = "Jqwik Arquillian support module"
				url = "https://github.com/$githubProjectName/$artifactId"
				licenses {
					license {
						name = "Eclipse Public License - v 2.0"
						url = "http://www.eclipse.org/legal/epl-v20.html"
					}
				}
				scm {
					connection = "scm:git:git://github.com/$githubProjectName/$artifactId.git"
					developerConnection = "scm:git:git://github.com/$githubProjectName/$artifactId.git"
					url = "https://github.com/$githubProjectName/$artifactId"
				}
			}
		}
	}
}

tasks.wrapper {
	gradleVersion = "8.12" // upgrade with: ./gradlew wrapper
}
