package no.nels.tsd;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.tsd.constants.JsonConstant;
import no.nels.vertx.commons.constants.MqJobStatus;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.util.Vector;
import java.util.function.Consumer;

public final class MqJobFacade {
    private static Logger logger = LoggerFactory.getLogger(MqJobFacade.class);

    private static String SEPARATOR = FileSystems.getDefault().getSeparator();

    /**
     * The json structure of received message should be
     * {"source" : {"reference":"", "user_name":"", "parent_path":"", "files":[], "folders":[]},
     * "destination": {"host":"", "user_name":"", "ssh_key":"", "parent_path":""}}
     *
     *
     */
    public static void pushDataToNels(JsonObject message, Consumer<byte[]> publishFeed, Consumer<byte[]> publishStatus) {
        String parentPathOfSource = message.getJsonObject(JsonConstant.SOURCE).getString(JsonConstant.PARENT_PATH);
        String userNameOfSource = message.getJsonObject(JsonConstant.SOURCE).getString(JsonConstant.USER_NAME);
        String reference = message.getJsonObject(JsonConstant.SOURCE).getString(JsonConstant.REFERENCE);
        JsonArray filesArray = message.getJsonObject(JsonConstant.SOURCE).getJsonArray(JsonConstant.FILES);
        JsonArray foldersArray = message.getJsonObject(JsonConstant.SOURCE).getJsonArray(JsonConstant.FOLDERS);

        String host = message.getJsonObject(JsonConstant.DESTINATION).getString(JsonConstant.HOST);
        String userNameOfDest = message.getJsonObject(JsonConstant.DESTINATION).getString(JsonConstant.USER_NAME);
        String sshKey = message.getJsonObject(JsonConstant.DESTINATION).getString(JsonConstant.SSH_KEY);
        String parentPathOfDest = message.getJsonObject(JsonConstant.DESTINATION).getString(JsonConstant.PARENT_PATH);


        ChannelSftp tsdChannelSftp = null;
        try (SftpConnection connection = new SftpConnection(host, userNameOfDest, sshKey)) {
            ChannelSftp nelsChannelSftp = connection.openSftpChannel();
            tsdChannelSftp = TsdSessionManager.getManager().openChannel(userNameOfSource, reference);

            transferData(publishFeed, publishStatus, parentPathOfSource, filesArray, foldersArray, parentPathOfDest, tsdChannelSftp, nelsChannelSftp);

        } catch (JSchException | FileNotFoundException | UnsupportedEncodingException | TsdException e) {
            logger.error(e);
            publishFailureMessage(publishStatus);
        } catch (IOException e) {
            logger.error(e);
            //TODO
        } finally {
            if (tsdChannelSftp != null && tsdChannelSftp.isConnected()) {
                tsdChannelSftp.disconnect();
            }
        }
    }

    /**
     * The json structure of received message should be
     * {"source" : {"host":"", "user_name":"", "ssh_key":"", "parent_path":"", "files":[], "folders":[]},
     * "destination" : {"parent_path":"", "user_name":"", "reference":""}}
     *
     */
    public static void pullDataFromNels(JsonObject message, Consumer<byte[]> publishFeed, Consumer<byte[]> publishStatus) {
        String host = message.getJsonObject(JsonConstant.SOURCE).getString(JsonConstant.HOST);
        String userNameOfSource = message.getJsonObject(JsonConstant.SOURCE).getString(JsonConstant.USER_NAME);
        String sshKey = message.getJsonObject(JsonConstant.SOURCE).getString(JsonConstant.SSH_KEY);
        String parentPathOfSource = message.getJsonObject(JsonConstant.SOURCE).getString(JsonConstant.PARENT_PATH);
        JsonArray filesArray = message.getJsonObject(JsonConstant.SOURCE).getJsonArray(JsonConstant.FILES);
        JsonArray foldersArray = message.getJsonObject(JsonConstant.SOURCE).getJsonArray(JsonConstant.FOLDERS);

        String parentPathOfDest = message.getJsonObject(JsonConstant.DESTINATION).getString(JsonConstant.PARENT_PATH);
        String userNameOfDest = message.getJsonObject(JsonConstant.DESTINATION).getString(JsonConstant.USER_NAME);
        String reference = message.getJsonObject(JsonConstant.DESTINATION).getString(JsonConstant.REFERENCE);

        ChannelSftp tsdChannelSftp = null;
        try (SftpConnection connection = new SftpConnection(host, userNameOfSource, sshKey)) {
            ChannelSftp nelsChannelSftp = connection.openSftpChannel();
            tsdChannelSftp = TsdSessionManager.getManager().openChannel(userNameOfDest, reference);

            transferData(publishFeed, publishStatus, parentPathOfSource, filesArray, foldersArray, parentPathOfDest, nelsChannelSftp, tsdChannelSftp);

        } catch (JSchException | FileNotFoundException | UnsupportedEncodingException | TsdException e) {
            logger.error(e);
            publishFailureMessage(publishStatus);
        } catch (IOException e) {
            logger.error(e);
            //TODO
        } finally {
            if (tsdChannelSftp != null && tsdChannelSftp.isConnected()) {
                tsdChannelSftp.disconnect();
            }
        }
    }

    private static void transferData(Consumer<byte[]> publishFeed, Consumer<byte[]> publishStatus,
                                     String parentPathOfSource, JsonArray filesArray, JsonArray foldersArray, String parentPathOfDest,
                                     ChannelSftp fromChannelSftp, ChannelSftp toChannelSftp) {
        publishStatus.accept(new JsonObject().put(JsonConstant.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonConstant.COMPLETION, 0.0).encode().getBytes());

        float finishedNumber = 0;
        float totalNumber = filesArray.size() + foldersArray.size();
        StringBuilder stringBuilder = new StringBuilder();
        JsonObject temp = new JsonObject();
        for (Object item : filesArray) {
            try {
                String from = StringUtils.join(parentPathOfSource, SEPARATOR, item.toString());
                String to = StringUtils.join(parentPathOfDest, SEPARATOR, item.toString());
                stringBuilder.append("transfer file from ").append(from).append(" to ").append(to);
                publishFeed.accept(stringBuilder.toString().getBytes());

                transferFile(fromChannelSftp, from, toChannelSftp, to);
                finishedNumber += 1.0;
                publishFeed.accept("successful".getBytes());

            } catch (SftpException e) {
                logger.error(e);
                if (e.getMessage().contains("Permission denied")) {
                    publishFeed.accept("failed - Permission denied".getBytes());
                } else {
                    publishFeed.accept("failed".getBytes());
                }
            }
            stringBuilder.setLength(0);
            temp.put(JsonConstant.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonConstant.COMPLETION, finishedNumber / totalNumber);
            publishStatus.accept(temp.encode().getBytes());
            temp.clear();
        }

        for (Object item : foldersArray) {
            try {
                String from = StringUtils.join(parentPathOfSource, SEPARATOR, item.toString());
                String to = StringUtils.join(parentPathOfDest, SEPARATOR, item.toString());
                stringBuilder.append("transfer folder from ").append(from).append(" to ").append(to);
                publishFeed.accept(stringBuilder.toString().getBytes());

                transferFolder(item.toString(), fromChannelSftp, parentPathOfSource, toChannelSftp, parentPathOfDest);
                finishedNumber += 1.0;
                publishFeed.accept("successful".getBytes());
            } catch (SftpException e) {
                logger.error(e);
                if (e.getMessage().contains("Permission denied")) {
                    publishFeed.accept("failed - Permission denied".getBytes());
                } else {
                    publishFeed.accept("failed".getBytes());
                }
            }
            stringBuilder.setLength(0);
            temp.put(JsonConstant.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonConstant.COMPLETION, finishedNumber / totalNumber);
            publishStatus.accept(temp.encode().getBytes());
            temp.clear();
        }

        if (finishedNumber == totalNumber) {
            temp.put(JsonConstant.JOB_STATUS, MqJobStatus.SUCCESS.getValue()).put(JsonConstant.COMPLETION, 1.0);
        } else {
            temp.put(JsonConstant.JOB_STATUS, MqJobStatus.FAILURE.getValue()).put(JsonConstant.COMPLETION, finishedNumber / totalNumber);
        }
        publishStatus.accept(temp.encode().getBytes());
    }

    private static void transferFolder(String folderName, ChannelSftp fromChannelSftp, String fromParentPath, ChannelSftp toChannelSftp, String toParentPath) throws SftpException{
        //create directory
        String newFromPath = StringUtils.join(fromParentPath, SEPARATOR, folderName);
        String newToPath = StringUtils.join(toParentPath, SEPARATOR, folderName);
        toChannelSftp.mkdir(newToPath);
        Vector<ChannelSftp.LsEntry> entries = fromChannelSftp.ls(newFromPath);
        for (ChannelSftp.LsEntry entry : entries) {
            if (!entry.getFilename().startsWith(".")) {
                if (entry.getAttrs().isDir()) {
                    transferFolder(entry.getFilename(), fromChannelSftp, newFromPath, toChannelSftp, newToPath);
                } else {
                    transferFile(fromChannelSftp, StringUtils.join(newFromPath, SEPARATOR, entry.getFilename()),
                            toChannelSftp, StringUtils.join(newToPath, SEPARATOR, entry.getFilename()));
                }
            }
        }
    }

    private static void transferFile(ChannelSftp fromChannelSftp, String fromFilePath, ChannelSftp toChannelSftp, String toFilePath) throws SftpException{
        fromChannelSftp.get(fromFilePath, toChannelSftp.put(toFilePath));
    }

    private static void publishFailureMessage(Consumer<byte[]> publishStatus) {
        publishStatus.accept(new JsonObject().put(JsonConstant.JOB_STATUS, MqJobStatus.FAILURE.getValue()).put(JsonConstant.COMPLETION, 0.0).encode().getBytes());
    }
}
