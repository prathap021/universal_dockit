import 'package:flutter/material.dart';
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

// ---------------------------------------------------------------------------
// Document Picker Screen
// ---------------------------------------------------------------------------

class DocumentPickerScreen extends StatefulWidget {
  const DocumentPickerScreen({super.key});

  @override
  State<DocumentPickerScreen> createState() => _DocumentPickerScreenState();
}

class _DocumentPickerScreenState extends State<DocumentPickerScreen> {
  final _dockit = UniversalDockit();
  String? _status;
  bool _loading = false;

  // Sample file paths — in a real app these would come from file_picker,
  // downloads folder, or any other source.
  static const _sampleDocuments = [
    _DocumentEntry(
      label: 'PDF Report',
      icon: Icons.picture_as_pdf,
      color: Color(0xFFE94560),
      path: '/storage/emulated/0/Download/sample.pdf',
      type: DocType.pdf,
    ),
    _DocumentEntry(
      label: 'Word Document (.docx)',
      icon: Icons.description,
      color: Color(0xFF2185D0),
      path: '/storage/emulated/0/Download/sample.docx',
      type: DocType.docx,
    ),
    _DocumentEntry(
      label: 'Word Document (.doc)',
      icon: Icons.description,
      color: Color(0xFF2185D0),
      path: '/storage/emulated/0/Download/sample.doc',
      type: DocType.doc,
    ),
    _DocumentEntry(
      label: 'Excel Spreadsheet (.xlsx)',
      icon: Icons.table_chart,
      color: Color(0xFF21BA45),
      path: '/storage/emulated/0/Download/sample.xlsx',
      type: DocType.xlsx,
    ),
    _DocumentEntry(
      label: 'Excel Spreadsheet (.xls)',
      icon: Icons.table_chart,
      color: Color(0xFF21BA45),
      path: '/storage/emulated/0/Download/sample.xls',
      type: DocType.xls,
    ),
    _DocumentEntry(
      label: 'PowerPoint (.pptx)',
      icon: Icons.slideshow,
      color: Color(0xFFE67E22),
      path: '/storage/emulated/0/Download/sample.pptx',
      type: DocType.pptx,
    ),
    _DocumentEntry(
      label: 'PowerPoint (.ppt)',
      icon: Icons.slideshow,
      color: Color(0xFFE67E22),
      path: '/storage/emulated/0/Download/sample.ppt',
      type: DocType.ppt,
    ),
    _DocumentEntry(
      label: 'Plain Text (.txt)',
      icon: Icons.text_snippet,
      color: Color(0xFF9B59B6),
      path: '/storage/emulated/0/Download/sample.txt',
      type: DocType.txt,
    ),
    _DocumentEntry(
      label: 'CSV Spreadsheet (.csv)',
      icon: Icons.grid_on,
      color: Color(0xFF1ABC9C),
      path: '/storage/emulated/0/Download/sample.csv',
      type: DocType.csv,
    ),
    _DocumentEntry(
      label: 'Rich Text Format (.rtf)',
      icon: Icons.article,
      color: Color(0xFFE91E8C),
      path: '/storage/emulated/0/Download/sample.rtf',
      type: DocType.rtf,
    ),
    _DocumentEntry(
      label: 'OpenDocument Text (.odt)',
      icon: Icons.description_outlined,
      color: Color(0xFF3498DB),
      path: '/storage/emulated/0/Download/sample.odt',
      type: DocType.odt,
    ),
    _DocumentEntry(
      label: 'OpenDocument Spreadsheet (.ods)',
      icon: Icons.table_view,
      color: Color(0xFF27AE60),
      path: '/storage/emulated/0/Download/sample.ods',
      type: DocType.ods,
    ),
    _DocumentEntry(
      label: 'OpenDocument Presentation (.odp)',
      icon: Icons.present_to_all,
      color: Color(0xFFF39C12),
      path: '/storage/emulated/0/Download/sample.odp',
      type: DocType.odp,
    ),
  ];

  Future<void> _openDocument(_DocumentEntry entry) async {
    setState(() {
      _loading = true;
      _status = null;
    });

    try {
      final success = await _dockit.openDocument(
        entry.path,
        docType: entry.type,
      );
      setState(() {
        _status = success ? '✅ Opened ${entry.label}' : '❌ Failed to open';
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
      body: CustomScrollView(
        slivers: [
          _buildAppBar(),
          if (_status != null) _buildStatusBanner(),
          _buildGrid(),
        ],
      ),
      floatingActionButton: _loading
          ? const FloatingActionButton(
              onPressed: null,
              backgroundColor: Color(0xFF16213E),
              child: CircularProgressIndicator(
                color: Color(0xFFE94560),
                strokeWidth: 2,
              ),
            )
          : null,
    );
  }

  Widget _buildAppBar() {
    return SliverAppBar(
      expandedHeight: 180,
      pinned: true,
      backgroundColor: const Color(0xFF16213E),
      flexibleSpace: FlexibleSpaceBar(
        title: const Text(
          'Universal Dockit',
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.w700,
            fontSize: 20,
          ),
        ),
        background: Container(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFF16213E), Color(0xFF0F3460)],
            ),
          ),
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const SizedBox(height: 24),
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: const LinearGradient(
                      colors: [Color(0xFFE94560), Color(0xFF9B1B33)],
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFFE94560).withOpacity(0.4),
                        blurRadius: 20,
                        spreadRadius: 4,
                      ),
                    ],
                  ),
                  child: const Icon(
                    Icons.folder_open_rounded,
                    color: Colors.white,
                    size: 36,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Native Document Viewer',
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.6),
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildStatusBanner() {
    return SliverToBoxAdapter(
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        margin: const EdgeInsets.fromLTRB(16, 12, 16, 0),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: const Color(0xFF16213E),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: _status!.startsWith('✅')
                ? const Color(0xFF21BA45).withOpacity(0.5)
                : const Color(0xFFE94560).withOpacity(0.5),
          ),
        ),
        child: Text(
          _status!,
          style: TextStyle(
            color: _status!.startsWith('✅')
                ? const Color(0xFF21BA45)
                : const Color(0xFFE94560),
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }

  Widget _buildGrid() {
    return SliverPadding(
      padding: const EdgeInsets.all(16),
      sliver: SliverGrid(
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          mainAxisSpacing: 12,
          crossAxisSpacing: 12,
          childAspectRatio: 1.1,
        ),
        delegate: SliverChildBuilderDelegate(
          (context, index) => _DocumentCard(
            entry: _sampleDocuments[index],
            onTap: () => _openDocument(_sampleDocuments[index]),
          ),
          childCount: _sampleDocuments.length,
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Document Card Widget
// ---------------------------------------------------------------------------

class _DocumentCard extends StatefulWidget {
  const _DocumentCard({required this.entry, required this.onTap});

  final _DocumentEntry entry;
  final VoidCallback onTap;

  @override
  State<_DocumentCard> createState() => _DocumentCardState();
}

class _DocumentCardState extends State<_DocumentCard>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;
  late final Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 150),
      lowerBound: 0.95,
      upperBound: 1.0,
      value: 1.0,
    );
    _scale = _ctrl;
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  void _onTapDown(_) => _ctrl.reverse();
  void _onTapUp(_) => _ctrl.forward();
  void _onTapCancel() => _ctrl.forward();

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: _onTapDown,
      onTapUp: _onTapUp,
      onTapCancel: _onTapCancel,
      onTap: widget.onTap,
      child: ScaleTransition(
        scale: _scale,
        child: Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                const Color(0xFF16213E),
                widget.entry.color.withOpacity(0.08),
              ],
            ),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: widget.entry.color.withOpacity(0.25),
              width: 1.5,
            ),
            boxShadow: [
              BoxShadow(
                color: widget.entry.color.withOpacity(0.1),
                blurRadius: 12,
                spreadRadius: 0,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: widget.entry.color.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    widget.entry.icon,
                    color: widget.entry.color,
                    size: 24,
                  ),
                ),
                const Spacer(),
                Text(
                  widget.entry.label,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                    height: 1.3,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Text(
                  widget.entry.type.name.toUpperCase(),
                  style: TextStyle(
                    color: widget.entry.color,
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.8,
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

// ---------------------------------------------------------------------------
// Data Model
// ---------------------------------------------------------------------------

class _DocumentEntry {
  const _DocumentEntry({
    required this.label,
    required this.icon,
    required this.color,
    required this.path,
    required this.type,
  });

  final String label;
  final IconData icon;
  final Color color;
  final String path;
  final DocType type;
}
