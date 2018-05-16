package no.norstore.storebioinfo.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import no.norstore.storebioinfo.Config;
import no.norstore.storebioinfo.constants.ConfigName;

import java.io.*;
import java.nio.file.FileSystems;

public final class SftpConnection implements Closeable {
    private static final String ALPHA_NUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private Session session;
    private String sshKeyFile;

    public SftpConnection(final String host, final String userName, final String sshKey) throws JSchException, FileNotFoundException, UnsupportedEncodingException {
        try {
            JSch jSch = new JSch();
            session = jSch.getSession(userName, host, 22);
            JSch.setConfig("StrictHostKeyChecking", "no");
            sshKeyFile = createSshKeyFile(sshKey);
            jSch.addIdentity(sshKeyFile);
            session.connect();
        } catch (JSchException | FileNotFoundException | UnsupportedEncodingException e) {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            if (sshKeyFile != null) {
                deleteSshKeyFile(sshKeyFile);
            }
            throw e;
        }
    }

    public ChannelSftp openSftpChannel() throws JSchException {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.setBulkRequests(128);
        channelSftp.setInputStream(new ByteArrayInputStream(new byte[32768]));
        channelSftp.setOutputStream(new ByteArrayOutputStream(32768));
        channelSftp.connect();
        return channelSftp;
    }

    @Override
    public void close() throws IOException {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        if (sshKeyFile != null) {
            deleteSshKeyFile(sshKeyFile);
        }
    }

    private String createSshKeyFile(String sshKey) throws FileNotFoundException, UnsupportedEncodingException{
        String absoluteFileName = Config.valueOf(ConfigName.SSH_KEYS_FOLDER) + FileSystems.getDefault().getSeparator() + getAlphaNumeric(10);
        try (PrintWriter writer = new PrintWriter(absoluteFileName, "UTF-8")) {
            writer.println(sshKey);
            return absoluteFileName;
        }
    }

    private void deleteSshKeyFile(String sshKeyFile) {
        File temp = new File(sshKeyFile);
        if (temp.exists()) {
            temp.delete();
        }
    }

    private String getAlphaNumeric(int len) {
        StringBuffer sb = new StringBuffer(len);
        for (int i = 0; i < len; i++) {
            int ndx = (int) (Math.random() * ALPHA_NUM.length());
            sb.append(ALPHA_NUM.charAt(ndx));
        }
        return sb.toString();
    }
}
