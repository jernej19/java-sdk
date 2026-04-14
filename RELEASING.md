# Releasing the PandaScore Java SDK

This document describes the full process for cutting a new release of `pandascore-sdk-java`. The goal is to be able to **review and test a new version before it reaches production**, so broken releases never require a version bump to fix.

---

## Overview

A release goes through three stages:

```
1. PREPARE  → Automated workflow creates a release branch, bumps the version,
              publishes a release-candidate (RC) version to GitHub Packages,
              and opens a review PR.

2. TEST     → You and your colleagues test the RC against test-repo. If issues
              are found, fix them on the release branch and re-run Prepare
              to cut a new RC. Repeat until the RC is green.

3. PUBLISH  → Once the PR is reviewed and merged, manually trigger the
              production publish workflow to release the final version.
```

Production consumers pinned to the current stable version (e.g. `1.0.0`) are **never affected by RC versions** — pre-release SemVer suffixes (`-rc.N`) are not picked up by standard Maven version resolution.

---

## Prerequisites

Before you start, make sure you have:

- **Write access** to the `jernej19/java-sdk` repository
- **Ability to run GitHub Actions workflows** on the repo
- **A GitHub personal access token** (classic) with `read:packages` scope, if you plan to pull RC versions in `test-repo` locally. Set it up in `~/.m2/settings.xml`:
  ```xml
  <settings>
    <servers>
      <server>
        <id>github-pandascore</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_PAT_WITH_read:packages</password>
      </server>
    </servers>
  </settings>
  ```
- **A version number** decided following [Semantic Versioning](https://semver.org/):
  - `MAJOR` — breaking API changes
  - `MINOR` — new backwards-compatible features
  - `PATCH` — backwards-compatible bug fixes

---

## Stage 1 — Prepare the Release

### Step 1.1: Trigger the "Prepare Release" workflow

1. Go to the repo on GitHub → **Actions** tab
2. In the left sidebar, click **Prepare Release**
3. Click the **Run workflow** dropdown (top right)
4. Make sure **Use workflow from: `main`** is selected
5. Enter the target version in the **Release version** field (e.g. `1.1.0`)
   - Must be plain semver `X.Y.Z` — do NOT add `v` prefix or suffixes; the RC suffix is added automatically
6. Click **Run workflow**

### Step 1.2: Wait for the workflow to complete

The workflow takes ~2–3 minutes and does the following:

| Step | What it does |
|---|---|
| Checkout main | Pulls the latest `main` branch |
| Validate version | Ensures the input matches `X.Y.Z` semver format |
| Create release branch | Creates `release/v1.1.0` (or checks it out if it already exists) |
| Bump version | Updates `build.gradle.kts`, `README.md`, `QUICKSTART.md` to the new version |
| Commit version bump | Commits and pushes to `release/v1.1.0` |
| Build & test | Runs `./gradlew build` (compile + unit tests) |
| Publish RC | Publishes `1.1.0-rc.<run_number>` to GitHub Packages (the committed files keep `1.1.0` — only the published artifact has the RC suffix) |
| Upload artifacts | Uploads the built JARs (main, sources, javadoc) to the workflow run for download |
| Open PR | Opens a pull request titled `Release v1.1.0` with the `release` label and testing instructions |

### Step 1.3: Find the PR and the RC version

1. Go to the **Pull requests** tab on GitHub
2. Open the PR titled `Release v1.1.0 — Ready for Testing`
3. The PR body contains the **exact RC version** to use, e.g.:
   ```
   RC published: co.pandascore:pandascore-sdk-java:1.1.0-rc.42
   ```
4. Note this version — you'll use it in Stage 2

### Step 1.4: Update the CHANGELOG (manual)

The prepare workflow does **not** touch `CHANGELOG.md` — this is intentional, since the changelog entry requires a human summary.

1. Check out the release branch locally:
   ```bash
   git fetch origin
   git checkout release/v1.1.0
   ```
2. Edit `CHANGELOG.md` to add a new section for the version:
   ```markdown
   ## [1.1.0] - 2026-04-14

   ### Added
   - New feature X
   - New feature Y

   ### Fixed
   - Bug Z

   ### Changed
   - Behavior W
   ```
3. Commit and push:
   ```bash
   git add CHANGELOG.md
   git commit -m "Update CHANGELOG for v1.1.0"
   git push origin release/v1.1.0
   ```
4. The PR updates automatically — check that the changelog entry appears in the diff.

---

## Stage 2 — Test the Release Candidate

### Step 2.1: Point test-repo at the RC version

The `test-repo/` directory is a Maven project that consumes the SDK as a dependency. It's pinned to the production version via a property — changing that property to the RC version is a one-line edit.

1. Open `test-repo/pom.xml`
2. Find the `pandascore.sdk.version` property (around line 20):
   ```xml
   <pandascore.sdk.version>1.0.0</pandascore.sdk.version>
   ```
3. Change it to the RC version from the PR body:
   ```xml
   <pandascore.sdk.version>1.1.0-rc.42</pandascore.sdk.version>
   ```
4. **Do NOT commit this change** — it's only for local testing

### Step 2.2: Run the test-repo test suite

From the repository root:

```bash
cd test-repo
mvn clean test
```

Expected result: `BUILD SUCCESS` with all tests passing.

If Maven reports authentication errors like `401 Unauthorized`, check your `~/.m2/settings.xml` has a valid GitHub PAT with `read:packages` scope (see Prerequisites above).

### Step 2.3: Run manual smoke tests (optional but recommended)

Beyond the automated test-repo suite, exercise the SDK against a real or staging AMQPS feed:

1. In `test-repo/`, write a small `main` class that:
   - Calls `SDKConfig.setOptions(...)` with your test credentials
   - Creates a `RabbitMQFeed` and connects
   - Prints a few messages
2. Run it with `mvn exec:java -Dexec.mainClass=...`
3. Verify:
   - Connection establishes
   - Messages are received and parsed
   - Disconnection and reconnection work
   - Recovery fires and replays buffered messages

### Step 2.4: Share the RC with colleagues

Colleagues can pull the same RC from GitHub Packages without any local builds:

1. Send them the RC version string (`1.1.0-rc.42`)
2. They update their own `test-repo/pom.xml` (or any consumer project) the same way
3. They run their tests and report results in the PR

---

## Stage 3 — Fix Issues (If Needed)

If testing reveals a problem, **do not cut a new version number**. Cut a new RC from the same release branch.

### Step 3.1: Fix the bug on the release branch

```bash
git checkout release/v1.1.0
# ... make changes ...
git add <files>
git commit -m "Fix <bug description>"
git push origin release/v1.1.0
```

### Step 3.2: Re-run the Prepare Release workflow

1. Go to **Actions → Prepare Release → Run workflow**
2. Enter the **same version** as before (e.g. `1.1.0`)
3. Click **Run workflow**

The workflow detects the existing `release/v1.1.0` branch, skips the version bump (since files are already at `1.1.0`), rebuilds, and publishes a new RC — e.g. `1.1.0-rc.43`. The PR body is updated automatically with the new RC coordinates.

### Step 3.3: Re-test with the new RC

Repeat Stage 2 with the new RC version. Continue until tests pass.

---

## Stage 4 — Publish to Production

Once the RC is green and the PR is reviewed:

### Step 4.1: Review and merge the PR

1. Someone other than the author reviews the PR
2. Verify:
   - Version bumps are correct in `build.gradle.kts`, `README.md`, `QUICKSTART.md`
   - `CHANGELOG.md` entry is present and accurate
   - CI checks are green
   - At least one person has ticked the "tested" box
3. Click **Merge pull request** (use "Create a merge commit" or "Squash and merge" — whichever your team prefers)

### Step 4.2: Trigger the production publish

1. Go to **Actions** tab → **Publish to GitHub Packages** (the manual workflow)
2. Click **Run workflow** → **Use workflow from: `main`** → **Run workflow**
3. Wait for the workflow to complete (~2 minutes)
4. It will:
   - Run the tests one more time as a safety net
   - Publish the final version (e.g. `1.1.0`) from the committed files in `build.gradle.kts`

### Step 4.3: Verify the production publish

1. Go to the repo → **Packages** (right sidebar on the repo home page)
2. Confirm `pandascore-sdk-java` version `1.1.0` is listed
3. Check that older versions (`1.0.0`, the previous `1.1.0-rc.*` releases) are still there — nothing was overwritten

---

## Stage 5 — Post-Release

### Step 5.1: Revert test-repo

If you edited `test-repo/pom.xml` locally to use the RC version, either:
- Revert to the previous production version (`1.0.0`), or
- Bump to the new production version (`1.1.0`) and commit

### Step 5.2: Tag the release (optional)

Create a git tag for future reference:

```bash
git checkout main
git pull
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin v1.1.0
```

### Step 5.3: Announce the release

Let downstream consumers know a new version is available:
- Post in the relevant Slack channel
- Update any integration docs that reference the SDK version

### Step 5.4: Clean up

The release branch `release/v1.1.0` can be deleted now:

```bash
git push origin --delete release/v1.1.0
```

---

## Why This Process Works

| Concern | How we address it |
|---|---|
| Broken release requires version bump to fix | RCs are disposable — cut as many as you need |
| Production users accidentally pick up pre-release code | SemVer pre-release suffixes (`-rc.N`) are excluded from standard version resolution |
| Release review happens after the fact | The PR is the review — version bump, changelog, CI, and tester approvals all happen before merge |
| Testers need to rebuild the JAR locally | RCs are published to GitHub Packages — anyone with access can pull them |
| Doc version strings go stale | The prepare workflow updates `README.md` and `QUICKSTART.md` automatically |

---

## FAQ / Troubleshooting

### Q: What if the Prepare Release workflow fails on the publish step?
Check the workflow logs in Actions. Most common causes:
- **GitHub token expired** — regenerate and update repo secrets
- **Version conflict** — GitHub Packages rejects republishing the same version; bump `github.run_number` (re-run the workflow) to cut a new RC

### Q: Can I skip the RC step and publish directly?
Yes — you can always trigger **Publish to GitHub Packages** manually on `main` to cut a release without a review PR. Reserve this for hotfixes; the PR flow is safer for normal releases.

### Q: What if two people trigger Prepare Release for the same version at the same time?
The second run will detect the existing branch and still cut a new RC with a higher `run_number`. No branches collide, but coordinate with your team to avoid confusion.

### Q: How do I roll back a bad production release?
Published packages cannot be deleted from history — the only fix is to publish a patch version (e.g. `1.1.1`) that reverts or fixes the bad change. This is why the RC testing step exists: to catch issues before a version number is burned.

### Q: What if test-repo doesn't exist for the version I'm testing?
`test-repo` lives in the same repo on `main`. If you're testing a release that modifies test-repo itself, check out the release branch and run `mvn test` from the release branch's copy.

### Q: Can I test the RC in a separate consumer project (not test-repo)?
Yes. Any Maven or Gradle project that has GitHub Packages authentication configured can pull the RC. Just add `co.pandascore:pandascore-sdk-java:1.1.0-rc.42` as a dependency.

---

## Quick Reference

| Action | Where | Command/Click |
|---|---|---|
| Start a release | GitHub Actions | `Prepare Release` → Run with version `X.Y.Z` |
| Test an RC | Local / test-repo | Edit `pandascore.sdk.version` in `test-repo/pom.xml`, run `mvn test` |
| Cut a new RC | GitHub Actions | `Prepare Release` → Run with same version (after pushing fixes to release branch) |
| Update changelog | Local | Edit `CHANGELOG.md` on the release branch, commit, push |
| Merge the release | GitHub PR | Review + merge the `Release vX.Y.Z` PR |
| Publish to production | GitHub Actions | `Publish to GitHub Packages` → Run |
| Tag the release | Local | `git tag -a vX.Y.Z && git push origin vX.Y.Z` |
