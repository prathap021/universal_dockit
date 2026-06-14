import Flutter
import UIKit

/// UniversalDockitPlugin — Flutter plugin entry point.
///
/// Each document type is routed to a dedicated, single-purpose view controller:
///
/// ┌──────────────────────────────┬──────────────────────────────────────────┐
/// │ Format(s)                    │ View Controller                          │
/// ├──────────────────────────────┼──────────────────────────────────────────┤
/// │ pdf                          │ PDFViewerViewController  (PDFKit)        │
/// │ xlsx                         │ XLSXViewerViewController (CoreXLSX)      │
/// │ txt                          │ TxtViewerViewController  (UITextView)    │
/// │ csv                          │ CsvViewerViewController  (UITextView)    │
/// │ rtf                          │ RtfViewerViewController  (NSAttrString)  │
/// │ doc, docx, xls               │ QuickLookViewerViewController (QL)       │
/// │ ppt, pptx                    │ QuickLookViewerViewController (QL)       │
/// │ odt, ods, odp                │ QuickLookViewerViewController (QL)       │
/// └──────────────────────────────┴──────────────────────────────────────────┘
public class UniversalDockitPlugin: NSObject, FlutterPlugin {

    // MARK: - Registration

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(
            name: "universal_dockit",
            binaryMessenger: registrar.messenger()
        )
        registrar.addMethodCallDelegate(UniversalDockitPlugin(), channel: channel)
    }

    // MARK: - Method Channel Handler

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {

        case "openDocument":
            guard
                let args     = call.arguments as? [String: Any],
                let filePath = args["filePath"] as? String,
                let docType  = args["docType"]  as? String
            else {
                result(FlutterError(
                    code: "INVALID_ARGS",
                    message: "filePath and docType are required",
                    details: nil
                ))
                return
            }
            let features = args["features"] as? [String: Any] ?? [:]
            openDocument(filePath: filePath, docType: docType, features: features, result: result)

        case "getPlatformVersion":
            result("iOS \(UIDevice.current.systemVersion)")

        default:
            result(FlutterMethodNotImplemented)
        }
    }

    // MARK: - Document Routing

    private func openDocument(
        filePath: String,
        docType: String,
        features: [String: Any],
        result: @escaping FlutterResult
    ) {
        let url = URL(fileURLWithPath: filePath)

        guard FileManager.default.fileExists(atPath: filePath) else {
            result(FlutterError(
                code: "FILE_NOT_FOUND",
                message: "File not found: \(filePath)",
                details: nil
            ))
            return
        }

        DispatchQueue.main.async {
            guard let rootVC = Self.keyRootViewController() else {
                result(FlutterError(
                    code: "NO_VIEW_CONTROLLER",
                    message: "Could not obtain root view controller",
                    details: nil
                ))
                return
            }

            let viewController: UIViewController

            switch docType.lowercased() {

            // ── PDF ──────────────────────────────────────────────────────────
            // PDFKit (built-in, iOS 11+): continuous scroll, pinch-zoom,
            // thumbnail strip, page counter.
            case "pdf":
                viewController = PDFViewerViewController(fileURL: url)

            // ── XLSX ─────────────────────────────────────────────────────────
            // CoreXLSX (Apache 2.0): pure-Swift OOXML parser.
            // Parses worksheets → HTML table → WKWebView.
            case "xlsx":
                viewController = XLSXViewerViewController(fileURL: url)

            // ── TXT ──────────────────────────────────────────────────────────
            // UITextView with monospace font. Reads file as UTF-8 String.
            case "txt":
                viewController = TxtViewerViewController(fileURL: url)

            // ── EPUB ─────────────────────────────────────────────────────────
            case "epub":
                viewController = EpubViewerViewController(fileURL: url)

            // ── CBZ ──────────────────────────────────────────────────────────
            case "cbz":
                viewController = CbzViewerViewController(fileURL: url)

            // ── CSV ──────────────────────────────────────────────────────────
            // UITextView with monospace font. RFC-4180 parser produces a
            // column-aligned plain-text table with │ separators.
            case "csv":
                viewController = CsvViewerViewController(fileURL: url)

            // ── RTF ──────────────────────────────────────────────────────────
            // NSAttributedString native RTF parser (Foundation, iOS 7+).
            // Dark-theme colour override applied on top. UITextView display.
            case "rtf":
                viewController = RtfViewerViewController(fileURL: url)

            // ── Office formats (legacy binary + OOXML) ───────────────────────
            // QuickLook (built-in, iOS 12+): Apple's native document preview.
            // Handles DOC, DOCX, XLS, PPT, PPTX with no dependencies.
            case "doc", "docx", "xls", "ppt", "pptx":
                viewController = QuickLookViewerViewController(fileURL: url)

            // ── OpenDocument formats ─────────────────────────────────────────
            // QuickLook handles ODT, ODS, ODP natively on iOS 12+.
            case "odt", "ods", "odp":
                viewController = QuickLookViewerViewController(fileURL: url)

            default:
                result(FlutterError(
                    code: "UNSUPPORTED_TYPE",
                    message: "Unsupported document type: \(docType)",
                    details: nil
                ))
                return
            }

            let nav = UINavigationController(rootViewController: viewController)
            nav.modalPresentationStyle = .fullScreen
            
            if let isDark = features["darkMode"] as? Bool, isDark {
                nav.overrideUserInterfaceStyle = .dark
            }
            
            rootVC.present(nav, animated: true)
            result(true)
        }
    }

    // MARK: - Private Helpers

    private static func keyRootViewController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap     { $0.windows }
            .first       { $0.isKeyWindow }?
            .rootViewController
    }
}
