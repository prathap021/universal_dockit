import 'dart:io';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:universal_dockit/universal_dockit.dart';

void main() {
  runApp(const UniversalDockitExampleApp());
}

class UniversalDockitExampleApp extends StatelessWidget {
  const UniversalDockitExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Universal Dockit',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.light,
      ),
      home: const DocumentPickerScreen(),
    );
  }
}

class DocumentPickerScreen extends StatefulWidget {
  const DocumentPickerScreen({super.key});

  @override
  State<DocumentPickerScreen> createState() => _DocumentPickerScreenState();
}

class _DocumentPickerScreenState extends State<DocumentPickerScreen> {
  final _dockit = UniversalDockit();
  String? _status;
  bool _loading = false;
  bool _enableSearch = true;
  bool _enableZoom = true;
  bool _startInDarkMode = false;

  Future<String?> _resolveFilePath(PlatformFile file) async {
    final path = file.path;
    if (path != null && File(path).existsSync()) {
      return path;
    }

    final bytes = file.bytes;
    if (bytes == null) return null;

    final dir = await getTemporaryDirectory();
    final ext = file.extension ?? 'bin';
    final name = file.name.isNotEmpty ? file.name : 'document.$ext';
    final dest = File('${dir.path}/$name');
    await dest.writeAsBytes(bytes, flush: true);
    return dest.path;
  }

  Future<void> _openFile(PlatformFile file) async {
    setState(() {
      _loading = true;
      _status = null;
    });

    try {
      final path = await _resolveFilePath(file);
      if (path == null) {
        setState(() => _status = 'Could not access file.');
        return;
      }

      // Current Android viewer flow: only method-channel enabled features appear.
      final success = await _dockit.openDocument(
        path,
        features: DocumentFeatures(
          search: _enableSearch,
          zoomInOut: _enableZoom,
          darkMode: _startInDarkMode,
        ),
      );

      setState(() {
        _status = success
            ? 'Successfully opened ${file.name}'
            : 'Failed to open document';
      });
    } on ArgumentError catch (e) {
      setState(() => _status = 'Error: ${e.message}');
    } on PlatformException catch (e) {
      setState(() => _status = 'Error: ${e.message ?? e.code}');
    } catch (e) {
      setState(() => _status = 'Error: $e');
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _pickAndOpenDocument() async {
    setState(() => _loading = true);

    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: [
          'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
          'txt', 'csv', 'rtf', 'odt', 'ods', 'odp'
        ],
        allowMultiple: false,
        withData: false,
      );

      if (result == null || result.files.isEmpty) {
        setState(() => _loading = false);
        return;
      }

      await _openFile(result.files.first);
    } catch (e) {
      setState(() => _status = 'Error: $e');
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Universal Dockit')),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  children: [
                    const Text(
                      'Viewer Feature Flags',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                    SwitchListTile(
                      title: const Text('Enable Search'),
                      value: _enableSearch,
                      onChanged: (value) => setState(() => _enableSearch = value),
                    ),
                    SwitchListTile(
                      title: const Text('Enable Zoom In/Out'),
                      value: _enableZoom,
                      onChanged: (value) => setState(() => _enableZoom = value),
                    ),
                    SwitchListTile(
                      title: const Text('Start In Dark Mode'),
                      value: _startInDarkMode,
                      onChanged: (value) =>
                          setState(() => _startInDarkMode = value),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),
            if (_status != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 24),
                child: Text(
                  _status!,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: _status!.startsWith('Successfully')
                        ? Colors.green
                        : Colors.red,
                    fontSize: 16,
                  ),
                ),
              ),
            Center(
              child: ElevatedButton(
                onPressed: _loading ? null : _pickAndOpenDocument,
                child: const Text('Pick & Open Document'),
              ),
            ),
            if (_loading)
              const Padding(
                padding: EdgeInsets.only(top: 24),
                child: CircularProgressIndicator(),
              ),
          ],
        ),
      ),
    );
  }
}