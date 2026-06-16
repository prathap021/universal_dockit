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

public class Document {
    public static final int PART_MODE_SLIDE = 0;
    public static final int PART_MODE_NOTES = 1;

    public static final int DOCTYPE_TEXT = 0;
    public static final int DOCTYPE_SPREADSHEET = 1;
    public static final int DOCTYPE_PRESENTATION = 2;
    public static final int DOCTYPE_DRAWING = 3;
    public static final int DOCTYPE_OTHER = 4;

    public static final int MOUSE_EVENT_BUTTON_DOWN = 0;
    public static final int MOUSE_EVENT_BUTTON_UP = 1;
    public static final int MOUSE_EVENT_MOVE = 2;

    public static final int KEY_EVENT_PRESS = 0;
    public static final int KEY_EVENT_RELEASE = 1;

    public static final long LOK_FEATURE_DOCUMENT_PASSWORD = 1;
    public static final long LOK_FEATURE_DOCUMENT_PASSWORD_TO_MODIFY = (1 << 1);
    public static final long LOK_FEATURE_PART_IN_INVALIDATION_CALLBACK = (1 << 2);
    public static final long LOK_FEATURE_NO_TILED_ANNOTATIONS = (1 << 3);

    private final ByteBuffer handle;
    private MessageCallback messageCallback = null;

    public Document(ByteBuffer handle) {
        this.handle = handle;
        bindMessageCallback();
    }

    public void setMessageCallback(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    private void messageRetrieved(int signalNumber, String payload) {
        if (messageCallback != null) {
            messageCallback.messageRetrieved(signalNumber, payload);
        }
    }

    private native void bindMessageCallback();

    public native void destroy();

    public native int getPart();

    public native void setPart(int partIndex);

    public native int getParts();

    public native String getPartName(int partIndex);

    public native void setPartMode(int partMode);

    public native String getPartPageRectangles();

    public native long getDocumentHeight();

    public native long getDocumentWidth();

    private native int getDocumentTypeNative();

    public native void setClientZoom(int nTilePixelWidth, int nTilePixelHeight, int nTileTwipWidth, int nTileTwipHeight);

    public native void saveAs(String url, String format, String options);

    private native void paintTileNative(
        ByteBuffer buffer,
        int canvasWidth,
        int canvasHeight,
        int tilePositionX,
        int tilePositionY,
        int tileWidth,
        int tileHeight
    );

    public int getDocumentType() {
        return getDocumentTypeNative();
    }

    public void paintTile(
        ByteBuffer buffer,
        int canvasWidth,
        int canvasHeight,
        int tilePositionX,
        int tilePositionY,
        int tileWidth,
        int tileHeight
    ) {
        paintTileNative(
            buffer,
            canvasWidth,
            canvasHeight,
            tilePositionX,
            tilePositionY,
            tileWidth,
            tileHeight
        );
    }

    public native void initializeForRendering();

    public native void postKeyEvent(int type, int charCode, int keyCode);

    public native void postMouseEvent(int type, int x, int y, int count, int button, int modifier);

    public native void postUnoCommand(String command, String arguments, boolean notifyWhenFinished);

    public native void setTextSelection(int type, int x, int y);

    public native void setGraphicSelection(int type, int x, int y);

    public native String getTextSelection(String mimeType);

    public native boolean paste(String mimeType, String data);

    public native void resetSelection();

    public native String getCommandValues(String command);

    public interface MessageCallback {
        void messageRetrieved(int signalNumber, String payload);
    }
}
