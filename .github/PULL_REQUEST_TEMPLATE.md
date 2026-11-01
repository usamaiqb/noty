<!--
PR title must follow Conventional Commits, e.g.
  feat: add widget support
  fix(notifications): handle null note on restart
  docs: update build steps
This title becomes the squashed commit on main and feeds the release notes.
-->

## Description

<!-- What does this PR change, and why? -->

Closes #

## Checklist

- [ ] PR title follows [Conventional Commits](https://www.conventionalcommits.org)
- [ ] `./gradlew test` and `./gradlew lint` pass locally
- [ ] UI changes include screenshots / screen recordings (if applicable)

### Release PRs only

- [ ] `versionCode` bumped by exactly 1 **and** `versionName` updated in `app/build.gradle`
- [ ] Branch is named `release/v<versionName>`
- [ ] `CHANGELOG.md` has a `## [<versionName>]` entry
- [ ] `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` added (F-Droid "What's New")
- [ ] README translation table is up to date (merge the open `docs: update translation table` PR, or run `bash scripts/translation-status.sh`)
