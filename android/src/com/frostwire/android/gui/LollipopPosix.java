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

import android.content.Context;
import com.frostwire.jlibtorrent.swig.swig_posix_file_functions;
import com.frostwire.jlibtorrent.swig.swig_posix_stat;
import com.frostwire.logging.Logger;
import com.frostwire.util.FileSystem;

import java.io.File;

/**
 * @author gubatron
 * @author aldenml
 */
public final class LollipopPosix extends swig_posix_file_functions {

    private static final Logger LOG = Logger.getLogger(LollipopPosix.class);

    private final Context context;
    private final FileSystem fs;

    public LollipopPosix(Context context, FileSystem fs) {
        this.context = context;
        this.fs = fs;
    }

    @Override
    public int open(String pathname, int flags, int mode) {
        LOG.info("open: " + pathname);
        File f = new File(pathname);
        try {
            return LollipopFileSystem.openFD(context, f);
        } catch (Throwable e) {
            LOG.error("Can't open file: " + pathname, e);
        }

        return -1;
    }

    @Override
    public int mkdir(String pathname, int mode) {
        LOG.info("mkdir: " + pathname);
        boolean r = fs.mkdirs(new File(pathname));
        return r ? 0 : -1;
    }

    @Override
    public int rename(String oldpath, String newpath) {
        LOG.info("rename: " + oldpath + " -> " + newpath);
        return super.rename(oldpath, newpath);
    }

    @Override
    public int remove(String pathname) {
        LOG.info("remove: " + pathname);
        boolean r = fs.delete(new File(pathname));
        return r ? 0 : -1;
    }

    @Override
    public int stat(String path, swig_posix_stat buf) {
        //LOG.info("stat: " + path);
        File f = new File(path);
        //int S_ISDIR = f.isDirectory() ? 0040000 : 0;
        //int S_IFREG = 0100000;

        //buf.setMode(S_ISDIR | S_IFREG);
        int length = (int) f.length();
        buf.setSize(length);
        int t = Integer.MAX_VALUE;//(int) (fs.lastModified(f) / 1000);
        buf.setAtime(t);
        buf.setMtime(t);
        buf.setCtime(t);

        return 0;
    }
}
