import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:universal_dockit/universal_dockit.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  runApp(UniversalDockitExampleApp(prefs: prefs));
}

class UniversalDockitExampleApp extends StatefulWidget {
  final SharedPreferences prefs;
  const UniversalDockitExampleApp({super.key, required this.prefs});

  @override
  State<UniversalDockitExampleApp> createState() => _AppState();
}

class _AppState extends State<UniversalDockitExampleApp> {
  bool _isDarkMode = false;

  @override
  void initState() {
    super.initState();
    _isDarkMode = widget.prefs.getBool('darkMode') ?? false;
  }

  void toggleTheme() {
    setState(() {
      _isDarkMode = !_isDarkMode;
      widget.prefs.setBool('darkMode', _isDarkMode);
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Universal Dockit',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.light,
        scaffoldBackgroundColor: Colors.grey.shade50,
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF6366F1), brightness: Brightness.light),
      ),
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF111827),
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF8B5CF6), brightness: Brightness.dark),
      ),
      themeMode: _isDarkMode ? ThemeMode.dark : ThemeMode.light,
      home: DocumentDashboard(
        prefs: widget.prefs,
        toggleTheme: toggleTheme,
        isDarkMode: _isDarkMode,
      ),
    );
  }
}

class DocumentRecord {
  final String path;
  final String name;
  final int size;
  final DateTime accessedAt;
  bool isFavorite;

  DocumentRecord({
    required this.path,
    required this.name,
    required this.size,
    required this.accessedAt,
    this.isFavorite = false,
  });

  Map<String, dynamic> toJson() => {
        'path': path,
        'name': name,
        'size': size,
        'accessedAt': accessedAt.toIso8601String(),
        'isFavorite': isFavorite,
      };

  factory DocumentRecord.fromJson(Map<String, dynamic> json) => DocumentRecord(
        path: json['path'],
        name: json['name'],
        size: json['size'],
        accessedAt: DateTime.parse(json['accessedAt']),
        isFavorite: json['isFavorite'] ?? false,
      );
}

class DocumentDashboard extends StatefulWidget {
  final SharedPreferences prefs;
  final VoidCallback toggleTheme;
  final bool isDarkMode;

  const DocumentDashboard({
    super.key,
    required this.prefs,
    required this.toggleTheme,
    required this.isDarkMode,
  });

  @override
  State<DocumentDashboard> createState() => _DashboardState();
}

class _DashboardState extends State<DocumentDashboard> {
  final _dockit = UniversalDockit();
  List<DocumentRecord> _documents = [];
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadDocuments();
    // Enable full screen mode
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
  }

  void _loadDocuments() {
    final docsJson = widget.prefs.getStringList('documents') ?? [];
    setState(() {
      _documents = docsJson.map((e) => DocumentRecord.fromJson(jsonDecode(e))).toList();
      _documents.sort((a, b) => b.accessedAt.compareTo(a.accessedAt));
    });
  }

  void _saveDocuments() {
    final docsJson = _documents.map((e) => jsonEncode(e.toJson())).toList();
    widget.prefs.setStringList('documents', docsJson);
  }

  void _addOrUpdateDocument(String path, String name, int size) {
    final existingIdx = _documents.indexWhere((d) => d.path == path);
    if (existingIdx != -1) {
      final doc = _documents.removeAt(existingIdx);
      _documents.insert(
          0,
          DocumentRecord(
            path: doc.path,
            name: doc.name,
            size: doc.size,
            accessedAt: DateTime.now(),
            isFavorite: doc.isFavorite,
          ));
    } else {
      _documents.insert(
          0,
          DocumentRecord(
            path: path,
            name: name,
            size: size,
            accessedAt: DateTime.now(),
          ));
    }
    _saveDocuments();
    setState(() {});
  }

  void _toggleFavorite(DocumentRecord doc) {
    setState(() {
      doc.isFavorite = !doc.isFavorite;
    });
    _saveDocuments();
  }

  Future<void> _openDocument(String path, String name, int size) async {
    setState(() => _isLoading = true);
    try {
      if (!File(path).existsSync()) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('File not found! It may have been deleted.')));
        _documents.removeWhere((d) => d.path == path);
        _saveDocuments();
        return;
      }
      _addOrUpdateDocument(path, name, size);
      await _dockit.openDocument(path);
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _pickFromLocal() async {
    setState(() => _isLoading = true);
    try {
      final result = await FilePicker.platform.pickFiles(withData: false);
      if (result != null && result.files.isNotEmpty) {
        final file = result.files.first;
        if (file.path != null) {
          await _openDocument(file.path!, file.name, file.size);
        }
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _openFromUrl() async {
    final urlController = TextEditingController();
    final url = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Open from URL'),
        content: TextField(
          controller: urlController,
          decoration: const InputDecoration(hintText: 'https://example.com/document.pdf'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          ElevatedButton(onPressed: () => Navigator.pop(context, urlController.text), child: const Text('Download')),
        ],
      ),
    );

    if (url != null && url.isNotEmpty) {
      setState(() => _isLoading = true);
      try {
        final response = await http.get(Uri.parse(url));
        if (response.statusCode == 200) {
          final dir = await getTemporaryDirectory();
          final uri = Uri.parse(url);
          final filename = uri.pathSegments.isNotEmpty ? uri.pathSegments.last : 'downloaded_file.pdf';
          final file = File('${dir.path}/$filename');
          await file.writeAsBytes(response.bodyBytes);
          await _openDocument(file.path, filename, file.lengthSync());
        } else {
          throw Exception('Failed to download: HTTP ${response.statusCode}');
        }
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error downloading: $e')));
      } finally {
        if (mounted) setState(() => _isLoading = false);
      }
    }
  }

  void _showFileInfo(DocumentRecord doc) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('File Information'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Name: ${doc.name}', style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Text('Size: ${(doc.size / 1024).toStringAsFixed(2)} KB'),
            const SizedBox(height: 8),
            Text('Last Opened: ${doc.accessedAt.toString().split('.')[0]}'),
            const SizedBox(height: 8),
            Text('Path: ${doc.path}', style: const TextStyle(fontSize: 10, color: Colors.grey)),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () {
                Navigator.pop(context);
                Share.shareXFiles([XFile(doc.path)], text: 'Check out this document: ${doc.name}');
              },
              child: const Text('Share')),
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Close')),
        ],
      ),
    );
  }

  Widget _buildDocTile(DocumentRecord doc) {
    final ext = doc.name.split('.').last.toLowerCase();
    return ListTile(
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(color: Theme.of(context).colorScheme.primaryContainer, borderRadius: BorderRadius.circular(8)),
        child: Text(ext.toUpperCase(), style: TextStyle(color: Theme.of(context).colorScheme.primary, fontWeight: FontWeight.bold, fontSize: 12)),
      ),
      title: Text(doc.name, maxLines: 1, overflow: TextOverflow.ellipsis),
      subtitle: Text('${(doc.size / 1024).toStringAsFixed(1)} KB • ${doc.accessedAt.day}/${doc.accessedAt.month}/${doc.accessedAt.year}'),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton(
            icon: Icon(doc.isFavorite ? Icons.star : Icons.star_border, color: doc.isFavorite ? Colors.amber : Colors.grey),
            onPressed: () => _toggleFavorite(doc),
          ),
          IconButton(
            icon: const Icon(Icons.info_outline),
            onPressed: () => _showFileInfo(doc),
          ),
        ],
      ),
      onTap: () => _openDocument(doc.path, doc.name, doc.size),
    );
  }

  @override
  Widget build(BuildContext context) {
    final favorites = _documents.where((d) => d.isFavorite).toList();
    
    return Scaffold(
      appBar: AppBar(
        title: const Text('Universal Opener', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: Icon(widget.isDarkMode ? Icons.light_mode : Icons.dark_mode),
            onPressed: widget.toggleTheme,
          ),
        ],
      ),
      body: Stack(
        children: [
          ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
                      icon: const Icon(Icons.folder),
                      label: const Text('Local Storage'),
                      onPressed: _pickFromLocal,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
                      icon: const Icon(Icons.link),
                      label: const Text('Open URL'),
                      onPressed: _openFromUrl,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              if (favorites.isNotEmpty) ...[
                const Text('Favorites', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                ...favorites.map(_buildDocTile),
                const Divider(height: 32),
              ],
              const Text('Recent Files', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              if (_documents.isEmpty)
                const Padding(
                  padding: EdgeInsets.all(32.0),
                  child: Center(child: Text('No recent documents', style: TextStyle(color: Colors.grey))),
                )
              else
                ..._documents.take(10).map(_buildDocTile),
            ],
          ),
          if (_isLoading)
            Container(
              color: Colors.black54,
              child: const Center(child: CircularProgressIndicator()),
            ),
        ],
      ),
    );
  }
}