import UIKit

/// Adds optional search / zoom toolbar actions based on `DockitFeatures`.
/// Buttons are only shown when the corresponding feature flag is enabled.
enum DockitFeatureToolbar {

  static func makeItems(
    features: DockitFeatures,
    searchVisible: Bool,
    onZoomOut: Selector,
    onZoomIn: Selector,
    onSearchPrev: Selector,
    onSearchNext: Selector,
    target: AnyObject?
  ) -> [UIBarButtonItem] {
    var items: [UIBarButtonItem] = []

    if features.searchEnabled && searchVisible {
      items.append(
        UIBarButtonItem(
          image: UIImage(systemName: "chevron.up"),
          style: .plain,
          target: target,
          action: onSearchPrev
        )
      )
      items.append(
        UIBarButtonItem(
          image: UIImage(systemName: "chevron.down"),
          style: .plain,
          target: target,
          action: onSearchNext
        )
      )
    }

    if features.zoomEnabled {
      items.append(
        UIBarButtonItem(
          image: UIImage(systemName: "minus.magnifyingglass"),
          style: .plain,
          target: target,
          action: onZoomOut
        )
      )
      items.append(
        UIBarButtonItem(
          image: UIImage(systemName: "plus.magnifyingglass"),
          style: .plain,
          target: target,
          action: onZoomIn
        )
      )
    }

    return items
  }
}
