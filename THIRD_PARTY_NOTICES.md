# Third-party notices

HeapScout includes the following runtime components. This inventory was verified against the `0.1.0` runtime classpath and production frontend bundle on 2026-08-09. Dependency updates must update this file before release.

## JVM application

| Project | Included version(s) | License |
| --- | --- | --- |
| Kotlin standard library and reflection | 2.2.21 | Apache-2.0 |
| JetBrains annotations | 13.0 | Apache-2.0 |
| Spring Boot | 3.5.16 | Apache-2.0 |
| Spring Framework | 6.2.19 | Apache-2.0 |
| Micrometer | 1.15.12 | Apache-2.0 |
| Jackson | 2.21 / 2.21.4 | Apache-2.0 |
| Apache Tomcat Embed | 10.1.55 | Apache-2.0 |
| Hibernate Validator | 8.0.3.Final | Apache-2.0 |
| Jakarta Validation API | 3.0.2 | Apache-2.0 |
| Jakarta Annotations API | 2.1.1 | EPL-2.0 or GPL-2.0 with Classpath Exception |
| JBoss Logging | 3.6.3.Final | Apache-2.0 |
| ClassMate | 1.7.3 | Apache-2.0 |
| Logback | 1.5.34 | EPL-2.0 or LGPL-2.1-only |
| SLF4J | 2.0.18 | MIT |
| Apache Log4j API / SLF4J adapter | 2.24.3 | Apache-2.0 |
| SnakeYAML | 2.4 | Apache-2.0 |

The executable JAR preserves dependency JARs, including their `META-INF` license and notice files. The complete Apache License 2.0 text is also available in [LICENSE](LICENSE).

## Browser application

| Project | Included version | License |
| --- | --- | --- |
| React and React DOM | 19.2.8 | MIT |
| Scheduler | 0.27.0 | MIT |
| Zustand | 5.0.12 | MIT |

React, React DOM, and Scheduler carry this notice:

> MIT License
> Copyright (c) Meta Platforms, Inc. and affiliates.

Zustand carries this notice:

> MIT License
> Copyright (c) 2019 Paul Henschel

The MIT license terms for the components above are:

> Permission is hereby granted, free of charge, to any person obtaining a copy
> of this software and associated documentation files (the "Software"), to deal
> in the Software without restriction, including without limitation the rights
> to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
> copies of the Software, and to permit persons to whom the Software is
> furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in all
> copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
> SOFTWARE.

## Packaged Java runtime

GitHub Actions builds application images with the Temurin JDK 21 selected by `actions/setup-java`. `jpackage` copies the runtime's `legal/` directory into each application image; those files are the authoritative notices for OpenJDK (GPL-2.0 with Classpath Exception) and its bundled third-party components.
