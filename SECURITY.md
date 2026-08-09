# Security policy

## Supported versions

HeapScout is pre-release software. Only the latest tagged version will receive security fixes until a stable support policy is published.

## Reporting a vulnerability

Use GitHub private vulnerability reporting when the repository is published. Do not attach a real heap dump to a public issue: heap dumps can contain credentials, tokens, personal information, and complete application object values.

Include the HeapScout version, operating system, JVM version, steps to reproduce with a synthetic fixture, and the expected security impact. Maintainers should acknowledge a report within seven days.

## Local security boundary

- The application binds to `127.0.0.1` by default.
- Requests with a non-loopback `Host` are rejected to reduce DNS-rebinding exposure.
- File-picker mutations require JSON and do not enable cross-origin requests.
- Heap dumps are read from local paths and are not uploaded by HeapScout.
- Telemetry is disabled and no object values are written to logs.
- Exported reports must be treated as sensitive until reviewed and redacted.
