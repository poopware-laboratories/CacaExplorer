# GitHub setup

This directory is the standalone source repository for [poopware-laboratories/CacaExplorer](https://github.com/poopware-laboratories/CacaExplorer). The first hosted APK build has passed.

## Create and push

Run from this directory after signing into the GitHub CLI:

```sh
gh auth login
gh repo create OWNER/cacaexplorer --private --source=. --remote=origin
```

Use `--public` instead if you want the code public. Before the first push, set the signing secret so CI APKs can update the development APK already installed on the Xoom:

```sh
python3 -c 'import base64,pathlib,sys; sys.stdout.buffer.write(base64.b64encode(pathlib.Path(".private/debug.keystore").read_bytes()))' | gh secret set CACA_DEBUG_KEYSTORE_BASE64 --repo OWNER/cacaexplorer
git push -u origin main
```

The key remains local and in GitHub's encrypted secret store. It is a development key using the standard `android` password, not a production release key. Do not commit it. A private repository is suggested for initial setup, but AGPL also permits publishing it publicly.

## Build and download

The **Build APK** workflow runs for pushes to main and codex branches, v-prefixed tags, pull requests, and manual dispatches. Open **Actions → Build APK → a successful run → Artifacts** to download the ZIP containing the APK and its SHA-256 checksum. Normal builds remain Actions artifacts. Version-tag builds and manual runs with a release tag also publish the APK and checksum to GitHub Releases.

After the first push, a manual build can also be started with:

```sh
gh workflow run apk.yml --repo OWNER/cacaexplorer
```

Each workflow run receives an increasing Android version code and a `-ci.RUN_NUMBER` version suffix. Retries of the same run keep that version. Install APKs in increasing run order. Keep the workflow's run numbering consistent when migrating repositories; Android refuses downgrades. Local builds retain the manifest's base version code.

Pull-request builds intentionally use temporary signing keys and have no access to the repository signing secret. Without the secret, other CI builds also use temporary keys and emit a warning. Those APKs are useful for testing but cannot replace an installation signed by a different key. Configure the stable key rather than uninstalling an app whose settings you want to retain.

The workflow uses an Ubuntu 24.04 x86_64 runner, Temurin JDK 21, API 13 stubs, build tools 30.0.3, and R8/D8 8.3.37. It runs all host tests, checks downloads against pinned SHA-256 hashes, compiles only against Honeycomb APIs, verifies legacy v1 APK signing for API 13, and uploads the result for 30 days. Actions are pinned to commit hashes and the job has read-only repository permissions.

## Publish downloadable APK releases

Open **Actions → Build APK → Run workflow**, select `main`, and enter `v0.1.1` in **release_tag**. After tests and signing verification pass, the workflow creates that tag at the built commit and publishes a GitHub Release with the APK and checksum as direct downloads. Leave release_tag empty for an ordinary test build.

Alternatively, push a version tag:

```sh
git tag v0.1.2
git push origin v0.1.2
```

Tags must use `vMAJOR.MINOR.PATCH`, optionally followed by `-alpha.N`, `-beta.N`, or `-rc.N`. Such suffixes create GitHub prereleases. The APK version name matches the tag without `v`, while version codes keep increasing using this workflow's shared run number. Set the stable signing secret described above before distributing APKs intended to update one another; otherwise each fresh runner generates a temporary key.

Release creation runs in a separate job with repository write permission, after the read-only build job succeeds. Reruns leave existing releases unchanged. Manual runs refuse to publish if the requested tag already points to a different commit. Releases in a private repository remain accessible only to people who can access that repository. This workflow does not publish to an app store.
