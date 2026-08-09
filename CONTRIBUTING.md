# Contributing to HeapScout

Heap dumps are large and often contain production secrets. Do not commit real customer dumps, credentials, object values, or unredacted incident data. Use synthetic fixtures or a dump generated from a disposable local JVM.

## Set up

Requirements are JDK 21, Node.js 20 or newer, and npm 10 or newer.

```bash
cd frontend
npm ci
npm test
npm run build

cd ..
./gradlew test :app:bootJar
```

Run the app with `java -jar app/build/libs/heapscout.jar`. The server must remain bound to `127.0.0.1`; changes that introduce remote upload or telemetry require a separate architecture decision and explicit user consent.

## Design boundaries

- `engine` is framework-free Kotlin and must not depend on Spring, HTTP, or UI types.
- `app` triggers the same analysis use case from HTTP and launcher adapters.
- `frontend` performs HTTP calls only through `src/api/api-client.ts`.
- Parsing must stay bounded by metadata cardinality, validate record lengths before allocation, and preserve cancellation checks.
- Array sizes and other estimates must remain visibly labelled in API and UI output.

Architecture decisions are recorded in `docs/adr`. Add or update an ADR when changing the parser strategy, privacy boundary, execution modes, or distribution contract.

## Pull-request checklist

- Add deterministic tests for parser, search, comparison, or state changes.
- Run frontend tests/build, JVM tests, and `npm audit --audit-level=high`.
- Test corrupt/truncated input and recovery behavior for parser changes.
- Do not log object values or expose the server beyond loopback.
- Update `THIRD_PARTY_NOTICES.md` when runtime dependencies change.
- Update README, product spec, and an ADR when a user-facing contract changes.

Security issues should follow [SECURITY.md](SECURITY.md) rather than a public issue.
