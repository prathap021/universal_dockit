import UIKit
import PDFKit

/// PDFViewerViewController — renders PDF files using Apple's PDFKit.
/// PDFKit is built-in from iOS 11+, zero extra dependencies.
///
/// Features:
/// - Continuous scrolling page layout
/// - Pinch-to-zoom
/// - Page navigation thumbnails
/// - Customised dark-themed navigation bar
final class PDFViewerViewController: UIViewController {

    // MARK: - Properties

    private let fileURL: URL
    private var pdfView: PDFView!
    private var thumbnailView: PDFThumbnailView!
    private var pageLabel: UILabel!

    // MARK: - Init

    init(fileURL: URL) {
        self.fileURL = fileURL
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError() }

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadPDF()
    }

    // MARK: - UI Setup

    private func setupUI() {
        view.backgroundColor = .white

        // Navigation bar
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

        // PDF View
        pdfView = PDFView()
        pdfView.translatesAutoresizingMaskIntoConstraints = false
        pdfView.autoScales = true
        pdfView.displayMode = .singlePageContinuous
        pdfView.displayDirection = .vertical
        pdfView.backgroundColor = .white
        pdfView.pageShadowsEnabled = true
        view.addSubview(pdfView)

        // Thumbnail view at bottom
        thumbnailView = PDFThumbnailView()
        thumbnailView.translatesAutoresizingMaskIntoConstraints = false
        thumbnailView.pdfView = pdfView
        thumbnailView.thumbnailSize = CGSize(width: 50, height: 70)
        thumbnailView.backgroundColor = .white
        thumbnailView.layoutMode = .horizontal
        view.addSubview(thumbnailView)

        // Page counter label
        pageLabel = UILabel()
        pageLabel.translatesAutoresizingMaskIntoConstraints = false
        pageLabel.textColor = .darkText
        pageLabel.font = UIFont.monospacedDigitSystemFont(ofSize: 13, weight: .medium)
        pageLabel.textAlignment = .center
        view.addSubview(pageLabel)

        NSLayoutConstraint.activate([
            pdfView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            pdfView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            pdfView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            pdfView.bottomAnchor.constraint(equalTo: thumbnailView.topAnchor),

            pageLabel.bottomAnchor.constraint(equalTo: thumbnailView.topAnchor, constant: -4),
            pageLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),

            thumbnailView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            thumbnailView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            thumbnailView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            thumbnailView.heightAnchor.constraint(equalToConstant: 80),
        ])

        // Observe page changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(pageChanged),
            name: .PDFViewPageChanged,
            object: pdfView
        )
    }

    // MARK: - PDF Loading

    private func loadPDF() {
        guard let document = PDFDocument(url: fileURL) else {
            showError("Could not load PDF document.")
            return
        }
        pdfView.document = document
        updatePageLabel()
    }

    // MARK: - Helpers

    @objc private func pageChanged() { updatePageLabel() }

    private func updatePageLabel() {
        guard let doc = pdfView.document, let current = pdfView.currentPage else { return }
        let index = doc.index(for: current) + 1
        pageLabel.text = "\(index) / \(doc.pageCount)"
    }

    private func showError(_ message: String) {
        let label = UILabel()
        label.text = "⚠️ \(message)"
        label.textColor = .white
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            label.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            label.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
        ])
    }

    @objc private func closeTapped() {
        dismiss(animated: true)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}
