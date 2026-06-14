#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint universal_dockit.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'universal_dockit'
  s.version          = '1.0.0'
  s.summary          = 'Native document viewer for Flutter — PDF, Office, ODF, TXT, CSV, RTF.'
  s.description      = <<-DESC
    A Flutter plugin that renders documents natively on Android and iOS.
    Supports PDF (PDFKit), XLSX (CoreXLSX), Office via QuickLook, plain
    text, CSV, and RTF — all without commercial dependencies.
  DESC
  s.homepage         = 'https://github.com/example/universal_dockit'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files     = 'universal_dockit/Sources/universal_dockit/**/*.swift'
  s.dependency 'Flutter'
  s.platform         = :ios, '13.0'
  s.swift_version    = '5.7'

  # ── Open-source CocoaPods dependency ─────────────────────────────────────
  # CoreXLSX (Apache 2.0) — pure-Swift XLSX parser
  # https://github.com/CoreOffice/CoreXLSX
  s.dependency 'CoreXLSX', '~> 0.14'

  # ZIPFoundation (MIT) — pure-Swift ZIP library for EPUB/CBZ extraction
  s.dependency 'ZIPFoundation', '~> 0.9'

  # ── Built-in Apple frameworks (no cost, no third-party code) ─────────────
  #   PDFKit    — PDF rendering     (iOS 11+)
  #   QuickLook — Office/ODF preview (iOS 12+)
  #   WebKit    — WKWebView          (iOS 8+)  [reserved for future use]
  s.frameworks = 'PDFKit', 'QuickLook'

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386'
  }
end
