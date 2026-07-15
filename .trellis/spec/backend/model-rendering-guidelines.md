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
- The audit boundary is every generated Magneticraft blockstate, not only the
  models named in the historical conversion manifest. Follow every variant,
  multipart model and Magneticraft parent before deciding the effective layer.
- A standard JSON block model whose face-sampled local texture contains any
  alpha below 255 requires at least `minecraft:cutout`. True blended content
  declares `minecraft:translucent` explicitly; PNG alpha values alone do not
  override the source model's material semantics.
- Unformed multiblock controller cubes use retained Nova textures with alpha
  and therefore declare `minecraft:cutout`, independently from the formed
  runtime scene.

### 4. Validation and error matrix

| Condition | Required result |
|---|---|
| glTF material omits `alphaMode` | Parse as `OPAQUE` |
| glTF material uses `OPAQUE`, `MASK` or `BLEND` | Map exactly as above |
| glTF material uses any other value | Fail parsing and include `$.materials[i].alphaMode` |
| Primitive omits its material | Use `OPAQUE` |
| MCX has no alpha metadata | Use compatibility default `MASK` |
| Wrapper declares `render_type` | Preserve the explicit static override |
| Generated face uses a transparent local texture with no safe layer | Fail the all-block model contract |
| A generated blockstate or Magneticraft model parent is missing | Fail with the owning model ID |
| An `OPAQUE` glTF primitive samples a transparent local texture | Fail with the source and texture IDs |

### 5. Good, base and bad cases

- Good: a masked grinder beside a stone wall discards transparent texels and
  the wall retains all visible faces.
- Base: an opaque electrical enclosure uses the solid layer and occludes like
  a normal opaque model.
- Good: every unformed multiblock controller cube uses the shared cutout render
  type while its formed state remains an empty static model plus runtime scene.
- Bad: a masked pneumatic tube is baked as solid, so transparent texels write
  depth and neighboring blocks appear to have missing faces.
- Bad: only converted MCX/glTF wrappers are audited while an ordinary controller
  cube still samples a transparent retained texture through the solid layer.

### 6. Tests required

- Parser unit tests assert default `OPAQUE`, explicit `MASK`/`BLEND`, invalid
  values with the material path, and MCX compatibility `MASK`.
- Resource contract tests recursively traverse every generated blockstate and
  Magneticraft model parent, inspect all face-sampled local PNG textures and
  parse every MCX/glTF source reachable from those blockstates.
- The closure test rejects missing models/textures, unsafe explicit overrides,
  transparent standard models below cutout, and transparent glTF primitives
  declared as `OPAQUE`.
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

// Wrong: a retained transparent controller texture silently uses solid.
ModelFile idle = models().cubeAll(definition.id(), controllerTexture);

// Correct: apply the shared safe layer at the data-generation boundary.
ModelFile idle = models().cubeAll(definition.id(), controllerTexture)
        .renderType(CUTOUT_RENDER_TYPE);
```
