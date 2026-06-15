import UIKit
import QuickLook

/// QuickLookViewerViewController — uses Apple's QuickLook framework to preview
/// Office documents (DOC, DOCX, XLS, XLSX, PPT, PPTX) and OpenDocument formats
/// (ODT, ODS, ODP). QuickLook is built-in from iOS 12+ and renders these
/// formats natively with no third-party dependencies.
final class QuickLookViewerViewController: QLPreviewController, QLPreviewControllerDataSource, QLPreviewControllerDelegate {

    private let fileURL: URL

    init(fileURL: URL) {
        self.fileURL = fileURL
        super.init(nibName: nil, bundle: nil)
        self.dataSource = self
        self.delegate = self
    }

    required init?(coder: NSCoder) { fatalError() }

    // MARK: - QLPreviewControllerDataSource

    func numberOfPreviewItems(in controller: QLPreviewController) -> Int { 1 }

    func previewController(
        _ controller: QLPreviewController,
        previewItemAt index: Int
    ) -> QLPreviewItem {
        fileURL as QLPreviewItem
    }

    // MARK: - QLPreviewControllerDelegate

    func previewController(
        _ controller: QLPreviewController,
        shouldOpen url: URL,
        for item: QLPreviewItem
    ) -> Bool {
        return false // Prevent opening links externally
    }
}
