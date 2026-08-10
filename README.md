# HeapScout

HeapScout is a local-first Java/Kotlin heap dump explorer focused on guided diagnosis, fast search, and side-by-side comparison.

> Status: functional MVP. Native packages are produced by the release workflow; the first public release still needs a GitHub repository/tag and cross-platform artifact verification.

## Product principles

- **Local by default** — heap dumps never leave the machine unless the user explicitly moves them.
- **Explain the result** — show why an object or class is suspicious, not only a table of numbers.
- **Compare first-class** — treat before/after dump comparison as a primary workflow.
- **Bounded memory** — stream HPROF records and avoid keeping every heap object in the analyzer heap.
- **Easy to run** — publish self-contained packages for macOS, Windows, and Linux through GitHub Releases.

## MVP scope

- Open OpenJDK/HotSpot binary HPROF files with the system file picker, a local path, or a launcher argument.
- Show dump metadata, class histogram, object count, and estimated shallow size.
- Search classes with simple filters such as `name:cache size>10MB count>1000`.
- Compare class histograms between two dumps.
- Preserve parse progress and actionable failures in the UI.

Advanced retained-size analysis, dominator trees, and GC-root paths are intentionally deferred until the streaming parser and UX are stable.

## Repository layout

```text
engine/    Pure Kotlin HPROF parsing and analysis contracts
app/       Local Spring Boot process and HTTP adapter
frontend/  React + TypeScript user interface
docs/      Product decisions and architecture records
```

## Development prerequisites

- JDK 21
- Node.js 20+
- npm 10+

## Build and run

```bash
git clone https://github.com/logcat-io/heap-scout.git
cd heap-scout

cd frontend
npm ci
npm run build

cd ..
./gradlew test :app:bootJar
java -jar app/build/libs/heapscout.jar
```

Open <http://127.0.0.1:8911>. During UI development, run `npm run dev` in `frontend/`; Vite proxies `/api` to the local JVM process.

The system file picker is available when HeapScout runs in a desktop session. In a genuinely headless environment, the UI detects that limitation before opening a dialog and keeps the absolute-path input available.

To analyze a dump as soon as the application starts, pass the file as a non-option argument:

```bash
java -jar app/build/libs/heapscout.jar /path/to/java_pid1234.hprof
```

The UI restores the most recent analysis when the browser is refreshed. Results live only for the current HeapScout process, and at most ten sessions are retained in memory. Source dumps are never modified or deleted.

## Install from a GitHub release

The release workflow runs for every `v*` tag and attaches a self-contained application image for macOS, Windows, and Linux. Users do not need Node.js or a separate JDK:

1. Download the archive matching the operating system and CPU architecture from [GitHub Releases](https://github.com/logcat-io/heap-scout/releases).
2. Download the adjacent `.sha256` file and verify the archive checksum.
3. Extract the archive.
4. Launch `HeapScout.app` on macOS, `HeapScout.exe` on Windows, or `bin/HeapScout` on Linux.
5. Choose a `.hprof` or `.bin` file in the system dialog.

These development builds are not code-signed. macOS Gatekeeper and Windows SmartScreen may therefore require the user to explicitly allow the first launch. Signing and notarization are release-owner tasks because they require platform credentials.

For the release-grade JVM verification, including a real HotSpot dump and a sparse 1GiB HPROF under a 256MiB test heap, run:

```bash
./gradlew \
  -Dheapscout.runHprofIntegration=true \
  -Dheapscout.runLargeHprofIntegration=true \
  test :app:bootJar
```

## Privacy

Heap dumps can contain credentials, personal information, tokens, and application data. HeapScout binds only to loopback, analyzes files in place, and has no remote telemetry. The file picker returns a path to the local JVM process; the browser never uploads or duplicates the dump.

## License

HeapScout source is licensed under Apache License 2.0. Runtime dependency licenses and required notices are recorded in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md); every dependency update must refresh that inventory.
