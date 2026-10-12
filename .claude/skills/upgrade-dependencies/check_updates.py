"""Report what the repo and dev server could upgrade to. Read-only: it changes
no files and downloads no jars.

Run from the repo root: python .claude/skills/upgrade-dependencies/check_updates.py [line]

Picks the newest Paper line that is usable end to end — Fill has a STABLE
build, paper-api has a -stable coordinate, and MockBukkit publishes an artifact
for it — unless a line (e.g. 26.2) is given. Then reports the server jar, the
paper-api coordinate, the MockBukkit artifact, each third-party plugin's newest
release for that line, the Maven plugins and dependencies pinned in the poms,
and the GitHub Actions used by CI. Only stable releases are ever proposed.
"""
import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

# Fill and Modrinth both ask for a User-Agent that identifies the project.
USER_AGENT = "mrsperry/mc-plugins-upgrade (github.com/mrsperry/mc-plugins)"

FILL = "https://fill.papermc.io/v3/projects/paper"
PAPER_API_METADATA = ("https://repo.papermc.io/repository/maven-public/"
                      "io/papermc/paper/paper-api/maven-metadata.xml")
MOCKBUKKIT_METADATA = ("https://repo.maven.apache.org/maven2/org/mockbukkit/mockbukkit/"
                       "mockbukkit-v{}/maven-metadata.xml")
MODRINTH = "https://api.modrinth.com/v2/project/{}/version"
MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/{}/{}/maven-metadata.xml"
GITHUB_RELEASE = "https://api.github.com/repos/{}/releases/latest"

# Resolved by the Paper line above, or built here, so not checked as plain dependencies.
LINE_MANAGED = ("paper-api", "mockbukkit-v", "mc-utils")

# Third-party jars the dev server runs, by Modrinth slug. Spark is bundled into
# Paper, so it isn't here. Add a row when a new third-party plugin goes on the server.
PLUGINS = {
    "FastAsyncWorldEdit": "fastasyncworldedit",
    "FastAsyncVoxelSniper": "fastasyncvoxelsniper",
}

STABLE_RELEASE = re.compile(r"^\d+(\.\d+)*$")


def get(url, params=None):
    if params:
        url += "?" + urllib.parse.urlencode(params)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def exists(url):
    try:
        get(url)
        return True
    except urllib.error.HTTPError as ex:
        if ex.code == 404:
            return False
        raise


def version_key(version):
    return [int(part) for part in re.findall(r"\d+", version)]


def stable_builds(version):
    # /builds/latest ignores the channel, so always filter explicitly.
    return json.loads(get(f"{FILL}/versions/{version}/builds", {"channel": "STABLE"}))


def paper_api_stable(version, metadata):
    pattern = re.escape(version) + r"\.build\.(\d+)-stable"
    builds = sorted({int(n) for n in re.findall(pattern, metadata)})
    return f"{version}.build.{builds[-1]}-stable" if builds else None


def mockbukkit_line(family, version):
    # The artifactId is inconsistent across lines: v26.1.2 carries the patch, v26.2 doesn't.
    for candidate in dict.fromkeys([version, family]):
        if exists(MOCKBUKKIT_METADATA.format(candidate)):
            metadata = get(MOCKBUKKIT_METADATA.format(candidate))
            releases = [v for v in re.findall(r"<version>([^<]+)</version>", metadata)
                        if STABLE_RELEASE.match(v)]
            return f"mockbukkit-v{candidate}", max(releases, key=version_key)
    return None, None


def resolve_line(families, metadata, wanted):
    """Newest (family, version) that has a stable server, paper-api and MockBukkit."""
    candidates = []
    for family, versions in families.items():
        if wanted and family != wanted and wanted not in versions:
            continue
        for version in versions:  # newest first; skips rc/pre ids
            if not STABLE_RELEASE.match(version) or (wanted and wanted not in (family, version)):
                continue
            candidates.append((family, version))
            break
    candidates.sort(key=lambda pair: version_key(pair[1]), reverse=True)

    skipped = []
    for family, version in candidates:
        builds = stable_builds(version)
        api = paper_api_stable(version, metadata)
        artifact, mockbukkit = mockbukkit_line(family, version)
        missing = [name for name, ok in
                   (("STABLE server build", builds), ("-stable paper-api", api),
                    ("MockBukkit artifact", artifact)) if not ok]
        if not missing:
            return family, version, builds[0], api, artifact, mockbukkit, skipped
        skipped.append(f"{version}: no {', no '.join(missing)}")
    return None, None, None, None, None, None, skipped


def current_state():
    pom = Path("pom.xml").read_text(encoding="utf-8")
    prop = lambda name: (re.search(rf"<{name}>([^<]+)</{name}>", pom) or [None, "?"])[1]
    artifact = (re.search(r"<artifactId>(mockbukkit-v[^<]+)</artifactId>", pom) or [None, "?"])[1]
    print("Current")
    print(f"  paper.version       {prop('paper.version')}")
    print(f"  mockbukkit          {artifact}:{prop('mockbukkit.version')}")
    history = Path("_server/version_history.json")
    if history.exists():
        print(f"  dev server runs     {json.loads(history.read_text())['currentVersion']}")
    plugins = Path("_server/plugins")
    if plugins.exists():
        for jar in sorted(plugins.glob("*.jar")):
            if any(jar.name.lower().startswith(name.lower()) for name in PLUGINS):
                print(f"  {jar.name}")


def latest_release(group, artifact):
    metadata = get(MAVEN_CENTRAL.format(group.replace(".", "/"), artifact))
    releases = [v for v in re.findall(r"<version>([^<]+)</version>", metadata)
                if STABLE_RELEASE.match(v)]
    return max(releases, key=version_key)


def pinned_artifacts():
    """(file, groupId, artifactId, version) for every plugin and dependency
    that pins a version, with ${property} references resolved."""
    poms = [Path("pom.xml")] + sorted(p for p in Path(".").glob("*/pom.xml"))
    properties = {}
    for pom in poms:
        block = re.search(r"<properties>(.*?)</properties>", pom.read_text(encoding="utf-8"), re.S)
        if block:
            properties.update(re.findall(r"<([\w.-]+)>([^<]*)</\1>", block.group(1)))

    found = {}
    for pom in poms:
        text = re.sub(r"<!--.*?-->", "", pom.read_text(encoding="utf-8"), flags=re.S)
        for kind, body in re.findall(r"<(plugin|dependency)>(.*?)</\1>", text, re.S):
            tags = dict(re.findall(r"<(groupId|artifactId|version)>([^<]+)</\1>", body))
            if "artifactId" not in tags or "version" not in tags:
                continue
            group = tags.get("groupId", "org.apache.maven.plugins" if kind == "plugin" else None)
            version = re.sub(r"\$\{([^}]+)\}", lambda m: properties.get(m.group(1), m.group(0)),
                             tags["version"])
            if group and not tags["artifactId"].startswith(LINE_MANAGED):
                found.setdefault((group, tags["artifactId"], version), pom.as_posix())
    return [(pom, *key) for key, pom in found.items()]


def report_build_tooling():
    print("\nMaven plugins and dependencies")
    for pom, group, artifact, version in sorted(pinned_artifacts(), key=lambda row: row[2]):
        try:
            latest = latest_release(group, artifact)
        except urllib.error.HTTPError:
            latest = "?"
        mark = "" if latest in (version, "?") else f"  -> {latest}"
        print(f"  {artifact:<26} {version:<10}{mark}  ({pom})")

    print("\nGitHub Actions")
    for workflow in sorted(Path(".github/workflows").glob("*.yml")):
        for action, tag in re.findall(r"uses:\s*([\w.-]+/[\w.-]+)@(\S+)",
                                      workflow.read_text(encoding="utf-8")):
            try:
                latest = json.loads(get(GITHUB_RELEASE.format(action)))["tag_name"]
            except urllib.error.HTTPError:
                latest = "?"
            major = latest.split(".")[0]
            mark = "" if major in (tag, "?") else f"  -> {major} ({latest})"
            print(f"  {action:<26} {tag:<10}{mark}  ({workflow.as_posix()})")


def main():
    wanted = sys.argv[1] if len(sys.argv) > 1 else None
    current_state()

    families = json.loads(get(FILL))["versions"]
    support = {entry["version"]["id"]: entry["version"]
               for entry in json.loads(get(f"{FILL}/versions"))["versions"]}
    metadata = get(PAPER_API_METADATA)

    family, version, build, api, artifact, mockbukkit, skipped = resolve_line(families, metadata, wanted)
    print()
    for line in skipped:
        print(f"Skipped {line}")
    if not version:
        print("No usable Paper line found.")
        sys.exit(1)

    download = build["downloads"]["server:default"]
    info = support.get(version, {})
    print(f"Target line {version}  (support: {info.get('support', {}).get('status', '?')}, "
          f"Java {info.get('java', {}).get('version', {}).get('minimum', '?')}+)")
    print(f"  server jar          build {build['id']}  {download['name']}")
    print(f"                      {download['url']}")
    print(f"                      sha256 {download['checksums']['sha256']}")
    print(f"  paper.version       {api}")
    print(f"  mockbukkit          {artifact}:{mockbukkit}")

    for name, slug in PLUGINS.items():
        releases = json.loads(get(MODRINTH.format(slug), {
            "loaders": '["paper"]', "game_versions": json.dumps([version]),
            "include_changelog": "false"}))
        # Some snapshots are mis-tagged as releases, so check the version number too.
        releases = [r for r in releases if r["version_type"] == "release"
                    and "snapshot" not in r["version_number"].lower()]
        if not releases:
            print(f"  {name:<19} no release for {version}")
            continue
        latest = max(releases, key=lambda r: r["date_published"])
        jar = next(f for f in latest["files"] if f["primary"])
        print(f"  {name:<19} {latest['version_number']}  {jar['filename']}")
        print(f"                      {jar['url']}")
        print(f"                      sha512 {jar['hashes']['sha512']}")

    java = (re.search(r"<maven.compiler.release>(\d+)|<maven.compiler.source>(\d+)",
                      Path("pom.xml").read_text(encoding="utf-8")) or [None])
    print(f"  java                compiles for {next((g for g in java.groups() if g), '?')}, "
          f"line needs {info.get('java', {}).get('version', {}).get('minimum', '?')}+")

    report_build_tooling()


if __name__ == "__main__":
    main()
