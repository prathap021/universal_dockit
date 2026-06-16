import UIKit
import WebKit

/// Toolbar + in-page search/zoom helpers for WKWebView-based viewers.
final class DockitWebViewControls: NSObject, UISearchBarDelegate {
  weak var host: UIViewController?
  weak var webView: WKWebView?
  var features = DockitFeatures(searchEnabled: false, zoomEnabled: false, darkModeEnabled: false)

  private var searchVisible = false
  private var searchQuery = ""
  private let searchBar = UISearchBar(frame: .zero)

  func attach(host: UIViewController, webView: WKWebView) {
    self.host = host
    self.webView = webView
    searchBar.delegate = self
    searchBar.placeholder = "Search document..."
    searchBar.searchBarStyle = .minimal
    refreshToolbar()
  }

  func apply(features: DockitFeatures) {
    self.features = features
    if !features.searchEnabled {
      searchVisible = false
      searchQuery = ""
      searchBar.text = nil
      host?.navigationItem.titleView = nil
    }
    webView?.scrollView.pinchGestureRecognizer?.isEnabled = features.zoomEnabled
    refreshToolbar()
  }

  func refreshToolbar() {
    guard let host else { return }

    var items: [UIBarButtonItem] = []

    if features.searchEnabled && searchVisible {
      items += DockitFeatureToolbar.makeItems(
        features: features,
        searchVisible: true,
        onZoomOut: #selector(zoomOut),
        onZoomIn: #selector(zoomIn),
        onSearchPrev: #selector(searchPrevious),
        onSearchNext: #selector(searchNext),
        target: self
      )
    } else if features.zoomEnabled {
      items += DockitFeatureToolbar.makeItems(
        features: features,
        searchVisible: false,
        onZoomOut: #selector(zoomOut),
        onZoomIn: #selector(zoomIn),
        onSearchPrev: #selector(searchPrevious),
        onSearchNext: #selector(searchNext),
        target: self
      )
    }

    if features.searchEnabled {
      let icon = searchVisible ? "xmark" : "magnifyingglass"
      items.append(
        UIBarButtonItem(
          image: UIImage(systemName: icon),
          style: .plain,
          target: self,
          action: #selector(toggleSearch)
        )
      )
    }

    host.navigationItem.rightBarButtonItems = items.reversed()
    host.navigationItem.titleView = (features.searchEnabled && searchVisible) ? searchBar : nil
  }

  @objc private func toggleSearch() {
    guard features.searchEnabled else { return }
    searchVisible.toggle()
    if !searchVisible {
      searchQuery = ""
      searchBar.text = nil
      clearHighlights()
    }
    refreshToolbar()
    if searchVisible {
      searchBar.becomeFirstResponder()
    }
  }

  @objc private func zoomIn() {
    guard features.zoomEnabled, let scrollView = webView?.scrollView else { return }
    let next = min(scrollView.zoomScale * 1.2, scrollView.maximumZoomScale)
    scrollView.setZoomScale(next, animated: true)
  }

  @objc private func zoomOut() {
    guard features.zoomEnabled, let scrollView = webView?.scrollView else { return }
    let next = max(scrollView.zoomScale / 1.2, scrollView.minimumZoomScale)
    scrollView.setZoomScale(next, animated: true)
  }

  @objc private func searchNext() {
    guard features.searchEnabled, !searchQuery.isEmpty else { return }
    find(forward: true)
  }

  @objc private func searchPrevious() {
    guard features.searchEnabled, !searchQuery.isEmpty else { return }
    find(forward: false)
  }

  func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String) {
    searchQuery = searchText
    if searchText.isEmpty {
      clearHighlights()
    } else {
      find(forward: true)
    }
  }

  func searchBarSearchButtonClicked(_ searchBar: UISearchBar) {
    searchBar.resignFirstResponder()
  }

  private func find(forward: Bool) {
    guard let webView, !searchQuery.isEmpty else { return }
    let escaped = searchQuery
      .replacingOccurrences(of: "\\", with: "\\\\")
      .replacingOccurrences(of: "'", with: "\\'")
    let js = "window.find('\(escaped)', false, false, \(forward ? "true" : "false"));"
    webView.evaluateJavaScript(js, completionHandler: nil)
  }

  private func clearHighlights() {
    webView?.evaluateJavaScript("window.getSelection().removeAllRanges();", completionHandler: nil)
  }
}
