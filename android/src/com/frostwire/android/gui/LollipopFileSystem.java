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
import android.os.storage.StorageManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import com.frostwire.logging.Logger;
import com.frostwire.util.FileSystem;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 */
public final class LollipopFileSystem implements FileSystem {

    private static final Logger LOG = Logger.getLogger(LollipopFileSystem.class);

    private final Context context;

    public LollipopFileSystem(Context context) {
        this.context = context;
    }

    @Override
    public String getName(File file) {
        return file.getName();
    }

    @Override
    public File getParentFile(File file) {
        return file.getAbsoluteFile().getParentFile();
    }

    @Override
    public boolean isDirectory(File file) {
        DocumentFile f = getDocumentFile(context, file, false);
        return f != null ? f.isDirectory() : false;
    }

    @Override
    public boolean isFile(File file) {
        DocumentFile f = getDocumentFile(context, file, false);
        return f != null ? f.isFile() : false;
    }

    @Override
    public long lastModified(File file) {
        DocumentFile f = getDocumentFile(context, file, false);
        return f != null ? f.lastModified() : 0;
    }

    @Override
    public long length(File file) {
        DocumentFile f = getDocumentFile(context, file, false);
        return f != null ? f.length() : 0;
    }

    @Override
    public boolean canRead(File file) {
        DocumentFile f = getDocumentFile(context, file, false);
        return f != null ? f.canRead() : false;
    }

    @Override
    public boolean canWrite(File file) {
        DocumentFile f = getDocumentFile(context, file, false);
        return f != null ? f.canWrite() : false;
    }

    @Override
    public boolean delete(File file) {
        DocumentFile f = getDocumentFile(context, file, false);
        return f != null ? f.delete() : false;
    }

    @Override
    public boolean exists(File file) {
        DocumentFile f = getDocumentFile(context, file, false);
        return f != null ? f.exists() : false;
    }

    @Override
    public File[] listFiles(File file) {
        DocumentFile f = getDocumentFile(context, file, false);
        if (f != null) {
            DocumentFile[] arr = f.listFiles();
            File[] r = new File[arr.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = new File(getPath(context, arr[i].getUri()));
            }
            return r;
        } else {
            return new File[0];
        }
    }

    @Override
    public boolean renameTo(File file, String displayName) {
        DocumentFile f = getDocumentFile(context, file, false);
        if (f != null) {
            return f.renameTo(displayName);
        } else {
            return false;
        }
    }

    @Override
    public boolean mkdirs(File file) {
        DocumentFile f = getDocumentFile(context, file, true);
        return f != null;
    }

    @Override
    public void write(File file, byte[] data) throws IOException {
        OutputStream out = null;
        try {
            out = openWrite(file);
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    @Override
    public OutputStream openWrite(File file) throws IOException {
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

            return context.getContentResolver().openOutputStream(f.getUri());
        } else {
            return new FileOutputStream(file);
        }
    }

    public int openFD(File file) throws IOException {
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
            ParcelFileDescriptor fd = cr.openFileDescriptor(uri, "rw");
            return fd.detachFd();
        } else {
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE);
            return fd.detachFd();
        }
    }

    public Uri getDocumentUri(File file) {
        return getDocumentUri(context, file);
    }

    private static Uri getDocumentUri(Context context, File file) {
        String baseFolder = getExtSdCardFolder(file, context);
        if (baseFolder == null) {
            return null;
        }

        String volumeId = getVolumeId(context, baseFolder);
        if (volumeId == null) {
            return null;
        }

        String fullPath = file.getAbsolutePath();
        String relativePath = baseFolder.length() < fullPath.length() ? fullPath.substring(baseFolder.length() + 1) : "";

        relativePath = relativePath.replace("/", "%2F");
        String uri = "content://com.android.externalstorage.documents/tree/" + volumeId + "%3A" + relativePath;

        return Uri.parse(uri);
    }

    private static String getPath(Context ctx, final Uri treeUri) {
        if (treeUri == null) {
            return null;
        }

        StorageManager mStorageManager = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);

        String volumePath = getVolumePath(mStorageManager, getVolumeIdFromTreeUri(treeUri));
        if (volumePath == null) {
            return File.separator;
        }
        if (volumePath.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length() - 1);
        }

        String documentPath = getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }

        String path = volumePath;

        if (documentPath.length() > 0) {
            if (documentPath.startsWith(File.separator)) {
                path = volumePath + documentPath;
            } else {
                path = volumePath + File.separator + documentPath;
            }
        }

        return path;
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

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
     * null is returned.
     */
    private static String getExtSdCardFolder(final File file, Context context) {
        String[] extSdPaths = getExtSdCardPaths(context);
        try {
            for (int i = 0; i < extSdPaths.length; i++) {
                if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
                    return extSdPaths[i];
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    private static String[] getExtSdCardPaths(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : ContextCompat.getExternalFilesDirs(context, "external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index >= 0) {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                } else {
                    LOG.warn("ext sd card path wrong: " + file.getAbsolutePath());
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1");
        return paths.toArray(new String[0]);
    }

    private static String getVolumeId(Context context, final String volumePath) {
        try {
            StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);

                if (path != null) {
                    if (path.equals(volumePath)) {
                        return (String) getUuid.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String getVolumePath(StorageManager mStorageManager, final String volumeId) {
        try {
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary && "primary".equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid.equals(volumeId)) {
                        return (String) getPath.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");

        if (split.length > 0) {
            return split[0];
        } else {
            return null;
        }
    }

    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) {
            return split[1];
        } else {
            return File.separator;
        }
    }

    private static String getTreeDocumentId(Uri documentUri) {
        final List<String> paths = documentUri.getPathSegments();
        if (paths.size() >= 2 && "tree".equals(paths.get(0))) {
            return paths.get(1);
        }
        throw new IllegalArgumentException("Invalid URI: " + documentUri);
    }
}
