package no.norstore.storebioinfo.facades;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.vertx.commons.constants.MqJobStatus;
import no.norstore.storebioinfo.Config;
import no.norstore.storebioinfo.sftp.SftpConnection;
import no.norstore.storebioinfo.utils.SbiUtils;
import no.norstore.storebioinfo.constants.ConfigName;
import no.norstore.storebioinfo.constants.JsonKey;
import no.norstore.storebioinfo.helpers.JdbcHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.Stream2StreamAO;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.PackingIrodsInputStream;

import java.io.*;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public final class MqJobFacade {
    private static Logger logger = LoggerFactory.getLogger(MqJobFacade.class);

    /**
     * The json structure of received message should be
     * {"remote_host":"", "user_name":"", "ssh_key":"", "dataset_id":"", "subtype":"", "parent_path_of_source":"", "destination_path":"",
     * "files":[], "folders":[]}
     *
     * parent_path_of_source doesn't contain subtype name
     *
     */
    public static void pushDataToNels(JsonObject message, Consumer<byte[]> publishFeed, Consumer<byte[]> publishStatus) {
        double progressPercentage;

        String host = message.getString(JsonKey.REMOTE_HOST);
        String userName = message.getString(JsonKey.USER_NAME);
        String sshKey = message.getString(JsonKey.SSH_KEY);
        String dataSetId = message.getString(JsonKey.DATASET_ID).trim();
        String dataSet = message.getString(JsonKey.DATASET).trim();
        String subtype = message.getString(JsonKey.SUBTYPE).trim();
        long subtypeId = message.getLong(JsonKey.SUBTYPE_ID);
        String parentPathOfSource = message.getString(JsonKey.PARENT_PATH_OF_SOURCE).trim();
        String destinationPath = message.getString(JsonKey.DESTINATION_PATH).trim();
        JsonArray filesArray = message.getJsonArray(JsonKey.FILES);
        JsonArray foldersArray = message.getJsonArray(JsonKey.FOLDERS);


        List<String> relativePathOfFetchedFiles = new ArrayList<>();
        relativePathOfFetchedFiles.addAll(filesArray.getList());

        publishStatus.accept(new JsonObject().put(JsonKey.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonKey.COMPLETION, 0.0).encode().getBytes());
        try {
            logger.debug("Query db and fetch the folder structure");
            List<String> foldersList = foldersArray.getList();
            if (foldersList.size() != 0) {
                relativePathOfFetchedFiles.addAll(JdbcHelper.fetchRelativePathOfFiles(subtypeId, foldersArray.getList(), parentPathOfSource.isEmpty() ? subtype : StringUtils.join(subtype, FileSystems.getDefault().getSeparator(), parentPathOfSource)));
            }
            progressPercentage = 0.05;
            publishStatus.accept(new JsonObject().put(JsonKey.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonKey.COMPLETION, progressPercentage).encode().getBytes());
        } catch (SQLException e) {
            logger.error(e);
            publishFailureMessage(publishStatus);
            return;
        }

        try (SftpConnection connection = new SftpConnection(host, userName, sshKey)) {
            ChannelSftp channelSftp = connection.openSftpChannel();

            //create folder structure in remote host
            logger.debug("Create folder structure in NeLS");

            List<String[]> fetchedFileList = prepareDirectoryHierarchy(subtype, parentPathOfSource, destinationPath, relativePathOfFetchedFiles, channelSftp);
            progressPercentage += 0.05;
            publishStatus.accept(new JsonObject().put(JsonKey.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonKey.COMPLETION, progressPercentage).encode().getBytes());

            IRODSAccount irodsAccount = new IRODSAccount(Config.valueOf(ConfigName.IRODS_HOST),
                    Integer.parseInt(Config.valueOf(ConfigName.IRODS_PORT)),
                    Config.valueOf(ConfigName.IRODS_USER),
                    Config.valueOf(ConfigName.IRODS_PASSWORD),
                    Config.valueOf(ConfigName.IRODS_HOME),
                    Config.valueOf(ConfigName.IRODS_ZONE),
                    Config.valueOf(ConfigName.DEFAULT_STORAGE_RESOURCE));
            IRODSAccessObjectFactory irodsAccessObjectFactory = null;
            IRODSFile irodsFile = null;
            BufferedReader br = null;

            try {
                logger.debug("Determine if files are existing in Sbi cache and run the irods script");
                irodsAccessObjectFactory = new IRODSAccessObjectFactoryImpl(new IRODSSession(new IRODSSimpleProtocolManager()));


                List<String> args = new ArrayList<>();
                args.add(StringUtils.join(dataSetId, "_", subtype, ".tar.gz"));
                args.add(dataSetId);
                args.add(subtype);
                args.add(Config.valueOf(ConfigName.IRODS_USER));
                if (parentPathOfSource.isEmpty()) {
                    args.add(subtype);
                } else {
                    args.add(StringUtils.join(subtype, FileSystems.getDefault().getSeparator(), parentPathOfSource));
                }

                //check if every file is existing in cache
                boolean isRunningIrodsScript = false;
                for (String[] info : fetchedFileList) {
                    String str = StringUtils.join(new String[]{Config.valueOf(ConfigName.IRODS_HOME), dataSetId, info[1]}, FileSystems.getDefault().getSeparator());
                    irodsFile = irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(str);
                    if (!irodsFile.exists()) {
                        logger.debug(str + " is not existing in irods cache");
                        isRunningIrodsScript = true;
                        break;
                    }
                }


                progressPercentage += 0.05;
                publishStatus.accept(new JsonObject().put(JsonKey.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonKey.COMPLETION, progressPercentage).encode().getBytes());

                StringBuilder stringBuilder = new StringBuilder();
                if (isRunningIrodsScript) {
                    logger.debug("Running irods script");
                    String parentPathWithSubtype = parentPathOfSource.isEmpty() ? subtype : StringUtils.join(new String[]{subtype, parentPathOfSource}, FileSystems.getDefault().getSeparator());
                    for (Object obj : filesArray.getList()) {
                        args.add(StringUtils.join("\"", parentPathWithSubtype, FileSystems.getDefault().getSeparator(), obj.toString(), "\""));
                    }

                    for (Object obj : foldersArray.getList()) {
                        args.add(StringUtils.join("\"", parentPathWithSubtype, FileSystems.getDefault().getSeparator(), obj.toString(), "\""));
                    }

                    //run irods script
                    String remoteArg = StringUtils.join(args, " ");
                    logger.debug("Irods script arguements: " + remoteArg);
                    br = new BufferedReader(new InputStreamReader(
                            irodsAccessObjectFactory.getRemoteExecutionOfCommandsAO(irodsAccount).executeARemoteCommandAndGetStreamGivingCommandNameAndArgs("untarAndPrepareinCache", remoteArg)));

                    try {
                        String line;
                        while ((line = br.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        logger.error(e);
                        publishFailureMessage(publishStatus);
                        return;
                    }

                    String result = stringBuilder.toString();
                    if (result.contains("ERROR") || !result.contains("SUCCESS")) {
                        if (!(result.contains("ERROR: rmUtil") && result.contains("SUCCESS"))) {
                            SbiUtils.sendIrodsExceptionByEmail(dataSetId, "extract data from", result);
                            logger.error(result);
                            publishFailureMessage(publishStatus);
                            return;
                        }
                    }
                    progressPercentage += 0.05;
                    publishStatus.accept(new JsonObject().put(JsonKey.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonKey.COMPLETION, progressPercentage).encode().getBytes());
                    logger.debug(result);
                }


                //transfer data from Sbi's cache to NeLS
                logger.debug("Transfer data from Sbi cache to NeLS");
                Stream2StreamAO stream2Stream = irodsAccessObjectFactory.getStream2StreamAO(irodsAccount);

                JsonObject temp = new JsonObject();
                int failedNumber = 0;

                String[] pathInfo;
                for (int i = 0; i < fetchedFileList.size(); i++) {
                    pathInfo = fetchedFileList.get(i);
                    stringBuilder.setLength(0);
                    stringBuilder.append("transfer data from ").append(dataSet).append(FileSystems.getDefault().getSeparator()).append(pathInfo[1]).append(" to ").append(pathInfo[2]);

                    logger.debug(stringBuilder.toString());

                    publishFeed.accept(stringBuilder.toString().getBytes());

                    String str = StringUtils.join(new String[]{Config.valueOf(ConfigName.IRODS_HOME), dataSetId, pathInfo[1]}, FileSystems.getDefault().getSeparator());

                    stringBuilder.setLength(0);
                    stringBuilder.append(pathInfo[2]).append(FileSystems.getDefault().getSeparator()).append(pathInfo[0]);
                    try (PackingIrodsInputStream packingIrodsInputStream = new PackingIrodsInputStream(irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFileInputStream(str));
                         OutputStream outputStream = channelSftp.put(stringBuilder.toString())) {
                        stream2Stream.streamToStreamCopyUsingStandardIO(packingIrodsInputStream, outputStream);

                        temp.clear();

                        temp.put(JsonKey.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonKey.COMPLETION, progressPercentage + (1.0 - progressPercentage) / fetchedFileList.size() * (i + 1));
                        publishStatus.accept(temp.encode().getBytes());

                        publishFeed.accept("successful".getBytes());
                    } catch (JargonException | SftpException e) {
                        logger.error(e);
                        publishFeed.accept("failed".getBytes());
                        failedNumber += 1;
                    } catch (IOException e) {
                        //this exception may happen because closing the packingIrodsInputStream or outputStream
                        logger.error(e);
                    }

                }
                temp.clear();
                if (failedNumber == 0) {
                    temp.put(JsonKey.JOB_STATUS, MqJobStatus.SUCCESS.getValue()).put(JsonKey.COMPLETION, 1.0);
                } else {
                    double finalPercentage = progressPercentage + (1.0 - progressPercentage) / fetchedFileList.size() * (fetchedFileList.size() - failedNumber);
                    temp.put(JsonKey.JOB_STATUS, MqJobStatus.FAILURE.getValue()).put(JsonKey.COMPLETION, finalPercentage);
                }
                publishStatus.accept(temp.encode().getBytes());
            } catch (JargonException e) {
                logger.error(e);
                SbiUtils.sendIrodsExceptionByEmail(dataSetId, "extract data from", e.getMessage());
                publishFailureMessage(publishStatus);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
                if (irodsFile != null) {
                    try {
                        irodsFile.close();
                    } catch (JargonException e) {
                    }
                }
                if (irodsAccessObjectFactory != null) {
                    try {
                        irodsAccessObjectFactory.closeSession(irodsAccount);
                    } catch (JargonException e) {
                    }
                }
            }
        } catch (JSchException | FileNotFoundException | UnsupportedEncodingException e) {
            logger.error(e);
            publishFailureMessage(publishStatus);
        } catch (IOException e) {
            logger.error(e);
            //TODO How to do if exception happens here. This exception may be triggered by closing sftp connection
        }
    }

    private static List<String[]> prepareDirectoryHierarchy(String subtype, String parentPathOfSource, String destinationPath, List<String> relativePathOfFetchedFiles, ChannelSftp channelSftp) {
        List<String[]> fetchedFileList = new ArrayList<>(relativePathOfFetchedFiles.size()); // it contains fileName, absoluteFilePath(starting with subtype name), destinationPath
        StringBuilder filePathToExtract = new StringBuilder();
        StringBuilder destinationPathBuilder = new StringBuilder();
        Map<Integer, List<String>> createdPaths = new LinkedHashMap<>();
        //item contains file name
        relativePathOfFetchedFiles.stream().forEach(item -> {
            String[] paths;
            String[] subPaths;

            int index = item.lastIndexOf(FileSystems.getDefault().getSeparator());
            String fileName = item.substring(index + 1).replaceAll("'", "\\'");

            filePathToExtract.append(subtype).append(File.separator);
            if (!parentPathOfSource.isEmpty()) {
                filePathToExtract.append(parentPathOfSource).append(File.separator);
            }
            if (index != -1) {

                filePathToExtract.append(item.replaceAll("'", "\\'"));

                paths = item.substring(0, index).split(FileSystems.getDefault().getSeparator());
                for (int i = 0; i < paths.length; i++) {
                    if (!destinationPath.isEmpty()) {
                        destinationPathBuilder.append(destinationPath).append(FileSystems.getDefault().getSeparator());
                    }
                    subPaths = new String[i + 1];
                    for (int j = 0; j <= i; j++) {
                        subPaths[j] = paths[j];
                    }
                    String path = StringUtils.join(subPaths, FileSystems.getDefault().getSeparator());
                    destinationPathBuilder.append(path);

                    if (createdPaths.containsKey(i)) {
                        if (!createdPaths.get(i).contains(path)) {
                            createRemoteFolder(channelSftp, destinationPathBuilder.toString());
                            createdPaths.get(i).add(path);
                        }
                    } else {
                        createdPaths.put(i, new ArrayList<>());
                        createRemoteFolder(channelSftp, destinationPathBuilder.toString());
                        createdPaths.get(i).add(path);
                    }

                    if (i == paths.length - 1) {
                        fetchedFileList.add(new String[]{fileName, filePathToExtract.toString(), destinationPathBuilder.toString()});
                    }
                    destinationPathBuilder.delete(0, destinationPathBuilder.length());
                }
            } else {
                filePathToExtract.append(fileName);
                fetchedFileList.add(new String[]{fileName, filePathToExtract.toString(), destinationPath});
            }
            filePathToExtract.delete(0, filePathToExtract.length());
        });
        return fetchedFileList;
    }

    /**
     * The json structure of received message should be
     * {"remote_host":"", "user_name":"", "ssh_key":"", "dataset_id":"", "subtype":"", "parent_path_of_source":"", "destination_path":"",
     * "files":[], "folders":[]}
     *
     */
    public static void pullDataFromNels(JsonObject message, Consumer<byte[]> publishFeed, Consumer<byte[]> publishStatus) {

        String host = message.getString(JsonKey.REMOTE_HOST);
        String userName = message.getString(JsonKey.USER_NAME);
        String sshKey = message.getString(JsonKey.SSH_KEY);
        String dataSetId = message.getString(JsonKey.DATASET_ID).trim();
        String dataSet = message.getString(JsonKey.DATASET).trim();
        String subtype = message.getString(JsonKey.SUBTYPE).trim();
        long subtypeId = message.getLong(JsonKey.SUBTYPE_ID);
        String relativeDestinationPath = message.getString(JsonKey.DESTINATION_PATH).trim();
        String parentPathOfSource = message.getString(JsonKey.PARENT_PATH_OF_SOURCE).trim();

        JsonArray transferredFils = message.getJsonArray(JsonKey.FILES);
        JsonArray transferredFolders = message.getJsonArray(JsonKey.FOLDERS);

        publishStatus.accept(new JsonObject().put(JsonKey.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonKey.COMPLETION, 0.0).encode().getBytes());
        try {
            logger.debug("Locking the data set. DatasetId:" + dataSetId);
            if (!JdbcHelper.lockDataSet(dataSetId, true)) {
                logger.warn("Failed to lock dataset:" + dataSetId);
                publishFailureMessage(publishStatus);
                return;
            }
        } catch (SQLException e) {
            logger.error(e);
            publishFailureMessage(publishStatus);
            return;
        }

        try (SftpConnection connection = new SftpConnection(host, userName, sshKey)) {
            ChannelSftp channelSftp = connection.openSftpChannel();
            List<String> dataInfo = new ArrayList<>(); //keep each file and its size, update the info in db
            long totalSize = 0;
            List<String[]> fileInfoList = new ArrayList<>();// the first is the relative path of file in NeLS, the second is the relative path of file in temp location of sbi
            StringBuilder stringBuilder = new StringBuilder();

            //assemble file info of remote host
            for (Object item : transferredFils) {
                String fileName = item.toString();

                String filePath = StringUtils.join(new String[]{parentPathOfSource, fileName}, FileSystems.getDefault().getSeparator());
                try {
                    Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(filePath);

                    long fileSize = entries.elementAt(0).getAttrs().getSize();

                    totalSize = totalSize + fileSize;
                    if (relativeDestinationPath.isEmpty()) {
                        stringBuilder.append(subtype).append(FileSystems.getDefault().getSeparator()).append(fileName).append(",").append(fileSize);
                        fileInfoList.add(new String[]{filePath, StringUtils.join(new String[]{dataSetId, subtype, fileName}, FileSystems.getDefault().getSeparator())});
                    } else {
                        stringBuilder.append(subtype).append(FileSystems.getDefault().getSeparator()).append(relativeDestinationPath).append(FileSystems.getDefault().getSeparator()).append(fileName).append(",").append(fileSize);
                        fileInfoList.add(new String[]{filePath, StringUtils.join(new String[]{dataSetId, subtype, relativeDestinationPath, fileName}, FileSystems.getDefault().getSeparator())});
                    }
                    dataInfo.add(stringBuilder.toString());
                    stringBuilder.setLength(0);
                } catch (SftpException e) {
                    logger.error(e);
                    publishFailureMessage(publishStatus);
                    return;
                }
            }

            for (Object item : transferredFolders) {
                try {
                    totalSize = totalSize + assembleFolderInfo(fileInfoList, dataInfo, channelSftp, dataSetId, subtype, parentPathOfSource, "", item.toString(), relativeDestinationPath);
                } catch (SftpException e) {
                    logger.error(e);
                    publishFailureMessage(publishStatus);
                    return;
                }
            }

            logger.debug("The total size of files is " + totalSize);
            dataInfo.stream().forEach(info -> logger.debug(info));


            if (totalSize > 0) {
                //check if there is enough quota
                if (!JdbcHelper.isDiskQuotaEnough(dataSetId, totalSize)) {
                    publishFeed.accept("Not enough disk quota".getBytes());
                    publishFailureMessage(publishStatus);
                    return;
                }

                //create folder structure in SBI
                fileInfoList.stream().forEach(str -> createFolders(str[1]));

                double progressPercentage = 0.2;
                publishStatus.accept(new JsonObject().put(JsonKey.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonKey.COMPLETION, progressPercentage).encode().getBytes());

                //fetch data from NeLS to Sbi's temp location
                stringBuilder.setLength(0);

                String[] str;
                for (int i = 0; i < fileInfoList.size(); i++) {
                    str = fileInfoList.get(i);
                    String tempInSbi = StringUtils.join(dataSet, str[1].substring(str[1].indexOf(FileSystems.getDefault().getSeparator())));
                    stringBuilder.append("fetch data from ").append(str[0]).append(" to ").append(tempInSbi);

                    logger.debug(stringBuilder.toString());

                    publishFeed.accept(stringBuilder.toString().getBytes());
                    stringBuilder.setLength(0);
                    try (OutputStream outputStream = new FileOutputStream(StringUtils.join(new String[]{Config.valueOf(ConfigName.IRODS_UPLOAD_PATH), str[1]}, FileSystems.getDefault().getSeparator()))) {
                        channelSftp.get(str[0], outputStream);
                        publishFeed.accept("successful".getBytes());
                    } catch (SftpException e) {
                        logger.error(e);
                        publishFeed.accept("failed".getBytes());
                        publishFailureMessage(publishStatus);
                        return;
                    } catch (IOException e) {
                        logger.error(e);
                        //TODO this exception is happened when closing outputStream
                    }
                }

                JsonObject temp = new JsonObject();
                progressPercentage += 0.4;
                temp.put(JsonKey.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonKey.COMPLETION, progressPercentage);
                publishStatus.accept(temp.encode().getBytes());

                temp.clear();

                if (JdbcHelper.updateSbiDbForFetchDataFromNels(dataSetId, subtypeId, subtype, dataInfo, totalSize)) {
                    logger.debug("Updating sbi db is successful");
                    temp.put(JsonKey.JOB_STATUS, MqJobStatus.SUCCESS.getValue()).put(JsonKey.COMPLETION, 1.0);
                    publishStatus.accept(temp.encode().getBytes());
                } else {
                    publishFailureMessage(publishStatus);
                    logger.debug("Updating sbi db is failure");
                }
            } else {
                publishFeed.accept("The size of data you want to transfer is zero.".getBytes());
                publishStatus.accept(new JsonObject().put(JsonKey.JOB_STATUS, MqJobStatus.SUCCESS.getValue()).put(JsonKey.COMPLETION, 1.0).encode().getBytes());
            }


        } catch (JSchException | FileNotFoundException | UnsupportedEncodingException | SQLException e) {
            logger.error(e);
            publishFailureMessage(publishStatus);
        } catch (IOException e) {
            logger.error(e);
            //TODO How to do if exception happens here. This exception may be triggered by closing sftp connection
        } finally {
            try {
                logger.debug("Unlocking the data set");
                JdbcHelper.lockDataSet(dataSetId, false);
            } catch (SQLException e) {
                //TODO How to do if we couldn't unlock the data set
                logger.error(e);
            }
            try {
                cleanUploadedDataInSbi(dataSetId);
            } catch (IOException e) {
                //TODO How to do if we couldn't clean up the temp
                logger.error(e);
            }
        }

    }

    private static void publishFailureMessage(Consumer<byte[]> publishStatus) {
        publishStatus.accept(new JsonObject().put(JsonKey.JOB_STATUS, MqJobStatus.FAILURE.getValue()).put(JsonKey.COMPLETION, 0.0).encode().getBytes());
    }

    private static void createFolders(String filePath) {
        int index = filePath.lastIndexOf(FileSystems.getDefault().getSeparator());
        File folder = new File(StringUtils.join(new String[]{Config.valueOf(ConfigName.IRODS_UPLOAD_PATH), filePath.substring(0, index)}, FileSystems.getDefault().getSeparator()));
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private static long assembleFolderInfo(List<String[]> fileInfoList, List<String> dataInfo,
                                           ChannelSftp channelSftp, String dataSetId, String subtype,
                                           String parentPathOfSource, String relativePathOfSource, String folderName, String relativeDestinationPath) throws SftpException {
        String folderPath;
        String relativeFolderPath;
        if (relativePathOfSource != null && !relativePathOfSource.isEmpty()) {
            folderPath = StringUtils.join(new String[]{parentPathOfSource, relativePathOfSource, folderName}, FileSystems.getDefault().getSeparator());
            relativeFolderPath = StringUtils.join(new String[]{relativePathOfSource, folderName}, FileSystems.getDefault().getSeparator());
        } else {
            folderPath = StringUtils.join(new String[]{parentPathOfSource, folderName}, FileSystems.getDefault().getSeparator());
            relativeFolderPath = folderName;
        }
        Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(folderPath);
        long totalSize = 0;
        for (ChannelSftp.LsEntry entry : entries) {
            if (!entry.getFilename().startsWith(".")) {
                if (entry.getAttrs().isDir()) {
                    if (relativePathOfSource != null && !relativePathOfSource.isEmpty()) {
                        totalSize = totalSize + assembleFolderInfo(fileInfoList, dataInfo,
                                channelSftp, dataSetId, subtype,
                                parentPathOfSource, relativePathOfSource + FileSystems.getDefault().getSeparator() + folderName, entry.getFilename(), relativeDestinationPath);
                    } else {
                        totalSize = totalSize + assembleFolderInfo(fileInfoList, dataInfo,
                                channelSftp, dataSetId, subtype,
                                parentPathOfSource, folderName, entry.getFilename(), relativeDestinationPath);
                    }
                } else {
                    String fileName = entry.getFilename();
                    long fileSize = entry.getAttrs().getSize();
                    totalSize = totalSize + fileSize;
                    String filePath = StringUtils.join(new String[]{folderPath, fileName}, FileSystems.getDefault().getSeparator());
                    String str;
                    String info;
                    if (relativeDestinationPath != null && !relativeDestinationPath.isEmpty()) {
                        str = StringUtils.join(new String[]{dataSetId, subtype, relativeDestinationPath, relativeFolderPath, fileName}, FileSystems.getDefault().getSeparator());
                        info = StringUtils.join(new String[]{subtype, relativeDestinationPath, relativeFolderPath, fileName}, FileSystems.getDefault().getSeparator()) + "," + fileSize;
                    } else {
                        str = StringUtils.join(new String[]{dataSetId, subtype, relativeFolderPath, fileName}, FileSystems.getDefault().getSeparator());
                        info = StringUtils.join(new String[]{subtype, relativeFolderPath, fileName}, FileSystems.getDefault().getSeparator()) + "," + fileSize;
                    }
                    fileInfoList.add(new String[]{filePath, str});
                    dataInfo.add(info);
                }
            }
        }
        return totalSize;
    }

    private static void createRemoteFolder(ChannelSftp channelSftp, String remoteFolder){
        try {
            channelSftp.mkdir(remoteFolder);
        } catch (SftpException e) {
            logger.error(e);
        }
    }

    private static void cleanUploadedDataInSbi(String dataSetId) throws IOException {
        File file = new File(StringUtils.join(new String[]{Config.valueOf(ConfigName.IRODS_UPLOAD_PATH), dataSetId}, File.separator));
        if (file.exists()) {
            FileUtils.deleteDirectory(file);
        }
    }
}
