# Universal Dockit 🗂️

[![pub package](https://img.shields.io/pub/v/universal_dockit.svg)](https://pub.dev/packages/universal_dockit)
[![License: MIT](https://img.shields.io/badge/license-MIT-purple.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/platform-android%20%7C%20ios-blue.svg)](https://flutter.dev/)

A powerful, high-performance Flutter plugin for viewing a wide variety of documents natively on Android and iOS. 

**Universal Dockit** leverages the best open-source native SDKs and built-in OS frameworks to render documents quickly and beautifully, keeping the Flutter layer minimal and efficient. 

Absolutely **no commercial dependencies**, **no cloud processing**, and **100% on-device rendering**.

---

## ✨ Features

* **Universal Format Support**: Open 13 different document types including PDF, Word, Excel, PowerPoint, Text, CSV, RTF, and OpenDocument formats.
* **Native Performance**: Documents are rendered natively using optimized libraries like `PdfiumAndroid` on Android and `PDFKit`/`QuickLook` on iOS.
* **Dark Mode Ready**: Beautiful, cohesive dark-themed UI out-of-the-box for HTML-rendered documents, text, and CSVs.
* **Smart Rendering**: Formula evaluation in spreadsheets, native zooming and scrolling for PDFs, and column-aligned rendering for CSVs.
* **Fully Offline**: Everything is parsed and rendered entirely on the device.

---

## 📄 Supported Formats & Native Renderers

The plugin intelligently routes each file type to the most appropriate native renderer:


| Format | Extension | Android Engine | iOS Engine |
| :--- | :--- | :--- | :--- |
| **PDF** | `.pdf` | [PdfiumAndroid](https://github.com/barteksc/AndroidPdfViewer) (Hardware accelerated, pinch-to-zoom) | **PDFKit** (Built-in) |
| **Word** | `.doc`, `.docx` | **Apache POI** → Styled HTML → WebView | **QuickLook** (Built-in) |
| **Excel** | `.xlsx` | **Apache POI** → HTML Table → WebView | [CoreXLSX](https://github.com/CoreOffice/CoreXLSX) → HTML Table → WebView |
| **Excel (Legacy)**| `.xls` | **Apache POI** → HTML Table → WebView | **QuickLook** (Built-in) |
| **PowerPoint**| `.ppt`, `.pptx` | **Apache POI** → Bitmap Slides → ScrollView | **QuickLook** (Built-in) |
| **Text** | `.txt` | Native `TextView` (Monospace, memory-efficient) | Native `UITextView` (Monospace) |
| **CSV** | `.csv` | RFC-4180 Parser → HTML Table → WebView | Aligned Plain-text Table → `UITextView` |
| **Rich Text** | `.rtf` | Tag Stripper → `Html.fromHtml` → `TextView` | `NSAttributedString` → `UITextView` |
| **OpenDocument**| `.odt`, `.ods`, `.odp`| **Apache ODF Toolkit** → DOM Traversal → WebView| **QuickLook** (Built-in) |

---

## 🛠 Setup & Installation

**Requirements:**
* **Flutter SDK**: `>=3.10.0`
* **Dart SDK**: `^3.0.0`

Add `universal_dockit` to your `pubspec.yaml`:

```yaml
dependencies:
  universal_dockit: ^1.0.0
```

### 🤖 Android Setup

1. **Minimum SDK**: Ensure your `minSdkVersion` is at least **24** in `android/app/build.gradle`.
2. **MultiDex**: Apache POI is a large library. Ensure `multiDexEnabled true` is set in your app's `build.gradle` (usually enabled by default on modern Flutter versions).

Because the plugin handles the heavy lifting with its own `build.gradle.kts`, you don't need to add any specific repository exclusions manually in your app module.

### 🍎 iOS Setup

1. **Minimum iOS Version**: Ensure your iOS deployment target is at least **iOS 13.0** in your `ios/Podfile`:
   ```ruby
   platform :ios, '13.0'
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

Supported `DocType` values: `pdf`, `doc`, `docx`, `xls`, `xlsx`, `ppt`, `pptx`, `txt`, `csv`, `rtf`, `odt`, `ods`, `odp`.

---

## 🏗 Architecture Under the Hood

To keep the codebase maintainable and performant, the native code is split using the Strategy Pattern:

* **Android**: `DocumentViewerActivity` acts as a host and dispatches rendering to specific implementations of `DocumentRenderer` (e.g., `PdfDocumentRenderer`, `DocxDocumentRenderer`). Communication is handled via a `RenderCallbacks` interface.
* **iOS**: `UniversalDockitPlugin.swift` routes requests to dedicated `UIViewController` subclasses (e.g., `PDFViewerViewController`, `XLSXViewerViewController`, `CsvViewerViewController`).

---

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

*Open-source libraries used internally:*
* *[PdfiumAndroid](https://github.com/barteksc/AndroidPdfViewer) (Apache 2.0)*
* *[Apache POI](https://poi.apache.org/) (Apache 2.0)*
* *[Apache ODF Toolkit](https://odftoolkit.org/) (Apache 2.0)*
* *[CoreXLSX](https://github.com/CoreOffice/CoreXLSX) (Apache 2.0)*
