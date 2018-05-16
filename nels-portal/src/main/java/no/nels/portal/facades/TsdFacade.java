package no.nels.portal.facades;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import no.nels.client.UserApi;
import no.nels.portal.utilities.JSFUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Vector;

public final class TsdFacade {
    public static void transferTsdFolderToNels(ChannelSftp channelSftp, String tsdPath, String nelsPath, String folderName)
            throws Exception{
        UserApi.createFolder(
                SecurityFacade.getLoggedInUser().getId(),
                SecurityFacade.getUserBeingViewed().getId(),
                nelsPath, folderName);
        Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(tsdPath);

        if (entries != null) {
            for (ChannelSftp.LsEntry entry : entries) {
                if (!entry.getFilename().startsWith(".")) {
                    if (entry.getAttrs().isDir()) {
                        transferTsdFolderToNels(channelSftp,
                                tsdPath + FileSystems.getDefault().getSeparator() + entry.getFilename(),
                                nelsPath + FileSystems.getDefault().getSeparator() + folderName, entry.getFilename());
                    } else {
                        try (InputStream tsdFile = channelSftp.get(tsdPath + FileSystems.getDefault().getSeparator() + entry.getFilename())) {
                            UserApi.createFile(SecurityFacade.getLoggedInUser().getId(),
                                    SecurityFacade.getUserBeingViewed().getId(),
                                    nelsPath + FileSystems.getDefault().getSeparator() + folderName, entry.getFilename(), tsdFile);

                        }
                    }
                }
            }
        }
    }





    public static void transferNelsFolderToTsd(ChannelSftp channelSftp, String tsdPath, String nelsPath) throws SftpException, IOException{
        Files.walkFileTree(FileSystems.getDefault().getPath(JSFUtils.getLocalFilePath(nelsPath)), new TraverseFiles(tsdPath, JSFUtils.getLocalFilePath(nelsPath),channelSftp));
    }

    private static class TraverseFiles extends SimpleFileVisitor<Path> {
        private String tsdPath;
        private String nelsPath;
        private ChannelSftp channelSftp;

        public TraverseFiles(String tsdPath, String nelsPath, ChannelSftp channelSftp) {
            this.tsdPath = tsdPath;
            this.nelsPath = nelsPath;
            this.channelSftp = channelSftp;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String newTsdPath;
            if (dir.equals(Paths.get(nelsPath))) {
                newTsdPath = this.tsdPath + FileSystems.getDefault().getSeparator() + dir.getFileName();
            } else {
                newTsdPath = this.tsdPath + dir.toString().substring(Paths.get(nelsPath).getParent().toString().length());
            }
            SftpATTRS sftpATTRS = null;
            try {
                sftpATTRS = channelSftp.lstat(newTsdPath);
            } catch (SftpException e) {}

            if (sftpATTRS != null) {
                return FileVisitResult.CONTINUE;
            } else {
                try {
                    channelSftp.mkdir(newTsdPath);
                    return FileVisitResult.CONTINUE;
                } catch (SftpException e) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            try (InputStream nelsFile = new FileInputStream(file.toFile())) {
                String tsdFilePath = this.tsdPath + file.toString().substring(Paths.get(nelsPath).getParent().toString().length());
                channelSftp.put(nelsFile, tsdFilePath);
                return FileVisitResult.CONTINUE;
            } catch (SftpException e) {
                return FileVisitResult.CONTINUE;
            }
        }
    }
}
