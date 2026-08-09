# ADR-0002: Independent streaming HPROF core for the MVP

- Status: accepted
- Date: 2026-08-09

## Context

Eclipse MAT and Eclipse Jifa already implement deep heap analysis, including histograms, leak suspects, OQL, comparison, and dominator trees. They are mature EPL-2.0 projects, but embedding MAT brings an OSGi/p2 runtime and couples distribution to platform-specific Eclipse bundles.

The first useful HeapScout slice needs only metadata, a class histogram, search, and histogram comparison. Those can be computed in a streaming pass without retaining every object edge.

## Decision

Create a pure Kotlin `engine` module with framework-free analysis contracts and a streaming OpenJDK/HotSpot HPROF adapter. The parser keeps class/string metadata and class aggregates, not a full object graph.

Advanced graph analysis will be introduced behind a separate engine port. Eclipse MAT/Jifa remains a valid future adapter, but no internal MAT type may cross into the domain contract.

## Trade-offs

- The MVP stays small, testable, and permissively licensable.
- Array shallow sizes may be estimates because HPROF does not encode every VM layout detail; the UI must label estimates.
- Retained size, dominators, GC-root paths, and full object inspection are deferred.
- Supporting additional HPROF variants requires explicit fixtures and compatibility tests.

## Failure handling

- Reject unknown identifier widths and impossible record lengths before allocation.
- Include the byte offset and record tag in parse errors.
- Check cancellation between records and large skips.
- Never load a record-sized byte array unless it has a strict upper bound.
