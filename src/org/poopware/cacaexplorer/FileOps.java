// SPDX-License-Identifier: AGPL-3.0-or-later
package org.poopware.cacaexplorer;

import java.io.*;

/** Filesystem operations independent of Android; never silently overwrite. */
public final class FileOps {
    private FileOps() {}
    public static File named(File parent, String name) throws IOException {
        if (name.length() == 0 || name.equals(".") || name.equals("..") ||
            name.indexOf('/') >= 0 || name.indexOf('\0') >= 0)
            throw new IOException("Enter a single file or folder name.");
        return new File(parent, name);
    }
    private static void regular(File f) throws IOException {
        File expected = new File(f.getParentFile().getCanonicalFile(), f.getName());
        if (!expected.equals(f.getCanonicalFile())) throw new IOException("Symbolic links are not supported: " + f);
    }
    public static void copy(File source, File target) throws IOException {
        String from = source.getCanonicalPath(), to = target.getCanonicalPath();
        if (to.equals(from) || to.startsWith(from + File.separator))
            throw new IOException("Cannot copy a folder into itself.");
        regular(source);
        if (target.exists()) throw new IOException("Destination already exists: " + target.getName());
        if (source.isDirectory()) {
            File[] children = source.listFiles();
            if (children == null) throw new IOException("Cannot read " + source);
            if (!target.mkdir()) throw new IOException("Cannot create " + target);
            try {
                for (File child : children) copy(child, new File(target, child.getName()));
            } catch (IOException e) {
                try { delete(target); } catch (IOException cleanup) { /* report original failure */ }
                throw e;
            }
        } else {
            if (!source.isFile()) throw new IOException("Not a regular file: " + source);
            if (!target.createNewFile()) throw new IOException("Destination already exists.");
            boolean complete = false;
            try {
                InputStream in = new FileInputStream(source);
                try {
                    FileOutputStream out = new FileOutputStream(target);
                    try {
                        byte[] buf = new byte[65536]; int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                        out.getFD().sync();
                    } finally { out.close(); }
                } finally { in.close(); }
                complete = true;
            } finally { if (!complete) target.delete(); }
        }
    }
    public static void move(File source, File target) throws IOException {
        regular(source);
        String from = source.getCanonicalPath(), to = target.getCanonicalPath();
        if (to.equals(from) || to.startsWith(from + File.separator)) throw new IOException("Cannot move into itself.");
        if (target.exists()) throw new IOException("Destination already exists.");
        if (!source.renameTo(target)) {
            copy(source, target);
            try { delete(source); } catch (IOException e) {
                throw new IOException("Copied successfully, but source removal failed. Check both locations: " + e.getMessage());
            }
        }
    }
    public static void delete(File f) throws IOException {
        regular(f);
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children == null) throw new IOException("Cannot read " + f);
            for (File child : children) delete(child);
        }
        if (!f.delete()) throw new IOException("Cannot delete " + f);
    }
}
