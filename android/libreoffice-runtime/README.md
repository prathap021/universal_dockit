# LibreOffice Android Runtime

Word, Excel, and PowerPoint rendering on Android uses **LibreOfficeKit** when native
files are present in this directory.

## Install (required once)

1. Download a LibreOffice or Collabora Office APK:
   - [LibreOffice on F-Droid](https://f-droid.org/packages/org.documentfoundation.libreoffice/)
   - Collabora Office from Play Store

2. Run the setup script from the repo root:

```bash
chmod +x android/scripts/setup_libreoffice_runtime.sh
./android/scripts/setup_libreoffice_runtime.sh /path/to/libreoffice.apk
```

3. Rebuild the example app:

```bash
cd example && flutter run
```

## What gets installed

- `jniLibs/<abi>/*.so` — `liblo-native-code.so` and NSS dependencies (~80 MB per ABI)
- `assets/` — LibreOffice program data required at runtime

## Behavior

- **With runtime:** `.doc/.docx`, `.xls/.xlsx`, `.ppt/.pptx` → LibreOffice converts to PDF → PDFView
- **Without runtime:** falls back to Apache POI renderers (HTML or PDF)

## License

LibreOffice is MPL 2.0. Bundling its binaries requires MPL compliance (attribution + source offer).
