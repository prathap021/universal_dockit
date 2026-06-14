# Universal Dockit 🗂️

[![pub package](https://img.shields.io/pub/v/universal_dockit.svg)](https://pub.dev/packages/universal_dockit)
[![License: MIT](https://img.shields.io/badge/license-MIT-purple.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/platform-android%20%7C%20ios-blue.svg)](https://flutter.dev/)

> ⚠️ **Notice:** This plugin is currently under **Beta version**. APIs, UI, and rendering behavior may change as we continue to improve support for various document types.


A powerful, high-performance Flutter plugin for viewing a wide variety of documents natively on Android and iOS. 

**Universal Dockit** leverages the best open-source native SDKs and built-in OS frameworks to render documents quickly and beautifully, keeping the Flutter layer minimal and efficient. 

Absolutely **no commercial dependencies**, **no cloud processing**, and **100% on-device rendering**.

---

## ✨ Features

* **Universal Format Support**: Open 15 different document types including PDF, Word, Excel, PowerPoint, EPUB, CBZ, Text, CSV, RTF, and OpenDocument formats.
* **Native Performance**: Documents are rendered natively using optimized libraries like `PdfiumAndroid` on Android and `PDFKit`/`QuickLook` on iOS.
* **Format-Specific Themes**: Beautiful, color-coded native headers (e.g., Blue for Word, Green for Excel, Purple for EPUB) provide a cohesive, premium UI out-of-the-box.
* **Smart Rendering**: Formula evaluation in spreadsheets, native zooming and scrolling for PDFs, and HTML structural extraction for E-Books and Comic Books.
* **Fully Offline**: Everything is parsed and rendered entirely on the device without cloud APIs or heavy monolithic frameworks.

---

## 📄 Supported Formats & Native Renderers

The plugin intelligently routes each file type to the most appropriate native renderer:

| Format | Extension | Android Engine | iOS Engine |
| :--- | :--- | :--- | :--- |
| **PDF** | `.pdf` | [PdfiumAndroid](https://github.com/barteksc/AndroidPdfViewer) (Hardware accelerated) | **PDFKit** (Built-in) |
| **Word (doc, docx)** | `.doc`, `.docx` | [All Documents Reader SDK](https://github.com/ahmadullahpk/all-documents-reader) | **QuickLook** (Built-in) |
| **Excel (xls, xlsx)** | `.xls`, `.xlsx` | [All Documents Reader SDK](https://github.com/ahmadullahpk/all-documents-reader) | [CoreXLSX](https://github.com/CoreOffice/CoreXLSX) → HTML Table → WebView |
| **PowerPoint (ppt, pptx)**| `.ppt`, `.pptx` | [All Documents Reader SDK](https://github.com/ahmadullahpk/all-documents-reader) | **QuickLook** (Built-in) |
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
  universal_dockit: ^1.0.0
```

### 🤖 Android Setup

1. **Minimum SDK**: Ensure your `minSdkVersion` is at least **24** in `android/app/build.gradle`.
2. **JitPack Repository**: Ensure that JitPack is added to your project's `settings.gradle` or root `build.gradle` because the document reader SDK is hosted there.

Because the plugin handles the heavy lifting with its own `build.gradle.kts`, you don't need to add any specific repository exclusions manually in your app module.

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

By default, the plugin automatically enables all standard viewer features (zooming, searching, page navigation, etc.). You can configure specific features by passing a `DocumentFeatures` object.

```dart
await _universalDockitPlugin.openDocument(
  filePath,
  features: const DocumentFeatures(
    darkMode: true, // Enable native dark mode rendering
    zoomInOut: true,
    search: true,
    // Note: All other features like textSelection, renderImages, etc., 
    // are enabled by default. Only specify what you want to change!
  ),
);
```

---

## 🏗 Architecture Under the Hood

To keep the codebase maintainable and performant, the native code is split using the Strategy Pattern:

* **Android**: `UniversalDockitPlugin` handles dispatching. Office documents (Word, Excel, PPT) are routed to the specialized `All_Document_Reader_Activity` from the SDK. Other formats use `DocumentViewerActivity`, which acts as a host and dispatches rendering to specific consolidated implementations of `DocumentRenderer` (e.g., `PdfDocumentRenderer`, `EpubDocumentRenderer`). Communication is handled via a `RenderCallbacks` interface.
* **iOS**: `UniversalDockitPlugin.swift` routes requests to dedicated `UIViewController` subclasses (e.g., `PDFViewerViewController`, `XLSXViewerViewController`, `EpubViewerViewController`).

---

## 🤝 Contributing and Feedback

We welcome contributions and feedback! Here’s how you can help:

### 🐛 Reporting Bugs
If you find a bug, please [open an issue](https://github.com/prathap021/universal_dockit/issues) with:
* A clear description of the problem.
* Steps to reproduce the issue.
* The document format (e.g., PDF, DOCX) causing the issue.
* Your Flutter version and the platform (Android/iOS) where the issue occurs.

### 💡 Requesting Features
Feature requests are always welcome! If there's a specific document format, UI feature, or performance enhancement you'd like to see:
* [Open an issue](https://github.com/prathap021/universal_dockit/issues) outlining your feature request.
* Describe the use case and why it would be helpful.

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
* *[All Documents Reader](https://github.com/ahmadullahpk/all-documents-reader)*
* *[CoreXLSX](https://github.com/CoreOffice/CoreXLSX) (Apache 2.0)*
* *[ZIPFoundation](https://github.com/weichsel/ZIPFoundation) (MIT)*
