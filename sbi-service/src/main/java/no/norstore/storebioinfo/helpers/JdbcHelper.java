package no.norstore.storebioinfo.helpers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.vertx.commons.db.DBHelper;
import no.norstore.storebioinfo.Config;
import no.norstore.storebioinfo.constants.ConfigName;
import no.norstore.storebioinfo.utils.CompressUtils;
import no.norstore.storebioinfo.utils.IrodsUtils;
import no.norstore.storebioinfo.utils.SbiUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.RemoteExecutionOfCommandsAO;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public final class JdbcHelper {
    private static Logger logger = LoggerFactory.getLogger(JdbcHelper.class);

    public static boolean updateOnlyDatasetState(String dataSetId, String state) throws SQLException {

        String sql = "UPDATE data_set SET state=? WHERE data_set_id=?";
        try (Connection connection = DBHelper.getBasicDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state);
            statement.setString(2, dataSetId);
            statement.executeUpdate();
            return true;
        }

    }

    public static List<String> fetchRelativePathOfFiles(long subtypeId, List<String> folders, String parentFolder) throws SQLException{
        StringBuilder stringBuilder = new StringBuilder();
        List<String> parentPaths = new ArrayList<>(folders.size());
        folders.stream().forEach(folder -> parentPaths.add(StringUtils.join(new String[]{parentFolder, folder}, FileSystems.getDefault().getSeparator())));
        stringBuilder.append("SELECT element FROM resource_subentries WHERE resource_id=").append(subtypeId).append(" AND ");
        for (int i = 0; i < parentPaths.size(); i++) {
            if (i == (parentPaths.size() - 1)) {
                stringBuilder.append("element LIKE '").append(parentPaths.get(i)).append("%'");
            } else {
                stringBuilder.append("element LIKE '").append(parentPaths.get(i)).append("%' OR ");
            }
        }
        logger.debug(stringBuilder.toString());
        try (Connection connection = DBHelper.getBasicDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(stringBuilder.toString())){
            List<String> result = new ArrayList<>();
            while (resultSet.next()) {
                String element = resultSet.getString("element");
                Optional<String> optional = parentPaths.stream().filter(element::startsWith).findFirst();
                element = element.substring(optional.get().length(), element.indexOf(","));
                result.add(optional.get().substring(optional.get().lastIndexOf(FileSystems.getDefault().getSeparator()) + 1) + element);
            }
            return result;
        }
    }

    /**
     * This method is going to lock or unlock a data set depending on the isLocked parameter. If it is going to lock a data set,
     * it needs to check if this data set has been locked or not.
     *
     * @param dataSetId represents which data set you want to lock or unlock.
     * @param isLocked represents the status of lock. If isLocked is true, it means you want to lock this data set.
     *                 If isLocked is false, it means you want to unlock this data set.
     * @return
     * @throws SQLException
     */
    public static synchronized boolean lockDataSet(String dataSetId, boolean isLocked) throws SQLException{
        String sql;
        if (isLocked) {
            sql = "SELECT locked FROM data_set WHERE data_set_id='" + dataSetId + "'";
            JsonObject jsonObject = DBHelper.getOne(sql, "locked");
            boolean locked = Boolean.parseBoolean(jsonObject.getValue("locked").toString());
            if (locked) {
                return false;
            }
        }
        sql = "UPDATE data_set SET locked=? WHERE data_set_id=?";
        try (Connection connection = DBHelper.getBasicDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, isLocked);
            statement.setString(2, dataSetId);
            statement.executeUpdate();
            return true;
        }
    }

    public static boolean isDiskQuotaEnough(String dataSetId, long diskQuotaNeeded) throws SQLException {
        String selectQuota = "SELECT id, disk_quota FROM disk_quota WHERE quota_id IN (SELECT quota_id FROM data_set WHERE data_set_id='" + dataSetId + "')";
        JsonObject quota = DBHelper.getOne(selectQuota, "id", "disk_quota");
        long remainingQuota = Long.parseLong(quota.getValue("disk_quota").toString());
        return remainingQuota >= diskQuotaNeeded;
    }

    public static boolean updateSbiDbForFetchDataFromNels(String dataSetId, long subtypeId, String subtype, List<String> addedFiles, long size) {
        logger.debug("Start to update sbi db for subtype " + subtype + "-" + subtypeId + "of data set " + dataSetId);
        String selectQuota = "SELECT id, disk_quota FROM disk_quota WHERE quota_id IN (SELECT quota_id FROM data_set WHERE data_set_id='" + dataSetId + "')";
        JsonObject quota;
        try {
            quota = DBHelper.getOne(selectQuota, "id", "disk_quota");
        } catch (SQLException e) {
            logger.error(e);
            return false;
        }
        long oldQuota = Long.parseLong(quota.getValue("disk_quota").toString());
        logger.debug("The old quota is " + oldQuota);
        long newQuota = oldQuota - size;
        logger.debug("The new quota is " + newQuota);
        int quotaId = Integer.parseInt(quota.getValue("id").toString());

        StringBuilder insertToSubtype = new StringBuilder();
        insertToSubtype.append("INSERT INTO resource_subentries (resource_id, element) VALUES ");
        for (int i = 0; i < addedFiles.size(); i++) {
            insertToSubtype.append("(").append(subtypeId).append(",'").append(addedFiles.get(i)).append("')");
            if (i != (addedFiles.size() - 1)) {
                insertToSubtype.append(",");
            }
        }
        logger.debug(insertToSubtype.toString());

        String updateSubtypeSize = "UPDATE resource SET size=size+" + size +" WHERE id=?";
        String updateDiskQuota = "UPDATE disk_quota SET disk_quota=? WHERE id=?";
        String updateDataSetState = "UPDATE data_set SET state=? WHERE data_set_id=?";

        Connection connection = null;
        try {
            connection = DBHelper.getBasicDataSource().getConnection();
            connection.setAutoCommit(false);
            try (Statement insertToSubtypeStatement = connection.createStatement();
                 PreparedStatement updateSubtypeSizeStatement = connection.prepareStatement(updateSubtypeSize);
                 PreparedStatement updateDiskQuotaStatement = connection.prepareStatement(updateDiskQuota);
                 PreparedStatement updateDataSetStateStatement = connection.prepareStatement(updateDataSetState)) {

                logger.debug("insert resource_subentries");
                insertToSubtypeStatement.executeUpdate(insertToSubtype.toString());

                logger.debug("Update subtype size");
                updateSubtypeSizeStatement.setLong(1, subtypeId);
                updateSubtypeSizeStatement.executeUpdate();

                logger.debug("Update disk quota");
                updateDiskQuotaStatement.setLong(1, newQuota);
                updateDiskQuotaStatement.setInt(2, quotaId);
                updateDiskQuotaStatement.executeUpdate();

                logger.debug("Update data set status");
                updateDataSetStateStatement.setString(1, "COMPLETE");
                updateDataSetStateStatement.setString(2, dataSetId);
                updateDataSetStateStatement.executeUpdate();

                if (appendTempFolderToTarFile(dataSetId, subtype)) {
                    logger.debug("Appending temp folder to irods is successful");
                    connection.commit();
                    return true;
                } else {
                    logger.debug("Appending temp folder to irods is failure");
                    connection.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            logger.error(e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                }
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e1) {
                }
            }
        }
    }

    public static List<String> dataSetSubtypeName(int dataSetTypeId) {
        String selectDataSetTypeSubtypes = "SELECT element FROM data_set_type_subtypes where datasettype_id = " + String.valueOf(dataSetTypeId);
        JsonObject dataSetSubtypeRS;
        try {
            dataSetSubtypeRS = DBHelper.select(selectDataSetTypeSubtypes, "element");
        } catch (SQLException e) {
            logger.error(e);
            return null;
        }
        return dataSetSubtypeRS.getJsonArray("element").getList();
    }

    public static boolean addDataSetToProject(String dataSetId, String federatedId, int dataSetTypeId, long projectId, String name, String description) throws SQLException, NullPointerException{


        List<String> subtypes = dataSetSubtypeName(dataSetTypeId);
        String selectDataSetTypeName = "SELECT name FROM data_set_type where id = " + String.valueOf(dataSetTypeId);
        JsonObject dataSetTypeRS;
        try {
            dataSetTypeRS = DBHelper.getOne(selectDataSetTypeName, "name");
        } catch (SQLException e) {
            logger.error(e);
            return false;
        }
        String dataSetTypeName = dataSetTypeRS.getString("name");
        String sql = "select project.externalref, quota.quota_id from project inner join quota on project.project_quota = quota.id and project.project_id = " + projectId;
        JsonObject result;
        try {
            result = DBHelper.getOne(sql, "externalref", "quota_id");
        } catch (SQLException e) {
            logger.error(e);
            return false;
        }
        String projectExternalRef = result.getString("externalref");
        String quotaId = result.getString("quota_id");
        //insert into project_policy
        String externalid = UUID.randomUUID().toString();
        String insertToProjectPolicy = "INSERT INTO project_policy (policy_id, externalid, project_id) VALUES ((select max(policy_id) from project_policy ) + 1, '" + externalid + "','" + projectExternalRef +"')";

        Connection connection = null;
        try {
            connection = DBHelper.getBasicDataSource().getConnection();
            connection.setAutoCommit(false);
            Statement insertToProjectPolicyStatement = connection.createStatement();
            logger.debug("insert into project_policy");
            insertToProjectPolicyStatement.executeUpdate(insertToProjectPolicy, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = insertToProjectPolicyStatement.getGeneratedKeys();
            rs.next();
            int policyId = rs.getInt(1);
            logger.debug("policy_id:" + policyId);
            String insertToDataSet = "INSERT INTO data_set (id, created, data_set_id, description, sub_resources, name, owner_id, quota_id, state, type, project_policy, public, locked)" +
                    "VALUES ((select max(id) from data_set ) + 1, now()" + ",'"+ dataSetId + "','" + description + "',true,'" + name +"','" + federatedId + "','" + quotaId + "','IRODS_UPLOADED','" + dataSetTypeName + "'," + policyId + ", false,false)";

            Statement insertToDataSetStatement = connection.createStatement();
            logger.debug("insert into data_set");
            logger.debug("sql:" + insertToDataSet);
            insertToDataSetStatement.executeUpdate(insertToDataSet, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs2 = insertToDataSetStatement.getGeneratedKeys();
            rs2.next();
            int dataSetIdKey = rs2.getInt(1);
            logger.debug("data_set_id:" + dataSetIdKey);

            List<String> batchInsert = new ArrayList<>();
            String insertToResourceSQL, insertToResource = "INSERT INTO resource (created, externalref, in_user_context, local_resource, name, owner_id, quota_id, size, state, type, use_default_project_policy, valid_type, dataset_id, project_policy) VALUES ";

            for(String subtypeName : subtypes) {
                batchInsert.add("(now()" + ",'" + UUID.randomUUID().toString() + "'," + "false,false,'" + subtypeName + "','" + federatedId + "','" + quotaId + "',0,'INIT','" + subtypeName + "',false, false," + dataSetIdKey + "," + policyId + ")");
            }
            insertToResourceSQL =  insertToResource + batchInsert.stream().collect(Collectors.joining(","));

            logger.debug("insert into resource. sql:" + insertToResourceSQL);
            Statement insertToResourceStatement = connection.createStatement();

            insertToResourceStatement.executeUpdate(insertToResourceSQL);

            boolean ret = addDataSet(dataSetId, dataSetTypeId);
            if(ret){
                connection.commit();
                return true;
            }else {
                connection.rollback();
                return false;
            }

        }catch (SQLException e) {
            logger.error("addDataSetToProject error:" + e.getLocalizedMessage());
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    logger.error(e1.getLocalizedMessage());
                }
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error(e.getLocalizedMessage());
                }
            }
        }
    }

    private static boolean addDataSet(String dataSetId, int dataSetTypeId) {

        boolean result = false;
        String tmpFolder = new StringBuilder().append(FileSystems.getDefault().getSeparator())
                .append("tmp")
                .append(FileSystems.getDefault().getSeparator())
                .append(dataSetId).toString();
        try {
            FileUtils.forceMkdir(new File(tmpFolder));
            List<String> fileNames = new ArrayList<>();
            List<String> subtypes = dataSetSubtypeName(dataSetTypeId);
            for (String subtype : subtypes) {
                String subtypeFolder = new StringBuilder().append(tmpFolder)
                        .append(FileSystems.getDefault().getSeparator())
                        .append(subtype).toString();
                FileUtils.forceMkdir(new File(subtypeFolder));
                String tmpTarFile = new StringBuilder().append(tmpFolder)
                        .append(FileSystems.getDefault().getSeparator())
                        .append(dataSetId)
                        .append("_")
                        .append(subtype)
                        .append(".tar.gz").toString();
                fileNames.add(tmpTarFile);
                // Compress the sub type archives
                CompressUtils.compressFolderUsingTar(subtypeFolder, tmpTarFile);
                FileUtils.forceDelete(new File(subtypeFolder));
            }
            // Copy the archives to iRods
            result = IrodsUtils.copyLocalFileToIrods(dataSetId, null, Arrays.asList(tmpFolder), fileNames, dataSetId);
            FileUtils.forceDelete(new File(tmpFolder));
        } catch (IOException e) {
            logger.error("Failed to addDataSet:" + e.getLocalizedMessage());
        }
        return result;
    }

    private static boolean appendTempFolderToTarFile(String dataSetId, String subType) {
        logger.debug("Start to append temp folder to irods");
        IRODSAccount irodsAccount = new IRODSAccount(Config.valueOf(ConfigName.IRODS_HOST),
                Integer.parseInt(Config.valueOf(ConfigName.IRODS_PORT)),
                Config.valueOf(ConfigName.IRODS_USER),
                Config.valueOf(ConfigName.IRODS_PASSWORD),
                Config.valueOf(ConfigName.IRODS_HOME),
                Config.valueOf(ConfigName.IRODS_ZONE),
                Config.valueOf(ConfigName.DEFAULT_STORAGE_RESOURCE));

        IRODSAccessObjectFactory irodsAccessObjectFactory = null;
        BufferedReader br = null;
        try {
            irodsAccessObjectFactory = new IRODSAccessObjectFactoryImpl(new IRODSSession(new IRODSSimpleProtocolManager()));

            RemoteExecutionOfCommandsAO remoteExecutionObject = irodsAccessObjectFactory.getRemoteExecutionOfCommandsAO(irodsAccount);

            StringBuilder cmd = new StringBuilder();
            cmd.append(dataSetId).append("_").append(subType).append(" ").
                    append(subType).append(" ").
                    append(dataSetId).append(" ").
                    append(Config.valueOf(ConfigName.IRODS_USER)).append(" ").
                    append(dataSetId);

            logger.debug("executing the script with this command: " + cmd.toString());


            br = new BufferedReader(
                    new InputStreamReader(remoteExecutionObject.executeARemoteCommandAndGetStreamGivingCommandNameAndArgs("newAppendToTarUsingiCmd", cmd.toString())));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            String result = sb.toString();

            if (result.contains("ERROR") || !result.contains("SUCCESS")) {
                SbiUtils.sendIrodsExceptionByEmail(dataSetId, "append data to", result);
                logger.error(result);
                return false;
            }

            logger.debug(result);
            return true;
        } catch (JargonException e) {
            SbiUtils.sendIrodsExceptionByEmail(dataSetId, "append data to", e.getMessage());
            logger.error(e);
            return false;
        } catch (IOException e) {
            logger.error(e);
            return false;
        } finally {
            if (irodsAccessObjectFactory != null) {
                try {
                    irodsAccessObjectFactory.closeSession(irodsAccount);
                } catch (JargonException e) {
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
    }


    public static boolean addProjectMember(long projectId, String federatedId, int role) {
        String updateUserProject = "INSERT INTO user_project (project_id, user_id) VALUES (?, (SELECT user_id FROM esysbio_user WHERE federated_id=?))";
        String updateUserRoleProject = "INSERT INTO user_role_project (urp_project_id, urp_role_id, urp_user_id) VALUES (?, ?, (SELECT user_id FROM esysbio_user WHERE federated_id=?))";

        try (Connection connection = DBHelper.getBasicDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement updateUserProjectStatement = connection.prepareStatement(updateUserProject)) {
                updateUserProjectStatement.setLong(1, projectId);
                updateUserProjectStatement.setString(2, federatedId);
                updateUserProjectStatement.executeUpdate();
            } catch (SQLException e) {
                logger.error("Updating user_project table failed, error code is " + e.getErrorCode(), e.getCause());
                return false;
            }

            try (PreparedStatement updateUserRoleProjectStatement = connection.prepareStatement(updateUserRoleProject)) {
                updateUserRoleProjectStatement.setLong(1, projectId);
                updateUserRoleProjectStatement.setInt(2, role);
                updateUserRoleProjectStatement.setString(3, federatedId);
                updateUserRoleProjectStatement.executeUpdate();
                connection.commit();
                return true;
            } catch (SQLException e) {
                logger.error("updating user_role_project table failed, error code is " + e.getErrorCode(), e.getCause());
                connection.rollback();
                return false;
            }
        } catch (SQLException e) {
            logger.error("Sql internal issue, error code is " + e.getErrorCode(), e.getCause());
            return false;
        }
    }

    public static boolean removeProjectMember(long projectId, String federatedId) {
        try (Connection connection = DBHelper.getBasicDataSource().getConnection()) {
            connection.setAutoCommit(false);
            String updateUserProject = "DELETE FROM user_project WHERE project_id=? AND user_id IN (SELECT user_id FROM esysbio_user WHERE federated_id=?)";
            try (PreparedStatement updateUserProjectStatement = connection.prepareStatement(updateUserProject)) {
                updateUserProjectStatement.setLong(1, projectId);
                updateUserProjectStatement.setString(2, federatedId);
                updateUserProjectStatement.executeUpdate();
            } catch (SQLException e) {
                logger.error("Updating user_project table failed, error code is " + e.getErrorCode(), e.getCause());
                return false;
            }

            String updateUserRoleProject = "DELETE FROM user_role_project WHERE urp_project_id=? AND urp_user_id IN (SELECT user_id FROM esysbio_user WHERE federated_id=?)";
            try (PreparedStatement updateUserRoleProjectStatement = connection.prepareStatement(updateUserRoleProject)) {
                updateUserRoleProjectStatement.setLong(1, projectId);
                updateUserRoleProjectStatement.setString(2, federatedId);
                updateUserRoleProjectStatement.executeUpdate();
                connection.commit();
                return true;
            } catch (SQLException e) {
                logger.error("updating user_role_project table failed, error code is " + e.getErrorCode(), e.getCause());
                connection.rollback();
                return false;
            }
        } catch (SQLException e) {
            logger.error("Sql internal issue, error code is " + e.getErrorCode(), e.getCause());
            return false;
        }
    }


    public static long getSizeForResource(long resourceId) {
        String sql = "SELECT element from resource_subentries where resource_id = " + String.valueOf(resourceId);
        List<String> elements = new ArrayList<>();
        JsonArray jsonArray;
        JsonObject rs;
        long total = 0;
        try {
            rs = DBHelper.select(sql, "element");
            jsonArray = rs.getJsonArray("element");
            for (int i = 0; i < jsonArray.size(); i++) {
                elements.add(jsonArray.getString(i));
            }
            total = elements.stream().mapToLong(e -> new Long(getSize(e))).sum();

        } catch (SQLException e) {
            logger.error(e);

        }
        return total;
    }

    private static long getSize(String s) {
        List<String> items = Arrays.asList(s.split("\\s*,\\s*"));
        return Long.valueOf(items.get(items.size() - 1));
    }

}
