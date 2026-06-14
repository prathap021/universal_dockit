import 'package:flutter_test/flutter_test.dart';
import 'package:universal_dockit/universal_dockit.dart';
import 'package:universal_dockit/universal_dockit_platform_interface.dart';
import 'package:universal_dockit/universal_dockit_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

/// Mock platform that implements every abstract method on [UniversalDockitPlatform].
class MockUniversalDockitPlatform
    with MockPlatformInterfaceMixin
    implements UniversalDockitPlatform {
  // Tracks the last call to openDocument for assertion use.
  String? lastOpenedFilePath;
  DocType? lastOpenedDocType;

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<bool> openDocument({
    required String filePath,
    required DocType docType,
    DocumentFeatures? features,
  }) async {
    lastOpenedFilePath = filePath;
    lastOpenedDocType = docType;
    return true;
  }
}

void main() {
  final UniversalDockitPlatform initialPlatform = UniversalDockitPlatform.instance;

  test('$MethodChannelUniversalDockit is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelUniversalDockit>());
  });

  test('getPlatformVersion', () async {
    final plugin = UniversalDockit();
    final fakePlatform = MockUniversalDockitPlatform();
    UniversalDockitPlatform.instance = fakePlatform;

    expect(await plugin.getPlatformVersion(), '42');
  });

  test('openDocument returns true and forwards args to platform', () async {
    final plugin = UniversalDockit();
    final fakePlatform = MockUniversalDockitPlatform();
    UniversalDockitPlatform.instance = fakePlatform;

    final result = await plugin.openDocument(
      '/data/user/0/com.example/files/report.pdf',
      docType: DocType.pdf,
    );

    expect(result, isTrue);
    expect(fakePlatform.lastOpenedFilePath,
        '/data/user/0/com.example/files/report.pdf');
    expect(fakePlatform.lastOpenedDocType, DocType.pdf);
  });

  test('openDocument infers docType from file extension', () async {
    final plugin = UniversalDockit();
    final fakePlatform = MockUniversalDockitPlatform();
    UniversalDockitPlatform.instance = fakePlatform;

    await plugin.openDocument('/downloads/sheet.xlsx');

    expect(fakePlatform.lastOpenedDocType, DocType.xlsx);
  });

  test('DocType.fromPath resolves all supported extensions', () {
    final cases = {
      'file.pdf': DocType.pdf,
      'file.doc': DocType.doc,
      'file.docx': DocType.docx,
      'file.xls': DocType.xls,
      'file.xlsx': DocType.xlsx,
      'file.ppt': DocType.ppt,
      'file.pptx': DocType.pptx,
      'file.txt': DocType.txt,
      'file.csv': DocType.csv,
      'file.rtf': DocType.rtf,
      'file.odt': DocType.odt,
      'file.ods': DocType.ods,
      'file.odp': DocType.odp,
    };
    for (final entry in cases.entries) {
      expect(
        DocType.fromPath(entry.key),
        entry.value,
        reason: 'Expected ${entry.value} for ${entry.key}',
      );
    }
  });

  test('DocType.fromPath throws ArgumentError for unknown extension', () {
    expect(() => DocType.fromPath('file.xyz'), throwsArgumentError);
  });
}
