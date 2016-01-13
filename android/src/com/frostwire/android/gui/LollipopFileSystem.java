/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2016, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.provider.DocumentFile;
import com.frostwire.util.FileSystem;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author gubatron
 * @author aldenml
 */
public final class LollipopFileSystem implements FileSystem {

    private final Context context;

    public LollipopFileSystem(Context context) {
        this.context = context;
    }

    @Override
    public RandomAccessFile openRandom(File file, String mode) throws IOException {
        return null;
    }

    private static ParcelFileDescriptor openFD(Context context, File file, String mode) throws IOException {
        if (!("r".equals(mode) || "w".equals(mode) || "rw".equals(mode))) {
            throw new IllegalArgumentException("Only r, w or rw modes supported");
        }
        File parent = file.getParentFile();
        if (parent == null) {
            throw new IOException("Can't create file: " + file);
        }
        DocumentFile f = getDocumentFile(context, parent, true);
        if (f != null) {
            String name = file.getName();
            DocumentFile temp = f.findFile(name);
            if (temp == null) {
                f = f.createFile("application/octet-stream", name);
            } else {
                if (temp.isDirectory()) {
                    throw new IOException("Can't create file: " + file);
                } else {
                    f = temp;
                }
            }

            ContentResolver cr = context.getContentResolver();
            Uri uri = f.getUri();
            return cr.openFileDescriptor(uri, "rw");
        } else {
            int m;
            if ("r".equals(mode)) {
                m = ParcelFileDescriptor.MODE_READ_ONLY;
            } else if ("w".equals(mode)) {
                m = ParcelFileDescriptor.MODE_WRITE_ONLY;
            } else {
                m = ParcelFileDescriptor.MODE_READ_WRITE;
            }

            m = m | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE;

            return ParcelFileDescriptor.open(file, m);
        }
    }

    private static DocumentFile getDocumentFile(Context context, File file, boolean create) {
        String baseFolder = getExtSdCardFolder(file, context);
        if (baseFolder == null) {
            if (create) {
                return file.mkdirs() ? DocumentFile.fromFile(file) : null;
            } else {
                return file.exists() ? DocumentFile.fromFile(file) : null;
            }
        }

        String fullPath = file.getAbsolutePath();
        String relativePath = baseFolder.length() < fullPath.length() ? fullPath.substring(baseFolder.length() + 1) : "";

        String[] segments = relativePath.split("/");

        Uri rootUri = getDocumentUri(context, new File(baseFolder));
        DocumentFile f = DocumentFile.fromTreeUri(context, rootUri);

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            DocumentFile child = f.findFile(segment);
            if (child != null) {
                f = child;
            } else {
                if (create) {
                    if (f.exists() && f.isDirectory() && f.canWrite()) {
                        f = f.createDirectory(segment);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }

        return f;
    }
}
