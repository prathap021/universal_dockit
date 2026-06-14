class DocumentFeatures {
  final CommonFeatures common;
  final PdfFeatures pdf;
  final WordFeatures word;
  final ExcelFeatures excel;
  final PptFeatures ppt;
  final TxtFeatures txt;
  final CsvFeatures csv;
  final HtmlFeatures html;
  final OdtFeatures odt;

  const DocumentFeatures({
    this.common = const CommonFeatures(),
    this.pdf = const PdfFeatures(),
    this.word = const WordFeatures(),
    this.excel = const ExcelFeatures(),
    this.ppt = const PptFeatures(),
    this.txt = const TxtFeatures(),
    this.csv = const CsvFeatures(),
    this.html = const HtmlFeatures(),
    this.odt = const OdtFeatures(),
  });

  Map<String, dynamic> toMap() {
    return {
      'common': common.toMap(),
      'pdf': pdf.toMap(),
      'word': word.toMap(),
      'excel': excel.toMap(),
      'ppt': ppt.toMap(),
      'txt': txt.toMap(),
      'csv': csv.toMap(),
      'html': html.toMap(),
      'odt': odt.toMap(),
    };
  }
}

class CommonFeatures {
  final bool openLocalFile;
  final bool openFromUrl;
  final bool zoomInOut;
  final bool searchText;
  final bool shareDocument;
  final bool fileInformation;
  final bool offlineViewing;

  const CommonFeatures({
    this.openLocalFile = true,
    this.openFromUrl = true,
    this.zoomInOut = true,
    this.searchText = true,
    this.shareDocument = true,
    this.fileInformation = true,
    this.offlineViewing = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'openLocalFile': openLocalFile,
      'openFromUrl': openFromUrl,
      'zoomInOut': zoomInOut,
      'searchText': searchText,
      'shareDocument': shareDocument,
      'fileInformation': fileInformation,
      'offlineViewing': offlineViewing,
    };
  }
}

class PdfFeatures {
  final bool pageNavigation;
  final bool textSelection;
  final bool search;
  final bool thumbnails;

  const PdfFeatures({
    this.pageNavigation = true,
    this.textSelection = true,
    this.search = true,
    this.thumbnails = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'pageNavigation': pageNavigation,
      'textSelection': textSelection,
      'search': search,
      'thumbnails': thumbnails,
    };
  }
}

class WordFeatures {
  final bool richTextRendering;
  final bool images;
  final bool tables;
  final bool hyperlinks;

  const WordFeatures({
    this.richTextRendering = true,
    this.images = true,
    this.tables = true,
    this.hyperlinks = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'richTextRendering': richTextRendering,
      'images': images,
      'tables': tables,
      'hyperlinks': hyperlinks,
    };
  }
}

class ExcelFeatures {
  final bool sheetNavigation;
  final bool cellFormatting;
  final bool mergedCells;
  final bool freezePanes;

  const ExcelFeatures({
    this.sheetNavigation = true,
    this.cellFormatting = true,
    this.mergedCells = true,
    this.freezePanes = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'sheetNavigation': sheetNavigation,
      'cellFormatting': cellFormatting,
      'mergedCells': mergedCells,
      'freezePanes': freezePanes,
    };
  }
}

class PptFeatures {
  final bool slideNavigation;
  final bool slideThumbnails;
  final bool fullScreenPresentation;
  final bool imagesAndShapes;

  const PptFeatures({
    this.slideNavigation = true,
    this.slideThumbnails = true,
    this.fullScreenPresentation = true,
    this.imagesAndShapes = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'slideNavigation': slideNavigation,
      'slideThumbnails': slideThumbnails,
      'fullScreenPresentation': fullScreenPresentation,
      'imagesAndShapes': imagesAndShapes,
    };
  }
}

class TxtFeatures {
  final bool wordWrap;
  final bool fontSizeAdjustment;
  final bool search;

  const TxtFeatures({
    this.wordWrap = true,
    this.fontSizeAdjustment = true,
    this.search = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'wordWrap': wordWrap,
      'fontSizeAdjustment': fontSizeAdjustment,
      'search': search,
    };
  }
}

class CsvFeatures {
  final bool tableView;
  final bool search;
  final bool sortColumns;

  const CsvFeatures({
    this.tableView = true,
    this.search = true,
    this.sortColumns = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'tableView': tableView,
      'search': search,
      'sortColumns': sortColumns,
    };
  }
}

class HtmlFeatures {
  final bool offlineRendering;
  final bool cssSupport;
  final bool images;
  final bool hyperlinks;

  const HtmlFeatures({
    this.offlineRendering = true,
    this.cssSupport = true,
    this.images = true,
    this.hyperlinks = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'offlineRendering': offlineRendering,
      'cssSupport': cssSupport,
      'images': images,
      'hyperlinks': hyperlinks,
    };
  }
}

class OdtFeatures {
  final bool basicRendering;
  final bool images;
  final bool tables;
  final bool multipleSheetsSlides;

  const OdtFeatures({
    this.basicRendering = true,
    this.images = true,
    this.tables = true,
    this.multipleSheetsSlides = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'basicRendering': basicRendering,
      'images': images,
      'tables': tables,
      'multipleSheetsSlides': multipleSheetsSlides,
    };
  }
}
