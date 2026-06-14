import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
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
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFE94560),
          brightness: Brightness.dark,
        ),
        scaffoldBackgroundColor: const Color(0xFF1A1A2E),
        fontFamily: 'Roboto',
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
  PlatformFile? _pickedFile;

  Future<void> _pickAndOpenDocument() async {
    setState(() {
      _loading = true;
      _status = null;
      _pickedFile = null;
    });

    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: [
          'pdf',
          'doc',
          'docx',
          'xls',
          'xlsx',
          'ppt',
          'pptx',
          'txt',
          'csv',
          'rtf',
          'odt',
          'ods',
          'odp',
        ],
        allowMultiple: false,
      );

      if (result == null || result.files.isEmpty) {
        setState(() => _loading = false);
        return;
      }

      final file = result.files.first;
      setState(() => _pickedFile = file);

      if (file.path == null) {
        setState(() {
          _status = '❌ Could not get file path (possibly content URI)';
          _loading = false;
        });
        return;
      }

      final success = await _dockit.openDocument(file.path!);

      setState(() {
        _status = success
            ? '✅ Opened: ${file.name}'
            : '❌ Failed to open document';
      });
    } on ArgumentError catch (e) {
      setState(() => _status = '❌ ${e.message}');
    } catch (e) {
      setState(() => _status = '❌ Error: $e');
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1A1A2E),
      appBar: AppBar(
        backgroundColor: const Color(0xFF16213E),
        title: const Text('Universal Dockit'),
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            const SizedBox(height: 40),

            // Pick Button
            FilledButton.icon(
              onPressed: _loading ? null : _pickAndOpenDocument,
              icon: const Icon(Icons.folder_open_rounded, size: 28),
              label: const Text(
                'Pick Document from Device',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
              ),
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(70),
                backgroundColor: const Color(0xFFE94560),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
            ),

            const SizedBox(height: 32),

            // Status Banner
            if (_status != null)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFF16213E),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: _status!.startsWith('✅')
                        ? const Color(0xFF21BA45).withValues(alpha: 0.5)
                        : const Color(0xFFE94560).withValues(alpha: 0.5),
                  ),
                ),
                child: Text(
                  _status!,
                  style: TextStyle(
                    color: _status!.startsWith('✅')
                        ? const Color(0xFF21BA45)
                        : const Color(0xFFE94560),
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),

            const SizedBox(height: 40),

            // Selected File Info
            if (_pickedFile != null)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: const Color(0xFF16213E),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: Colors.white12),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Selected File',
                      style: TextStyle(
                        color: Colors.white54,
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _pickedFile!.name,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${_pickedFile!.extension?.toUpperCase() ?? 'UNKNOWN'} • '
                      '${(_pickedFile!.size / 1024).toStringAsFixed(1)} KB',
                      style: const TextStyle(color: Colors.white54),
                    ),
                  ],
                ),
              ),

            const Spacer(),

            if (_loading)
              const CircularProgressIndicator(color: Color(0xFFE94560)),
          ],
        ),
      ),
    );
  }
}
