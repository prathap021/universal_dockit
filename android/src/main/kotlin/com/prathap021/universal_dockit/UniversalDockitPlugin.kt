package com.prathap021.universal_dockit

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** UniversalDockitPlugin */
class UniversalDockitPlugin :
    FlutterPlugin,
    MethodCallHandler,
    ActivityAware {

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var activity: Activity? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "universal_dockit")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "openDocument" -> {
                val filePath = call.argument<String>("filePath")
                val docType = call.argument<String>("docType")
                val features = call.argument<Map<String, Any>>("features")

                if (filePath == null || docType == null) {
                    result.error("INVALID_ARGS", "filePath and docType are required", null)
                    return
                }

                var resolvedPath = filePath
                if (resolvedPath.startsWith("file://")) {
                    resolvedPath = resolvedPath.substring(7)
                } else if (resolvedPath.startsWith("content://")) {
                    try {
                        val uri = android.net.Uri.parse(resolvedPath)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val tempFile = java.io.File(context.cacheDir, "temp_doc_${System.currentTimeMillis()}.$docType")
                            val outputStream = java.io.FileOutputStream(tempFile)
                            inputStream.copyTo(outputStream)
                            inputStream.close()
                            outputStream.close()
                            resolvedPath = tempFile.absolutePath
                        }
                    } catch (e: Exception) {
                        result.error("URI_RESOLVE_ERROR", "Failed to resolve content URI: ${e.message}", null)
                        return
                    }
                }

                if (!java.io.File(resolvedPath).exists()) {
                    result.error("FILE_NOT_FOUND", "File not found: $resolvedPath", null)
                    return
                }

                try {
                        if (activity == null) {
                            result.error("NO_ACTIVITY", "No activity available", null)
                            return
                        }
                        val intent = Intent(activity, DocumentViewerActivity::class.java).apply {
                            putExtra(DocumentViewerActivity.EXTRA_FILE_PATH, resolvedPath)
                            putExtra(DocumentViewerActivity.EXTRA_DOC_TYPE, docType)
                            if (features != null) {
                                putExtra("features", HashMap(features))
                            }
                        }
                        activity!!.startActivity(intent)
                        result.success(true)
                } catch (e: Exception) {
                    result.error("OPEN_ERROR", "Failed to open document: ${e.message}", null)
                }
            }

            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
