import UIKit
import WebKit
import ZIPFoundation

public class EpubViewerViewController: UIViewController {
    private let fileURL: URL
    private var webView: WKWebView!

    public init(fileURL: URL) {
        self.fileURL = fileURL
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadEPUB()
    }

    private func setupUI() {
        title = "EPUB / E-Book"
        view.backgroundColor = .systemBackground

        let closeButton = UIBarButtonItem(
            barButtonSystemItem: .done,
            target: self,
            action: #selector(closeTapped)
        )
        navigationItem.rightBarButtonItem = closeButton

        let config = WKWebViewConfiguration()
        config.preferences.setValue(true, forKey: "allowFileAccessFromFileURLs")
        webView = WKWebView(frame: view.bounds, configuration: config)
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(webView)
    }

    @objc private func closeTapped() {
        dismiss(animated: true)
    }

    private func loadEPUB() {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            
            let tempDir = FileManager.default.temporaryDirectory
                .appendingPathComponent("epub_cache_\(UUID().uuidString)")
            
            do {
                try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)
                try FileManager.default.unzipItem(at: self.fileURL, to: tempDir)
                
                let containerURL = tempDir.appendingPathComponent("META-INF/container.xml")
                let containerData = try String(contentsOf: containerURL)
                
                guard let rootfileMatch = containerData.range(of: "full-path=\"([^\"]+)\"", options: .regularExpression) else {
                    throw NSError(domain: "EPUB", code: 1, userInfo: [NSLocalizedDescriptionKey: "OPF not found"])
                }
                
                let opfPathRaw = String(containerData[rootfileMatch])
                let opfPath = opfPathRaw.replacingOccurrences(of: "full-path=\"", with: "").replacingOccurrences(of: "\"", with: "")
                
                let opfURL = tempDir.appendingPathComponent(opfPath)
                let opfData = try String(contentsOf: opfURL)
                let opfDir = opfURL.deletingLastPathComponent()
                
                var manifest: [String: String] = [:]
                var spineIds: [String] = []
                
                let itemRegex = try NSRegularExpression(pattern: "<item[^>]+id=\"([^\"]+)\"[^>]+href=\"([^\"]+)\"")
                let nsOpfData = opfData as NSString
                let itemMatches = itemRegex.matches(in: opfData, range: NSRange(location: 0, length: nsOpfData.length))
                for match in itemMatches {
                    if match.numberOfRanges >= 3 {
                        let id = nsOpfData.substring(with: match.range(at: 1))
                        let href = nsOpfData.substring(with: match.range(at: 2))
                        manifest[id] = href
                    }
                }
                
                let itemrefRegex = try NSRegularExpression(pattern: "<itemref[^>]+idref=\"([^\"]+)\"")
                let itemrefMatches = itemrefRegex.matches(in: opfData, range: NSRange(location: 0, length: nsOpfData.length))
                for match in itemrefMatches {
                    if match.numberOfRanges >= 2 {
                        let idref = nsOpfData.substring(with: match.range(at: 1))
                        spineIds.append(idref)
                    }
                }
                
                var html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                html += "<style>body{padding:16px;font-family:sans-serif;max-width:800px;margin:0 auto;}</style>"
                html += "</head><body>"
                
                if spineIds.isEmpty {
                    html += "<p>Could not parse EPUB spine.</p>"
                } else {
                    for id in spineIds {
                        if let href = manifest[id] {
                            let chapterURL = opfDir.appendingPathComponent(href)
                            if let chapterData = try? String(contentsOf: chapterURL) {
                                if let bodyRange = chapterData.range(of: "<body[^>]*>(.*?)</body>", options: [.regularExpression, .caseInsensitive, .dotMatchesLineSeparators]) {
                                    let bodyRaw = String(chapterData[bodyRange])
                                    let strippedStart = bodyRaw.replacingOccurrences(of: "<body[^>]*>", with: "", options: .regularExpression)
                                    let strippedEnd = strippedStart.replacingOccurrences(of: "</body>", with: "", options: .caseInsensitive)
                                    html += "<div>\(strippedEnd)</div>"
                                } else {
                                    html += "<div>\(chapterData)</div>"
                                }
                                html += "<hr style='margin: 40px 0; border: none; border-bottom: 1px solid #ccc;'/>"
                            }
                        }
                    }
                }
                html += "</body></html>"
                
                DispatchQueue.main.async {
                    self.webView.loadHTMLString(html, baseURL: opfDir)
                }
                
            } catch {
                DispatchQueue.main.async {
                    self.webView.loadHTMLString("<html><body><p>Error loading EPUB: \(error.localizedDescription)</p></body></html>", baseURL: nil)
                }
            }
        }
    }
}
