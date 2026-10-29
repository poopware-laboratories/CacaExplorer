# CacaExplorer

A native file manager for **Android 3.2.1 Honeycomb / API 13**, designed for the Motorola Xoom. The layout takes cues from Dolphin and Finder: a Places sidebar, large file icons, detailed list view, tabs, and clickable breadcrumbs. Written in Java against the actual Honeycomb SDK, with no runtime libraries, network permission, or root requirement.

## Using it

- Tap a folder or file to open it. Hold a file for its action menu.
- Use **Icon view / List view** to switch layouts. **Location** accepts a full path.
- **Select** starts multiple selection with checkboxes and a visible action bar. Tap items, then **Copy** or **Move**, open the destination, and press **Paste**. **Select all** selects every visible item; **Done** exits selection. Tap Select again for delete, ZIP, and sharing.
- Copy or Move, browse to another folder or tab, and press **Paste** in the top toolbar. A clipboard banner identifies the pending files and destination. Tap the banner to clear it. The clipboard survives theme/view switches and restarts. Copies can be pasted again; successful moves clear the clipboard. Existing destinations are refused.
- **+** adds a tab; hold a tab to close it. Up to 12 folder tabs restore across launches.
- **Menu** contains sort order, hidden files, bookmarks, themes, storage information, and refresh.
- **Search** searches subfolders; Back exits search. The filter field normally searches the current folder only.
- **Recent files** scans shared storage and sorts files by modification time.
- File actions include a selectable UTF-8 text preview, ZIP creation/extraction, properties, and sharing through installed apps. APK files open Android's package installer.

## Themes

All 13 built-in palettes were imported from the current [Poopterm theme source](https://github.com/poopware-laboratories/poopterm/blob/main/src/theme.rs): macOS Dark/Light, Nord, Rosé Pine/Moon/Dawn, Catppuccin Latte/Frappé/Macchiato/Mocha, Bark, AMOLED Black, and Poop. The generated source records its input SHA-256. Foreground, background, border, and all eight palette colors are retained. The UI uses the blue palette color for accents, falling back to cyan or foreground when needed for readable contrast.

Graphite (the default), Dolphin light, and a custom four-color theme are also included. Custom colors are background, foreground, panel, and accent in `#RRGGBB` format, comma-separated. Themes change the interface without changing the file layout.

To refresh the imported palette data:

```sh
python3 tools/import-themes.py /path/to/poopterm/src/theme.rs
```

## Build and test

From the root of this standalone repository:

```sh
sh setup-sdk.sh
sh test.sh
sh build.sh
adb install -r dist/CacaExplorer-26.10.0.apk
```

Requires JDK 21, Python 3, curl, unzip, and zip. `setup-sdk.sh` downloads pinned official Google archives (API 13 stubs, build tools 30.0.3, and R8/D8 8.3.37), verifies their SHA-256 hashes, and keeps them in ignored `.work/honeycomb-sdk`. No Gradle or preinstalled Android SDK is required. Supported hosts: Linux x86_64 and macOS; Apple Silicon requires Rosetta for the native SDK tools.

The scripts honor `JAVA_HOME` or `CACA_JAVA_HOME`; Homebrew JDK 21 is also detected. Set `CACA_SDK_ROOT`, `ANDROID_JAR`, `ANDROID_BUILD_TOOLS`, or `CACA_R8_JAR` to override individual SDK locations. Build output goes to ignored `dist/` inside this repository, alongside its SHA-256 checksum. The filename follows `versionName` in AndroidManifest.xml.

[GitHub setup instructions](docs/GITHUB_SETUP.md) cover repository creation, signing, and running the included APK workflow. The workflow builds on pushes, pull requests, and manual dispatches and uploads an APK artifact. Version tags or manual runs with a release tag publish the APK and checksum to GitHub Releases.

The APK is signed with a local development key stored in the ignored `.private/debug.keystore` directory. Preserve that key for compatible updates. Development signing is verified for API 13 with the legacy JAR/v1 scheme. No release key or credentials are committed.

## Validation and limits

The APK has installed and launched on the connected Android 3.2.1 Xoom; its screen was captured for layout review. The USB connection is intermittent, so check the final install command's result. Automated host tests exercise binary and recursive copying, empty folders, destination conflicts, moving, invalid names, symlink refusal, ZIP round trips, malformed/truncated archives, unsafe archive paths, and failed-extraction cleanup. Compilation uses the API 13 boot classpath so newer Android/Java APIs cannot slip in accidentally.

This is an initial alpha, with a substantial feature set rather than a claim of maturity equal to Poopterm. Remaining work includes broader on-device operation tests, thumbnails, split panes, undo/trash, operation cancellation, archive browsing, keyboard shortcuts, and accessibility review. File operations show a modal busy indicator and run off the main thread. Transfers are not crash-transactional; a batch can partially complete if a later item fails, and cross-filesystem moves can leave both copies if source removal fails. Errors report these cases. Deletion is permanent.

Recursive search/recents are bounded to 20,000 visited directories and 1,000 results; recent results are sorted within the collected set. Text previews stop at 256 KiB. ZIP extraction is limited to 1 GiB and 20,000 entries and refuses path traversal, duplicates, and existing extraction folders. ZIPs do not preserve Unix permissions or symlinks. Symbolic links are not supported by destructive or archive operations. Normal Android application permissions apply; protected system folders may be readable but cannot be changed.

AI-assisted implementation, directed by the owner of Poopterm. CacaExplorer is licensed under AGPL-3.0-or-later, matching Poopterm; see LICENSE. This subproject is separate from the NuraXoom boot image.

## Versioning

Like Poopterm: **YEAR.MONTH.PATCH**, such as `26.10.0`, with a random monthly codename. Patches increment within the month and reset to 0 next month. CacaExplorer keeps its own counter. `version.json` holds the version and codename; `python3 tools/version.py --next` prepares the next release and synchronizes the Android manifest. About, APK filenames, and release titles use this version. The initial monthly series is **26.10 — Lunar Gecko**.
