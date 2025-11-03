plugins {
	id("java-library")
	id("maven-publish")
}

val githubProjectName = "jqwik-team"
val artifactId = "jqwik-arquillian"
val moduleGroupId = "net.jqwik"
val jqwikVersion = "1.9.2"
val junitPlatformVersion = "1.11.3"
val arquillianVersion = "1.9.1.Final"
val shrinkwrapVersion = "1.2.6"
val assertJVersion = "3.27.0"
val jakartaEeVersion = "10.0.0"
val payaraVersion = "6.2024.12"
val payaraArquillianVersion = "3.1"
val jqwikArquillianVersion = "0.1.0-SNAPSHOT"

// Payara Embedded reflects into JDK internals that are closed since Java 17
val payaraJvmArgs = listOf(
	"--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED",
	"--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED",
	"--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
	"--add-opens=java.base/java.lang=ALL-UNNAMED",
	"--add-opens=java.base/java.net=ALL-UNNAMED",
	"--add-opens=java.base/java.nio=ALL-UNNAMED",
	"--add-opens=java.base/java.util=ALL-UNNAMED",
	"--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
	"--add-opens=java.base/sun.net.www.protocol.jrt=ALL-UNNAMED",
	"--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED",
	"--add-opens=java.management/sun.management=ALL-UNNAMED",
	"--add-opens=java.naming/javax.naming.spi=ALL-UNNAMED",
	"--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED",
	"--add-opens=java.logging/java.util.logging=ALL-UNNAMED"
)

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

// Payara Embedded only lets the HTTP ports be configured. The remaining listeners of its default
// domain collide with any Payara or GlassFish server that already runs on the machine.
val payaraPortOffset = 10000
val payaraDomainXml = layout.buildDirectory.file("payara/domain.xml")
val payaraDefaultPorts = listOf(8080, 8181, 4848, 7676, 3700, 3820, 3920, 8686)

val shiftPayaraPorts by tasks.registering {
	val testRuntimeClasspath = configurations.testRuntimeClasspath
	inputs.files(testRuntimeClasspath)
	outputs.file(payaraDomainXml)
	doLast {
		val payaraJar = testRuntimeClasspath.get().files.single { it.name.startsWith("payara-embedded-all") }
		val defaultDomain = zipTree(payaraJar).matching { include("config/domain.xml") }.singleFile.readText()
		val shiftedDomain = payaraDefaultPorts.fold(defaultDomain) { domain, port ->
			domain.replace("\"$port\"", "\"${port + payaraPortOffset}\"")
		}
		payaraDomainXml.get().asFile.writeText(shiftedDomain)
	}
}

tasks.test {
	useJUnitPlatform {
		includeEngines("jqwik")
	}
	dependsOn(shiftPayaraPorts)
	jvmArgs(payaraJvmArgs)
	systemProperty("payara.domain.xml", payaraDomainXml.get().asFile.absolutePath)
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

	implementation("net.jqwik:jqwik-engine:${jqwikVersion}")
	implementation("org.junit.platform:junit-platform-launcher:${junitPlatformVersion}")
	implementation("org.jboss.arquillian.test:arquillian-test-spi")
	implementation("org.jboss.arquillian.container:arquillian-container-test-spi")
	implementation("org.jboss.arquillian.core:arquillian-core-impl-base")
	implementation("org.jboss.arquillian.test:arquillian-test-impl-base")
	implementation("org.jboss.arquillian.container:arquillian-container-impl-base")
	implementation("org.jboss.arquillian.container:arquillian-container-test-impl-base")
	implementation("org.jboss.shrinkwrap:shrinkwrap-impl-base:${shrinkwrapVersion}")

	testImplementation("org.assertj:assertj-core:${assertJVersion}")
	testImplementation("jakarta.platform:jakarta.jakartaee-api:${jakartaEeVersion}")
	testRuntimeOnly("fish.payara.arquillian:arquillian-payara-server-embedded:${payaraArquillianVersion}")
	testRuntimeOnly("fish.payara.extras:payara-embedded-all:${payaraVersion}")
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
