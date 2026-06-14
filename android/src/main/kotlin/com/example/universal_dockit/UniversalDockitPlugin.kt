package com.example.universal_dockit

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

                if (filePath == null || docType == null) {
                    result.error("INVALID_ARGS", "filePath and docType are required", null)
                    return
                }

                val currentActivity = activity
                if (currentActivity == null) {
                    result.error("NO_ACTIVITY", "No activity available", null)
                    return
                }

                try {
                    val intent = Intent(currentActivity, DocumentViewerActivity::class.java).apply {
                        putExtra(DocumentViewerActivity.EXTRA_FILE_PATH, filePath)
                        putExtra(DocumentViewerActivity.EXTRA_DOC_TYPE, docType)
                    }
                    currentActivity.startActivity(intent)
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
