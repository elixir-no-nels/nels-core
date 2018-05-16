package no.nels.tsd;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jcraft.jsch.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public final class TsdSessionManager {
    private static TsdSessionManager manager;

    public static TsdSessionManager getManager() {
        if (manager == null) {
            manager = new TsdSessionManager();
        }
        return manager;
    }

    private Table<String, String, Session> sessionTable;

    private TsdSessionManager() {
        this.sessionTable = HashBasedTable.create();
    }

    public ChannelSftp openChannel(String userName, String reference) throws JSchException, TsdException{
        if (this.sessionTable.contains(userName, reference) && this.sessionTable.get(userName, reference).isConnected()) {
            ChannelSftp channelSftp = (ChannelSftp) this.sessionTable.get(userName, reference).openChannel("sftp");
            channelSftp.setBulkRequests(128);
            channelSftp.setInputStream(new ByteArrayInputStream(new byte[32768]));
            channelSftp.setOutputStream(new ByteArrayOutputStream(32768));
            channelSftp.connect();
            return channelSftp;
        }
        throw new TsdException("Session doesn't exist any longer, please reconnect with Tsd.");
    }

    public void removeTsdSession(String userName, String reference) {
        Session session = this.sessionTable.remove(userName, reference);
        if (session.isConnected()) {
            session.disconnect();
        }
    }

    public String createTsdSession(String userName, String password, String otc, String host, int port) throws JSchException {
        JSch jSch = new JSch();
        Session session = jSch.getSession(userName, host, port);
        JSch.setConfig("StrictHostKeyChecking", "no");
        session.setUserInfo(new TsdUserInfo(password, otc));
        session.connect();
        String uuid = UUID.randomUUID().toString();
        if (this.sessionTable.containsRow(userName)) {
            this.sessionTable.row(userName).clear();
            this.sessionTable.row(userName).put(uuid, session);
        } else {
            this.sessionTable.put(userName, uuid, session);
        }
        return uuid;
    }

    private class TsdUserInfo implements UserInfo, UIKeyboardInteractive {
        private String password;
        private String otc;

        public TsdUserInfo(String password, String otc) {
            this.password = password;
            this.otc = otc;
        }

        @Override
        public String[] promptKeyboardInteractive(String destination, String name,
                                                  String instruction, String[] prompt, boolean[] echo) {
            if (prompt[0].contains("Password")) {
                return new String[]{password};
            } else {
                return new String[]{otc};
            }
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptPassword(String s) {
            return false;
        }

        @Override
        public boolean promptPassphrase(String s) {
            return false;
        }

        @Override
        public boolean promptYesNo(String s) {
            return false;
        }

        @Override
        public void showMessage(String s) {
        }
    }
}
