class DocumentFeatures {
  // Viewer controls
  final bool zoomInOut;
  final bool search;
  final bool darkMode;

  const DocumentFeatures({
    this.zoomInOut = true,
    this.search = true,
    this.darkMode = false,
  });

  Map<String, dynamic> toMap() {
    return {
      'zoomInOut': zoomInOut,
      'search': search,
      'darkMode': darkMode,
    };
  }
}
