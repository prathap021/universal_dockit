# Universal Dockit 🗂️

[![pub package](https://img.shields.io/pub/v/universal_dockit.svg)](https://pub.dev/packages/universal_dockit)
[![License: MIT](https://img.shields.io/badge/license-MIT-purple.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/platform-android%20%7C%20ios-blue.svg)](https://flutter.dev/)

> ⚠️ **Notice:** This plugin is currently under **Beta version**. APIs, UI, and rendering behavior may change as we continue to improve support for various document types.

A Flutter plugin for viewing documents natively on **Android** and **iOS**.

**Universal Dockit** keeps the Flutter layer minimal: you pass a file path (and optional settings), and the plugin opens a native viewer screen. Rendering is **100% on-device** — no cloud APIs and no commercial SDKs.

---

## ✨ Features

* **15 document formats** — PDF, Word, Excel, PowerPoint, EPUB, CBZ, TXT, CSV, RTF, and OpenDocument (ODT/ODS/ODP).
* **Native renderers per platform** — PdfiumAndroid / PDFKit, Apache POI / QuickLook, CoreXLSX, ZIP parsers, and more.
* **Android Office pipeline** — Word and Excel parsed with [Apache POI](https://poi.apache.org/) and rendered as HTML in a WebView; PowerPoint is converted to PDF and shown in the PDF viewer.
* **Configurable viewer features** — `search`, `zoomInOut`, and `darkMode` passed from Flutter via method channel and applied on both platforms.
* **Fully offline** — all parsing and display happens on the device.

---

## 📄 Supported Formats & Native Renderers

| Format | Extension | Android | iOS |
| :--- | :--- | :--- | :--- |
| **PDF** | `.pdf` | [PdfiumAndroid](https://github.com/barteksc/AndroidPdfViewer) | **PDFKit** |
| **Word** | `.doc`, `.docx` | Apache POI (HWPF/XWPF) → HTML → WebView | **QuickLook** |
| **Excel** | `.xls`, `.xlsx` | Apache POI (HSSF/XSSF) → styled HTML table → WebView | [CoreXLSX](https://github.com/CoreOffice/CoreXLSX) → HTML → WebView (`.xlsx`); **QuickLook** (`.xls`) |
| **PowerPoint** | `.ppt`, `.pptx` | Convert to PDF → PdfiumAndroid PDFView | **QuickLook** |
| **EPUB** | `.epub` | ZIP + spine/HTML → WebView | [ZIPFoundation](https://github.com/weichsel/ZIPFoundation) → WebView |
| **CBZ** | `.cbz` | ZIP + images → WebView | ZIPFoundation → WebView |
| **Text** | `.txt` | `TextView` (monospace) | `UITextView` |
| **CSV** | `.csv` | RFC-4180 → HTML table → WebView | Aligned plain-text → `UITextView` |
| **RTF** | `.rtf` | `Html.fromHtml` → `TextView` | `NSAttributedString` → `UITextView` |
| **OpenDocument** | `.odt`, `.ods`, `.odp` | In-house ZIP/XML → HTML → WebView | **QuickLook** |

### Android Office notes

* **`.docx` / `.xlsx` / `.pptx`** — full POI parse with layout-oriented HTML output (tables, merged cells, slide anchors, paragraph styles where supported).
* **`.doc` / `.ppt`** — limited compatibility mode (text-oriented fallback); a banner is shown when fidelity is reduced.
* **SpreadsheetML XML** (`.xls` saved as XML) — fallback parser when POI `WorkbookFactory` cannot open the file.
* **Strict / edge DOCX** — XML zip fallback extracts paragraph text when OOXML parsing fails.

---

## 🛠 Setup & Installation

**Requirements**

| | Minimum |
| :--- | :--- |
| Flutter | `>=3.0.0` |
| Dart | `>=3.0.0 <4.0.0` |
| Android `minSdk` | **26** |
| iOS deployment target | **13.0** |

Add the package:

```yaml
dependencies:
  universal_dockit: ^1.0.1
```

### 🤖 Android

1. Set `minSdk` to **26** in `android/app/build.gradle` (or `build.gradle.kts`).
2. Ensure `google()` and `mavenCentral()` are in your project repositories.
3. No extra Activity or third-party document SDK is required — the plugin ships its own `DocumentViewerActivity` and POI-based parsers.

**Optional:** The plugin resolves `content://` URIs by copying to a cache file before opening. For best results, pass a readable local file path when possible.

### 🍎 iOS

1. Set the deployment target to at least **13.0** in `ios/Podfile`:

   ```ruby
   platform :ios, '13.0'
   ```

2. Install pods:

   ```bash
   cd ios && pod install
   ```

No additional `Info.plist` keys are required for the plugin's built-in viewers. If you pick files from iCloud or cloud providers in your app, copy them to a local path (or bytes → temp file) before calling `openDocument`.

---

## 🚀 Usage

```dart
import 'package:universal_dockit/universal_dockit.dart';

final dockit = UniversalDockit();

// Type is inferred from the file extension.
await dockit.openDocument('/path/to/report.pdf');

// Or pass the type explicitly (e.g. no extension).
await dockit.openDocument(
  '/path/to/file_without_extension',
  docType: DocType.docx,
);
```

Supported `DocType` values:  
`pdf`, `doc`, `docx`, `xls`, `xlsx`, `ppt`, `pptx`, `epub`, `cbz`, `txt`, `csv`, `rtf`, `odt`, `ods`, `odp`.

### Document features

`DocumentFeatures` exposes **three** viewer options. They are sent over the method channel as `features` and interpreted by native code on **Android and iOS**.

```dart
await dockit.openDocument(
  filePath,
  features: const DocumentFeatures(
    search: true,      // default: true
    zoomInOut: true,   // default: true
    darkMode: false,   // default: false — initial dark theme
  ),
);
```

| Flag | Default | Android | iOS |
| :--- | :--- | :--- | :--- |
| `search` | `true` | Shows search control in the document app bar; highlights matches in WebView / plain text. | Applied on custom viewers (PDF, XLSX, TXT, CSV, RTF, EPUB, CBZ) via native feature configuration. QuickLook uses system preview (no custom search bar). |
| `zoomInOut` | `true` | Shows zoom in/out in the app bar; WebView zoom + text size for text documents. | Enables pinch zoom on WebView-based viewers; adjusts PDF zoom behavior on `PDFViewerViewController`. QuickLook uses system gestures. |
| `darkMode` | `false` | Dark theme for viewer UI and HTML injection for WebView content; shows dark-mode toggle when enabled. | Sets dark interface style on the presented navigation / QuickLook controller. |

Set a flag to `false` to disable that capability for the opened document session.

---

## 🏗 Architecture

**Flutter**

* `UniversalDockit.openDocument()` → `MethodChannel('universal_dockit')` with `filePath`, `docType`, and `features`.

**Android**

* `UniversalDockitPlugin` → `DocumentViewerActivity` (host).
* Per-format `DocumentRenderer` implementations (`WordDocumentRenderer`, `ExcelDocumentRenderer`, `PowerPointDocumentRenderer`, `PdfDocumentRenderer`, etc.).
* Office files: Apache POI → structured models → HTML → WebView.
* `RenderCallbacks` bridges parsing/rendering back to the Activity (WebView, PDFView, TextView).

**iOS**

* `UniversalDockitPlugin` routes by `docType` to dedicated view controllers (`PDFViewerViewController`, `XLSXViewerViewController`, `QuickLookViewerViewController`, etc.).
* `DockitFeatures` / `DockitFeatureConfigurable` apply `search`, `zoomInOut`, and `darkMode` on supported custom viewers.

---

## 📦 Example app

The bundled example (`example/`) demonstrates:

* Picking a file with `file_picker`
* Resolving iOS cloud paths via `path_provider` temp copy when needed
* Toggling `DocumentFeatures` before `openDocument`

Run from the example directory:

```bash
flutter run
```

---

## 🤝 Contributing

### Report a bug

[Open an issue](https://github.com/prathap021/universal_dockit/issues) with:

* Steps to reproduce
* Document format and sample file characteristics (if possible)
* Flutter version and platform (Android / iOS)

### Pull requests

1. Fork the repo
2. Create a feature branch
3. Commit with a clear message
4. Open a PR

---

## 📝 License

MIT — see [LICENSE](LICENSE).

**Open-source libraries used internally**

* [PdfiumAndroid / android-pdf-viewer](https://github.com/barteksc/AndroidPdfViewer) (Apache 2.0)
* [Apache POI](https://poi.apache.org/) (Apache 2.0)
* [CoreXLSX](https://github.com/CoreOffice/CoreXLSX) (Apache 2.0)
* [ZIPFoundation](https://github.com/weichsel/ZIPFoundation) (MIT)
