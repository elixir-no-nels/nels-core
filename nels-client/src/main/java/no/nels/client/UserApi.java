package no.nels.client;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import no.nels.client.model.FileFolder;
import no.nels.client.model.SSHCredential;
import no.nels.commons.abstracts.AProjectMembership;
import no.nels.commons.abstracts.ASystemUser;
import no.nels.commons.constants.JsonObjectKey;
import no.nels.commons.constants.JsonObjectValue;
import no.nels.commons.constants.db.ProjectUsers;
import no.nels.commons.constants.db.User;
import no.nels.commons.model.*;
import no.nels.commons.model.systemusers.NormalUser;
import no.nels.commons.utilities.IDpUtilities;
import no.nels.commons.utilities.UserTypeUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UserApi extends ApiClient{

    private static final Logger logger = LogManager.getLogger(UserApi.class);

    public static List<FileFolder> listItems(long callerId, long nelsId,
                                             String path) throws Exception {
        return APIProxy.listItems(callerId, nelsId, path);
    }

    public static void createFolder(long callerId, long nelsId,
                                    String parentFolderPath, String name) throws Exception {
        APIProxy.createFolder(callerId, nelsId, parentFolderPath, name);
    }

    public static void createFile(long callerId, long nelsId,
                                  String parentFolderPath, String name, InputStream inputStream) throws Exception {
        APIProxy.createFile(callerId, nelsId, parentFolderPath, name, inputStream);
    }


    public static void deleteSelectedElements(long callerId, long nelsId, List<String> selectedItems) throws Exception {
        APIProxy.deleteFileSystemElement(callerId, nelsId, selectedItems);
    }

    private static void renameFileSystemElement(long callerId, long nelsId,
                                                String parentFolderPath, String oldName, String newName)
            throws Exception {
        APIProxy.renameFileSystemElement(callerId, nelsId, parentFolderPath, oldName, newName);
    }

    public static void renameFile(long callerId, long nelsId,
                                  String parentFolderPath, String oldName, String newName)
            throws Exception {

        renameFileSystemElement(callerId, nelsId, parentFolderPath,
                oldName, newName);
    }

    public static SSHCredential getSshCredential(long callerId, long nelsId)
            throws Exception {
        return APIProxy.getSshCredential(callerId, nelsId);
    }

    public static boolean copy(long callerId, long nelsId, String[] src, String dst) {
        Jobs.addCopyJob(nelsId, src, dst);
        return true;
    }

    public static boolean move(long callerId, long nelsId, String[] src, String dst) {
        Jobs.addMoveJob(nelsId, src, dst);
        return true;
    }


    public static boolean copyFile(long callerId, long nelsId,
                                   String sourceFilePath, String destinationParentFolderPath) throws Exception {
        APIProxy.copyFileSystemElement(callerId, nelsId, new String[]{sourceFilePath}, destinationParentFolderPath);
        return true;
    }

    public static boolean copyFolder(long callerId, long nelsId,
                                     String sourceFolderPath, String destinationParentFolderPath, String storageSourceFolderPath) throws Exception {
        return UserApi.copyFile(callerId, nelsId, sourceFolderPath, destinationParentFolderPath);

    }

    public static boolean moveFile(long callerId, long nelsId,
                                   String sourceFilePath, String destinationParentFolderPath) throws Exception {
        APIProxy.moveFileSystemElement(callerId, nelsId, new String[]{sourceFilePath}, destinationParentFolderPath);
        return true;

    }

    public static boolean moveFolder(long callerId, long nelsId,
                                     String sourceFolderPath, String destinationParentFolderPath, String storageSourceFolderPath) throws Exception {
        return UserApi.moveFile(callerId, nelsId, sourceFolderPath, destinationParentFolderPath);
    }

    public static NelsUser getNelsUserById(long id) {
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/users/" + id).request().get();
        int status = response.getStatus();
        if (status != 200) {
            logger.error(",response code:" + status);
            return null;
        }
        String responseBody = response.readEntity(String.class);

        JsonObject jsonObject = new JsonObject(responseBody);

        NelsUser ret = new NelsUser(
                new IDPUser(IDpUtilities.getIDp(jsonObject.getInteger(User.IDP_NUMBER)), jsonObject.getString(User.IDP_USER_NAME), jsonObject.getString(User.NAME), jsonObject.getString(User.EMAIL), jsonObject.getString(User.AFFILIATION)),
                jsonObject.getInteger(User.ID),
                UserTypeUtilities.getUserType(jsonObject.getInteger(User.USER_TYPE)),
                jsonObject.getBoolean(User.IS_ACTIVE));

        return ret;
    }

    public static NumberIndexedList getAllNelsUsers() {
        NumberIndexedList ret = new NumberIndexedList();
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/users").request().get();
        int status = response.getStatus();
        if (status != 200) {
            logger.error(",response code:" + status);
            return ret;
        }
        String responseBody = response.readEntity(String.class);
        JsonArray jsonArray = new JsonObject(responseBody).getJsonArray(JsonObjectKey.DATA);
        jsonArray.stream().map(JsonObject.class::cast).forEach(jsonObject ->
                ret.add(new NelsUser(
                        new IDPUser(IDpUtilities.getIDp(jsonObject.getInteger(User.IDP_NUMBER)), jsonObject.getString(User.IDP_USER_NAME), jsonObject.getString(User.NAME), jsonObject.getString(User.EMAIL), jsonObject.getString(User.AFFILIATION)),
                        jsonObject.getInteger(User.ID),
                        UserTypeUtilities.getUserType(jsonObject.getInteger(User.USER_TYPE)),
                        jsonObject.getBoolean(User.IS_ACTIVE)))
        );

        return ret;
    }

    public static String getNelsUsers(Optional<String> limit, Optional<String> offset, Optional<String> sort) {

        String query = limit.isPresent() ? (offset.isPresent() ? ("?limit=" + limit.get() + "&offset=" + offset.get()) : ("?limit=" + limit.get())) : (offset.isPresent() ? ("?offset=" + offset.get()) : (""));

        if (sort.isPresent()) {
            query = query + "&sort=" + sort.get();
        }
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/users" + query).request().get();
        int status = response.getStatus();
        if (status != 200) {
            logger.error("response code:" + status);
            throw new InternalServerErrorException();
        } else {
            return response.readEntity(String.class);
        }
    }

    public static boolean activateNelsUser(List<NelsUser> nelsUser) {
        logger.debug("activate nels user");
        return updateNelsUserActiveStatus(true, nelsUser);
    }

    public static boolean deactivateNelsUser(List<NelsUser> nelsUser) {
        logger.debug("deactivate nels user");
        return updateNelsUserActiveStatus(false, nelsUser);
    }

    // column isactivie in DB should be modified, it should be called isactive
    private static boolean updateNelsUserActiveStatus(boolean newStatus, List<NelsUser> nelsUser) {

        if (nelsUser != null && nelsUser.size() > 0) {
            for (NelsUser user : nelsUser) {
                if (user.isActive() != newStatus) {
                    try {
                        logger.debug("update nels user active status");
                        if (newStatus) {
                            getMasterClient().target(Config.getMasterApiUrl() + "/users/" + user.getId() + "/do").request().post(Entity.json(new JsonObject().put(JsonObjectKey.METHOD, JsonObjectValue.ACTIVATE).encode()));
                        } else {
                            getMasterClient().target(Config.getMasterApiUrl() + "/users/" + user.getId() + "/do").request().post(Entity.json(new JsonObject().put(JsonObjectKey.METHOD, JsonObjectValue.DEACTIVATE).encode()));
                        }
                    } catch (RuntimeException e) {
                        logger.error(e);
                        logger.debug("update nels user active status fails");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static long getNelsUsersCount() {
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/users/count").request().get();
        if (response.getStatus() == 200) {
            JsonObject jsonObject = new JsonObject(response.readEntity(String.class));
            return jsonObject.getInteger(JsonObjectKey.COUNT);
        } else {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Update system user type to a user type
     */
    public static boolean modifySystemUserType(List<NelsUser> nelsUser, ASystemUser newUserType) {
        logger.debug("Modify system user type");
        if (nelsUser != null && nelsUser.size() > 0) {
            for (NelsUser user : nelsUser) {
                try {
                    getMasterClient().property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).target(Config.getMasterApiUrl() + "/users/" + user.getId()).request().method("PATCH", Entity.json(new JsonObject().put(User.USER_TYPE, newUserType.getId()).encode()));
                } catch (RuntimeException e) {
                    logger.error(e);
                    logger.debug("Modify system user type fails");
                    return false;
                }
            }
        }
        return true; //none of users is updated
    }

    public static boolean isUserRegistered(IDPUser idpUser) {
        return getNelsUser(idpUser) != null;
    }

    public static NelsUser registerUser(IDPUser idpUser) {
        NelsUser ret = getNelsUser(idpUser);
        if (ret == null) { // not yet registered
            JsonObject requestBody = new JsonObject();
            requestBody.put(User.IDP_NUMBER, idpUser.getIdp().getId());
            requestBody.put(User.IDP_USER_NAME, idpUser.getIdpUsername());
            requestBody.put(User.USER_TYPE, new NormalUser().getId());
            requestBody.put(User.IS_ACTIVE, true);
            requestBody.put(User.NAME, idpUser.getFullname());
            requestBody.put(User.EMAIL, idpUser.getEmail());
            requestBody.put(User.AFFILIATION, idpUser.getAffiliation());
            Response response = getMasterClient().target(Config.getMasterApiUrl() + "/users").request().post(Entity.json(requestBody.encode()));

            if (response.getStatus() == HttpResponseStatus.CREATED.code()) {
                return getNelsUser(idpUser);
            }
        }
        return ret;
    }

    public static NelsUser getNelsUser(IDPUser idpUser) {
        return getNelsUserByIdpUserName(idpUser.getIdpUsername());
    }

    public static NelsUser getNelsUserByIdpUserName(String idpUserName) {
        NelsUser ret = null;

        JsonObject requestBody = new JsonObject();
        requestBody.put(User.IDP_USER_NAME, idpUserName);

        String responseStr = "";
        try {
            responseStr = requestSearchNelsUsers(requestBody.encode(), Optional.empty(), Optional.empty(), Optional.empty());
        }catch (InternalServerErrorException e) {
            logger.error(e.getLocalizedMessage());
        }
        JsonArray jsonArray = new JsonArray(responseStr);
        if (jsonArray.size() != 0) {
            JsonObject result = jsonArray.getJsonObject(0);
            ret = new NelsUser(new IDPUser(IDpUtilities.getIDp(result.getInteger(User.IDP_NUMBER)), result.getString(User.IDP_USER_NAME), result.getString(User.NAME), result.getString(User.EMAIL), result.getString(User.AFFILIATION)), result.getInteger(User.ID),
                    UserTypeUtilities.getUserType(result.getInteger(User.USER_TYPE)), result.getBoolean(User.IS_ACTIVE));
        }
        return ret;

    }

    public static NumberIndexedList searchNeLSUsers(long id, String emailPartial, String namePartial) {
        NumberIndexedList ret = new NumberIndexedList();
        if (id == -1 && emailPartial.trim().equals("") && namePartial.trim().equals("")) {
            return getAllNelsUsers();
        }
        //caution - no check is done for SQL Injection on string filter input parameters
        JsonObject requestBody = new JsonObject();
        if (id != -1) {
            requestBody.put(User.ID, id);
        } else {
            if (!emailPartial.equals("")) {
                requestBody.put(User.EMAIL, emailPartial);
            }
            if (!namePartial.equals("")) {
                requestBody.put(User.NAME, namePartial);
            }
        }
        String responseStr = "";
        try {
            responseStr = requestSearchNelsUsers(requestBody.encode(), Optional.empty(), Optional.empty(), Optional.empty());
        }catch (InternalServerErrorException e) {
            logger.error(e.getLocalizedMessage());
        }
        JsonArray jsonArray = new JsonArray(responseStr);
        jsonArray.stream().map(JsonObject.class::cast).forEach(result ->
                ret.add(new NelsUser(new IDPUser(IDpUtilities.getIDp(result.getInteger(User.IDP_NUMBER)), result.getString(User.IDP_USER_NAME), result.getString(User.NAME), result.getString(User.EMAIL), result.getString(User.AFFILIATION)), result.getInteger(User.ID),
                        UserTypeUtilities.getUserType(result.getInteger(User.USER_TYPE)), result.getBoolean(User.IS_ACTIVE)))
        );
        return ret;
    }

    public static NumberIndexedList getProjectsForUser(long userId){
        NumberIndexedList ret = new NumberIndexedList();
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/users/" + userId + "/projects/ids").request().get();
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonArray jsonArray = new JsonArray(response.readEntity(String.class));
            JsonObject jsonObject;
            for (int i = 0; i < jsonArray.size(); i++) {
                jsonObject = jsonArray.getJsonObject(i);
                int projectId = jsonObject.getInteger(ProjectUsers.PROJECT_ID);

                NelsProject nelsProject = ProjectApi.getProject(projectId);
                AProjectMembership membership = ProjectApi.getProjectMembershipType(projectId, userId);
                if (nelsProject != null && membership != null) {
                    ret.add(new ProjectUser(i, null, nelsProject, membership));
                } else {
                    throw new InternalServerErrorException();
                }
            }
        }
        return ret;
    }

    public static String getProjects(long userId, Optional<String> limit, Optional<String> offset) {
        String query = limit.isPresent() ? (offset.isPresent() ? ("?limit=" + limit.get() + "&offset=" + offset.get()) : ("?limit=" + limit.get())) : (offset.isPresent() ? ("?offset=" + offset.get()) : (""));
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/users/" + userId + "/projects" + query).request().get();
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            return response.readEntity(String.class);
        } else {
            throw new InternalServerErrorException();
        }
    }

    public static String searchNelsUsers(String queryStr, Optional<String> limit, Optional<String> offset, Optional<String> sort) {
        JsonObject requestBody = new JsonObject();
        requestBody.put(JsonObjectKey.QUERY, queryStr);
        return requestSearchNelsUsers(requestBody.encode(), limit, offset, sort);
    }

    private static String requestSearchNelsUsers(String json, Optional<String> limit, Optional<String> offset, Optional<String> sort) {

        Map<String, String> queryParam = new HashMap<>();
        limit.ifPresent(param -> queryParam.put("limit", param));
        offset.ifPresent(param -> queryParam.put("offset", param));
        sort.ifPresent(param -> queryParam.put("sort", param));
        Response response;
        if (queryParam.size() > 0) {
            response = getMasterClient().target(Config.constructFullUrl("/users/query", new Object[0], queryParam)).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
        } else {
            response = getMasterClient().target(Config.constructFullUrl("/users/query")).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
        }
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new InternalServerErrorException(response.readEntity(String.class));
        }
    }

}
