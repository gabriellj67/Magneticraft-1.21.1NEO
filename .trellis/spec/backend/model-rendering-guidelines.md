# MCX and glTF Rendering Contracts

## Scenario: Preserve source-material alpha semantics

### 1. Scope / trigger

Apply whenever an MCX/glTF primitive is parsed, baked as a block/item model or
rendered as a runtime named part. The contract prevents transparent model
texels from writing opaque depth and making adjacent blocks appear hollow or
see-through.

### 2. Signatures

```java
enum AlphaMode { OPAQUE, MASK, BLEND }

record Primitive(
        List<Vertex> vertices,
        List<Integer> indices,
        ResourceLocation texture,
        int tintIndex,
        AlphaMode alphaMode
) { }
```

```java
// Static baked path
static ResourceLocation inferredRenderType(ModelScene scene)

// Runtime named-part path
static RenderType renderType(ModelScene scene, RenderStyle style,
        ResourceLocation texture)
```

### 3. Contracts

| Source material | Scene mode | Static Forge layer | Runtime entity layer |
|---|---|---|---|
| glTF omitted / `OPAQUE` | `OPAQUE` | `minecraft:solid` | `entitySolid` |
| glTF `MASK` | `MASK` | `minecraft:cutout` | `entityCutoutNoCull` |
| glTF `BLEND` | `BLEND` | `minecraft:translucent` | `entityTranslucent` |
| MCX material | `MASK` | `minecraft:cutout` | `entityCutoutNoCull` |

- A scene containing several modes uses the most permissive mode in this
  order: `OPAQUE < MASK < BLEND`.
- An explicit Forge `render_type` in a model wrapper may override only the
  inferred static layer. Runtime named parts still follow the source scene.
- `MASK` texels are discarded before depth is written. They may reveal an
  opaque block behind the model, but may not suppress that block's faces.

### 4. Validation and error matrix

| Condition | Required result |
|---|---|
| glTF material omits `alphaMode` | Parse as `OPAQUE` |
| glTF material uses `OPAQUE`, `MASK` or `BLEND` | Map exactly as above |
| glTF material uses any other value | Fail parsing and include `$.materials[i].alphaMode` |
| Primitive omits its material | Use `OPAQUE` |
| MCX has no alpha metadata | Use compatibility default `MASK` |
| Wrapper declares `render_type` | Preserve the explicit static override |

### 5. Good, base and bad cases

- Good: a masked grinder beside a stone wall discards transparent texels and
  the wall retains all visible faces.
- Base: an opaque electrical enclosure uses the solid layer and occludes like
  a normal opaque model.
- Bad: a masked pneumatic tube is baked as solid, so transparent texels write
  depth and neighboring blocks appear to have missing faces.

### 6. Tests required

- Parser unit tests assert default `OPAQUE`, explicit `MASK`/`BLEND`, invalid
  values with the material path, and MCX compatibility `MASK`.
- Resource contract tests parse at least one released opaque electrical model
  and two released masked machine/pipe models.
- Static layer tests assert `solid`, `cutout` and `translucent` inference.
- Graphical acceptance uses a newly created Creative world, switches to the
  English input method only after entering it, and places an opaque wall
  immediately beside and behind a masked glTF model.

### 7. Wrong vs correct

```java
// Wrong: all static glTF scenes silently enter the default solid layer.
return geometry.bake(context, baker, materialGetter, modelState, overrides,
        modelLocation);

// Correct: retain material semantics and select the matching Forge layer.
ResourceLocation layer = inferredRenderType(scene);
RenderTypeGroup group = context.getRenderType(layer);
return bakeWithRenderType(scene, group);
```
