# Spring Statemachine - Community Project

![These aren't the droids you're looking for](https://upload.wikimedia.org/wikipedia/en/3/3c/These_Aren%27t_The_Droids_You%27re_Looking_For.png)

This is a community project continuing work on Spring Statemachine. It is not an
official Spring project and is not endorsed or maintained by the Spring team.
The original project is no longer maintained; see the
[announcement](https://spring.io/blog/2025/04/21/spring-cloud-data-flow-commercial).

This is a partial continuation of the former Spring Statemachine project. This
community project supports only the former `spring-statemachine-core` module;
it does not support or maintain the other modules or the complete former
project.

**Disclaimer:** This community project and its code are provided "as-is,"
without warranties of any kind. The maintainers and contributors accept no
liability for claims, damages, or other liabilities arising from the use of
this code.

## Building from Source

This repository uses a [Gradle](https://gradle.org)-based build system. Run
`./gradlew` from the root of the source tree.

### Prerequisites

[Git](https://git-scm.com/) and [JDK 25](https://www.oracle.com/java/technologies/downloads/).

Set the `JAVA_HOME` environment variable to the JDK installation directory. The
build requires Java 25.

### Check out sources

```shell
git clone https://github.com/codamind/spring-statemachine-core-community.git
```

### Build and test

```shell
./gradlew build
```

Discover more commands with `./gradlew tasks`.

## Contributing

Pull requests are welcome.

If you want to contribute, search GitHub tickets marked `help wanted` or open a
ticket to discuss your idea first and avoid duplicate work.

## License

Spring Statemachine is open-source software released under the
[Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).