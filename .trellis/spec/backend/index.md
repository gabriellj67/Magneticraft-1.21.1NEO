# Magneticraft Java/Forge Development Guidelines

> Project-specific contracts for the Forge 1.20.1 pure-Java migration.

## Overview

This is a greenfield port whose behavioral authority is the Nova Magneticraft
1.12 branch. The current repository began as a minimal scaffold, so these files
capture the explicitly accepted migration conventions that all implementation
tasks must establish and preserve.

## Guidelines index

| Guide | Scope | Status |
|---|---|---|
| [Directory Structure](./directory-structure.md) | Java packages, resources and client/common boundaries | Complete |
| [Persistence and Data](./database-guidelines.md) | NBT, SavedData, recipes, config and datagen | Complete |
| [Error Handling](./error-handling.md) | Setup invariants, reload failures and packet validation | Complete |
| [Logging](./logging-guidelines.md) | SLF4J levels, context and rate control | Complete |
| [Quality](./quality-guidelines.md) | Java/Forge rules, provenance and verification gates | Complete |
| [Electrical Data Contracts](./electrical-data-contracts.md) | Atomic voltage-tier, transformer and machine-profile reload/sync contracts | Complete |
| [Physical Electrical Network Runtime](./electrical-network-runtime.md) | Multi-terminal topology, exact RC exchange, telemetry and electrical NBT | Complete |

## Pre-development checklist

Before modifying production code or resources:

1. Read [Directory Structure](./directory-structure.md).
2. Read [Quality](./quality-guidelines.md).
3. Read [Persistence and Data](./database-guidelines.md) for block entities,
   networks, recipes, config or generated resources.
4. Read [Error Handling](./error-handling.md) and
   [Logging](./logging-guidelines.md) for packets, reload listeners, optional
   integrations or lifecycle code.
5. Read [Electrical Data Contracts](./electrical-data-contracts.md) before
   changing native-electricity data, simulation, persistence, packets or UI.
6. Read [Physical Electrical Network Runtime](./electrical-network-runtime.md)
   before changing terminal identity, RC exchange, topology, couplers or
   electrical telemetry.
7. Read `../guides/code-reuse-thinking-guide.md` before adding a shared helper,
   base class, constant or configuration value.
8. Read `../guides/cross-layer-thinking-guide.md` when a change crosses three or
   more of registry, data, simulation, persistence, networking, menu and client
   rendering.
9. Inspect the Nova 1.12 behavior before selecting a Forge 1.20.1 API shape.

## Quality check

- Run every applicable gate in `quality-guidelines.md`.
- Trace state across simulation, persistence, packet/menu synchronization and
  rendering when more than one layer is affected.
- Confirm source/assets provenance and the architecture-only boundary for
  `hypersmc/magneticraft2-1.20.x`.
- Inspect the exact staged path list before the required Chinese commit.

## Language

Specifications and source documentation are written in English. User-facing
communication and Git commit messages are written in Simplified Chinese.
