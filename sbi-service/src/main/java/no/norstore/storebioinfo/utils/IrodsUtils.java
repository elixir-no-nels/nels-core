package no.norstore.storebioinfo.utils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.norstore.storebioinfo.Config;
import no.norstore.storebioinfo.constants.ConfigName;
import no.norstore.storebioinfo.helpers.JdbcHelper;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.OverwriteException;
import org.irods.jargon.core.packinstr.TransferOptions;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.transfer.DefaultTransferControlBlock;
import org.irods.jargon.core.transfer.TransferControlBlock;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by weizhang on 1/6/17.
 */
public final class IrodsUtils {
    private static Logger logger = LoggerFactory.getLogger(IrodsUtils.class);
    private static IRODSAccount irodsAccount;
    private static String homeDirectory = "";
    private static String defaultStorageResource = "";

    static {
        irodsAccount = new IRODSAccount(Config.valueOf(ConfigName.IRODS_HOST),
                Integer.parseInt(Config.valueOf(ConfigName.IRODS_PORT)),
                Config.valueOf(ConfigName.IRODS_USER),
                Config.valueOf(ConfigName.IRODS_PASSWORD),
                Config.valueOf(ConfigName.IRODS_HOME),
                Config.valueOf(ConfigName.IRODS_ZONE),
                Config.valueOf(ConfigName.DEFAULT_STORAGE_RESOURCE));
        homeDirectory = Config.valueOf(ConfigName.IRODS_HOME);
        defaultStorageResource = Config.valueOf(ConfigName.DEFAULT_STORAGE_RESOURCE);
    }

    public static String getVersion() {

        String version = null;
        try {
            IRODSAccessObjectFactory irodsAccessObjectFactory = new IRODSAccessObjectFactoryImpl(new IRODSSession(new IRODSSimpleProtocolManager()));
            version = irodsAccessObjectFactory.getIRODSServerProperties(irodsAccount).getRelVersion();
        } catch (JargonException ex) {
            logger.warn("Failed to get version. " + ex.getLocalizedMessage());
        }
        return version;
    }


    public static boolean copyLocalFileToIrods(String dataSetId, String stateAfterwards, List<String> localFiles, List<String> fileNames,
                                               String irodsFolder) {
        boolean fileCopied = false;
        IRODSAccessObjectFactory irodsAccessObjectFactory = null;
        try {
            JdbcHelper.updateOnlyDatasetState(dataSetId, "IRODS_UPLOADING");


            irodsAccessObjectFactory = new IRODSAccessObjectFactoryImpl(new IRODSSession(new IRODSSimpleProtocolManager()));

            TransferOptions transferOptions = new TransferOptions();
            transferOptions.setComputeAndVerifyChecksumAfterTransfer(true);

            for (String localFile : localFiles) {
                int splitIndex = localFile.lastIndexOf("/");
                String remoteName = localFile.substring(splitIndex + 1);

                TransferControlBlock transferControlBlock = DefaultTransferControlBlock
                        .instance();
                transferControlBlock.setTransferOptions(transferOptions);
                IRODSFile irodsFile = irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(homeDirectory + "/" + remoteName);
                logger.info("LOCAL FILE:" + localFile);
                logger.info("IRODS HOME:" + homeDirectory + " - IRODS STORAGE:" + defaultStorageResource);
                logger.debug("IRODS path:" + irodsFile.getAbsolutePath());
                Boolean created = irodsFile.mkdir();
                logger.info("create folder:" + created);
                File file;
                for (String fileName : fileNames) {
                    file = new File(fileName);
                    irodsAccessObjectFactory.getDataTransferOperations(irodsAccount).putOperation(file, irodsFile, null, null);
                    logger.info("Copied file " + fileName + " to irods " + irodsFile.getAbsolutePath());
                }
                logger.info("Copied file " + remoteName + " to irods");
            }
            // ifs.close();
            fileCopied = true;
        } catch (OverwriteException ow) {
            ow.printStackTrace();
        } catch (DataNotFoundException de) {
            de.printStackTrace();
        } catch (JargonException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            String newState = stateAfterwards == null ? "IRODS_UPLOADED" : stateAfterwards;
            try {
                JdbcHelper.updateOnlyDatasetState(dataSetId, newState);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (irodsAccessObjectFactory != null) {
                try {
                    irodsAccessObjectFactory.closeSession(irodsAccount);
                } catch (JargonException e) {
                }
            }
        }

        return fileCopied;
    }
}
