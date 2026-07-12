# Porting sources and provenance

This document is the source-of-truth for code and asset provenance during the
Minecraft 1.20.1 port. A source being listed here does not by itself authorize
copying from it; the reuse policy in the table is mandatory.

## Fixed reference snapshots

| Source | Fixed commit | Declared/detected license | Role and reuse policy |
|---|---|---|---|
| [Nova-Committee/Magneticraft](https://github.com/Nova-Committee/Magneticraft) | `4108ca9bb332d11965c30e0c592b310d0858f251` | GPL-2.0 | Behavioral and asset authority for the 1.12 release. Derived work must remain GPL-2.0-compatible and retain attribution. |
| [Magneticraft-Team/Magneticraft, 1.14 branch](https://github.com/Magneticraft-Team/Magneticraft/tree/1.14) | `9c980e328b2c3368fa92b6fd464df0ff95a8f979` | GPL-2.0 | Incomplete official transition reference only. It does not override working 1.12 behavior. |
| [MochiButter/Magneticraft](https://github.com/MochiButter/Magneticraft) | `140fdf5c1076570aebf35a65e44a499482863812` | GPL-2.0 | Java/Forge 1.20.1 reference. Code may be adapted only after file-level review, with the derived location recorded below. |
| [hypersmc/magneticraft2-1.20.x](https://github.com/hypersmc/magneticraft2-1.20.x) | `9a70e6f64971d626b3668316cb8221448e48c471` | `All Rights Reserved` in mod metadata; repository license is not a reusable mod-source grant | Architecture comparison only. Do not copy source, resources, data, identifiers, or text. |

The snapshots are downloaded to the ignored `.references/` directory for local
inspection. They are not build inputs and must never be added to a commit or a
published artifact.

## Decision precedence

1. Observable behavior from the fixed Nova 1.12 snapshot.
2. Explicit product decisions recorded for this port.
3. Forge 1.20.1 and Minecraft 1.20.1 lifecycle and data contracts.
4. Official 1.14 and audited GPL-compatible 1.20.1 examples as implementation
   evidence, never as a substitute for behavior analysis.

## Reuse log

No source or asset from a reference repository was copied into the engineering
baseline. The first migrated configuration default,
`general.crushing_table_causes_fire=true`, was independently reimplemented from
the behavior declared by Nova 1.12 `systems/config/Config.kt`.

Future adaptations must add one row before the corresponding commit:

| Local path | Source path and commit | Adaptation summary | Reviewer |
|---|---|---|---|
| _None yet_ | | | |

## Magneticraft2 boundary

Allowed: compare package responsibilities, graph boundaries, save/load failure
modes, and public behavior visible in a running build.

Forbidden: transcribing or mechanically translating its classes, JSON schemas,
models, textures, language strings, recipes, or documentation. When it suggests
an architecture, implement that architecture independently from the Nova 1.12
behavior and Forge documentation.
