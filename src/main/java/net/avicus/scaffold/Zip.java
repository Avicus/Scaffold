package net.avicus.scaffold;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.File;
import java.io.IOException;

public class Zip {
    public static void create(File folder, final File zipFile) throws IOException, ZipException {
        ZipFile zip = new ZipFile(zipFile);
        ZipParameters params = new ZipParameters();
        File[] contents = folder.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory())
                    zip.addFolder(file, params);
                else
                    zip.addFile(file, params);
            }
        }
    }

    public static void extract(File zipFile, File directory) throws IOException, ZipException {
        ZipFile zip = new ZipFile(zipFile);
        zip.extractAll(directory.getAbsolutePath());
    }
}
