import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'doc_type.dart';
import 'universal_dockit_platform_interface.dart';

/// Method-channel implementation of [UniversalDockitPlatform].
///
/// Serialises [DocType] to its string identifier and sends
/// [filePath] + [docType] arguments to the native handler via the
/// `universal_dockit` method channel.
class MethodChannelUniversalDockit extends UniversalDockitPlatform {
  /// The method channel name must match the one declared in native code.
  @visibleForTesting
  final methodChannel = const MethodChannel('universal_dockit');

  @override
  Future<bool> openDocument({
    required String filePath,
    required DocType docType,
  }) async {
    final result = await methodChannel.invokeMethod<bool>(
      'openDocument',
      <String, String>{
        'filePath': filePath,
        'docType': docType.identifier,
      },
    );
    return result ?? false;
  }

  @override
  Future<String?> getPlatformVersion() async {
    return methodChannel.invokeMethod<String>('getPlatformVersion');
  }
}
