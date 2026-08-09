# ADR-0001: Local web application with packaged runtime

- Status: accepted
- Date: 2026-08-09

## Context

Heap dumps routinely contain production data and can be many gigabytes. A hosted upload service creates privacy, transfer-time, storage, and incident-response risks. A traditional JVM desktop UI simplifies file access but makes a polished, accessible data-heavy interface and cross-platform iteration slower.

## Decision

Run a JVM analysis process bound to `127.0.0.1` and serve a bundled React UI. The release pipeline will package the application and a runtime so end users do not need Node.js and eventually do not need a preinstalled JDK.

The application accepts local file paths through its launcher/API. Browser upload is not the default because it duplicates very large files.

Every release runner must start its newly built application image and verify the loopback API, bundled UI, DNS-rebinding guard, and packaged legal notices before the archive can be published.

## Trade-offs

- The browser UI is easy to iterate and test, but local-file selection needs a launcher or desktop bridge.
- A loopback HTTP boundary adds serialization overhead, but keeps the analysis use cases independent from the UI.
- Packaging a runtime increases artifact size, but removes Java-version setup from the user journey.

## Guardrails

- Default bind address is immutable loopback.
- No telemetry or remote API calls in the analysis path.
- Object field values must not be logged.
