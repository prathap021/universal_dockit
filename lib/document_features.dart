class DocumentFeatures {
  // General
  final bool openLocalFile;
  final bool openFromUrl;
  final bool zoomInOut;
  final bool search;
  final bool shareDocument;
  final bool fileInformation;
  final bool offlineViewing;
  final bool darkMode;

  // Navigation
  final bool pageNavigation;
  final bool sheetNavigation;
  final bool slideNavigation;
  final bool thumbnails;

  // Content Rendering
  final bool textSelection;
  final bool richTextRendering;
  final bool renderImages;
  final bool renderTables;
  final bool renderHyperlinks;

  // Specific Formatting
  final bool cellFormatting;
  final bool freezePanes;
  final bool wordWrap;
  final bool cssSupport;

  const DocumentFeatures({
    this.openLocalFile = true,
    this.openFromUrl = true,
    this.zoomInOut = true,
    this.search = true,
    this.shareDocument = true,
    this.fileInformation = true,
    this.offlineViewing = true,
    this.darkMode = false,
    this.pageNavigation = true,
    this.sheetNavigation = true,
    this.slideNavigation = true,
    this.thumbnails = true,
    this.textSelection = true,
    this.richTextRendering = true,
    this.renderImages = true,
    this.renderTables = true,
    this.renderHyperlinks = true,
    this.cellFormatting = true,
    this.freezePanes = true,
    this.wordWrap = true,
    this.cssSupport = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'openLocalFile': openLocalFile,
      'openFromUrl': openFromUrl,
      'zoomInOut': zoomInOut,
      'search': search,
      'shareDocument': shareDocument,
      'fileInformation': fileInformation,
      'offlineViewing': offlineViewing,
      'darkMode': darkMode,
      'pageNavigation': pageNavigation,
      'sheetNavigation': sheetNavigation,
      'slideNavigation': slideNavigation,
      'thumbnails': thumbnails,
      'textSelection': textSelection,
      'richTextRendering': richTextRendering,
      'renderImages': renderImages,
      'renderTables': renderTables,
      'renderHyperlinks': renderHyperlinks,
      'cellFormatting': cellFormatting,
      'freezePanes': freezePanes,
      'wordWrap': wordWrap,
      'cssSupport': cssSupport,
    };
  }
}
