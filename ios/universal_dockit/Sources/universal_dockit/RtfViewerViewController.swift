import UIKit

/// RtfViewerViewController — renders RTF files natively using Foundation's
/// NSAttributedString RTF parser.
///
/// Rendering strategy:
///  1. Load file as Data
///  2. Parse with NSAttributedString(data:options:documentAttributes:)
///     using .documentType = .rtf  (built-in since iOS 7)
///  3. Apply dark-theme colour override (replace dark text with #E0E0E0)
///  4. Display in a selectable UITextView — preserves bold, italic, font
///     sizes, and paragraph spacing from the original RTF document
///  5. Fallback: strip RTF control sequences → plain text in UITextView
///
/// No external dependencies — uses Foundation + UIKit only.
final class RtfViewerViewController: UIViewController, DockitFeatureConfigurable {

    // MARK: - Properties

    private let fileURL: URL
    private var textView: UITextView!
    private var activityIndicator: UIActivityIndicatorView!
    private var dockitFeatures = DockitFeatures(searchEnabled: true, zoomEnabled: true, darkModeEnabled: false)

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
        applyFeatureState()
        loadFile()
    }

    // MARK: - UI Setup

    private func setupUI() {
        view.backgroundColor = .systemBackground
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
        textView.backgroundColor = .systemBackground
        textView.textColor       = .label
        textView.font            = UIFont.systemFont(ofSize: 15)
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

    private func loadFile() {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            do {
                let data = try Data(contentsOf: self.fileURL)
                let attrString = self.parseRtf(data: data)
                DispatchQueue.main.async {
                    self.activityIndicator.stopAnimating()
                    self.textView.attributedText = attrString
                    self.textView.setContentOffset(.zero, animated: false)
                }
            } catch {
                DispatchQueue.main.async {
                    self.activityIndicator.stopAnimating()
                    self.textView.text = "Failed to load RTF: \(error.localizedDescription)"
                }
            }
        }
    }

    // MARK: - RTF Parsing

    /// Attempts NSAttributedString native RTF parsing.
    /// Falls back to manual control-word stripping on failure.
    private func parseRtf(data: Data) -> NSAttributedString {
        // Primary: Foundation native RTF parser
        if let parsed = try? NSAttributedString(
            data: data,
            options: [.documentType: NSAttributedString.DocumentType.rtf],
            documentAttributes: nil
        ) {
            return applyDarkTheme(to: parsed)
        }

        // Fallback: strip RTF control words, display as plain text
        let raw     = String(data: data, encoding: .utf8) ?? ""
        let plain   = stripRtfControlWords(raw)
        return NSAttributedString(
            string: plain,
            attributes: [
                .foregroundColor: UIColor.darkText,
                .font: UIFont.systemFont(ofSize: 15),
            ]
        )
    }

    // MARK: - Dark Theme Override

    /// Replaces dark text colours with the plugin's light text colour (#E0E0E0)
    /// so RTF documents are readable on the dark background.
    private func applyDarkTheme(to original: NSAttributedString) -> NSAttributedString {
        let mutable = NSMutableAttributedString(attributedString: original)
        let fullRange = NSRange(location: 0, length: mutable.length)

        mutable.enumerateAttribute(.foregroundColor, in: fullRange) { value, range, _ in
            let darkColor = UIColor.darkText
            if let color = value as? UIColor {
                var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
                color.getRed(&r, green: &g, blue: &b, alpha: &a)
                let brightness = (r * 299 + g * 587 + b * 114) / 1000
                if brightness < 0.5 {
                    mutable.addAttribute(.foregroundColor, value: darkColor, range: range)
                }
            } else {
                mutable.addAttribute(.foregroundColor, value: darkColor, range: range)
            }
        }
        return mutable
    }

    // MARK: - RTF Control Word Stripper (Fallback)

    /// Removes RTF group delimiters and control words.
    /// Used when NSAttributedString fails to parse the RTF file.
    private func stripRtfControlWords(_ rtf: String) -> String {
        var result = rtf
        // Remove nested groups (up to 10 passes for deep nesting)
        for _ in 0..<10 {
            let prev = result
            result = result.replacingOccurrences(
                of: #"\{[^{}]*\}"#, with: "", options: .regularExpression
            )
            if result == prev { break }
        }
        // Remove control words and remaining delimiters
        result = result
            .replacingOccurrences(of: #"\\[a-z]+[0-9]* ?"#, with: " ", options: .regularExpression)
            .replacingOccurrences(of: "{", with: "")
            .replacingOccurrences(of: "}", with: "")
            .replacingOccurrences(of: "\\\\", with: "")
        return result.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    // MARK: - Helpers

    private func applyNavBarTheme() {
        navigationController?.navigationBar.barTintColor = UIColor(hex: 0x16213E)
        navigationController?.navigationBar.titleTextAttributes = [.foregroundColor: UIColor.white]
        navigationController?.navigationBar.tintColor = UIColor(hex: 0xE94560)
    }

    func applyDockitFeatures(_ features: DockitFeatures) {
        dockitFeatures = features
        guard isViewLoaded else { return }
        applyFeatureState()
    }

    private func applyFeatureState() {
        textView?.isSelectable = dockitFeatures.searchEnabled
        textView?.isScrollEnabled = dockitFeatures.zoomEnabled || dockitFeatures.searchEnabled
    }

    @objc private func closeTapped() { dismiss(animated: true) }
}
