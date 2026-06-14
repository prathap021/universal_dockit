import 'doc_type.dart';
import 'universal_dockit_platform_interface.dart';

export 'doc_type.dart';

/// UniversalDockit — public API for the native document viewer plugin.
///
/// Usage:
/// ```dart
/// import 'package:universal_dockit/universal_dockit.dart';
///
/// final _dockit = UniversalDockit();
///
/// // Option 1: infer type from file path
/// await _dockit.openDocument('/storage/emulated/0/Download/report.pdf');
///
/// // Option 2: provide type explicitly
/// await _dockit.openDocument(
///   '/storage/emulated/0/Download/sheet.xlsx',
///   docType: DocType.xlsx,
/// );
/// ```
class UniversalDockit {
  /// Opens the document at [filePath] using the appropriate native viewer.
  ///
  /// If [docType] is omitted, the type is inferred from the file extension.
  /// Throws [ArgumentError] if the extension is unrecognised and [docType]
  /// is not supplied.
  ///
  /// Returns `true` when the native viewer was launched successfully.
  Future<bool> openDocument(String filePath, {DocType? docType}) {
    final resolvedType = docType ?? DocType.fromPath(filePath);
    return UniversalDockitPlatform.instance.openDocument(
      filePath: filePath,
      docType: resolvedType,
    );
  }

  /// Returns the current platform version string (useful for diagnostics).
  Future<String?> getPlatformVersion() =>
      UniversalDockitPlatform.instance.getPlatformVersion();
}
