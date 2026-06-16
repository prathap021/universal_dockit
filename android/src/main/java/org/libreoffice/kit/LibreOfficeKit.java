/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.kit;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.util.Log;

import java.nio.ByteBuffer;

public final class LibreOfficeKit {
    private static final String LOGTAG = LibreOfficeKit.class.getSimpleName();
    private static AssetManager mgr;
    private static boolean initializeDone = false;

    private LibreOfficeKit() {
    }

    public static void initializeLibrary() {
    }

    private static native boolean initializeNative(String dataDir, String cacheDir, String apkFile, AssetManager mgr);

    public static native ByteBuffer getLibreOfficeKitHandle();

    public static native void putenv(String string);

    public static native void redirectStdio(boolean state);

    public static synchronized void init(Context context) {
        if (initializeDone) {
            return;
        }

        Context app = context.getApplicationContext();
        mgr = app.getResources().getAssets();

        ApplicationInfo applicationInfo = app.getApplicationInfo();
        String dataDir = applicationInfo.dataDir;
        Log.i(LOGTAG, "Initializing LibreOfficeKit, dataDir=" + dataDir);

        redirectStdio(true);

        String cacheDir = app.getCacheDir().getAbsolutePath();
        String apkFile = applicationInfo.sourceDir;

        if (!initializeNative(dataDir, cacheDir, apkFile, mgr)) {
            Log.e(LOGTAG, "Initialize native failed!");
            throw new IllegalStateException("LibreOfficeKit native initialization failed");
        }
        initializeDone = true;
    }

    static {
        NativeLibLoader.load();
    }
}

class NativeLibLoader {
    private static boolean done = false;

    protected static synchronized void load() {
        if (done) {
            return;
        }
        System.loadLibrary("nspr4");
        System.loadLibrary("plds4");
        System.loadLibrary("plc4");
        System.loadLibrary("nssutil3");
        System.loadLibrary("freebl3");
        System.loadLibrary("sqlite3");
        System.loadLibrary("softokn3");
        System.loadLibrary("nss3");
        System.loadLibrary("nssckbi");
        System.loadLibrary("nssdbm3");
        System.loadLibrary("smime3");
        System.loadLibrary("ssl3");

        System.loadLibrary("c++_shared");
        System.loadLibrary("lo-native-code");
        done = true;
    }
}
