#!/usr/bin/env python3
"""Deterministically convert the pinned Nova 1.12 MCX/glTF snapshot to Forge OBJ assets.

The legacy formats are build inputs only.  Generated OBJ/MTL/model JSON and copied
textures are the sole runtime resources.  The output directories are owned by this
script so obsolete generated files can be detected or removed safely.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import shutil
import struct
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = PROJECT_ROOT / "scripts" / "legacy_model_manifest.json"
GENERATED_DIRECTORIES = (
    "models/block/legacy",
    "models/legacy",
    "textures/block/legacy",
)


def fail(message: str) -> "NoReturn":
    raise RuntimeError(message)


def number(value: float) -> str:
    if not math.isfinite(value):
        fail(f"non-finite model coordinate: {value}")
    text = f"{value:.9f}".rstrip("0").rstrip(".")
    return "0" if text in {"", "-0"} else text


def safe_name(value: str) -> str:
    normalized = re.sub(r"[^A-Za-z0-9_.-]+", "_", value).strip("_")
    return normalized or "unnamed"


def matrix_identity() -> list[list[float]]:
    return [
        [1.0, 0.0, 0.0, 0.0],
        [0.0, 1.0, 0.0, 0.0],
        [0.0, 0.0, 1.0, 0.0],
        [0.0, 0.0, 0.0, 1.0],
    ]


def matrix_multiply(left: Sequence[Sequence[float]], right: Sequence[Sequence[float]]) -> list[list[float]]:
    return [
        [sum(left[row][index] * right[index][column] for index in range(4)) for column in range(4)]
        for row in range(4)
    ]


def node_matrix(node: dict) -> list[list[float]]:
    if "matrix" in node:
        values = node["matrix"]
        if len(values) != 16:
            fail("glTF node matrix must contain sixteen values")
        return [[float(values[column * 4 + row]) for column in range(4)] for row in range(4)]

    translation = [float(value) for value in node.get("translation", [0.0, 0.0, 0.0])]
    scale = [float(value) for value in node.get("scale", [1.0, 1.0, 1.0])]
    x, y, z, w = [float(value) for value in node.get("rotation", [0.0, 0.0, 0.0, 1.0])]
    length = math.sqrt(x * x + y * y + z * z + w * w)
    if length == 0.0:
        x, y, z, w = 0.0, 0.0, 0.0, 1.0
    else:
        x, y, z, w = x / length, y / length, z / length, w / length

    rotation = [
        [1.0 - 2.0 * (y * y + z * z), 2.0 * (x * y - z * w), 2.0 * (x * z + y * w), 0.0],
        [2.0 * (x * y + z * w), 1.0 - 2.0 * (x * x + z * z), 2.0 * (y * z - x * w), 0.0],
        [2.0 * (x * z - y * w), 2.0 * (y * z + x * w), 1.0 - 2.0 * (x * x + y * y), 0.0],
        [0.0, 0.0, 0.0, 1.0],
    ]
    scaling = matrix_identity()
    scaling[0][0], scaling[1][1], scaling[2][2] = scale
    result = matrix_multiply(rotation, scaling)
    result[0][3], result[1][3], result[2][3] = translation
    return result


def transform_point(matrix: Sequence[Sequence[float]], point: Sequence[float]) -> tuple[float, float, float]:
    x, y, z = point
    return (
        matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z + matrix[0][3],
        matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z + matrix[1][3],
        matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z + matrix[2][3],
    )


def face_normal(points: Sequence[Sequence[float]]) -> tuple[float, float, float]:
    if len(points) < 3:
        return 0.0, 1.0, 0.0
    ax, ay, az = (points[1][index] - points[0][index] for index in range(3))
    bx, by, bz = (points[2][index] - points[0][index] for index in range(3))
    nx, ny, nz = ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx
    length = math.sqrt(nx * nx + ny * ny + nz * nz)
    return (0.0, 1.0, 0.0) if length <= 1.0e-12 else (nx / length, ny / length, nz / length)


@dataclass(frozen=True)
class Face:
    group: str
    texture: str
    vertices: tuple[tuple[float, float, float], ...]
    uvs: tuple[tuple[float, float], ...]


class Mesh:
    def __init__(self) -> None:
        self.faces: list[Face] = []

    def add_face(
        self,
        group: str,
        texture: str,
        vertices: Sequence[Sequence[float]],
        uvs: Sequence[Sequence[float]] | None,
    ) -> None:
        if len(vertices) < 3:
            return
        normalized_uvs = uvs if uvs is not None and len(uvs) == len(vertices) else [(0.0, 0.0)] * len(vertices)
        self.faces.append(
            Face(
                safe_name(group),
                texture,
                tuple(tuple(float(value) for value in vertex) for vertex in vertices),
                tuple((float(uv[0]), float(uv[1])) for uv in normalized_uvs),
            )
        )


class Conversion:
    def __init__(self, manifest_path: Path) -> None:
        self.manifest_path = manifest_path
        self.manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        if self.manifest.get("schema_version") != 1:
            fail("unsupported legacy model manifest schema")
        self.source_repository = (PROJECT_ROOT / self.manifest["source_repository"]).resolve()
        self.source_root = (PROJECT_ROOT / self.manifest["source_root"]).resolve()
        self.output_root = (PROJECT_ROOT / self.manifest["output_root"]).resolve()
        self.outputs: dict[Path, bytes] = {}

    def run(self) -> dict[Path, bytes]:
        self.verify_source()
        identifiers: set[str] = set()
        for artifact in self.manifest["artifacts"]:
            identifier = artifact["id"]
            if identifier in identifiers:
                fail(f"duplicate artifact id: {identifier}")
            identifiers.add(identifier)
            source = self.source_root / artifact["source"]
            if not source.is_file():
                fail(f"legacy model is missing: {source}")
            frame_count = int(artifact.get("animation_frames", 0))
            if frame_count < 0 or frame_count == 1:
                fail(f"animation_frames must be zero or at least two: {identifier}")
            if frame_count:
                if artifact["format"] != "gltf":
                    fail(f"only glTF artifacts can be sampled as animation frames: {identifier}")
                for frame in range(frame_count):
                    frame_artifact = dict(artifact)
                    frame_artifact["animation_fraction"] = frame / frame_count
                    frame_identifier = f"{identifier}_frame_{frame}"
                    mesh = self.apply_artifact_transform(
                        self.convert_gltf(source, frame_artifact),
                        frame_artifact,
                    )
                    if not mesh.faces:
                        fail(f"artifact selected no geometry: {frame_identifier}")
                    self.emit_artifact(frame_identifier, frame_artifact, mesh)
                continue
            if artifact["format"] == "mcx":
                mesh = self.convert_mcx(source, artifact)
            elif artifact["format"] == "gltf":
                mesh = self.convert_gltf(source, artifact)
            else:
                fail(f"unsupported legacy model format: {artifact['format']}")
            mesh = self.apply_artifact_transform(mesh, artifact)
            if not mesh.faces:
                fail(f"artifact selected no geometry: {identifier}")
            self.emit_artifact(identifier, artifact, mesh)
        return self.outputs

    @staticmethod
    def apply_artifact_transform(mesh: Mesh, artifact: dict) -> Mesh:
        translation = artifact.get("translation", [0.0, 0.0, 0.0])
        scale = artifact.get("scale", [1.0, 1.0, 1.0])
        if len(translation) != 3 or len(scale) != 3:
            fail("artifact translation and scale must contain three values")
        if translation == [0.0, 0.0, 0.0] and scale == [1.0, 1.0, 1.0]:
            return mesh
        transformed = Mesh()
        for face in mesh.faces:
            transformed.add_face(
                face.group,
                face.texture,
                [
                    (
                        vertex[0] * float(scale[0]) + float(translation[0]),
                        vertex[1] * float(scale[1]) + float(translation[1]),
                        vertex[2] * float(scale[2]) + float(translation[2]),
                    )
                    for vertex in face.vertices
                ],
                face.uvs,
            )
        return transformed

    def verify_source(self) -> None:
        if not self.source_root.is_dir():
            fail(f"pinned legacy source tree is missing: {self.source_root}")
        digest = hashlib.sha256()
        source_files = sorted(path for path in self.source_root.rglob("*") if path.is_file())
        for path in source_files:
            relative = path.relative_to(self.source_root).as_posix().encode("utf-8")
            content = path.read_bytes()
            digest.update(len(relative).to_bytes(4, "big"))
            digest.update(relative)
            digest.update(len(content).to_bytes(8, "big"))
            digest.update(content)
        actual = digest.hexdigest()
        expected = self.manifest["source_tree_sha256"]
        if actual != expected:
            fail(f"legacy source tree digest mismatch: expected {expected}, got {actual}")

    def convert_mcx(self, source: Path, artifact: dict) -> Mesh:
        data = json.loads(source.read_text(encoding="utf-8"))
        positions = data["quads"]["pos"]
        uvs = data["quads"]["tex"]
        indices = data["quads"]["indices"]
        include = set(artifact.get("include_parts", []))
        exclude = set(artifact.get("exclude_parts", []))
        mesh = Mesh()
        for part in data["parts"]:
            name = part["name"]
            if include and name not in include:
                continue
            if name in exclude:
                continue
            for face in indices[int(part["from"]):int(part["to"])]:
                vertex_indices, uv_indices = face[0], face[1]
                mesh.add_face(
                    name,
                    part["texture"],
                    [positions[index] for index in vertex_indices],
                    [uvs[index] for index in uv_indices],
                )
        return mesh

    def convert_gltf(self, source: Path, artifact: dict) -> Mesh:
        data = json.loads(source.read_text(encoding="utf-8"))
        buffers = [(source.parent / entry["uri"]).read_bytes() for entry in data.get("buffers", [])]
        animated = self.sample_animation(data, buffers, artifact)
        include_nodes = set(artifact.get("include_nodes", []))
        include_subtrees = set(artifact.get("include_subtrees", []))
        exclude_subtrees = set(artifact.get("exclude_subtrees", []))
        mesh = Mesh()

        scene_index = int(data.get("scene", 0))
        scene_roots = data.get("scenes", [{}])[scene_index].get("nodes", [])

        def visit(node_index: int, parent_matrix: Sequence[Sequence[float]], ancestors: tuple[str, ...]) -> None:
            node = data["nodes"][node_index]
            name = node.get("name", f"node_{node_index}")
            lineage = ancestors + (name,)
            rendered_node = dict(node)
            rendered_node.update(animated.get(node_index, {}))
            world_matrix = matrix_multiply(parent_matrix, node_matrix(rendered_node))
            included = not include_nodes and not include_subtrees
            if include_nodes and name in include_nodes:
                included = True
            if include_subtrees and any(root in lineage for root in include_subtrees):
                included = True
            if any(root in lineage for root in exclude_subtrees):
                included = False
            if included and "mesh" in node:
                self.append_gltf_mesh(mesh, data, buffers, int(node["mesh"]), node_index, name, world_matrix)
            for child in node.get("children", []):
                visit(int(child), world_matrix, lineage)

        for root in scene_roots:
            visit(int(root), matrix_identity(), tuple())
        return mesh

    def sample_animation(self, data: dict, buffers: Sequence[bytes], artifact: dict) -> dict[int, dict]:
        if "animation_fraction" not in artifact:
            return {}
        animations = data.get("animations", [])
        animation_index = int(artifact.get("animation_index", 0))
        if not 0 <= animation_index < len(animations):
            fail(f"glTF animation index is out of range: {animation_index}")
        animation = animations[animation_index]
        sampler_data: list[tuple[list[float], list[tuple[float, ...]], str]] = []
        duration = 0.0
        for sampler in animation["samplers"]:
            times = [values[0] for values in self.read_accessor(data, buffers, int(sampler["input"]))]
            samples = self.read_accessor(data, buffers, int(sampler["output"]))
            if not times or len(times) != len(samples):
                fail("glTF animation sampler has mismatched inputs and outputs")
            duration = max(duration, times[-1])
            sampler_data.append((times, samples, sampler.get("interpolation", "LINEAR")))
        sample_time = max(0.0, min(1.0, float(artifact["animation_fraction"]))) * duration
        result: dict[int, dict] = {}
        for channel in animation["channels"]:
            target = channel["target"]
            path = target["path"]
            if path not in {"translation", "rotation", "scale"}:
                fail(f"unsupported glTF animation target: {path}")
            times, samples, interpolation = sampler_data[int(channel["sampler"])]
            value = self.interpolate_animation(times, samples, interpolation, sample_time, path == "rotation")
            result.setdefault(int(target["node"]), {})[path] = list(value)
        return result

    @staticmethod
    def interpolate_animation(
        times: Sequence[float],
        samples: Sequence[Sequence[float]],
        interpolation: str,
        time: float,
        quaternion: bool,
    ) -> tuple[float, ...]:
        if time <= times[0]:
            return tuple(samples[0])
        if time >= times[-1]:
            return tuple(samples[-1])
        upper = next(index for index, value in enumerate(times) if value >= time)
        lower = upper - 1
        if interpolation == "STEP":
            return tuple(samples[lower])
        if interpolation != "LINEAR":
            fail(f"unsupported glTF animation interpolation: {interpolation}")
        span = times[upper] - times[lower]
        alpha = 0.0 if span <= 0.0 else (time - times[lower]) / span
        start = tuple(float(value) for value in samples[lower])
        end = tuple(float(value) for value in samples[upper])
        if not quaternion:
            return tuple(a + (b - a) * alpha for a, b in zip(start, end))
        dot = sum(a * b for a, b in zip(start, end))
        if dot < 0.0:
            end = tuple(-value for value in end)
            dot = -dot
        if dot > 0.9995:
            blended = tuple(a + (b - a) * alpha for a, b in zip(start, end))
        else:
            theta = math.acos(max(-1.0, min(1.0, dot)))
            sine = math.sin(theta)
            first = math.sin((1.0 - alpha) * theta) / sine
            second = math.sin(alpha * theta) / sine
            blended = tuple(a * first + b * second for a, b in zip(start, end))
        length = math.sqrt(sum(value * value for value in blended))
        return tuple(value / length for value in blended) if length > 0.0 else (0.0, 0.0, 0.0, 1.0)

    def append_gltf_mesh(
        self,
        target: Mesh,
        data: dict,
        buffers: Sequence[bytes],
        mesh_index: int,
        node_index: int,
        node_name: str,
        matrix: Sequence[Sequence[float]],
    ) -> None:
        mesh = data["meshes"][mesh_index]
        for primitive_index, primitive in enumerate(mesh["primitives"]):
            if int(primitive.get("mode", 4)) != 4:
                fail(f"only glTF triangle primitives are supported: mesh {mesh_index}")
            attributes = primitive["attributes"]
            positions = self.read_accessor(data, buffers, int(attributes["POSITION"]))
            texcoords = (
                self.read_accessor(data, buffers, int(attributes["TEXCOORD_0"]))
                if "TEXCOORD_0" in attributes
                else [(0.0, 0.0)] * len(positions)
            )
            raw_indices = (
                self.read_accessor(data, buffers, int(primitive["indices"]))
                if "indices" in primitive
                else [(index,) for index in range(len(positions))]
            )
            flat_indices = [int(values[0]) for values in raw_indices]
            if len(flat_indices) % 3 != 0:
                fail(f"glTF triangle index count is not divisible by three: mesh {mesh_index}")
            texture = self.gltf_texture(data, primitive)
            group = f"{safe_name(node_name)}_{node_index}_{primitive_index}"
            transformed = [transform_point(matrix, position) for position in positions]
            for offset in range(0, len(flat_indices), 3):
                triangle = flat_indices[offset:offset + 3]
                target.add_face(
                    group,
                    texture,
                    [transformed[index] for index in triangle],
                    [texcoords[index] for index in triangle],
                )

    @staticmethod
    def read_accessor(data: dict, buffers: Sequence[bytes], accessor_index: int) -> list[tuple[float, ...]]:
        accessor = data["accessors"][accessor_index]
        if "sparse" in accessor:
            fail("sparse glTF accessors are not supported")
        view = data["bufferViews"][int(accessor["bufferView"])]
        component_type = int(accessor["componentType"])
        formats = {
            5120: ("b", 1),
            5121: ("B", 1),
            5122: ("h", 2),
            5123: ("H", 2),
            5125: ("I", 4),
            5126: ("f", 4),
        }
        if component_type not in formats:
            fail(f"unsupported glTF accessor component type: {component_type}")
        component_format, component_size = formats[component_type]
        component_count = {
            "SCALAR": 1,
            "VEC2": 2,
            "VEC3": 3,
            "VEC4": 4,
            "MAT4": 16,
        }.get(accessor["type"])
        if component_count is None:
            fail(f"unsupported glTF accessor type: {accessor['type']}")
        stride = int(view.get("byteStride", component_size * component_count))
        base_offset = int(view.get("byteOffset", 0)) + int(accessor.get("byteOffset", 0))
        source = buffers[int(view["buffer"])]
        unpack_format = "<" + component_format * component_count
        values: list[tuple[float, ...]] = []
        for index in range(int(accessor["count"])):
            raw = struct.unpack_from(unpack_format, source, base_offset + index * stride)
            if accessor.get("normalized", False) and component_type != 5126:
                raw = Conversion.normalize_components(raw, component_type)
            values.append(tuple(float(value) for value in raw))
        return values

    @staticmethod
    def normalize_components(values: Sequence[int], component_type: int) -> tuple[float, ...]:
        if component_type == 5120:
            return tuple(max(value / 127.0, -1.0) for value in values)
        if component_type == 5121:
            return tuple(value / 255.0 for value in values)
        if component_type == 5122:
            return tuple(max(value / 32767.0, -1.0) for value in values)
        if component_type == 5123:
            return tuple(value / 65535.0 for value in values)
        if component_type == 5125:
            return tuple(value / 4294967295.0 for value in values)
        fail(f"unsupported normalized glTF component type: {component_type}")

    @staticmethod
    def gltf_texture(data: dict, primitive: dict) -> str:
        if "material" not in primitive:
            return "minecraft:block/iron_block"
        material = data["materials"][int(primitive["material"])]
        texture_info = material.get("pbrMetallicRoughness", {}).get("baseColorTexture")
        if texture_info is None:
            return "minecraft:block/iron_block"
        texture = data["textures"][int(texture_info["index"])]
        image = data["images"][int(texture["source"])]
        return image["uri"]

    def emit_artifact(self, identifier: str, artifact: dict, mesh: Mesh) -> None:
        textures = list(dict.fromkeys(face.texture for face in mesh.faces))
        rewritten = {texture: self.copy_texture(texture) for texture in textures}
        material_names = {texture: f"material_{index}_{safe_name(texture)}" for index, texture in enumerate(textures)}
        obj_lines = [
            "# Generated by scripts/convert_legacy_models.py; do not edit.",
            f"# Source: {artifact['source']} @ {self.manifest['source_commit']}",
            f"mtllib {identifier}.mtl",
            "",
        ]
        vertex_index = 1
        uv_index = 1
        normal_index = 1
        face_records: list[tuple[Face, list[int], list[int], int]] = []
        for face in mesh.faces:
            vertices: list[int] = []
            face_uvs: list[int] = []
            for vertex in face.vertices:
                obj_lines.append("v " + " ".join(number(value) for value in vertex))
                vertices.append(vertex_index)
                vertex_index += 1
            for uv in face.uvs:
                obj_lines.append("vt " + " ".join(number(value) for value in uv))
                face_uvs.append(uv_index)
                uv_index += 1
            normal = face_normal(face.vertices)
            obj_lines.append("vn " + " ".join(number(value) for value in normal))
            face_records.append((face, vertices, face_uvs, normal_index))
            normal_index += 1
        obj_lines.append("")
        active_group = None
        active_material = None
        for face, vertices, face_uvs, face_normal_index in face_records:
            if face.group != active_group:
                obj_lines.append(f"g {face.group}")
                active_group = face.group
            material = material_names[face.texture]
            if material != active_material:
                obj_lines.append(f"usemtl {material}")
                active_material = material
            references = [
                f"{vertex}/{texture}/{face_normal_index}"
                for vertex, texture in zip(vertices, face_uvs)
            ]
            obj_lines.append("f " + " ".join(references))

        mtl_lines = ["# Generated by scripts/convert_legacy_models.py; do not edit.", ""]
        for texture in textures:
            mtl_lines.extend(
                [
                    f"newmtl {material_names[texture]}",
                    "Ka 1 1 1",
                    "Kd 1 1 1",
                    f"map_Kd {rewritten[texture]}",
                    "",
                ]
            )

        particle = self.copy_texture(artifact.get("particle", textures[0]))
        model_json = {
            "flip_v": True,
            "loader": "forge:obj",
            "model": f"magneticraft:models/block/legacy/{identifier}.obj",
            "mtl_override": f"magneticraft:models/block/legacy/{identifier}.mtl",
            "textures": {"particle": particle},
        }
        self.add_output(f"models/block/legacy/{identifier}.obj", ("\n".join(obj_lines) + "\n").encode())
        self.add_output(
            f"models/block/legacy/{identifier}.mtl",
            ("\n".join(mtl_lines).rstrip("\n") + "\n").encode(),
        )
        self.add_output(
            f"models/legacy/{identifier}.json",
            (json.dumps(model_json, indent=2, sort_keys=True) + "\n").encode(),
        )

    def copy_texture(self, resource: str) -> str:
        if ":" not in resource:
            fail(f"texture is not namespaced: {resource}")
        namespace, path = resource.split(":", 1)
        path = path.removesuffix(".png")
        if namespace == "minecraft":
            if path.startswith("blocks/"):
                path = "block/" + path.removeprefix("blocks/")
            return f"minecraft:{path}"
        if namespace != "magneticraft":
            fail(f"unexpected legacy texture namespace: {resource}")
        if path.startswith("blocks/"):
            legacy_path = path.removeprefix("blocks/")
            source = self.source_root / "textures" / "blocks" / f"{legacy_path}.png"
        elif path.startswith("block/"):
            legacy_path = path.removeprefix("block/")
            source = self.source_root / "textures" / "block" / f"{legacy_path}.png"
        else:
            fail(f"unsupported legacy texture path: {resource}")
        if not source.is_file():
            fail(f"legacy texture is missing: {source}")
        destination = f"textures/block/legacy/{legacy_path}.png"
        self.add_output(destination, source.read_bytes())
        metadata = source.with_suffix(source.suffix + ".mcmeta")
        if metadata.is_file():
            self.add_output(destination + ".mcmeta", metadata.read_bytes())
        return f"magneticraft:block/legacy/{legacy_path}"

    def add_output(self, relative: str, content: bytes) -> None:
        path = self.output_root / relative
        existing = self.outputs.get(path)
        if existing is not None and existing != content:
            fail(f"two artifacts generated conflicting output: {path}")
        self.outputs[path] = content


def owned_existing_files(output_root: Path) -> set[Path]:
    files: set[Path] = set()
    for relative in GENERATED_DIRECTORIES:
        directory = (output_root / relative).resolve()
        if output_root not in directory.parents:
            fail(f"generated directory escaped output root: {directory}")
        if directory.is_dir():
            files.update(path.resolve() for path in directory.rglob("*") if path.is_file())
    return files


def write_outputs(conversion: Conversion, outputs: dict[Path, bytes]) -> None:
    for relative in GENERATED_DIRECTORIES:
        directory = (conversion.output_root / relative).resolve()
        if conversion.output_root not in directory.parents:
            fail(f"refusing to replace directory outside resource root: {directory}")
        if directory.exists():
            shutil.rmtree(directory)
    for path, content in sorted(outputs.items(), key=lambda entry: str(entry[0])):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
    print(f"generated {len(outputs)} deterministic legacy-model resources")


def check_outputs(conversion: Conversion, outputs: dict[Path, bytes]) -> None:
    expected = set(outputs)
    existing = owned_existing_files(conversion.output_root)
    missing = sorted(path for path in expected if not path.is_file())
    stale = sorted(path for path, content in outputs.items() if path.is_file() and path.read_bytes() != content)
    unexpected = sorted(existing - expected)
    if missing or stale or unexpected:
        details = []
        details.extend(f"missing: {path.relative_to(PROJECT_ROOT)}" for path in missing)
        details.extend(f"stale: {path.relative_to(PROJECT_ROOT)}" for path in stale)
        details.extend(f"unexpected: {path.relative_to(PROJECT_ROOT)}" for path in unexpected)
        fail("legacy model outputs are not reproducible:\n" + "\n".join(details))
    print(f"verified {len(outputs)} deterministic legacy-model resources")


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--check", action="store_true", help="compare generated bytes without modifying files")
    arguments = parser.parse_args(argv)
    try:
        conversion = Conversion(arguments.manifest.resolve())
        outputs = conversion.run()
        if arguments.check:
            check_outputs(conversion, outputs)
        else:
            write_outputs(conversion, outputs)
        return 0
    except (OSError, ValueError, KeyError, RuntimeError) as error:
        print(f"legacy model conversion failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
