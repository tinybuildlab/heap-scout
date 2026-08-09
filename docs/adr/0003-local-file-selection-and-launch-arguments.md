# ADR-0003: Native local-file selection and launch arguments

- Status: accepted
- Date: 2026-08-09

## Context

A browser cannot hand an arbitrary absolute file path to a local JVM process. A normal upload control would copy a potentially multi-gigabyte heap dump through the browser before analysis, increasing disk use and delaying the first result. Requiring users to type an absolute path also fails the product goal of opening a dump without prior instructions.

The same analysis use case must remain usable from a graphical launch, a terminal, and a headless environment.

## Decision

Expose local file selection through an application port backed by the operating system AWT file dialog. The React UI asks the loopback API to open that dialog, receives only the selected path, and starts the existing analysis use case. Cancelling the dialog is a normal result rather than an error.

Accept non-option launcher arguments through an `ApplicationRunner` adapter and pass each path to the same `HeapDumpAnalysisService`. The HTTP controller and startup adapter do not implement separate validation or analysis behavior.

Expose a bounded list of recent in-process analysis jobs so a browser refresh, or the browser opened after a launcher argument, reconnects to the newest job. Retain at most ten jobs and evict only the oldest inactive result. Never delete or modify a source dump during eviction.

## Failure handling

- Reject a second file-picker request while the first dialog is open.
- Require JSON content type for picker mutations so cross-origin HTML forms cannot trigger a dialog.
- Reject requests whose `Host` is not `127.0.0.1` or `localhost` to limit DNS-rebinding access to the loopback API.
- Return an actionable unavailable response when the runtime is headless or the desktop dialog cannot open.
- Keep manual path entry available as the headless recovery path.
- Revalidate the selected path in the analysis service because a file can be moved or deleted after selection.
- Ignore an invalid startup path without preventing the local application from starting; log only its argument position and failure category.

## Trade-offs

- AWT adds a desktop capability to the local adapter, but the domain and analysis engine remain UI-independent.
- Recent results survive browser refreshes but not JVM restarts. Persistent result caching is deferred because heap-derived artifacts require explicit lifecycle, compatibility, and privacy policies.
- The loopback endpoint can cause a local dialog to appear, so picker requests require the JSON API boundary, cross-origin browser calls are not enabled, and non-loopback host names are rejected.
