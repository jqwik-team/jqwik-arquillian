plugins {
	id("java-library")
	id("maven-publish")
	id("checkstyle")
	id("io.freefair.lombok") version "9.7.0"
}

val githubProjectName = "jqwik-team"
val artifactName = "jqwik-arquillian"
val jqwikVersion = "1.10.1"
val junitPlatformVersion = "1.14.4"
val arquillianVersion = "1.10.2.Final"
val shrinkwrapVersion = "1.2.6"
val assertJVersion = "3.27.7"
val jakartaEeVersion = "10.0.0"
val payaraVersion = "6.2025.11"
val payaraArquillianVersion = "3.1"
val checkstyleVersion = "14.1.0"
val lombokVersion = "1.18.48"
val jqwikArquillianVersion = "0.1.1-SNAPSHOT"

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

group = "net.jqwik"
version = jqwikArquillianVersion
description = "Jqwik Arquillian support module"

repositories {
	mavenCentral()
}

tasks.jar {
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
	options.release = 8
	options.encoding = "UTF-8"
}

tasks.compileTestJava {
	options.compilerArgs.add("-parameters")
	options.encoding = "UTF-8"
}

checkstyle {
	toolVersion = checkstyleVersion
	maxWarnings = 0
}

lombok {
	version = lombokVersion
}

// Payara Embedded only lets the HTTP ports be configured. The remaining listeners of its default
// domain collide with any Payara or GlassFish server that already runs on the machine.
val payaraPortOffset = 10000
val payaraDomainXml = layout.buildDirectory.file("payara/domain.xml")
val payaraDefaultPorts = listOf(8080, 8181, 4848, 7676, 3700, 3820, 3920, 8686)

fun withShiftedPayaraPorts(domainXml: String) = payaraDefaultPorts.fold(domainXml) { domain, port ->
	domain.replace("\"$port\"", "\"${port + payaraPortOffset}\"")
}

val shiftPayaraPorts = tasks.register("shiftPayaraPorts") {
	val testRuntimeClasspath = configurations.testRuntimeClasspath
	inputs.files(testRuntimeClasspath)
	outputs.file(payaraDomainXml)
	doLast {
		val payaraJar = testRuntimeClasspath.get().files.single { it.name.startsWith("payara-embedded-all") }
		val defaultDomain = zipTree(payaraJar).matching { include("config/domain.xml") }.singleFile.readText()
		payaraDomainXml.get().asFile.writeText(withShiftedPayaraPorts(defaultDomain))
	}
}

val payaraManagedTests = "*PayaraManaged*"

fun Test.reportLikeJqwikModules() {
	useJUnitPlatform {
		includeEngines("jqwik")
	}
	testLogging {
		events("passed", "skipped", "failed")
		showStandardStreams = true
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
}

tasks.test {
	reportLikeJqwikModules()
	filter {
		excludeTestsMatching(payaraManagedTests)
	}
	dependsOn(shiftPayaraPorts)
	jvmArgs(payaraJvmArgs)
	systemProperty("payara.domain.xml", payaraDomainXml.get().asFile.absolutePath)
}

val payaraServer = configurations.create("payaraServer") {
	isCanBeConsumed = false
}

// Arquillian accepts one container adapter on the class path, so the managed tests cannot share
// the class path that carries the embedded adapter.
val payaraManagedTestRuntime = configurations.create("payaraManagedTestRuntime") {
	isCanBeConsumed = false
	extendsFrom(configurations.testImplementation.get(), configurations.runtimeOnly.get())
}

val payaraServerDirectory = layout.buildDirectory.dir("payara-server")
val payaraServerInstalled = payaraServerDirectory.map { it.file("installed") }

// A running server writes into its own directory, so the marker file alone is the task output.
val installPayaraServer = tasks.register("installPayaraServer") {
	inputs.files(payaraServer)
	outputs.file(payaraServerInstalled)
	doLast {
		val serverDirectory = payaraServerDirectory.get().asFile
		delete(serverDirectory)
		copy {
			from(zipTree(payaraServer.singleFile))
			into(serverDirectory)
		}
		val domainXml = serverDirectory.resolve("payara6/glassfish/domains/domain1/config/domain.xml")
		domainXml.writeText(withShiftedPayaraPorts(domainXml.readText()))
		payaraServerInstalled.get().asFile.writeText(payaraVersion)
	}
}

val payaraManagedTest = tasks.register<Test>("payaraManagedTest") {
	description = "Runs the tests that need a Payara server in a JVM of its own."
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	reportLikeJqwikModules()
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.main.get().output + sourceSets.test.get().output + payaraManagedTestRuntime
	filter {
		includeTestsMatching(payaraManagedTests)
	}
	dependsOn(installPayaraServer)
	shouldRunAfter(tasks.test)
	systemProperty("arquillian.xml", "arquillian-payara-managed.xml")
	systemProperty("payara.home", payaraServerDirectory.get().dir("payara6").asFile.absolutePath)
	// The server has to load test classes compiled for the toolchain, whatever JAVA_HOME the build was started with
	environment("JAVA_HOME", javaLauncher.get().metadata.installationPath.asFile.absolutePath)
}

tasks.check {
	dependsOn(payaraManagedTest)
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

	payaraManagedTestRuntime("fish.payara.arquillian:arquillian-payara-server-managed:${payaraArquillianVersion}")
	payaraServer("fish.payara.distributions:payara-web:${payaraVersion}@zip")
}

publishing {
	publications {
		create<MavenPublication>("jqwikArquillian") {
			from(components["java"])
			pom {
				name = artifactName
				description = project.description
				url = "https://github.com/$githubProjectName/$artifactName"
				licenses {
					license {
						name = "Eclipse Public License - v 2.0"
						url = "http://www.eclipse.org/legal/epl-v20.html"
					}
				}
				scm {
					connection = "scm:git:git://github.com/$githubProjectName/$artifactName.git"
					developerConnection = "scm:git:git://github.com/$githubProjectName/$artifactName.git"
					url = "https://github.com/$githubProjectName/$artifactName"
				}
			}
		}
	}
}

tasks.wrapper {
	gradleVersion = "9.7.1" // upgrade with: ./gradlew wrapper
}
