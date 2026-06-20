# Universal Dockit 🗂️

[![pub package](https://img.shields.io/pub/v/universal_dockit.svg)](https://pub.dev/packages/universal_dockit)
[![License: MIT](https://img.shields.io/badge/license-MIT-purple.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/platform-android%20%7C%20ios-blue.svg)](https://flutter.dev/)

> ⚠️ **Notice:** This plugin is currently under **Beta version**. APIs, UI, and rendering behavior may change as we continue to improve support for various document types.


**Universal Dockit** leverages the best open-source native SDKs and built-in OS frameworks to render documents quickly and beautifully, keeping the Flutter layer minimal and efficient. 
---

## ✨ Features

* **Universal Format Support**: Open 15 different document types including PDF, Word, Excel, PowerPoint, EPUB, CBZ, Text, CSV, RTF, and OpenDocument formats.
* **Native Performance**: Documents are rendered natively using optimized libraries like `PdfiumAndroid` on Android and `PDFKit`/`QuickLook` on iOS.
* **Native Office Parsing**: Word, Excel, and PowerPoint files are parsed on-device with Apache POI and rendered into native WebView-friendly HTML.
* **Smart Rendering**: Formula evaluation in spreadsheets, merged-cell support, Office compatibility fallbacks, native zooming for documents, and HTML structural extraction for E-Books/Comic Books.
* **Feature-Gated Viewer Controls**: Android viewer actions are controlled only via method-channel feature flags (Search, Zoom, Dark Mode toggle).
* **Fully Offline**: Everything is parsed and rendered entirely on the device without cloud APIs or heavy monolithic frameworks.

---

## 📄 Supported Formats & Native Renderers

The plugin intelligently routes each file type to the most appropriate native renderer:

| Format | Extension | Android Engine | iOS Engine |
| :--- | :--- | :--- | :--- |
| **PDF** | `.pdf` | [PdfiumAndroid](https://github.com/barteksc/AndroidPdfViewer) (Hardware accelerated) | **PDFKit** (Built-in) |
| **Word (doc, docx)** | `.doc`, `.docx` | [Apache POI](https://poi.apache.org/) (HWPF/XWPF) → HTML → WebView | **QuickLook** (Built-in) |
| **Excel (xls, xlsx)** | `.xls`, `.xlsx` | [Apache POI](https://poi.apache.org/) (HSSF/XSSF) → Styled HTML Table → WebView | [CoreXLSX](https://github.com/CoreOffice/CoreXLSX) → HTML Table → WebView |
| **PowerPoint (ppt, pptx)**| `.ppt`, `.pptx` | [Apache POI](https://poi.apache.org/) (HSLF/XSLF) → HTML Slide Layout → WebView | **QuickLook** (Built-in) |
| **EPUB E-Book** | `.epub` | Native ZIP → spine/HTML extraction → WebView | [ZIPFoundation](https://github.com/weichsel/ZIPFoundation) → spine/HTML → WebView |
| **CBZ Comic Book** | `.cbz` | Native ZIP → Image extraction → WebView | [ZIPFoundation](https://github.com/weichsel/ZIPFoundation) → Image extraction → WebView |
| **Text** | `.txt` | Native `TextView` (Monospace, memory-efficient) | Native `UITextView` (Monospace) |
| **CSV** | `.csv` | RFC-4180 Parser → HTML Table → WebView | Aligned Plain-text Table → `UITextView` |
| **Rich Text** | `.rtf` | Native HTML Parser → `Html.fromHtml` → `TextView` | `NSAttributedString` → `UITextView` |
| **OpenDocument**| `.odt`, `.ods`, `.odp`| In-house ZIP/XML Parser → HTML → WebView| **QuickLook** (Built-in) |

---

## 🛠 Setup & Installation

**Requirements:**
* **Flutter SDK**: `>=3.10.0`
* **Dart SDK**: `>=3.0.0 <4.0.0`

Add `universal_dockit` to your `pubspec.yaml`:

```yaml
dependencies:
  universal_dockit: ^1.0.1
```

### 🤖 Android Setup

1. **Minimum SDK**: Ensure your `minSdkVersion` is at least **26** in `android/app/build.gradle`.
2. **Repositories**: Keep `google()` and `mavenCentral()` available in your Android project repositories.
3. **No external activity dependency required**: Office rendering is handled directly inside the plugin via native parsers.

### 🍎 iOS Setup

1. **Minimum iOS Version**: Ensure your iOS deployment target is at least **iOS 13.4** in your `ios/Podfile`:
   ```ruby
   platform :ios, '13.4'
   ```
2. **Install Pods**: Run the following from your `ios` directory:
   ```bash
   pod install
   ```

*(Note: The iOS implementation uses pure Swift libraries and built-in Apple frameworks like QuickLook and PDFKit).*

---

## 🚀 Usage

Using the plugin is incredibly simple. Just provide the absolute file path to the document.

```dart
import 'package:flutter/material.dart';
import 'package:universal_dockit/universal_dockit.dart';

// ... inside your widget class

final _universalDockitPlugin = UniversalDockit();

Future<void> openMyDocument(String filePath) async {
  try {
    // The plugin will automatically infer the document type from the extension.
    final success = await _universalDockitPlugin.openDocument(filePath);
    
    if (!success) {
      debugPrint("Could not open the document.");
    }
  } catch (e) {
    debugPrint("Error opening document: $e");
  }
}

// Alternatively, you can explicitly provide the document type:
// await _universalDockitPlugin.openDocument(filePath, docType: DocType.pdf);
```

### Explicit Document Types

If your file doesn't have an extension, or you want to force a specific renderer, you can pass the `DocType` enum explicitly:

```dart
await _universalDockitPlugin.openDocument(
  '/path/to/file_without_extension', 
  docType: DocType.docx,
);
```

Supported `DocType` values: `pdf`, `doc`, `docx`, `xls`, `xlsx`, `ppt`, `pptx`, `epub`, `cbz`, `txt`, `csv`, `rtf`, `odt`, `ods`, `odp`.

### Configuring Document Features

Viewer controls are feature-gated on Android. A control appears only when the corresponding `DocumentFeatures` flag is enabled from Flutter.

```dart
await _universalDockitPlugin.openDocument(
  filePath,
  features: const DocumentFeatures(
    darkMode: true, // Initial theme state
    zoomInOut: true,
    search: true,
    darkModeToggle: true, // Show dark mode toggle action in Android app bar
  ),
);
```

Current Android app bar controls:
- `search` -> Search action
- `zoomInOut` -> Zoom in/out actions
- `darkModeToggle` -> Theme toggle action

---


## 🤝 Contributing and Feedback

We welcome contributions and feedback! Here’s how you can help:

### 🐛 Reporting Bugs
If you find a bug, please [open an issue](https://github.com/prathap021/universal_dockit/issues) with:
* A clear description of the problem.
* Steps to reproduce the issue.
* The document format (e.g., PDF, DOCX) causing the issue.
* Your Flutter version and the platform (Android/iOS) where the issue occurs.


### 🛠️ Contributing Code
We love pull requests! If you'd like to contribute directly to the code:
1. Fork the repository.
2. Create a new branch for your feature or bugfix (`git checkout -b feature/my-new-feature`).
3. Commit your changes (`git commit -m 'Add some feature'`).
4. Push to the branch (`git push origin feature/my-new-feature`).
5. Open a Pull Request.

Please ensure your code follows standard Flutter linting rules.

---

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

*Open-source libraries used internally:*
* *[PdfiumAndroid](https://github.com/barteksc/AndroidPdfViewer) (Apache 2.0)*
* *[Apache POI](https://poi.apache.org/) (Apache 2.0)*
* *[CoreXLSX](https://github.com/CoreOffice/CoreXLSX) (Apache 2.0)*
* *[ZIPFoundation](https://github.com/weichsel/ZIPFoundation) (MIT)*
