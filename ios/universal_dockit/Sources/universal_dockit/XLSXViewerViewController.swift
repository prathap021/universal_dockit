import UIKit
import WebKit
import CoreXLSX

/// XLSXViewerViewController — renders .xlsx files natively using CoreXLSX.
///
/// CoreXLSX (https://github.com/CoreOffice/CoreXLSX, Apache 2.0) is a pure
/// Swift library that parses the OOXML/ZIP structure of .xlsx files without
/// any runtime dependencies. This viewer:
///   1. Parses each worksheet with CoreXLSX
///   2. Converts shared-string and inline-string cells to HTML table rows
///   3. Loads the HTML into a WKWebView for styled, zoomable display
///
/// Note: CoreXLSX supports .xlsx (OOXML) only.
///       Legacy .xls (binary OLE2) files are handled by QuickLookViewerViewController.
final class XLSXViewerViewController: UIViewController {

    // MARK: - Properties

    private let fileURL: URL
    private var webView: WKWebView!
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
        loadXLSX()
    }

    // MARK: - UI Setup

    private func setupUI() {
        view.backgroundColor = .systemBackground

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

        // WKWebView
        let config = WKWebViewConfiguration()
        webView = WKWebView(frame: .zero, configuration: config)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.backgroundColor = .systemBackground
        webView.scrollView.backgroundColor = .clear
        webView.isOpaque = false
        view.addSubview(webView)

        // Activity indicator
        activityIndicator = UIActivityIndicatorView(style: .large)
        activityIndicator.color = UIColor(red: 0.914, green: 0.271, blue: 0.376, alpha: 1)
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        activityIndicator.startAnimating()
        view.addSubview(activityIndicator)

        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            activityIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }

    // MARK: - XLSX Parsing

    private func loadXLSX() {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            do {
                let html = try self.buildHTML()
                DispatchQueue.main.async {
                    self.activityIndicator.stopAnimating()
                    self.webView.loadHTMLString(html, baseURL: nil)
                }
            } catch {
                DispatchQueue.main.async {
                    self.activityIndicator.stopAnimating()
                    self.showError("Failed to parse XLSX: \(error.localizedDescription)")
                }
            }
        }
    }

    private func buildHTML() throws -> String {
        // Open the XLSX file with CoreXLSX
        let xlsxFile = try XLSXFile(filepath: fileURL.path)

        // Fetch shared strings (text stored in a shared pool for de-duplication)
        let sharedStrings = try xlsxFile.parseSharedStrings()

        var html = htmlHeader("Excel Spreadsheet")

        // Iterate each worksheet path
        for wbPath in try xlsxFile.parseWorksheetPaths() {
            let ws = try xlsxFile.parseWorksheet(at: wbPath)

            // Derive sheet name from path (e.g. "xl/worksheets/sheet1.xml" → "Sheet1")
            let sheetName = wbPath
                .components(separatedBy: "/").last?
                .replacingOccurrences(of: ".xml", with: "")
                .capitalized ?? wbPath

            html += "<h2 class='sheet-title'>\(sheetName.htmlEscaped)</h2>\n"
            html += "<div class='table-wrapper'><table>\n"

            let rows = ws.data?.rows ?? []
            for (rowIndex, row) in rows.enumerated() {
                html += "<tr>"
                for cell in row.cells {
                    let value = cellValue(cell: cell, sharedStrings: sharedStrings)
                    let tag = rowIndex == 0 ? "th" : "td"
                    html += "<\(tag)>\(value.htmlEscaped)</\(tag)>"
                }
                html += "</tr>\n"
            }

            html += "</table></div>\n"
        }

        html += htmlFooter
        return html
    }

    /// Resolves a cell's display value from its type and shared strings table.
    private func cellValue(cell: Cell, sharedStrings: SharedStrings?) -> String {
        switch cell.type {
        case .sharedString:
            // Value is an index into the shared strings table
            if let indexStr = cell.value,
               let index = Int(indexStr),
               let ss = sharedStrings {
                return ss.items[safe: index]?.text ?? indexStr
            }
            return cell.value ?? ""

        case .inlineString:
            return cell.inlineString?.text ?? cell.value ?? ""

        case .boolean:
            return cell.value == "1" ? "TRUE" : "FALSE"

        default:
            // Numeric, formula result, date, error, etc.
            return cell.value ?? ""
        }
    }

    // MARK: - Error Display

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

    @objc private func closeTapped() { dismiss(animated: true) }

    // MARK: - HTML Templates

    private func htmlHeader(_ title: String) -> String {
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="UTF-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <title>\(title)</title>
        <style>
          :root {
            color-scheme: light dark;
            --bg: #FFFFFF; --surface: #F7F7F7; --accent: #E94560;
            --text: #111111; --border: #D9D9D9;
          }
          @media (prefers-color-scheme: dark) {
            :root {
              --bg: #121212; --surface: #1E1E1E;
              --text: #E0E0E0; --border: #333333;
            }
          }
          * { box-sizing: border-box; margin: 0; padding: 0; }
          body {
            background: var(--bg); color: var(--text);
            font-family: -apple-system, 'Helvetica Neue', Arial, sans-serif;
            font-size: 14px; line-height: 1.6; padding: 16px;
          }
          h2.sheet-title {
            margin: 20px 0 8px; padding: 8px 12px;
            background: var(--surface); border-left: 4px solid var(--accent);
            border-radius: 4px; color: var(--accent); font-size: 1.1em;
          }
          .table-wrapper { overflow-x: auto; margin-bottom: 24px; }
          table {
            width: 100%; border-collapse: collapse;
            background: var(--surface); border-radius: 8px; overflow: hidden;
          }
          th {
            background: var(--accent); color: #fff; font-weight: 600;
            padding: 10px 12px; text-align: left; font-size: 13px;
            white-space: nowrap;
          }
          td {
            padding: 7px 12px; border-bottom: 1px solid var(--border);
            font-size: 13px; white-space: nowrap;
          }
          tr:last-child td { border-bottom: none; }
          tr:nth-child(even) td { background: rgba(255,255,255,.03); }
        </style>
        </head>
        <body>
        """
    }

    private var htmlFooter: String { "\n</body></html>" }
}

// MARK: - Array Safe Subscript

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

// MARK: - String HTML Escaping

private extension String {
    var htmlEscaped: String {
        self
            .replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
            .replacingOccurrences(of: "\"", with: "&quot;")
    }
}
