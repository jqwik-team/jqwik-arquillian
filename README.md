[![CI](https://github.com/jqwik-team/jqwik-arquillian/actions/workflows/gradle.yml/badge.svg)](https://github.com/jqwik-team/jqwik-arquillian/actions/workflows/gradle.yml)
[![Version](https://img.shields.io/github/v/tag/jqwik-team/jqwik-arquillian?sort=semver&label=version)](https://github.com/jqwik-team/jqwik-arquillian/tags)

# jqwik-arquillian
Arquillian support for jqwik

## How to use

The module is not published to Maven Central yet. Install it into the local Maven repository
first:

```
./gradlew publishToMavenLocal
```

jqwik itself and the Arquillian test API come along as transitive dependencies. Add the Arquillian
container adapter of your server next to the module.

### Gradle

```kotlin
repositories {
	mavenCentral()
	mavenLocal()
}

dependencies {
	testImplementation("net.jqwik:jqwik-arquillian:0.1.0")
}

tasks.test {
	useJUnitPlatform {
		includeEngines("jqwik")
	}
}
```

### Maven

```xml
<dependency>
	<groupId>net.jqwik</groupId>
	<artifactId>jqwik-arquillian</artifactId>
	<version>0.1.0</version>
	<scope>test</scope>
</dependency>
```

Surefire finds the jqwik engine on the test class path from version 2.22.0 on.

## Usage

```java
@JqwikArquillianSupport
public class GreeterProperties {
	@Inject
	private Greeter greeter;

	@Deployment
	public static WebArchive deployment() {
		return ShrinkWrap.create(WebArchive.class)
			.addClass(Greeter.class)
			.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@Property
	void greetsEveryone(@ForAll @AlphaChars String name) {
		assertThat(greeter.greet(name)).endsWith(name);
	}
}
```

A property that Arquillian runs in-container is executed there as a whole. Value generation, all
tries and shrinking happen inside the container, and only the outcome travels back to the client.

Do not combine this module with `arquillian-junit5-container` on the same test class path.
Arquillian allows exactly one `TestRunner` per deployment.

jqwik, the JUnit Platform and this module are added to every deployment. Anything else a property
uses inside the container, an assertion library for instance, has to be part of the archive when
the container runs in a JVM of its own. An embedded container hides that, because it shares the
test class path.

## The documentation situation

Some of this was tricky ... Figuring out the Arquilian side specifically.
Arquillian documents how to write tests and how to write a container adapter. It does not
document how to integrate a new test framework.
How the client decides between a local and a remote run, how the in-container side avoids running the
lifecycle twice, and how results and exceptions are serialised are all implementation knowledge.

On the jqwik side the lifecycle hooks chapter of the user guide, together with the existing
modules such as jqwik-spring and jqwik-micronaut, was enough to find the right hooks, their
ordering through proximity and the registration pattern used here.

## Building

```
./gradlew check
```

`test` runs against Payara Embedded. `payaraManagedTest` runs against a Payara server in a JVM of
its own and downloads the server distribution, about 125 MB, on its first run. Both use ports
shifted by 10000, so a Payara or GlassFish server that already runs on the machine is left alone.

`./create-release.sh` cuts a release: it builds and tests the version the build file is working
towards, keeps the jar in `releases/`, commits and tags it, and opens the next snapshot. Nothing is
pushed. `./create-release.sh --help` lists the options.

The build needs JDK 21 to run the Payara tests. The library itself targets Java 8,
the same minimum version as jqwik.
