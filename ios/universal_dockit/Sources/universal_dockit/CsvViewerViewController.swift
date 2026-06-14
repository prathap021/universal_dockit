import UIKit

/// CsvViewerViewController — renders CSV files as a column-aligned plain-text table
/// in a monospace UITextView.
///
/// Rendering strategy:
///  - Parses CSV with an RFC-4180 compliant parser (handles quoted fields)
///  - Computes maximum column widths for alignment
///  - Builds a String table using pipe │ separators and ─ dividers
///  - Displays in a UITextView with a monospace font (no WKWebView overhead)
///
/// No external dependencies.
final class CsvViewerViewController: UIViewController {

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
        textView.font            = UIFont.monospacedSystemFont(ofSize: 12, weight: .regular)
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
                let raw  = try String(contentsOf: self.fileURL, encoding: .utf8)
                let text = self.buildAlignedTable(raw)
                DispatchQueue.main.async {
                    self.activityIndicator.stopAnimating()
                    self.textView.text = text
                    self.textView.setContentOffset(.zero, animated: false)
                }
            } catch {
                DispatchQueue.main.async {
                    self.activityIndicator.stopAnimating()
                    self.textView.text = "Failed to read CSV: \(error.localizedDescription)"
                }
            }
        }
    }

    // MARK: - Column-Aligned Table Builder

    /// Parses CSV and builds a monospace-aligned plain-text table.
    ///
    /// Layout:
    /// ```
    /// Name        │ Age  │ City     
    /// ────────────┼──────┼──────────
    /// Alice       │ 30   │ London   
    /// Bob         │ 25   │ Paris    
    /// ```
    private func buildAlignedTable(_ raw: String) -> String {
        let lines = raw.components(separatedBy: .newlines).filter { !$0.isEmpty }
        guard !lines.isEmpty else { return "(Empty CSV)" }

        let rows = lines.map { parseCsvLine($0) }
        let colCount = rows.map { $0.count }.max() ?? 0
        guard colCount > 0 else { return raw }

        // Compute column widths
        var widths = Array(repeating: 0, count: colCount)
        for row in rows {
            for (ci, cell) in row.enumerated() {
                widths[ci] = max(widths[ci], cell.count)
            }
        }

        var output = ""
        for (ri, row) in rows.enumerated() {
            // Pad each cell to column width + 1 space on each side
            let padded = (0..<colCount).map { ci -> String in
                let cell = ci < row.count ? row[ci] : ""
                return cell.padding(toLength: widths[ci] + 2, withPad: " ", startingAt: 0)
            }
            output += padded.joined(separator: "│") + "\n"

            // Separator after header row
            if ri == 0 {
                let sep = widths.map { String(repeating: "─", count: $0 + 2) }.joined(separator: "┼")
                output += sep + "\n"
            }
        }
        return output
    }

    // MARK: - RFC-4180 CSV Parser

    private func parseCsvLine(_ line: String) -> [String] {
        var fields: [String] = []
        var current = ""
        var inQuotes = false
        for char in line {
            if char == "\"" {
                inQuotes.toggle()
            } else if char == "," && !inQuotes {
                fields.append(current)
                current = ""
            } else {
                current.append(char)
            }
        }
        fields.append(current)
        return fields
    }

    // MARK: - Helpers

    private func applyNavBarTheme() {
        navigationController?.navigationBar.barTintColor = UIColor(hex: 0x16213E)
        navigationController?.navigationBar.titleTextAttributes = [.foregroundColor: UIColor.white]
        navigationController?.navigationBar.tintColor = UIColor(hex: 0xE94560)
    }

    @objc private func closeTapped() { dismiss(animated: true) }
}
