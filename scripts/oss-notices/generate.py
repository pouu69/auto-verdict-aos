#!/usr/bin/env python3
"""Generate the open-source notices shown in the app (Python stdlib only, offline).

Reads a Gradle dependency report plus the POM/JAR/AAR files already present in the
local Gradle cache, and writes two raw resources:

  app/src/main/res/raw/oss_notices.json         grouped libraries + license texts
  app/src/main/res/raw/oss_google_notices.json  open-source components bundled in
                                                 Play services / UMP AARs

Usage (see README.md):
  ./gradlew --offline -q :app:dependencies --configuration releaseRuntimeClasspath > /tmp/deps.txt
  python3 -I scripts/oss-notices/generate.py --deps /tmp/deps.txt

Exits with code 1 (after listing every problem) when an artifact's POM is missing from
the cache or its license can't be mapped — nothing is silently dropped. Output is
sorted and formatted deterministically so re-running produces no diff.
"""
import argparse
import hashlib
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent.parent
DEFAULT_OUT_DIR = REPO_ROOT / "app" / "src" / "main" / "res" / "raw"
SCHEMA_VERSION = 1
MAX_PARENT_DEPTH = 5
TEXT_ID_LENGTH = 10

# "+--- g:a:v -> v2 (*)" / "\--- g:a -> v2" / "|    +--- g:a:v (c)"
TREE_LINE = re.compile(r"^[|\s]*[+\\]--- (?P<body>.+?)\s*$")
TRAILING_MARKER = re.compile(r"\s+\((?P<marker>[*cn])\)$")


class Problems:
    """Collects fatal problems so all of them are reported in one run."""

    def __init__(self):
        self.items = []

    def add(self, message):
        self.items.append(message)

    def exit_if_any(self):
        if self.items:
            print("ERROR: open-source notices could not be generated:", file=sys.stderr)
            for item in self.items:
                print("  - " + item, file=sys.stderr)
            print("Fix: add the artifact to artifactLicenseOverrides in groups.json, or "
                  "build once so Gradle caches the missing POM, then re-run.", file=sys.stderr)
            sys.exit(1)


def warn(message):
    print("WARNING: " + message, file=sys.stderr)


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def read_text_file(relative):
    return normalize_text((SCRIPT_DIR / relative).read_text(encoding="utf-8"))


def normalize_text(text):
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    lines = [line.rstrip() for line in text.split("\n")]
    return "\n".join(lines).strip("\n")


# --- dependency report -----------------------------------------------------------

def parse_dependency_report(path, problems):
    """Returns (configuration name, sorted list of (group, artifact, version))."""
    configuration = None
    coordinates = set()
    with open(path, encoding="utf-8") as f:
        for raw in f:
            line = raw.rstrip("\n")
            header = re.match(r"^(\w+RuntimeClasspath) - ", line)
            if header:
                configuration = header.group(1)
                continue
            match = TREE_LINE.match(line)
            if not match:
                continue
            body = match.group("body")
            marker = TRAILING_MARKER.search(body)
            if marker:
                if marker.group("marker") in ("c", "n"):
                    continue  # constraint / unresolved declaration, not a resolved artifact
                body = body[: marker.start()]
            if body.endswith(" FAILED"):
                problems.add("unresolved dependency in report: " + body)
                continue
            if body.startswith("project "):
                continue
            requested, _, resolved = body.partition(" -> ")
            parts = requested.strip().split(":")
            if len(parts) < 2:
                problems.add("unparseable dependency line: " + line.strip())
                continue
            group, artifact = parts[0], parts[1]
            version = resolved.strip() if resolved else (parts[2] if len(parts) > 2 else "")
            if not version or "{" in version:
                problems.add("no resolved version for " + body)
                continue
            coordinates.add((group, artifact, version))
    if configuration is None:
        problems.add("no '<variant>RuntimeClasspath - ...' header found in " + str(path))
    return configuration, sorted(coordinates)


# --- Gradle cache ------------------------------------------------------------------

class GradleCache:
    def __init__(self, root):
        self.root = root / "caches" / "modules-2" / "files-2.1"

    def find(self, group, artifact, version, extensions):
        base = self.root / group / artifact / version
        if not base.is_dir():
            return None
        for ext in extensions:
            hits = sorted(base.glob("*/%s-%s.%s" % (artifact, version, ext)))
            if hits:
                return hits[0]
        return None


def strip_namespaces(root):
    for element in root.iter():
        if isinstance(element.tag, str) and "}" in element.tag:
            element.tag = element.tag.split("}", 1)[1]
    return root


def read_pom(path):
    return strip_namespaces(ET.parse(str(path)).getroot())


def pom_licenses(cache, group, artifact, version, problems):
    """Follows <parent> POMs until one declares <licenses>. Returns list of (name, url)."""
    coordinate = "%s:%s:%s" % (group, artifact, version)
    current = (group, artifact, version)
    for _ in range(MAX_PARENT_DEPTH + 1):
        pom_path = cache.find(*current, extensions=("pom",))
        if pom_path is None:
            problems.add("POM not in Gradle cache: %s:%s:%s (needed for %s)"
                         % (current + (coordinate,)))
            return None
        pom = read_pom(pom_path)
        licenses = [
            ((lic.findtext("name") or "").strip(), (lic.findtext("url") or "").strip())
            for lic in pom.findall("./licenses/license")
        ]
        if licenses:
            return licenses
        parent = pom.find("parent")
        if parent is None:
            break
        current = (parent.findtext("groupId").strip(), parent.findtext("artifactId").strip(),
                   parent.findtext("version").strip())
    problems.add("no <licenses> in POM or its parents: " + coordinate)
    return None


def pom_packaging(cache, group, artifact, version):
    pom_path = cache.find(group, artifact, version, extensions=("pom",))
    if pom_path is None:
        return None
    return (read_pom(pom_path).findtext("packaging") or "jar").strip()


# --- license mapping -------------------------------------------------------------

def map_license(catalog, overrides, coordinate, licenses, problems):
    """Maps POM license names onto one catalog id."""
    key = coordinate.rsplit(":", 1)[0]
    if key in overrides:
        return overrides[key]
    ids = set()
    for name, _url in licenses:
        hits = [entry["id"] for entry in catalog
                if any(re.search(p, name) for p in entry.get("pomNamePatterns", []))]
        if len(hits) != 1:
            problems.add("unknown license '%s' for %s" % (name, coordinate))
            return None
        ids.add(hits[0])
    if len(ids) != 1:
        problems.add("multiple licenses %s for %s (add an override to pick one)"
                     % (sorted(ids), coordinate))
        return None
    return ids.pop()


def license_text_from_archive(path):
    """Returns the LICENSE* text bundled in a JAR/AAR (top level or META-INF/)."""
    with zipfile.ZipFile(str(path)) as archive:
        names = sorted(n for n in archive.namelist()
                       if re.match(r"^(META-INF/)?LICENSE[^/]*$", n, re.IGNORECASE))
        if not names:
            return None
        return normalize_text(archive.read(names[0]).decode("utf-8"))


def assign_group(group_table, group_id):
    for entry in group_table:
        if group_id in entry["match"]:
            return entry
    for entry in group_table:
        for pattern in entry["match"]:
            if pattern.endswith(".*") and group_id.startswith(pattern[:-1]):
                return entry
    return None


def slug(value):
    return re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")


# --- Google third-party notices --------------------------------------------------

def google_third_party_notices(path, problems):
    """Returns {component name: text} from an AAR's third_party_licenses.{json,txt}.

    The JSON maps names to byte offsets in the TXT, where each entry is preceded by a
    "<name>:\\n\\n" header. Some AARs ship stale offsets, so an offset is only trusted
    when the header sits right before it; otherwise the header is located by search.
    """
    with zipfile.ZipFile(str(path)) as archive:
        names = set(archive.namelist())
        if "third_party_licenses.json" not in names or "third_party_licenses.txt" not in names:
            return {}
        index = json.loads(archive.read("third_party_licenses.json").decode("utf-8"))
        blob = archive.read("third_party_licenses.txt")
    result = {}
    for name in sorted(index):
        start, length = index[name]["start"], index[name]["length"]
        header = (name + ":\n\n").encode("utf-8")
        if not (blob[:start].endswith(header) and start + length <= len(blob)):
            candidates = []
            for m in re.finditer(b"(?:^|\n)" + re.escape(header), blob):
                begin = m.end()
                end = begin + length
                if end <= len(blob) and (end == len(blob) or blob[end:end + 1] == b"\n"):
                    candidates.append(begin)
            if len(candidates) != 1:
                problems.add("cannot locate '%s' in %s/third_party_licenses.txt" % (name, path.name))
                continue
            start = candidates[0]
        result[name] = normalize_text(blob[start:start + length].decode("utf-8"))
    return result


# --- main --------------------------------------------------------------------------

def dump(path, data):
    text = json.dumps(data, ensure_ascii=False, indent=1, sort_keys=False) + "\n"
    path.write_text(text, encoding="utf-8")
    return len(text.encode("utf-8"))


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--deps", required=True, help="output of ':app:dependencies "
                        "--configuration releaseRuntimeClasspath'")
    parser.add_argument("--gradle-home", default=os.environ.get("GRADLE_USER_HOME")
                        or str(Path.home() / ".gradle"))
    parser.add_argument("--out", default=str(DEFAULT_OUT_DIR))
    args = parser.parse_args()

    problems = Problems()
    config = load_json(SCRIPT_DIR / "groups.json")
    extra = load_json(SCRIPT_DIR / "extra.json")
    catalog = config["licenses"]
    catalog_by_id = {entry["id"]: entry for entry in catalog}
    cache = GradleCache(Path(args.gradle_home))

    configuration, coordinates = parse_dependency_report(args.deps, problems)
    problems.exit_if_any()

    # 1. Resolve each artifact's license (BOMs carry no code and are skipped).
    artifacts = []  # (coordinate, group id, license id, archive path)
    skipped_boms = []
    for group, artifact, version in coordinates:
        coordinate = "%s:%s:%s" % (group, artifact, version)
        if artifact.endswith("-bom") and pom_packaging(cache, group, artifact, version) == "pom":
            skipped_boms.append(coordinate)
            continue
        licenses = pom_licenses(cache, group, artifact, version, problems)
        if licenses is None:
            continue
        license_id = map_license(catalog, config["artifactLicenseOverrides"], coordinate,
                                 licenses, problems)
        if license_id is None:
            continue
        archive = cache.find(group, artifact, version, extensions=("aar", "jar"))
        artifacts.append((coordinate, group, license_id, archive))
    problems.exit_if_any()

    # 2. Licenses whose text lives in the artifact (MIT carries per-project copyright
    #    lines) get one entry per distinct text: "<id>-<artifact name>".
    licenses_out = {}
    resolved = []  # (coordinate, group id, final license id)
    artifact_text_ids = {}
    for coordinate, group, license_id, archive in artifacts:
        entry = catalog_by_id[license_id]
        final_id = license_id
        if entry.get("textFromArtifact"):
            text = license_text_from_archive(archive) if archive else None
            if not text:
                problems.add("no LICENSE file inside %s (required for %s)" % (archive, license_id))
                continue
            digest = hashlib.sha1(text.encode("utf-8")).hexdigest()
            if digest not in artifact_text_ids:
                artifact_text_ids[digest] = "%s-%s" % (license_id, coordinate.split(":")[1])
            final_id = artifact_text_ids[digest]
            licenses_out.setdefault(final_id, {"id": final_id, "name": entry["name"],
                                               "text": text, "url": None})
        elif final_id not in licenses_out:
            text = read_text_file(entry["textFile"]) if entry.get("textFile") else None
            licenses_out[final_id] = {"id": final_id, "name": entry["name"], "text": text,
                                      "url": entry.get("url")}
        resolved.append((coordinate, group, final_id))
    for entry in extra["licenses"]:
        licenses_out[entry["id"]] = {"id": entry["id"], "name": entry["name"],
                                     "text": read_text_file(entry["textFile"]) if entry.get("textFile") else None,
                                     "url": entry.get("url")}
    problems.exit_if_any()

    # 3. Group artifacts into list rows; one license per row.
    libraries = {}
    for coordinate, group, license_id in resolved:
        entry = assign_group(config["groups"], group)
        if entry is None:
            warn("group '%s' is not in groups.json; listing it under its group id" % group)
            entry = {"id": slug(group), "name": group}
        library = libraries.setdefault(entry["id"], {"id": entry["id"], "name": entry["name"],
                                                     "licenseIds": set(), "artifacts": []})
        library["licenseIds"].add(license_id)
        library["artifacts"].append(coordinate)
    for library in libraries.values():
        if len(library["licenseIds"]) != 1:
            problems.add("library '%s' mixes licenses %s; split it in groups.json"
                         % (library["id"], sorted(library["licenseIds"])))
    problems.exit_if_any()

    libraries_out = [
        {"id": lib["id"], "name": lib["name"], "licenseId": next(iter(lib["licenseIds"])),
         "artifacts": sorted(lib["artifacts"])}
        for lib in libraries.values()
    ]
    for lib in extra["libraries"]:
        libraries_out.append({"id": lib["id"], "name": lib["name"], "licenseId": lib["licenseId"],
                              "artifacts": list(lib["artifacts"])})

    ids = [lib["id"] for lib in libraries_out] + list(licenses_out)
    for value in ids:
        if "|" in value:
            problems.add("id '%s' contains '|' (reserved for route encoding)" % value)
    if len(set(lib["id"] for lib in libraries_out)) != len(libraries_out):
        problems.add("duplicate library ids")
    for lib in libraries_out:
        if lib["licenseId"] not in licenses_out:
            problems.add("library '%s' refers to unknown license '%s'" % (lib["id"], lib["licenseId"]))
    for lic in licenses_out.values():
        if not lic["text"] and not lic["url"]:
            problems.add("license '%s' has neither text nor url" % lic["id"])
    problems.exit_if_any()

    # Licenses: most libraries first, URL-only (Google terms) last; ties by id.
    library_count = {}
    for lib in libraries_out:
        library_count[lib["licenseId"]] = library_count.get(lib["licenseId"], 0) + 1
    used_licenses = [lic for lic in licenses_out.values() if lic["id"] in library_count]
    used_licenses.sort(key=lambda lic: (lic["text"] is None, -library_count[lic["id"]], lic["id"]))
    license_order = {lic["id"]: i for i, lic in enumerate(used_licenses)}
    libraries_out.sort(key=lambda lib: (license_order[lib["licenseId"]], lib["name"].casefold(), lib["id"]))

    # 4. Open-source components bundled inside Google SDK AARs.
    components = {}
    google_aars = 0
    for coordinate, group, license_id, archive in artifacts:
        if not catalog_by_id[license_id].get("googleThirdPartyNotices"):
            continue
        if archive is None:
            problems.add("AAR/JAR not in Gradle cache: " + coordinate)
            continue
        notices = google_third_party_notices(archive, problems)
        if notices:
            google_aars += 1
        for name, text in notices.items():
            current = components.get(name)
            if current is None or (len(text), text) > (len(current), current):
                components[name] = text
    problems.exit_if_any()

    texts = {}
    components_out = []
    for name in sorted(components, key=lambda n: (n.casefold(), n)):
        text = components[name]
        text_id = hashlib.sha1(text.encode("utf-8")).hexdigest()[:TEXT_ID_LENGTH]
        if texts.get(text_id, text) != text:
            problems.add("text id collision for '%s'; increase TEXT_ID_LENGTH" % name)
        texts[text_id] = text
        components_out.append({"name": name, "textId": text_id})
    problems.exit_if_any()

    out_dir = Path(args.out)
    notices_size = dump(out_dir / "oss_notices.json", {
        "schemaVersion": SCHEMA_VERSION,
        "source": configuration,
        "licenses": used_licenses,
        "libraries": libraries_out,
    })
    google_size = dump(out_dir / "oss_google_notices.json", {
        "schemaVersion": SCHEMA_VERSION,
        "texts": dict(sorted(texts.items())),
        "components": components_out,
    })

    groups_seen = len(set(group for _, group, _ in resolved))
    print("artifacts: %d (%d groups, %d BOM skipped)" % (len(resolved), groups_seen, len(skipped_boms)))
    for lic in used_licenses:
        count = sum(1 for _, _, lid in resolved if lid == lic["id"])
        print("  %-22s %3d artifacts, %2d libraries" % (lic["id"], count, library_count[lic["id"]]))
    print("libraries: %d, licenses: %d" % (len(libraries_out), len(used_licenses)))
    print("google components: %d names, %d distinct texts from %d AARs"
          % (len(components_out), len(texts), google_aars))
    print("wrote oss_notices.json (%d bytes), oss_google_notices.json (%d bytes)"
          % (notices_size, google_size))


if __name__ == "__main__":
    main()
