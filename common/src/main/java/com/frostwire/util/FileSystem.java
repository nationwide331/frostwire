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

package com.frostwire.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * This interface is to provide a limited functionality
 * abstraction layer in case you need to deal with highly
 * restricted environments (like Android).
 *
 * @author gubatron
 * @author aldenml
 */
public interface FileSystem {

    String getName(File file);

    File getParentFile(File file);

    boolean isDirectory(File file);

    boolean isFile(File file);

    long lastModified(File file);

    long length(File file);

    boolean canRead(File file);

    boolean canWrite(File file);

    boolean delete(File file);

    boolean exists(File file);

    File[] listFiles(File file);

    boolean renameTo(File file, String displayName);

    boolean mkdirs(File file);

    void write(File file, byte[] data) throws IOException;

    FileSystem DEFAULT = new FileSystem() {

        @Override
        public String getName(File file) {
            return file.getName();
        }

        @Override
        public File getParentFile(File file) {
            return file.getParentFile();
        }

        @Override
        public boolean isDirectory(File file) {
            return file.isDirectory();
        }

        @Override
        public boolean isFile(File file) {
            return file.isFile();
        }

        @Override
        public long lastModified(File file) {
            return file.lastModified();
        }

        @Override
        public long length(File file) {
            return file.length();
        }

        @Override
        public boolean canRead(File file) {
            return file.canRead();
        }

        @Override
        public boolean canWrite(File file) {
            return file.canWrite();
        }

        @Override
        public boolean delete(File file) {
            return file.delete();
        }

        @Override
        public boolean exists(File file) {
            return file.exists();
        }

        @Override
        public File[] listFiles(File file) {
            return file.listFiles();
        }

        @Override
        public boolean renameTo(File file, String displayName) {
            File parent = file.getParentFile();
            File target = parent != null ? new File(parent, displayName) : new File(displayName);
            return file.renameTo(target);
        }

        @Override
        public boolean mkdirs(File file) {
            return file.mkdirs();
        }

        @Override
        public void write(File file, byte[] data) throws IOException {
            FileUtils.writeByteArrayToFile(file, data);
        }
    };
}
