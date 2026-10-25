# Copilot instructions

Spring Statemachine Core (community fork): a single-module Gradle library (`java-library`) built on Spring Framework and Project Reactor. Main code is under `org.springframework.statemachine` (`action`, `config`, `guard`, `state`, `transition`, `trigger`, `region`, `persist`, `service`, `support`, ...).

## Environment

- Java 25 is required. `gradle\gradle-daemon-jvm.properties` pins the Gradle daemon to Java 25; the default `java` on PATH may be Java 8, so never rely on it.
- If Gradle cannot find a JDK 25, set `JAVA_HOME` before running it. A JDK 25 is typically at `C:\Users\<user>\.jdks\temurin-25*`.
- Always use the wrapper (`.\gradlew.bat`), never a globally installed Gradle.
- Windows/PowerShell: use backslash paths and `;` (not `&&`) between statements.

## Build and test

- All tests: `.\gradlew.bat test`
- Single class: `.\gradlew.bat test --tests "org.springframework.statemachine.state.ObjectStateTests"`
- Single method: `.\gradlew.bat test --tests "*ObjectStateTests.testName"`
- Compile only: `.\gradlew.bat compileJava compileTestJava`
- Run the smallest relevant test selection first; run the full suite before finishing larger changes.
- Check `--warning-mode all` output when touching build files and fix Gradle deprecations (the build must stay Gradle 10 compatible).

## Gradle conventions

- Dependency versions are managed by the Spring Boot BOM via `io.spring.dependency-management`: declare dependencies without versions unless the BOM does not manage them (for example `jsr305` uses `findbugsVersion` from `gradle.properties`).
- Use assignment syntax in Groovy DSL (`url = uri('...')`, `enabled = false`), not the deprecated space-assignment syntax.
- Keep build settings (`org.gradle.parallel`, `org.gradle.caching`, `org.gradle.jvmargs`) in `gradle.properties`, not in `gradle-wrapper.properties`.
- Use `tasks.withType(...).configureEach`, not eager task configuration.
- Public API stays on `api(...)`; test-only libraries on `testImplementation`; shared test helpers (e.g. `TestUtils`) live in `src/test`.

## Code style

- Tabs for indentation, as in existing files.
- New files start with the Apache 2.0 license header used by existing files, followed by the package declaration.
- Public types and methods carry Javadoc; follow the existing import grouping (`java.*`, `org.*`, then `reactor.*`, static imports at the top).
- Validate arguments with Spring's `Assert`; log through `org.apache.commons.logging`.
- Match the surrounding code; avoid unrelated refactors and reformatting.

## Reactive code

- The API is reactive (`Mono`/`Flux`, `*Reactively()` methods). Prefer composing reactive chains over blocking.
- Do not add `.block()` in main code paths. The existing `.block()` calls live in the legacy synchronous facade methods (lifecycle, persister, service); keep new blocking confined to such adapters.
- Never use `Thread.sleep` in main code; schedule via reactor operators or the configured task scheduler.

## Testing

- JUnit 5 (`org.junit.jupiter.api.Test`) with AssertJ (`assertThat`); Mockito via `mockito-junit-jupiter`.
- Use Awaitility (`await()`) for asynchronous conditions, not fixed sleeps; use `reactor-test` `StepVerifier` for reactive streams.
- Name test classes `<Subject>Tests` and place them in the same package as the code under test.
- Every bug fix or behaviour change needs a test. Tests must be deterministic and must not rely on timing.
- BlockHound has been removed; do not reintroduce it.

## Git and changes

- Keep changes small and focused; do not commit build output (`build\`, `.gradle\`, `.idea\`).
- Never commit secrets or machine-specific paths (such as a local JDK location) into the repository.
- Update `README.md` or docs when behaviour or setup changes.
