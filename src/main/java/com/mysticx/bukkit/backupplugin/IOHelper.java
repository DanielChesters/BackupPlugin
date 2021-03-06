package com.mysticx.bukkit.backupplugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Some helper methods for I/O handling
 *
 * @author MysticX
 *
 */
public final class IOHelper {

    // singleton
    private static IOHelper instance = new IOHelper();

    /**
     *
     * @return an instance of IOHelper
     */
    public static IOHelper getInstance() {
        return instance;
    }

    /**
     * Default constructor
     */
    private IOHelper() {
    }

    // Buffer size for zipping
    private static final int BUFFER = 8192;

    /**
     * Zips given directory
     *
     * @param directory
     * @param zip
     * @throws IOException
     */
    public void zipDirectory(File directory, File zip) throws IOException {
        zip.getParentFile().mkdirs();
        if (!zip.exists()) {
            zip.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(zip);
        ZipOutputStream zos = new ZipOutputStream(fileOutputStream);
        try {
            zip(directory, directory, zos);
        } catch (IOException ioex) {
            throw ioex;
        } finally {
            zos.close();
            fileOutputStream.close();
        }
    }

    private void zip(File directory, File base, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        byte[] buffer = new byte[BUFFER];
        int read = 0;
        for (int i = 0, n = files.length; i < n; i++) {
            if (files[i].isDirectory()) {
                zip(files[i], base, zos);
            } else {
                FileInputStream fileInputStream = new FileInputStream(files[i]);
                try {
                    ZipEntry entry = new ZipEntry(files[i].getPath().substring(base.getPath().length() + 1));
                    zos.putNextEntry(entry);
                    while (-1 != (read = fileInputStream.read(buffer))) {
                        zos.write(buffer, 0, read);
                    }
                } catch (IOException ioex) {
                    throw ioex;
                } finally {
                    fileInputStream.close();
                }

            }
        }
    }

    // streams
    private BufferedInputStream in = null;
    private BufferedOutputStream out = null;

    /**
     * Copies a directory
     *
     * @param source
     * @param target
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void copyDir(File source, File target) throws IOException {

        File[] files = source.listFiles();
        target.mkdirs();
        for (File file : files) {
            if (file.isDirectory()) {
                copyDir(file, new File(target.getAbsolutePath() + System.getProperty("file.separator") + file.getName()));
            } else {
                copyFile(file, new File(target.getAbsolutePath() + System.getProperty("file.separator") + file.getName()));
            }
        }
    }

    /**
     * Copies a file
     *
     * @param file
     * @param target
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void copyFile(File file, File target) throws IOException {
        in = new BufferedInputStream(new FileInputStream(file));
        out = new BufferedOutputStream(new FileOutputStream(target, true));
        int bytes = 0;
        while ((bytes = in.read()) != -1) {
            out.write(bytes);
        }
        in.close();
        out.close();
    }

    /**
     * Copies a file
     *
     * @param file
     * @param target
     * @param append
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void copyFile(File file, File target, boolean append) throws IOException {
        MessageHandler.log(Level.FINEST, "copy operation: " + file + " to " + target + " (" + append + ")");
        in = new BufferedInputStream(new FileInputStream(file));
        out = new BufferedOutputStream(new FileOutputStream(target, append));
        int bytes = 0;
        while ((bytes = in.read()) != -1) {
            out.write(bytes);
        }
        in.close();
        out.close();
    }

    /**
     * Delete given directory
     *
     * @param path
     * @return true, if successful
     */
    public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    /**
     * Delete given file
     *
     * @param path
     * @return true, if successful
     */
    public boolean deleteFile(File path) {
        MessageHandler.log(Level.FINE, "deleting file: " + path);
        return (path.delete());
    }

    /**
     * Delete old files in given directory
     *
     * @param path
     * @param world
     *            - the worldname
     * @param number
     *            of backups to keep
     * @return true, if successful
     */
    public boolean deleteOldFiles(File path, String world, int number) {
        boolean success = true;

        if (path.exists()) {
            File[] files = path.listFiles(new BackupFilter(world));

            if (files.length > number) {
                // too many files

                // sort array
                Arrays.sort(files, new FileComparator());

                for (int i = 0; i < files.length - number; i++) {
                    MessageHandler.log(Level.INFO, "deleting old backup file: " + files[i]);
                    if (!files[i].delete()) {
                        success = false;
                    }
                }

            }
        }
        return success;
    }

    private static class FileComparator implements Comparator<File> {

        @Override
        public int compare(File o1, File o2) {
            if (o1.lastModified() == o2.lastModified()) {
                return 0;
            }
            if (o1.lastModified() < o2.lastModified()) {
                return -1;
            }
            else {
                return 1;
            }

        }
    }
}

class BackupFilter implements FilenameFilter {

    private String world;

    public BackupFilter(String world) {
        super();
        this.world = world;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.startsWith(world);
    }
}
