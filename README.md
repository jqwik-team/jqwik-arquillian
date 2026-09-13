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
	testImplementation("net.jqwik:jqwik-arquillian:0.1.0-SNAPSHOT")
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
	<version>0.1.0-SNAPSHOT</version>
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

## Why the integration is a little tricky

Arquillian and a property-based engine each own the execution of a test method, so the work is
in deciding which side does what.

- **Arquillian addresses a test by name only.** Its remote surface is
  `TestRunner.execute(Class, String methodName)`. There is no channel for arguments, so generated
  values cannot be sent to the container try by try. The only workable split is to run the entire
  property inside the container, which means a second jqwik engine run is started there for a
  single method.
- **The client has to hand a property over without running it.** jqwik's `AroundPropertyHook`
  makes that possible: the hook leaves out `property.execute()` and asks Arquillian instead. The
  outcome that comes back is turned into the result type the engine reports on, so the client
  shows the property like any other.
- **Client and container can be the same JVM.** With an embedded container a system property
  cannot tell the in-container run from the client run. This module marks the in-container run
  with a thread local, set on the thread that serves the Arquillian request.
- **Two engine runs can share one JVM.** jqwik's stores belong to an engine run. With an embedded
  container the nested run and the client run see the same store repository, so this module keeps
  the Arquillian state that has to outlive a property outside of `Store`.
- **jqwik has to travel as complete jars.** API and engine find each other through
  `META-INF/services` files. ShrinkWrap's `addPackages` copies classes only, so the auxiliary
  archive is merged from the real jars, with service files of the same name concatenated.
- **The report travels separately from the failure.** jqwik publishes seed, original sample and
  shrunk sample as report entries, which is what makes them show up nicely in IDEs and build
  tools. The Arquillian protocol carries a throwable and a description, so the report is collected
  in the container and carried back as text.
- **jqwik has no hook for the end of a whole run.** The Arquillian suite has to be ended once,
  after the last container class. A JVM shutdown hook is too late, because container adapters
  remove shutdown hooks of their own while they stop. The suite is ended by a JUnit Platform
  `LauncherSessionListener` instead.
- **Lifecycle methods run on both sides.** `@BeforeContainer` and `@AfterContainer` methods run
  on the client and in the container. Keep them free of work that must happen only once.

## The documentation situation

Arquillian documents how to write tests and how to write a container adapter. It does not
document how to integrate a new test framework. `TestRunnerAdaptor`, `TestRunner`,
`AuxiliaryArchiveAppender`, `RemoteLoadableExtension` and the rule that only one `TestRunner` may
be present are explained nowhere but in the Javadoc, which is mostly one line per type. The
contract has to be read out of the existing integrations in `arquillian-core`: `junit5`, `testng`
and, as the closest model for a method that runs many times, the dormant Spock test runner. How
the client decides between a local and a remote run, how the in-container side avoids running the
lifecycle twice, and how results and exceptions are serialised are all implementation knowledge.

On the jqwik side the lifecycle hooks chapter of the user guide, together with the existing
modules such as jqwik-spring and jqwik-micronaut, was enough to find the right hooks, their
ordering through proximity and the registration pattern used here.

Container adapters add their own gaps. Payara Embedded, used for this module's own tests, only
lets the HTTP ports be configured. Its remaining listeners collide with any Payara or GlassFish
server already running on the machine, so the build derives a `domain.xml` with shifted ports.

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
