# Logging Guidelines

> Diagnostic logging for Magneticraft on clients and dedicated servers.

## Logger

Use SLF4J through Mojang's logging facade. The mod composition root owns the
general logger; a subsystem may own a named logger only when that materially
improves filtering.

```java
public static final Logger LOGGER = LogUtils.getLogger();
```

Use parameterized messages:

```java
Magneticraft.LOGGER.warn("Ignoring invalid machine recipe {}: {}", recipeId, reason);
```

Do not concatenate messages whose values are expensive to compute.

## Levels

- `ERROR`: an operation or subsystem cannot continue and requires operator or
  developer action. Attach the exception as the final logging argument.
- `WARN`: recoverable invalid data, disabled optional functionality or a state
  repair the operator should know about.
- `INFO`: low-frequency lifecycle facts such as loaded recipe counts or enabled
  integrations. Avoid routine block placement and per-player interaction logs.
- `DEBUG`: network graph rebuilds, capability decisions and detailed state
  transitions needed during development.
- `TRACE`: exceptionally noisy per-node diagnostics, disabled by default.

## Required context

- Prefer stable identifiers: recipe ID, module ID, dimension, block position and
  integration mod ID.
- State physical units when logging quantities that could be ambiguous.
- Log summary counts for bulk reloads and one contextual message per rejected
  entry. Do not log the same failure every tick.
- Security-relevant rejected packets may be logged at debug or rate-limited warn;
  include the sender UUID, never authentication/session secrets.

## What not to log

- Access tokens, credentials, full environment variables or private filesystem
  contents.
- Entire NBT payloads or inventories at info level.
- Normal player actions, expected capability misses or optional-mod absence as
  warnings.
- Client-only visual failures repeatedly from a render loop.

## Common mistakes

- Calling `System.out.println` or `Throwable#printStackTrace`.
- Emitting an exception message without the affected Magneticraft identifier.
- Logging and rethrowing at every layer, producing the same stack trace several
  times. Log at the boundary that handles or terminates the failure.
