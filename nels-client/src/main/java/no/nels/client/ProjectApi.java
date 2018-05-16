package no.nels.client;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import no.nels.commons.abstracts.AProjectMembership;
import no.nels.commons.constants.JsonObjectKey;
import no.nels.commons.constants.db.Project;
import no.nels.commons.constants.db.ProjectUsers;
import no.nels.commons.model.NelsProject;
import no.nels.commons.model.NelsUser;
import no.nels.commons.model.NumberIndexedList;
import no.nels.commons.model.ProjectUser;
import no.nels.commons.utilities.ProjectMembershipTypeUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class ProjectApi extends ApiClient{
    private static final Logger logger = LogManager.getLogger(ProjectApi.class);

    private static boolean assignUserRoleInProject(long projectId, long nelsId, String projectRole) {
        JsonObject body = new JsonObject();
        body.put("role", projectRole);

        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/projects/" + String.valueOf(projectId) + "/users/" + String.valueOf(nelsId)).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(body.encode()));

        if (response.getStatus() == 201) {
            return true;
        }
        return false;
    }

    public static boolean addUserToProject(long projectId, long nelsId, String projectRole) {
        return assignUserRoleInProject(projectId, nelsId, projectRole);
    }

    public static boolean changeProjectMembershipType(long projectId, long nelsId, String projectRole) {
        return assignUserRoleInProject(projectId, nelsId, projectRole);
    }

    public static boolean removeUserFromProject(long projectId, long nelsId) {


        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/projects/" + String.valueOf(projectId) + "/users/" + String.valueOf(nelsId)).request().delete();

        return response.getStatus() == 204;
    }


    public static boolean createProject(long projectId, String projectName) {

        JsonObject body = new JsonObject();
        body.put("project_name", projectName);

        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/projects/" + String.valueOf(projectId)).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(body.encode()));

        return response.getStatus() == 201;
    }

    public static boolean renameProject(long projectId, String newProjectName) {
        JsonObject body = new JsonObject();
        body.put("project_name", newProjectName);

        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/projects/" + String.valueOf(projectId) + "/rename").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(body.encode()));

        return response.getStatus() == HttpResponseStatus.NO_CONTENT.code();
    }


    public static boolean deleteProject(long projectId) {

        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/projects/" + String.valueOf(projectId)).request().delete();

        return response.getStatus() == HttpResponseStatus.NO_CONTENT.code();
    }

    public static NelsProject getProject(long id){
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/" + id).request().accept(MediaType.APPLICATION_JSON).get();
        NelsProject ret = null;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonObject jsonObject = new JsonObject(response.readEntity(String.class));
            try {
                ret = new NelsProject(jsonObject.getInteger(Project.ID), jsonObject.getString(Project.NAME), jsonObject.getString(Project.DESCRIPTION), df.parse(jsonObject.getString(Project.CREATION_DATE)));
            } catch (ParseException e) {
                ret = new NelsProject(jsonObject.getInteger(Project.ID), jsonObject.getString(Project.NAME), jsonObject.getString(Project.DESCRIPTION), Date.from(Instant.now()));
            }
        }
        return ret;
    }

    public static NumberIndexedList getAllProjects(){
        NumberIndexedList ret = new NumberIndexedList();
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects").request().accept(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonArray jsonArray = new JsonObject(response.readEntity(String.class)).getJsonArray(JsonObjectKey.DATA);
            ret = convertToNelsProjects(jsonArray);
        }
        return ret;
    }

    private static NumberIndexedList convertToNelsProjects(JsonArray jsonArray) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        NumberIndexedList ret = new NumberIndexedList();
        for (Object object : jsonArray) {
            JsonObject jsonObject = JsonObject.class.cast(object);
            try {
                ret.add(new NelsProject(jsonObject.getInteger(Project.ID), jsonObject.getString(Project.NAME), jsonObject.getString(Project.DESCRIPTION), df.parse(jsonObject.getString(Project.CREATION_DATE))));
            } catch (ParseException e) {
                ret.add(new NelsProject(jsonObject.getInteger(Project.ID), jsonObject.getString(Project.NAME), jsonObject.getString(Project.DESCRIPTION), Date.from(Instant.now())));
            }
        }
        return ret;
    }

    public static String getProjects(Optional<String> limit, Optional<String> offset, Optional<String> sort) {
        String query = limit.isPresent() ? (offset.isPresent() ? ("?limit=" + limit.get() + "&offset=" + offset.get()) : ("?limit=" + limit.get())) : (offset.isPresent() ? ("?offset=" + offset.get()) : (""));
        if (sort.isPresent()) {
            query = query + "&sort=" + sort.get();
        }
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects" + query).request().accept(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            return response.readEntity(String.class);
        } else {
            throw new InternalServerErrorException();
        }
    }

    public static long getProjectsCount() {
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/count").request().get();
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonObject jsonObject = new JsonObject(response.readEntity(String.class));
            int count = jsonObject.getInteger(JsonObjectKey.COUNT);
            return count;
        } else {
            throw new InternalServerErrorException();
        }
    }

    public static Boolean addNewProject(String name, String description) {
        JsonObject requestBody = new JsonObject();
        requestBody.put(Project.NAME, name);
        requestBody.put(Project.DESCRIPTION, description);

        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects").request().post(Entity.json(requestBody.encode()));

        if (response.getStatus() == HttpResponseStatus.CREATED.code()) {
            return true;
        }
        return false;
    }

    public static NumberIndexedList searchProjects(long id, String namePartial) {
        //caution - no check is done for SQL Injection on string filter input parameters
        NumberIndexedList ret = new NumberIndexedList();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        JsonObject requestBody = new JsonObject();
        if (id >= 0) {
            requestBody.put(Project.ID, id);
        }

        if (namePartial != null) {
            requestBody.put(Project.NAME, namePartial);
        }
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/query").request().post(Entity.json(requestBody.encode()));
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonArray jsonArray = new JsonArray(response.readEntity(String.class));
            ret = convertToNelsProjects(jsonArray);
        }
        return ret;
    }

    public static NelsProject getProjectByName(String name) {
        JsonObject requestBody = new JsonObject();
        requestBody.put(Project.NAME, name);
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/query").request().post(Entity.json(requestBody.encode()));
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonArray jsonArray = new JsonArray(response.readEntity(String.class));
            JsonObject jsonObject = jsonArray.getJsonObject(0);
            NelsProject ret = new NelsProject(jsonObject.getInteger(Project.ID), jsonObject.getString(Project.NAME), jsonObject.getString(Project.DESCRIPTION), Date.from(jsonObject.getInstant(Project.CREATION_DATE)));
            return ret;
        }
        return null;
    }

    public static boolean deleteProject(List<Long> projectIds) {
        for (long projectId : projectIds) {
            Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/" + projectId).request().delete();
            if (response.getStatus() != HttpResponseStatus.NO_CONTENT.code()) {
                return false;
            }
        }
        return true;
    }

    public static boolean addUserToProject(List<Long> userIds, List<Long> projectIds, AProjectMembership membership) {

        JsonObject jsonObject = new JsonObject();
        jsonObject.put(ProjectUsers.MEMBERSHIP_TYPE, membership.getId());
        for (long projectId : projectIds) {
            for (long userId : userIds) {
                Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/" + projectId + "/users/" + userId).request().post(Entity.json(jsonObject.encode()));
                if (response.getStatus() != HttpResponseStatus.OK.code()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean removeUserFromProject(List<Long> userIds, List<Long> projectIds) {
        for (long projectId : projectIds) {
            for (long userId : userIds) {
                Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/" + projectId + "/users/" + userId).request().delete();
                if (response.getStatus() != HttpResponseStatus.NO_CONTENT.code()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean changeProjectMembership(List<Long> userIds, List<Long> projectIds, AProjectMembership newMembership) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(ProjectUsers.MEMBERSHIP_TYPE, newMembership.getId());
        for (long projectId : projectIds) {
            for (long userId : userIds) {
                Response response = getMasterClient().property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).target(Config.getMasterApiUrl() + "/projects/" + projectId + "/users/" + userId).request().method("PATCH", Entity.json(jsonObject.encode()));
                if (response.getStatus() != HttpResponseStatus.NO_CONTENT.code()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<Integer> getPojectIdListForUser(long userId) {
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/users/" + userId + "/projects/ids").request().get();
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonArray jsonArray = new JsonArray(response.readEntity(String.class));
            List<Integer> list = new ArrayList<>(jsonArray.size());
            jsonArray.stream().map(JsonObject.class::cast).forEach(jsonObject -> list.add(jsonObject.getInteger(ProjectUsers.PROJECT_ID)));
            return list;
        }
        return null;
    }

    public static List<Integer> getUserIdListForProject(long projectId) {
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/" + projectId + "/users/ids").request().get();
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonArray jsonArray = new JsonArray(response.readEntity(String.class));
            List<Integer> list = new ArrayList<>(jsonArray.size());
            jsonArray.stream().map(JsonObject.class::cast).forEach(jsonObject -> list.add(jsonObject.getInteger(ProjectUsers.USER_ID)));
            return list;
        }
        return null;
    }

    public static AProjectMembership getProjectMembershipType(long projectId, long userId) {
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/" + projectId + "/users/" + userId).request().get();
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonObject jsonObject = new JsonObject(response.readEntity(String.class));
            return ProjectMembershipTypeUtilities.getProjectMembership(jsonObject.getInteger(ProjectUsers.MEMBERSHIP_TYPE));
        }
        return null;
    }

    public static NumberIndexedList getMembersInProject(long projectId) {
        NumberIndexedList ret = new NumberIndexedList();
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/" + projectId + "/users/ids").request().get();
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonArray jsonArray = new JsonArray(response.readEntity(String.class));
            JsonObject jsonObject;
            for (int i = 0; i < jsonArray.size(); i++) {
                jsonObject = jsonArray.getJsonObject(i);
                int userId = jsonObject.getInteger(ProjectUsers.USER_ID);
                NelsUser nelsUser = UserApi.getNelsUserById(userId);
                AProjectMembership membership = getProjectMembershipType(projectId, userId);
                if (nelsUser != null && membership != null) {
                    ret.add(new ProjectUser(i, nelsUser, null, membership));
                } else {
                    throw new InternalServerErrorException();
                }
            }
        }
        return ret;
    }

    public static boolean updateProject(long id, String name, String description) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(Project.NAME, name);
        jsonObject.put(Project.DESCRIPTION, description);
        Response response = getMasterClient().property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).target(Config.getMasterApiUrl() + "/projects/" + id).request().method("PATCH", Entity.json(jsonObject.encode()));
        if (response.getStatus() == HttpResponseStatus.NO_CONTENT.code()) {
            return true;
        }
        return false;
    }

    public static boolean updateProject(long id, String description) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(Project.DESCRIPTION, description);
        Response response = getMasterClient().property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).target(Config.getMasterApiUrl() + "/projects/" + id).request().method("PATCH", Entity.json(jsonObject.encode()));
        if (response.getStatus() == HttpResponseStatus.NO_CONTENT.code()) {
            return true;
        }
        return false;
    }

    public static boolean isProjectNameExisting(String name) {
        JsonObject requestBody = new JsonObject();
        requestBody.put(Project.NAME, name);
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/projects/query").request().post(Entity.json(requestBody.encode()));
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            JsonArray jsonArray = new JsonArray(response.readEntity(String.class));
            JsonObject jsonObject;
            for (int i = 0; i < jsonArray.size(); i++) {
                jsonObject = jsonArray.getJsonObject(i);
                if (jsonObject.getString(Project.NAME).equals(name)) {
                    return true;
                }
            }
            return false;
        }
        throw new InternalServerErrorException();
    }

    public static String searchNelsProjects(String queryStr, Optional<String> limit, Optional<String> offset, Optional<String> sort) {
        JsonObject requestBody = new JsonObject();
        requestBody.put(JsonObjectKey.QUERY, queryStr);
        return requestSearchNelsProjects(requestBody.encode(), limit, offset, sort);
    }

    private static String requestSearchNelsProjects(String json, Optional<String> limit, Optional<String> offset, Optional<String> sort) {

        Map<String, String> queryParam = new HashMap<>();
        limit.ifPresent(param -> queryParam.put("limit", param));
        offset.ifPresent(param -> queryParam.put("offset", param));
        sort.ifPresent(param -> queryParam.put("sort", param));
        Response response;
        if (queryParam.size() > 0) {
            response = getMasterClient().target(Config.constructFullUrl("/projects/query", new Object[0], queryParam)).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
        } else {
            response = getMasterClient().target(Config.constructFullUrl("/projects/query")).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
        }
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new InternalServerErrorException(response.readEntity(String.class));
        }
    }
}
