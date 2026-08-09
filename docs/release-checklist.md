# Release checklist

## Before tagging

- [ ] The GitHub repository visibility and owner are confirmed.
- [ ] CI is green on `main`.
- [ ] `npm audit --audit-level=high` reports no high or critical vulnerabilities.
- [ ] JVM tests, the actual HotSpot compatibility test, and the 1GiB/256MiB bounded-memory test pass.
- [ ] `THIRD_PARTY_NOTICES.md` matches the shipped runtime and frontend bundle.
- [ ] No `.hprof`, `.bin`, credentials, local paths, or private incident data are tracked.
- [ ] The release version uses a `vMAJOR.MINOR.PATCH` tag.

## Publish

Push the selected `v*` tag. The release workflow injects the tag version into Gradle, runs the real HotSpot HPROF compatibility test, builds the production frontend and executable JAR, creates native `jpackage` application images, starts each image on its native runner, and publishes archives plus SHA-256 checksums to the GitHub release.

The tag version is authoritative in the JAR and GitHub release. `jpackage`'s native application-image metadata keeps its platform default during 0.x releases because macOS rejects application versions whose first numeric component is zero.

## Verify release assets

- [ ] macOS archive contains `HeapScout.app`, `LICENSE`, and `THIRD_PARTY_NOTICES.md` within the app image.
- [ ] Windows archive contains `HeapScout.exe`, the runtime, and license files.
- [ ] Linux archive contains `bin/HeapScout`, the runtime, and license files under `lib/app`.
- [ ] Each application starts without a separately installed JDK or Node.js.
- [ ] Each checksum file validates its adjacent release archive.
- [ ] The system picker opens a valid `.hprof` and cancelling it leaves the app usable.
- [ ] A heap dump passed as a launcher argument starts and is restored in the browser.
- [ ] Search and two-dump comparison return deterministic results.
- [ ] Invalid and truncated dumps show actionable errors without crashing the process.

Unsigned builds can trigger macOS Gatekeeper or Windows SmartScreen. Code signing and notarization must be completed by the release owner when platform credentials become available.
