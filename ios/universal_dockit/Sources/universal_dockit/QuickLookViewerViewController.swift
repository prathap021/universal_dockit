import UIKit
import QuickLook

/// QuickLookViewerViewController — uses Apple's QuickLook framework to preview
/// Office documents (DOC, DOCX, XLS, XLSX, PPT, PPTX) and OpenDocument formats
/// (ODT, ODS, ODP). QuickLook is built-in from iOS 12+ and renders these
/// formats natively with no third-party dependencies.
///
/// Architecture: This view controller embeds QLPreviewController as a child
/// view controller for full-screen integration.
final class QuickLookViewerViewController: UIViewController {

    // MARK: - Properties

    private let fileURL: URL
    private var previewController: QLPreviewController!

    // MARK: - Init

    init(fileURL: URL) {
        self.fileURL = fileURL
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError() }

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        setupNavigationBar()
        embedPreviewController()
    }

    // MARK: - Setup

    private func setupNavigationBar() {
        title = fileURL.lastPathComponent
        navigationController?.navigationBar.barTintColor = UIColor(red: 0.086, green: 0.129, blue: 0.243, alpha: 1)
        navigationController?.navigationBar.titleTextAttributes = [.foregroundColor: UIColor.white]
        navigationController?.navigationBar.tintColor = UIColor(red: 0.914, green: 0.271, blue: 0.376, alpha: 1)

        let closeItem = UIBarButtonItem(
            image: UIImage(systemName: "xmark"),
            style: .plain,
            target: self,
            action: #selector(closeTapped)
        )
        navigationItem.leftBarButtonItem = closeItem
    }

    private func embedPreviewController() {
        previewController = QLPreviewController()
        previewController.dataSource = self
        previewController.delegate = self

        addChild(previewController)
        previewController.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(previewController.view)

        NSLayoutConstraint.activate([
            previewController.view.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            previewController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            previewController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            previewController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        previewController.didMove(toParent: self)
    }

    @objc private func closeTapped() {
        dismiss(animated: true)
    }
}

// MARK: - QLPreviewControllerDataSource

extension QuickLookViewerViewController: QLPreviewControllerDataSource {

    func numberOfPreviewItems(in controller: QLPreviewController) -> Int { 1 }

    func previewController(
        _ controller: QLPreviewController,
        previewItemAt index: Int
    ) -> QLPreviewItem {
        fileURL as QLPreviewItem
    }
}

// MARK: - QLPreviewControllerDelegate

extension QuickLookViewerViewController: QLPreviewControllerDelegate {

    func previewController(
        _ controller: QLPreviewController,
        shouldOpen url: URL,
        for item: QLPreviewItem
    ) -> Bool {
        return false // Prevent opening links externally
    }
}
