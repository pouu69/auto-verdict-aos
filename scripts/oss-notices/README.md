# Open-source notices

Regenerate `app/src/main/res/raw/oss_notices.json` and `oss_google_notices.json` whenever dependencies change (offline, Python stdlib only):

1. `./gradlew --offline -q :app:dependencies --configuration releaseRuntimeClasspath > /tmp/deps.txt`
2. `python3 -I scripts/oss-notices/generate.py --deps /tmp/deps.txt`
3. Commit both JSON files. On failure (unknown license, POM missing from the Gradle cache, mixed licenses in one group) fix `groups.json` (`groups` / `artifactLicenseOverrides`) or build once so Gradle caches the POM, then re-run. Non-Maven assets (fonts) live in `extra.json` with their texts in `texts/`.

Design: `docs/superpowers/specs/2026-10-18-ux-revamp-4-settings-cleanup-design.md` §7.
