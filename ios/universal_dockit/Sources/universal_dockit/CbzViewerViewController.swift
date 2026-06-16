import UIKit
import WebKit
import ZIPFoundation

public class CbzViewerViewController: UIViewController, DockitFeatureConfigurable {
    private let fileURL: URL
    private var webView: WKWebView!
    private var dockitFeatures = DockitFeatures(searchEnabled: true, zoomEnabled: true, darkModeEnabled: false)
    private let webControls = DockitWebViewControls()

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
        applyFeatureState()
        loadCBZ()
    }

    private func setupUI() {
        title = "CBZ / Comic Book"
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
        webControls.attach(host: self, webView: webView)
    }

    @objc private func closeTapped() {
        dismiss(animated: true)
    }

    func applyDockitFeatures(_ features: DockitFeatures) {
        dockitFeatures = features
        guard isViewLoaded else { return }
        webControls.apply(features: features)
    }

    private func applyFeatureState() {
        webControls.apply(features: dockitFeatures)
    }

    private func loadCBZ() {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            
            let tempDir = FileManager.default.temporaryDirectory
                .appendingPathComponent("cbz_cache_\(UUID().uuidString)")
            
            do {
                try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)
                try FileManager.default.unzipItem(at: self.fileURL, to: tempDir)
                
                let enumerator = FileManager.default.enumerator(at: tempDir, includingPropertiesForKeys: nil)
                var images: [URL] = []
                while let url = enumerator?.nextObject() as? URL {
                    if !url.hasDirectoryPath {
                        let ext = url.pathExtension.lowercased()
                        if ["png", "jpg", "jpeg", "webp", "gif"].contains(ext) {
                            images.append(url)
                        }
                    }
                }
                
                images.sort { $0.lastPathComponent < $1.lastPathComponent }
                
                var html = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                html += "<style>body{margin:0;background:#000;text-align:center;} img{max-width:100%;height:auto;display:block;margin:0 auto;}</style>"
                html += "</head><body>"
                if images.isEmpty {
                    html += "<p style='color:white'>No images found</p>"
                } else {
                    for img in images {
                        html += "<img src=\"\(img.lastPathComponent)\" />"
                    }
                }
                html += "</body></html>"
                
                DispatchQueue.main.async {
                    self.webView.loadHTMLString(html, baseURL: tempDir)
                }
            } catch {
                DispatchQueue.main.async {
                    self.webView.loadHTMLString("<html><body><p>Error loading CBZ: \(error.localizedDescription)</p></body></html>", baseURL: nil)
                }
            }
        }
    }
}
