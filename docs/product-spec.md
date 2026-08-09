# HeapScout product spec

## Problem

Java heap dump tools are powerful, but a user still needs to understand histograms, dominators, retained size, and GC roots before reaching an answer. Heap dumps are also large and sensitive, making upload-first services unsuitable for many production incidents.

## Primary user

A Java or Kotlin developer investigating memory growth or an `OutOfMemoryError` who has a local `.hprof` file but is not a JVM memory-analysis specialist.

## Core workflow

1. Choose one dump from the system file dialog without uploading or copying it.
2. See an immediate summary and a short list of evidence-backed suspects.
3. Search for an application class, package, object count, or size threshold.
4. Open a class to understand its instances and why it remains reachable.
5. Optionally compare a baseline dump with a later dump.
6. Export a small, redacted report that can be attached to an issue.

## MVP requirements

| Capability | MVP definition |
| --- | --- |
| Open | OpenJDK/HotSpot binary HPROF from the system picker, a local path, or a launcher argument |
| Overview | File metadata, object/class counts, estimated shallow heap |
| Search | Class/package substring and numeric count/size filters |
| Histogram | Pageable and sortable class aggregation |
| Compare | Count and shallow-size delta by normalized class name |
| Progress | Visible phase, bytes processed, cancellation, actionable error |
| Privacy | Loopback-only server, no telemetry, no remote upload |
| Distribution | Self-contained GitHub Release artifacts for macOS, Windows, and Linux |

## Explicit non-goals for the first release

- Android HPROF compatibility.
- IBM/OpenJ9 dump formats.
- Remote multi-user analysis or accounts.
- AI features that transmit dump contents.
- A custom query language equivalent to full OQL.

## Success criteria

- A new user can choose and open a valid 1 GB HPROF without copying a path or reading documentation beyond the first screen.
- Peak analyzer memory stays bounded by metadata cardinality rather than object count for the MVP histogram pass.
- Invalid and truncated dumps fail loudly with the byte offset and recovery advice.
- Search results and comparison deltas are covered by deterministic tests.
- A release can be installed without separately installing Node.js.

The release gate verifies the first two criteria with a structurally valid sparse 1GiB HPROF while the analyzer test JVM is limited to 256MiB. A separate HotSpot-generated dump test protects compatibility with real JDK output.

## Distribution contract

Each `v*` Git tag builds a self-contained `jpackage` application image on native macOS, Windows, and Linux runners. The package includes a Java runtime and the production React bundle. Launching HeapScout starts the loopback server and opens the system browser; a heap dump passed as a non-option launcher argument starts immediately. `--heapscout.open-browser=false` disables browser launch for headless use. Published development artifacts are unsigned until a release owner supplies platform signing credentials.

## Failure scenarios

| Failure | Detection | Recovery |
| --- | --- | --- |
| Truncated/corrupt dump | Record-boundary validation with byte offset | Keep app usable; explain how to recapture the dump |
| Analyzer runs out of memory | Process-level error and memory guidance | Restart analysis with configurable heap limit |
| Two analyses exhaust disk/RAM | Bounded active sessions and explicit close | Evict only closed/old sessions; never silently delete source dumps |
| System picker is unavailable | Headless/platform capability check and typed API error | Keep manual absolute-path input available |
| Multiple picker requests race | Atomic single-dialog guard | Reject the second request with an actionable conflict response |
| Browser is exposed to LAN | Bind to `127.0.0.1` only | Refuse non-loopback bind unless a future explicit server mode exists |
| Sensitive values leak into logs | Log metadata and IDs, never object values | Redact export and maintain a sensitive-data test suite |
