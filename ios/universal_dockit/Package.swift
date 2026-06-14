// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "universal_dockit",
    platforms: [
        .iOS("13.0")
    ],
    products: [
        .library(name: "universal-dockit", targets: ["universal_dockit"])
    ],
    dependencies: [
        .package(name: "FlutterFramework", path: "../FlutterFramework"),
        // CoreXLSX — open-source Swift library for reading .xlsx files
        // https://github.com/CoreOffice/CoreXLSX  (Apache 2.0)
        .package(
            url: "https://github.com/CoreOffice/CoreXLSX.git",
            from: "0.14.2"
        ),
    ],
    targets: [
        .target(
            name: "universal_dockit",
            dependencies: [
                .product(name: "FlutterFramework", package: "FlutterFramework"),
                .product(name: "CoreXLSX", package: "CoreXLSX"),
            ],
            resources: [
                // Uncomment to include privacy manifest:
                // .process("PrivacyInfo.xcprivacy"),
            ]
        )
    ]
)
