package no.norstore.storebioinfo.utils;

import com.ice.tar.TarEntry;
import com.ice.tar.TarOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for compressing files usign either zip or tar.
 */
public final class CompressUtils {

    /**
     * Size of buffer to use when creating a zip file.
     */
    final static int BYTE_BUFFER = 8096;

    /**
     * Class logger.
     */
    private static Logger logger = LoggerFactory.getLogger(CompressUtils.class);

    public static List<ArchiveEntry> compressFolderUsingTar(
            String folderToCompress, String tarFile) throws IOException {

        File folder = new File(folderToCompress);

        GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(
                tarFile));

        TarOutputStream tarArchive = new TarOutputStream(gzipOut);

        int chopIndex = folderToCompress.length();

        List<ArchiveEntry> archiveEntries = new LinkedList<ArchiveEntry>();

        gzipFolder(folder, tarArchive, archiveEntries, chopIndex, 0);

        tarArchive.close();

        return archiveEntries;

    }

    private static void gzipFolder(File folder, TarOutputStream tarArchive,
                                   List<ArchiveEntry> archiveFolders, int chopIndex, int folderLevel)
            throws IOException {
        File f = folder;

        if (f.isFile()) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(f);
                String entryName = f.getPath().substring(chopIndex + 1);

                ArchiveEntry lastEntry = archiveFolders.get(archiveFolders
                        .size() - 1);

                TarEntry entry = new TarEntry(lastEntry.getName() + "/"
                        + entryName);
                entry.setSize(f.length());
                tarArchive.putNextEntry(entry);

                BufferedInputStream buffIn = new BufferedInputStream(fis,
                        BYTE_BUFFER);

                byte[] data = new byte[BYTE_BUFFER];

                int count = 0;
                while ((count = buffIn.read(data, 0, BYTE_BUFFER)) != -1) {
                    tarArchive.write(data, 0, count);
                }

                tarArchive.closeEntry();
                tarArchive.flush();
                buffIn.close();
                // fis.close();

                lastEntry.getSubEntries().add(
                        lastEntry.getName() + "/" + entryName + ","
                                + f.length());
                long newSize = lastEntry.getSize() + f.length();
                lastEntry.setSize(newSize);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw e;
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            }

        }
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (folderLevel == 0) {
                ArchiveEntry archiveFolder = new ArchiveEntry(f.getName(),
                        f.length(), f.getName(), "");
                archiveFolders.add(archiveFolder);
            }
            folderLevel++;
            for (File fileInDir : files) {
                gzipFolder(fileInDir, tarArchive, archiveFolders, chopIndex,
                        folderLevel);
            }
        }

    }

}

