/// Supported document types for UniversalDockit.
///
/// Map file extensions to [DocType] values and pass them to [openDocument].
enum DocType {
  pdf,
  doc,
  docx,
  xls,
  xlsx,
  ppt,
  pptx,
  txt,
  csv,
  rtf,
  odt,
  ods,
  odp;

  /// Returns the string identifier used by the native layer.
  String get identifier => name; // e.g. DocType.pdf → "pdf"

  /// Infers the [DocType] from a file path extension.
  /// Throws [ArgumentError] if the extension is unrecognised.
  static DocType fromPath(String filePath) {
    final ext = filePath.split('.').last.toLowerCase();
    return DocType.fromExtension(ext);
  }

  /// Infers the [DocType] from a raw extension string (without the dot).
  /// Throws [ArgumentError] if the extension is unrecognised.
  static DocType fromExtension(String ext) {
    return switch (ext) {
      'pdf' => DocType.pdf,
      'doc' => DocType.doc,
      'docx' => DocType.docx,
      'xls' => DocType.xls,
      'xlsx' => DocType.xlsx,
      'ppt' => DocType.ppt,
      'pptx' => DocType.pptx,
      'txt' => DocType.txt,
      'csv' => DocType.csv,
      'rtf' => DocType.rtf,
      'odt' => DocType.odt,
      'ods' => DocType.ods,
      'odp' => DocType.odp,
      _ => throw ArgumentError('Unsupported document extension: .$ext'),
    };
  }
}
