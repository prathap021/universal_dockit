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
        scaffoldBackgroundColor: Colors.white,
        fontFamily: 'Poppins',
        colorScheme: const ColorScheme.light(
          primary: Color(0xFF6366F1),
          secondary: Color(0xFF8B5CF6),
          surface: Colors.white,
        ),
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

class _DocumentPickerScreenState extends State<DocumentPickerScreen>
    with SingleTickerProviderStateMixin {
  final _dockit = UniversalDockit();
  String? _status;
  bool _loading = false;
  PlatformFile? _pickedFile;
  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;

  static const _quickFormats = [
    ('PDF', 'pdf', Icons.picture_as_pdf),
    ('DOC', 'doc', Icons.description),
    ('DOCX', 'docx', Icons.description),
    ('XLS', 'xls', Icons.table_chart),
    ('XLSX', 'xlsx', Icons.table_chart),
    ('PPT', 'ppt', Icons.slideshow),
    ('PPTX', 'pptx', Icons.slideshow),
    ('TXT', 'txt', Icons.text_fields),
    ('CSV', 'csv', Icons.grid_on),
    ('RTF', 'rtf', Icons.note),
    ('ODT', 'odt', Icons.edit_document),
    ('ODS', 'ods', Icons.table_rows),
    ('ODP', 'odp', Icons.slideshow),
  ];

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _fadeAnimation = CurvedAnimation(
      parent: _animationController,
      curve: Curves.easeOutCubic,
    );
    _animationController.forward();
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  Future<String?> _resolveFilePath(PlatformFile file) async {
    final path = file.path;
    if (path != null && File(path).existsSync()) {
      return path;
    }

    final bytes = file.bytes;
    if (bytes == null) {
      return null;
    }

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
      _pickedFile = file;
    });

    try {
      final path = await _resolveFilePath(file);
      if (path == null) {
        setState(() {
          _status = 'Could not access file. Try again or use a local file copy.';
        });
        return;
      }

      final success = await _dockit.openDocument(path);

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

  Future<void> _pickFormat(String extension) async {
    setState(() {
      _loading = true;
      _status = null;
      _pickedFile = null;
    });

    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: [extension],
        allowMultiple: false,
        withData: true,
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

  Future<void> _pickAnyDocument() async {
    setState(() {
      _loading = true;
      _status = null;
      _pickedFile = null;
    });

    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: _quickFormats.map((e) => e.$2).toList(),
        allowMultiple: false,
        withData: true,
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
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        title: const Text(
          'Universal Dockit',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            letterSpacing: -0.5,
            color: Color(0xFF1F2937),
          ),
        ),
        centerTitle: true,
      ),
      body: Stack(
        children: [
          SafeArea(
            child: FadeTransition(
              opacity: _fadeAnimation,
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
                child: Column(
                  children: [
                    _buildHeroIcon(),
                    const SizedBox(height: 32),
                    if (_status != null) ...[
                      _buildStatusCard(),
                      const SizedBox(height: 16),
                    ],
                    if (_pickedFile != null) ...[
                      _buildSelectedFileCard(),
                      const SizedBox(height: 24),
                    ],
                    _buildQuickFormatGrid(),
                    const SizedBox(height: 24),
                    _buildActionButton(),
                  ],
                ),
              ),
            ),
          ),
          if (_loading) _buildLoadingOverlay(),
        ],
      ),
    );
  }

  Widget _buildLoadingOverlay() {
    return Container(
      color: Colors.black.withValues(alpha: 0.3),
      child: Center(
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 28),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(24),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.1),
                blurRadius: 20,
                offset: const Offset(0, 10),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(
                height: 44,
                width: 44,
                child: CircularProgressIndicator(
                  strokeWidth: 3.5,
                  valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF6366F1)),
                ),
              ),
              const SizedBox(height: 16),
              Text(
                'Opening document...',
                style: TextStyle(
                  color: Colors.grey.shade700,
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildQuickFormatGrid() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Quick open by format',
          style: TextStyle(
            color: Colors.grey.shade600,
            fontSize: 13,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: _quickFormats.map((entry) {
            final label = entry.$1;
            final ext = entry.$2;
            final icon = entry.$3;
            return FilterChip(
              label: Text(label),
              selected: false,
              onSelected: _loading ? null : (_) => _pickFormat(ext),
              avatar: Icon(icon, size: 18, color: const Color(0xFF6366F1)),
              backgroundColor: Colors.grey.shade50,
              selectedColor: const Color(0xFF6366F1).withValues(alpha: 0.1),
              labelStyle: TextStyle(
                color: Colors.grey.shade700,
                fontSize: 13,
                fontWeight: FontWeight.w500,
              ),
              side: BorderSide(color: Colors.grey.shade200),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildHeroIcon() {
    return TweenAnimationBuilder(
      tween: Tween<double>(begin: 0, end: 1),
      duration: const Duration(milliseconds: 1000),
      builder: (context, value, child) {
        return Transform.scale(
          scale: value,
          child: Container(
            height: 100,
            width: 100,
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [Color(0xFF6366F1), Color(0xFF8B5CF6)],
              ),
              shape: BoxShape.circle,
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF6366F1).withValues(alpha: 0.3),
                  blurRadius: 30,
                  spreadRadius: 5,
                ),
              ],
            ),
            child: const Icon(
              Icons.description_rounded,
              size: 50,
              color: Colors.white,
            ),
          ),
        );
      },
    );
  }

  Widget _buildStatusCard() {
    final isSuccess = _status!.startsWith('Successfully');
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isSuccess ? Colors.green.shade50 : Colors.red.shade50,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isSuccess ? Colors.green.shade200 : Colors.red.shade200,
        ),
      ),
      child: Row(
        children: [
          Icon(
            isSuccess ? Icons.check_circle_rounded : Icons.error_rounded,
            color: isSuccess ? Colors.green.shade700 : Colors.red.shade700,
            size: 24,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              _status!,
              style: TextStyle(
                color: isSuccess ? Colors.green.shade800 : Colors.red.shade800,
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSelectedFileCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        children: [
          _getFileIcon(_pickedFile!.extension),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _pickedFile!.name,
                  style: const TextStyle(
                    color: Color(0xFF1F2937),
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Text(
                  '${_pickedFile!.extension?.toUpperCase() ?? 'UNKNOWN'} • '
                  '${(_pickedFile!.size / 1024).toStringAsFixed(1)} KB',
                  style: TextStyle(
                    color: Colors.grey.shade600,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Colors.grey.shade200,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              Icons.check_circle,
              color: Colors.green.shade600,
              size: 20,
            ),
          ),
        ],
      ),
    );
  }

  Widget _getFileIcon(String? extension) {
    IconData iconData;
    Color color;

    switch (extension?.toLowerCase()) {
      case 'pdf':
        iconData = Icons.picture_as_pdf_rounded;
        color = const Color(0xFFE94560); // Red
      case 'doc':
      case 'docx':
      case 'rtf':
      case 'odt':
        iconData = Icons.description_rounded;
        color = const Color(0xFF2196F3); // Blue
      case 'xls':
      case 'xlsx':
      case 'csv':
      case 'ods':
        iconData = Icons.table_chart_rounded;
        color = const Color(0xFF4CAF50); // Green
      case 'ppt':
      case 'pptx':
      case 'odp':
        iconData = Icons.slideshow_rounded;
        color = const Color(0xFFFF9800); // Orange
      case 'txt':
        iconData = Icons.text_fields_rounded;
        color = Colors.grey.shade600; // Gray
      default:
        iconData = Icons.insert_drive_file_rounded;
        color = Colors.grey.shade600;
    }

    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Icon(iconData, color: color, size: 28),
    );
  }

  Widget _buildActionButton() {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: const LinearGradient(
          colors: [Color(0xFF6366F1), Color(0xFF8B5CF6)],
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF6366F1).withValues(alpha: 0.3),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: _loading ? null : _pickAnyDocument,
          borderRadius: BorderRadius.circular(16),
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: const [
                Icon(Icons.folder_open_rounded, color: Colors.white, size: 24),
                SizedBox(width: 12),
                Text(
                  'Browse All Documents',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}