import 'dart:io';
import 'dart:ui';

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
        brightness: Brightness.dark,
        scaffoldBackgroundColor: Colors.transparent,
        fontFamily: 'Poppins',
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
    ('PDF', 'pdf'),
    ('DOC', 'doc'),
    ('DOCX', 'docx'),
    ('XLS', 'xls'),
    ('XLSX', 'xlsx'),
    ('PPT', 'ppt'),
    ('PPTX', 'pptx'),
    ('TXT', 'txt'),
    ('CSV', 'csv'),
    ('RTF', 'rtf'),
    ('ODT', 'odt'),
    ('ODS', 'ods'),
    ('ODP', 'odp'),
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
          _status =
              '❌ Could not access file. Try again or use a local file copy.';
        });
        return;
      }

      final success = await _dockit.openDocument(path);

      setState(() {
        _status = success
            ? '✅ Opened ${file.name}'
            : '❌ Failed to open document';
      });
    } on ArgumentError catch (e) {
      setState(() => _status = '❌ ${e.message}');
    } on PlatformException catch (e) {
      setState(() => _status = '❌ ${e.message ?? e.code}');
    } catch (e) {
      setState(() => _status = '❌ Error: $e');
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
      setState(() => _status = '❌ Error: $e');
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
      setState(() => _status = '❌ Error: $e');
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text(
          'Universal Dockit',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w600,
            letterSpacing: -0.5,
          ),
        ),
        centerTitle: true,
        flexibleSpace: ClipRect(
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
            child: Container(color: Colors.black.withValues(alpha: 0.3)),
          ),
        ),
      ),
      body: Stack(
        children: [
          Container(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  Color(0xFF0F0C29),
                  Color(0xFF302B63),
                  Color(0xFF24243E),
                ],
              ),
            ),
            child: SafeArea(
              child: FadeTransition(
                opacity: _fadeAnimation,
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(24, 80, 24, 24),
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
          ),
          if (_loading) _buildLoadingOverlay(),
        ],
      ),
    );
  }

  Widget _buildLoadingOverlay() {
    return Container(
      color: Colors.black.withValues(alpha: 0.45),
      child: Center(
        child: ClipRRect(
          borderRadius: BorderRadius.circular(20),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 28),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: Colors.white.withValues(alpha: 0.2)),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const SizedBox(
                    height: 44,
                    width: 44,
                    child: CircularProgressIndicator(
                      strokeWidth: 3.5,
                      valueColor:
                          AlwaysStoppedAnimation<Color>(Color(0xFFE94560)),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Opening document...',
                    style: TextStyle(
                      color: Colors.white.withValues(alpha: 0.9),
                      fontSize: 15,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
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
            color: Colors.white.withValues(alpha: 0.7),
            fontSize: 13,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: _quickFormats.map((entry) {
            final label = entry.$1;
            final ext = entry.$2;
            return ActionChip(
              label: Text(label),
              onPressed: _loading ? null : () => _pickFormat(ext),
              backgroundColor: Colors.white.withValues(alpha: 0.1),
              labelStyle: const TextStyle(
                color: Colors.white,
                fontSize: 12,
                fontWeight: FontWeight.w600,
              ),
              side: BorderSide(color: Colors.white.withValues(alpha: 0.15)),
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
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  const Color(0xFFE94560).withValues(alpha: 0.8),
                  const Color(0xFF533483).withValues(alpha: 0.8),
                ],
              ),
              shape: BoxShape.circle,
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFFE94560).withValues(alpha: 0.3),
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
    final isSuccess = _status!.startsWith('✅');
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            isSuccess ? const Color(0xFF21BA45) : const Color(0xFFE94560),
            isSuccess ? const Color(0xFF1B9A3A) : const Color(0xFFC73A52),
          ],
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(
            isSuccess ? Icons.check_circle_rounded : Icons.error_rounded,
            color: Colors.white,
            size: 28,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              _status!,
              style: const TextStyle(
                color: Colors.white,
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
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [
                Colors.white.withValues(alpha: 0.1),
                Colors.white.withValues(alpha: 0.05),
              ],
            ),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: Colors.white.withValues(alpha: 0.2)),
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
                        color: Colors.white,
                        fontSize: 16,
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
                        color: Colors.white.withValues(alpha: 0.7),
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _getFileIcon(String? extension) {
    IconData iconData;
    Color color;

    switch (extension?.toLowerCase()) {
      case 'pdf':
        iconData = Icons.picture_as_pdf_rounded;
        color = const Color(0xFFE94560);
      case 'doc':
      case 'docx':
        iconData = Icons.description_rounded;
        color = const Color(0xFF2196F3);
      case 'xls':
      case 'xlsx':
        iconData = Icons.table_chart_rounded;
        color = const Color(0xFF4CAF50);
      case 'ppt':
      case 'pptx':
        iconData = Icons.slideshow_rounded;
        color = const Color(0xFFFF9800);
      default:
        iconData = Icons.insert_drive_file_rounded;
        color = Colors.white70;
    }

    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [color.withValues(alpha: 0.2), color.withValues(alpha: 0.1)],
        ),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Icon(iconData, color: color, size: 32),
    );
  }

  Widget _buildActionButton() {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        gradient: const LinearGradient(
          colors: [Color(0xFFE94560), Color(0xFFC73A52)],
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFFE94560).withValues(alpha: 0.4),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: _loading ? null : _pickAnyDocument,
          borderRadius: BorderRadius.circular(20),
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 18),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: const [
                Icon(Icons.folder_open_rounded, color: Colors.white, size: 28),
                SizedBox(width: 12),
                Text(
                  'Browse All Documents',
                  style: TextStyle(
                    fontSize: 18,
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
