## 1.0.1

* Replaced Apache POI with `all-documents-reader` SDK on Android for parsing Office documents (Word, Excel, PowerPoint) to resolve dependency conflicts and improve reliability.
* General bug fixes and stability improvements.

## 1.0.0

* Initial release of Universal Dockit!
* Added support for 15 different document types natively across Android and iOS.
* Implemented hardware-accelerated rendering for PDF, Office formats, and E-Books.
* Added `darkMode` boolean flag for dynamic dark mode switching across all document renderers.
* Adopted modern `ViewModel` architecture with `Dispatchers.IO` for background parsing on Android.
