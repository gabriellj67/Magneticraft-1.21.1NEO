# Error Handling

> Failure policy for a server-authoritative Forge mod.

## Principles

- Fail fast during registration or common setup when an invariant makes the mod
  unusable. Include the registry or data ID in the exception message.
- Isolate recoverable data errors. A malformed optional recipe, model or
  integration entry should identify itself, log once and be skipped when doing
  so cannot corrupt game state.
- Treat every client-to-server packet as untrusted operation intent. Validate
  sender, menu, dimension, position, distance, permissions, payload bounds and
  current block entity before mutating state.
- Keep the server authoritative. A client prediction mismatch may refresh the
  view; it must never overwrite durable server state.
- Optional integrations must not make classes from an absent mod load. Gate at
  the integration boundary and keep shared code free of optional API types.

## Error types

- Use `IllegalArgumentException` for invalid caller input and
  `IllegalStateException` for violated internal lifecycle invariants.
- Use a small domain exception only when callers can make a meaningful decision
  based on its type. Do not create a hierarchy that merely renames standard
  exceptions.
- Data codecs and recipe serializers return their native structured errors when
  the API supports them; add the Magneticraft identifier as context.

## Handling patterns

```java
public static void handle(SetMachineModeMessage message, Supplier<NetworkEvent.Context> context) {
    NetworkEvent.Context networkContext = context.get();
    networkContext.enqueueWork(() -> {
        ServerPlayer sender = networkContext.getSender();
        if (sender == null || !(sender.containerMenu instanceof MachineMenu menu)) {
            return;
        }
        menu.trySetMode(sender, message.pos(), message.mode());
    });
    networkContext.setPacketHandled(true);
}
```

- Packet handlers enqueue world changes on the logical server thread.
- Resource reload code reports every rejected ID and continues only when the
  remaining state is internally consistent.
- Cleanup belongs in lifecycle methods or `finally` when resources are actually
  acquired. Do not use broad catch blocks as control flow.
- Player-facing failures use translatable components. Logs keep diagnostic
  details; chat messages remain concise and do not expose filesystem paths or
  stack traces.

## Scenario: server-authoritative machine operation packet

### 1. Scope / trigger

Use this contract whenever a screen asks the server to change a machine mode,
button, filter or other durable setting. The packet carries intent, not trusted
machine state.

### 2. Signatures

```java
public record SetMachineModeMessage(BlockPos pos, int mode) {
}

static void handle(
        SetMachineModeMessage message,
        Supplier<NetworkEvent.Context> contextSupplier
)
```

Register the message as play-to-server only. Decode `mode` as a bounded integer
and do not accept a client-supplied dimension or player identity.

### 3. Contracts

- `pos`: must be the block position represented by the sender's currently open
  Magneticraft menu.
- `mode`: must be one of the modes supported by that exact machine instance.
- Sender dimension, permission and distance are derived from `ServerPlayer`.
- Success mutates state once on the server thread, calls `setChanged()` and
  synchronizes the menu/view through the normal data-slot or update mechanism.
- Rejection performs no mutation and sends no success acknowledgement.

### 4. Validation and error matrix

| Condition | Result | Log |
|---|---|---|
| Sender is absent | Ignore and mark packet handled | Debug if diagnosing transport |
| Current menu is not the expected type | Ignore | None |
| Menu position differs from `pos` | Reject | Rate-limited debug |
| Chunk or block entity is unavailable | Reject | Debug |
| Sender is too far away or lacks permission | Reject | Rate-limited debug |
| `mode` is outside the machine's supported set | Reject | Debug with mode and position |
| All checks pass | Apply exactly once and synchronize | None |

### 5. Good, base and bad cases

- Good: a player clicks a valid mode while the matching machine menu is open;
  the server applies it and every viewer observes the update.
- Base: the player closes the menu while the packet is in flight; the packet is
  ignored without an exception or state change.
- Bad: a modified client sends an arbitrary loaded position and extreme mode;
  both values are rejected and the targeted block entity remains unchanged.

### 6. Tests required

- Unit-test the supported-mode predicate at lower, upper and invalid bounds.
- GameTest a valid request and assert the server block entity changes once and
  is dirty for saving.
- Test stale-menu, wrong-position, unloaded-position, distance and invalid-mode
  requests; assert no durable state changes.
- Dedicated-server smoke test message registration for client-class leakage.

### 7. Wrong vs correct

```java
// Wrong: trusts the packet position and mutates from the network callback.
level.getBlockEntity(message.pos()).setMode(message.mode());

// Correct: enqueue work, derive the level from the sender, validate the open
// menu and let the machine enforce its supported modes before mutation.
context.enqueueWork(() -> menu.trySetMode(sender, message.pos(), message.mode()));
```

## Forbidden patterns

- Empty `catch` blocks or `catch (Exception)` that silently substitutes a value.
- `printStackTrace`, `System.out` or repeated per-tick error logging.
- Trusting a packet's dimension, position, amount or selected recipe without
  verifying it against the sender's current server state.
- Catching linkage errors to hide a wrongly loaded optional integration.
- Returning `null` where the Minecraft/Forge API provides `Optional`, an empty
  handler or an explicit failure result.

## Verification

- Test invalid, empty, boundary and stale inputs for serializers and packets.
- Verify malformed optional data does not prevent an otherwise valid server
  from starting.
- Verify invariant failures include enough context to locate the responsible
  registry name, module ID or block position.
