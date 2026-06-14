import UIKit

/// TxtViewerViewController — renders plain-text (.txt) files.
///
/// Rendering strategy:
///  - Reads the entire file as a UTF-8 String
///  - Displays in a selectable, scrollable UITextView with a monospace font
///  - Dark-themed background matching the rest of the plugin UI
///
/// No external dependencies — uses UIKit only.
final class TxtViewerViewController: UIViewController {

    // MARK: - Properties

    private let fileURL: URL
    private var textView: UITextView!
    private var activityIndicator: UIActivityIndicatorView!

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
        loadFile()
    }

    // MARK: - UI Setup

    private func setupUI() {
        view.backgroundColor = UIColor(hex: 0x0F3460)
        title = fileURL.lastPathComponent
        applyNavBarTheme()

        let closeItem = UIBarButtonItem(
            image: UIImage(systemName: "xmark"),
            style: .plain,
            target: self,
            action: #selector(closeTapped)
        )
        navigationItem.leftBarButtonItem = closeItem

        textView = UITextView()
        textView.translatesAutoresizingMaskIntoConstraints = false
        textView.backgroundColor = UIColor(hex: 0x0F3460)
        textView.textColor       = UIColor(hex: 0xE0E0E0)
        textView.font            = UIFont.monospacedSystemFont(ofSize: 13, weight: .regular)
        textView.isEditable      = false
        textView.isSelectable    = true
        textView.dataDetectorTypes = []
        textView.textContainerInset = UIEdgeInsets(top: 16, left: 12, bottom: 16, right: 12)
        view.addSubview(textView)

        activityIndicator = UIActivityIndicatorView(style: .large)
        activityIndicator.color = UIColor(hex: 0xE94560)
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        activityIndicator.startAnimating()
        view.addSubview(activityIndicator)

        NSLayoutConstraint.activate([
            textView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            textView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            textView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            textView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            activityIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }

    // MARK: - File Loading

    /// Reads the file on a background thread using String(contentsOf:)
    /// and displays it on the main thread.
    private func loadFile() {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            do {
                let text = try String(contentsOf: self.fileURL, encoding: .utf8)
                DispatchQueue.main.async {
                    self.activityIndicator.stopAnimating()
                    self.textView.text = text
                    self.textView.setContentOffset(.zero, animated: false)
                }
            } catch {
                // Try UTF-16 as fallback
                let fallback = (try? String(contentsOf: self.fileURL, encoding: .utf16)) ?? "Failed to read file."
                DispatchQueue.main.async {
                    self.activityIndicator.stopAnimating()
                    self.textView.text = fallback
                }
            }
        }
    }

    // MARK: - Helpers

    private func applyNavBarTheme() {
        navigationController?.navigationBar.barTintColor = UIColor(hex: 0x16213E)
        navigationController?.navigationBar.titleTextAttributes = [.foregroundColor: UIColor.white]
        navigationController?.navigationBar.tintColor = UIColor(hex: 0xE94560)
    }

    @objc private func closeTapped() { dismiss(animated: true) }
}

// MARK: - UIColor Hex Init

extension UIColor {
    convenience init(hex: UInt32) {
        let r = CGFloat((hex >> 16) & 0xFF) / 255
        let g = CGFloat((hex >>  8) & 0xFF) / 255
        let b = CGFloat( hex        & 0xFF) / 255
        self.init(red: r, green: g, blue: b, alpha: 1)
    }
}
