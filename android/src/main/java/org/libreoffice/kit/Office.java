/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.kit;

import java.nio.ByteBuffer;

public class Office {
    private final ByteBuffer handle;
    private MessageCallback messageCallback = null;

    public Office(ByteBuffer handle) {
        this.handle = handle;
        bindMessageCallback();
    }

    private native void bindMessageCallback();

    public native String getError();

    private native ByteBuffer documentLoadNative(String url);

    public Document documentLoad(String url) {
        ByteBuffer documentHandle = documentLoadNative(url);
        if (documentHandle != null) {
            return new Document(documentHandle);
        }
        return null;
    }

    public native void destroy();

    public native void destroyAndExit();

    public native void setDocumentPassword(String url, String pwd);

    public native void setOptionalFeatures(long options);

    public void setMessageCallback(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    private void messageRetrievedLOKit(int signalNumber, String payload) {
        if (messageCallback != null) {
            messageCallback.messageRetrieved(signalNumber, payload);
        }
    }

    public interface MessageCallback {
        void messageRetrieved(int signalNumber, String payload);
    }
}
