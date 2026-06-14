import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'doc_type.dart';
import 'universal_dockit_method_channel.dart';

/// Abstract platform interface for UniversalDockit.
///
/// Platform implementations must extend this class (not implement it) and
/// register themselves as the [instance] singleton.
abstract class UniversalDockitPlatform extends PlatformInterface {
  UniversalDockitPlatform() : super(token: _token);

  static final Object _token = Object();

  static UniversalDockitPlatform _instance = MethodChannelUniversalDockit();

  /// The default instance, backed by [MethodChannelUniversalDockit].
  static UniversalDockitPlatform get instance => _instance;

  static set instance(UniversalDockitPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  // ---------------------------------------------------------------------------
  // API
  // ---------------------------------------------------------------------------

  /// Opens the document at [filePath] using the native viewer.
  ///
  /// [docType] identifies the format so the native layer can pick the correct
  /// renderer. Use [DocType.fromPath] to infer the type automatically.
  ///
  /// Returns `true` on success.
  Future<bool> openDocument({
    required String filePath,
    required DocType docType,
  }) {
    throw UnimplementedError('openDocument() has not been implemented.');
  }

  /// Returns the platform version string (used for diagnostics).
  Future<String?> getPlatformVersion() {
    throw UnimplementedError('getPlatformVersion() has not been implemented.');
  }
}
