package no.norstore.storebioinfo.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.commons.constants.JsonObjectKey;
import no.nels.commons.constants.LogContextType;
import no.norstore.storebioinfo.Config;
import no.norstore.storebioinfo.Route;
import no.norstore.storebioinfo.constants.*;
import no.norstore.storebioinfo.exceptions.IllegalBodyException;
import no.norstore.storebioinfo.facades.ResrouceFacade;
import no.norstore.storebioinfo.helpers.JdbcHelper;
import no.norstore.storebioinfo.utils.ParameterizedCompositeFuture;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;
import org.irods.jargon.core.pub.io.IRODSFile;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class ProjectRequestHandler implements HttpRequestHandler {
    private static Logger logger = LoggerFactory.getLogger(ProjectRequestHandler.class);

    public void getProject(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            long projectId = map.get(UrlParam.PROJECT_ID);

            String sql = "SELECT project.project_id, project.description, project.name, project.contact_person, project.contact_affiliation, project.diskusage as disk_usage, project.contact_email, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name) WHERE project_id=?";

            Route.vertxDBHelper.getOne(sql, new JsonArray().add(projectId), result -> returnResponse(routingContext, HttpResponseStatus.OK, result));
        });
    }

    public void updateProject(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            long projectId = map.get(UrlParam.PROJECT_ID);

            JsonObject body = routingContext.getBodyAsJson();
            List<String> params = new ArrayList<>();

            JsonArray sqlParams = new JsonArray();
            if (body.containsKey(JsonKey.NAME)) {
                params.add("name=?");
                sqlParams.add(body.getString(JsonKey.NAME));
            }
            if (body.containsKey(JsonKey.DESCRIPTION)) {
                params.add("description=?");
                sqlParams.add(body.getString(JsonKey.DESCRIPTION));
            }
            if (body.containsKey(JsonKey.CONTACT_PERSON)) {
                params.add("contact_person=?");
                sqlParams.add(body.getString(JsonKey.CONTACT_PERSON));
            }
            if (body.containsKey(JsonKey.CONTACT_EMAIL)) {
                params.add("contact_email=?");
                sqlParams.add(body.getString(JsonKey.CONTACT_EMAIL));
            }
            if (body.containsKey(JsonKey.CONTACT_AFFILIATION)) {
                params.add("contact_affiliation=?");
                sqlParams.add(body.getString(JsonKey.CONTACT_AFFILIATION));
            }
            if (params.size() > 0) {
                sqlParams.add(projectId);
                String sql = "UPDATE project SET " + StringUtils.join(params, ",") + " WHERE project_id=?";

                Route.vertxDBHelper.updateOne(sql, sqlParams, result -> {
                    if (result.succeeded()) {
                        routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                    } else {
                        logger.error("", result.cause());
                        if (result.cause() instanceof SQLException) {
                            String stateCode = SQLException.class.cast(result.cause()).getSQLState();
                            if (stateCode.equals(SqlErrorCode.UNIQUE_VIOLATION)) {
                                routingContext.response().setStatusCode(HttpResponseStatus.CONFLICT.code()).end("Project name is already used.");
                            } else {
                                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                        .end(new JsonObject().put(JsonKey.DESCRIPTION, result.cause().getMessage()).encodePrettily());
                            }
                        } else {
                            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .end(new JsonObject().put(JsonKey.DESCRIPTION, result.cause().getMessage()).encodePrettily());
                        }
                    }
                });
            } else {
                throw new IllegalBodyException("Request body is missing");
            }

        });
    }

    public void deleteProject(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            JsonObject body = validateRequestBody(routingContext, JsonKey.FEDERATED_ID);
            long projectId = map.get(UrlParam.PROJECT_ID);
            JsonArray params = new JsonArray();
            params.add(projectId);


            String sql = "SELECT count(*) FROM project_policy WHERE project_id IN (SELECT externalref FROM project WHERE project.project_id=?)";
            Route.vertxDBHelper.count(sql, params, result -> {
                if (result.succeeded()) {
                    int count = result.result();
                    if (count > 0) {
                        routingContext.response().setStatusCode(HttpResponseStatus.PRECONDITION_FAILED.code()).end(
                                new JsonObject().put(JsonKey.DESCRIPTION, "This project includes existing datasets, you need to remove them first.").encodePrettily());
                    } else {
                        Route.vertxDBHelper.updateMultipleSqlInTransaction(finalResult -> {
                                    if (finalResult.succeeded()) {
                                        routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                                    } else {
                                        logger.error("", finalResult.cause());
                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                .end(new JsonObject().put(JsonKey.DESCRIPTION, finalResult.cause().getMessage()).encodePrettily());
                                    }
                                }, (sqlConnection, asyncResultHandler) ->
                                        sqlConnection.updateWithParams("DELETE FROM user_role_project WHERE urp_project_id=?", params, removeURPResult -> {
                                            if (removeURPResult.succeeded()) {
                                                sqlConnection.updateWithParams("DELETE FROM user_project WHERE project_id=?", params, removeUPResult -> {
                                                    if (removeUPResult.succeeded()) {
                                                        sqlConnection.updateWithParams("DELETE FROM project WHERE project_id=?", params, removeProjectResult -> {
                                                            if (removeProjectResult.succeeded()) {
                                                                Route.vertxDBHelper.getOne("SELECT project.name, quota2.id from project INNER JOIN quota2 on project.project_quota = quota2.quota_id and project.project_id=?", new JsonArray().add(projectId), stringAsyncResult -> {
                                                                    if (stringAsyncResult.succeeded()) {

                                                                        JsonObject r = new JsonObject(stringAsyncResult.result());
                                                                        JsonObject logText = new JsonObject();
                                                                        JsonArray fields = new JsonArray();
                                                                        fields.add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonObjectKey.NAME)
                                                                                .put(JsonObjectKey.VALUE, r.getString(JsonKey.NAME)))
                                                                                .add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonObjectKey.PROJECT_ID)
                                                                                        .put(JsonObjectKey.VALUE, projectId));

                                                                        logText.put(JsonObjectKey.FIELDS, fields);
                                                                        sendJournal(routingContext, new JsonObject().put(JsonObjectKey.CONTEXT_ID, LogContextType.SBI_QUOTA_PROJECT_REMOVED.getValue())
                                                                                .put(JsonObjectKey.OPERATOR_ID, -1)
                                                                                .put(JsonObjectKey.TARGET_ID, r.getLong(JsonKey.ID))
                                                                                .put(JsonObjectKey.TEXT, logText.encode())
                                                                                .put(JsonObjectKey.FEDERATED_ID, body.getString(JsonKey.FEDERATED_ID)));
                                                                        asyncResultHandler.handle(Future.succeededFuture());
                                                                    } else {
                                                                        asyncResultHandler.handle(Future.failedFuture(stringAsyncResult.cause()));
                                                                    }
                                                                });
                                                            } else {
                                                                asyncResultHandler.handle(Future.failedFuture(removeProjectResult.cause()));
                                                            }
                                                        });
                                                    } else {
                                                        asyncResultHandler.handle(Future.failedFuture(removeUPResult.cause()));
                                                    }
                                                });
                                            } else {
                                                asyncResultHandler.handle(Future.failedFuture(removeURPResult.cause()));
                                            }
                                        })
                        );
                    }
                } else {
                    logger.error("", result.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .end(new JsonObject().put(JsonKey.DESCRIPTION, result.cause().getMessage()).encodePrettily());
                }
            });
        });
    }

    public void getProjectMembers(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            long projectId = map.get(UrlParam.PROJECT_ID);

            String sql = "SELECT esysbio_user.user_id, esysbio_user.firstname, esysbio_user.surname, esysbio_user.email, esysbio_user.federated_id, user_role_project.urp_role_id FROM esysbio_user INNER JOIN user_role_project ON esysbio_user.user_id = user_role_project.urp_user_id AND user_role_project.urp_project_id=? WHERE esysbio_user.federated_id IS NOT NULL";
            Route.vertxDBHelper.select(sql, new JsonArray().add(projectId), result -> returnResponse(routingContext, HttpResponseStatus.OK, result));
        });
    }

    public void createProject(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            JsonObject body = validateRequestBody(routingContext, JsonKey.NAME, JsonKey.DESCRIPTION, JsonKey.FEDERATED_ID, JsonKey.QUOTA_ID);

            String name = body.getString(JsonKey.NAME);
            String description = body.getString(JsonKey.DESCRIPTION);
            String externalRef = UUID.randomUUID().toString();
            Instant startDate = Instant.now();
            String federatedId = body.getString(JsonKey.FEDERATED_ID);
            long quotaId = body.getLong(JsonKey.QUOTA_ID);
            String contactPerson = body.getString(JsonKey.CONTACT_PERSON, "");
            String contactEmail = body.getString(JsonKey.CONTACT_EMAIL, "");
            String contactAffiliation = body.getString(JsonKey.CONTACT_AFFILIATION, "");


            Route.vertxDBHelper.select("SELECT user_id FROM esysbio_user WHERE federated_id=?", new JsonArray().add(federatedId), userIdResult -> {
                if (userIdResult.succeeded()) {
                    JsonArray jsonArray = new JsonArray(userIdResult.result());
                    if (jsonArray.size() >= 1) {
                        String sql = "INSERT INTO project (name, description, externalref, startdate, owner_id, project_quota, contact_affiliation, contact_email, contact_person, restricted_access, diskquota, diskusage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
                        JsonArray params = new JsonArray();
                        params.add(name).add(description).add(externalRef).add(startDate).add(jsonArray.getJsonObject(0).getInteger(JsonKey.USER_ID)).add(quotaId).add(contactAffiliation).add(contactEmail).add(contactPerson).add(true).add(0).add(0);

                        Route.vertxDBHelper.insert(sql, params, asyncResult -> {
                            if (asyncResult.succeeded()) {

                                long newId = Long.valueOf(asyncResult.result());
                                Route.vertxDBHelper.getOne("select id from quota2 where quota_id=?", new JsonArray().add(quotaId), stringAsyncResult -> {
                                    if (stringAsyncResult.succeeded()) {
                                        JsonObject journalEntry = new JsonObject();
                                        JsonObject logText = new JsonObject();
                                        JsonArray fields = new JsonArray();
                                        fields.add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonObjectKey.NAME)
                                                    .put(JsonObjectKey.VALUE, name))
                                                .add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonObjectKey.PROJECT_ID)
                                                .put(JsonObjectKey.VALUE, newId));

                                        logText.put(JsonObjectKey.FIELDS, fields);
                                        journalEntry.put(JsonObjectKey.CONTEXT_ID, LogContextType.SBI_QUOTA_PROJECT_ADDED.getValue())
                                                .put(JsonObjectKey.OPERATOR_ID, -1)
                                                .put(JsonObjectKey.TARGET_ID, new JsonObject(stringAsyncResult.result()).getLong(JsonKey.ID))
                                                .put(JsonObjectKey.TEXT, logText.encode())
                                                .put(JsonObjectKey.FEDERATED_ID, federatedId);
                                        sendJournal(routingContext, journalEntry);

                                        routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end(new JsonObject().put(JsonKey.ID, asyncResult.result()).encode());
                                    } else {
                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                                    }
                                });

                            } else {
                                logger.error("", asyncResult.cause());
                                if (asyncResult.cause() instanceof SQLException) {
                                    String stateCode = SQLException.class.cast(asyncResult.cause()).getSQLState();
                                    if (stateCode.equals(SqlErrorCode.UNIQUE_VIOLATION)) {
                                        routingContext.response().setStatusCode(HttpResponseStatus.CONFLICT.code()).end("Project name is already used.");
                                    } else {
                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                .end(new JsonObject().put(JsonKey.DESCRIPTION, asyncResult.cause().getMessage()).encodePrettily());
                                    }
                                } else {
                                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                            .end(new JsonObject().put(JsonKey.DESCRIPTION, asyncResult.cause().getMessage()).encodePrettily());
                                }
                            }
                        });
                    } else {
                        routingContext.response().setStatusCode(HttpResponseStatus.PRECONDITION_FAILED.code()).end("The creator doesn't have sbi profile.");
                    }
                } else {
                    logger.error("", userIdResult.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .end(new JsonObject().put(JsonKey.DESCRIPTION, userIdResult.cause().getMessage()).encodePrettily());
                }
            });
        });
    }

    public void changeProjectMembers(RoutingContext routingContext) {

        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            long projectId = map.get(UrlParam.PROJECT_ID);
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.METHOD, JsonKey.DATA);
            String method = jsonObject.getString(JsonKey.METHOD);

            JsonArray responseBody = new JsonArray();
            JsonArray jsonArray = jsonObject.getJsonArray(JsonKey.DATA);

            switch (method) {
                case MethodName.ADD:
                    JsonObject object;
                    for (int i = 0; i < jsonArray.size(); i++) {
                        object = jsonArray.getJsonObject(i);
                        String federatedId = object.getString(JsonKey.FEDERATED_ID);
                        int role = object.getInteger(JsonKey.ROLE);
                        boolean bool = JdbcHelper.addProjectMember(projectId, federatedId, role);
                        if (!bool) {
                            responseBody.add(federatedId);
                        }
                    }
                    routingContext.response().end(responseBody.encode());
                    break;
                case MethodName.DELETE:
                    for (int i = 0; i < jsonArray.size(); i++) {
                        String federatedId = jsonArray.getString(i);
                        boolean bool = JdbcHelper.removeProjectMember(projectId, federatedId);
                        if (!bool) {
                            responseBody.add(federatedId);
                        }
                    }
                    routingContext.response().end(responseBody.encode());
                    break;
                default:
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            }
        });
    }

    public void searchProjects(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CONTACT_PERSON, JsonKey.DISK_USAGE, JsonKey.CREATION_DATE, JsonKey.UTILIZATION);

            JsonObject body = validateRequestBody(routingContext, JsonKey.QUERY);

            String query = body.getString(JsonKey.QUERY);

            StringBuilder builder = new StringBuilder();
            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.quota_size, quota2.used_size, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) ");
            builder.append("WHERE LOWER(project.name) LIKE LOWER('%" + query + "%') OR LOWER(quota2.name) LIKE LOWER('%" + query + "%') OR LOWER(project.contact_person) LIKE LOWER('%" + query + "%') ");
            if (sort.isPresent()) {
                switch (sort.get()) {
                    case "-id":
                        builder.append("order by project_id desc");
                        break;
                    case "id":
                        builder.append("order by project_id asc");
                        break;
                    case "-name":
                        builder.append("order by name desc");
                        break;
                    case "name":
                        builder.append("order by name asc");
                        break;
                    case "-contact_person":
                        builder.append("order by contact_person desc");
                        break;
                    case "contact_person":
                        builder.append("order by contact_person asc");
                        break;
                    case "-disk_usage":
                        builder.append("order by disk_usage desc");
                        break;
                    case "disk_usage":
                        builder.append("order by disk_usage asc");
                        break;
                    case "-creation_date":
                        builder.append("order by startdate desc");
                        break;
                    case "creation_date":
                        builder.append("order by startdate asc");
                        break;
                    case "-utilization":
                        builder.append("order by utilization desc");
                        break;
                    case "utilization":
                        builder.append("order by utilization asc");
                        break;
                    default:
                        builder.append("order by name");
                }
            } else {
                builder.append("order by name");
            }
            if (pair.getRight().isPresent()) {
                builder.append(" offset ").append(pair.getRight().get());
            }
            builder.append(" limit ").append(pair.getLeft().get());

            String countSql = "SELECT count(*) FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name) WHERE LOWER(project.name) LIKE LOWER('%" + query + "%') OR LOWER(quota2.name) LIKE LOWER('%" + query + "%') OR LOWER(project.contact_person) LIKE LOWER('%" + query + "%')";
            Route.vertxDBHelper.select(builder.toString(), result -> returnResponseWithCount(routingContext, HttpResponseStatus.OK, result, countSql));
        });
    }

    public void getProjects(RoutingContext routingContext) {

        String federatedId = routingContext.request().getHeader(JsonKey.FEDERATED_ID_IN_HEADER);

        if (StringUtils.isEmpty(federatedId)) {
            validateRequest(routingContext, () -> {
                Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

                Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CONTACT_PERSON, JsonKey.DISK_USAGE, JsonKey.CREATION_DATE, JsonKey.UTILIZATION);

                StringBuilder builder = new StringBuilder();

                if (sort.isPresent()) {
                    switch (sort.get()) {
                        case "-id":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by project_id desc");
                            break;
                        case "id":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by project_id asc");
                            break;
                        case "-name":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by name desc");
                            break;
                        case "name":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by name asc");
                            break;
                        case "-contact_person":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by contact_person desc");
                            break;
                        case "contact_person":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by contact_person asc");
                            break;
                        case "-disk_usage":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by disk_usage desc");
                            break;
                        case "disk_usage":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by disk_usage asc");
                            break;
                        case "-creation_date":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by startdate desc");
                            break;
                        case "creation_date":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by startdate asc");
                            break;
                        case "-utilization":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by utilization desc");
                            break;
                        case "utilization":
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by utilization asc");
                            break;
                        default:
                            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, cast(quota2.used_size as FLOAT)/NULLIF(quota2.quota_size, 0) AS utilization, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by name");
                    }
                } else {
                    builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name ) order by name");
                }

                if (pair.getRight().isPresent()) {
                    builder.append(" offset ").append(pair.getRight().get());
                }

                builder.append(" limit ").append(pair.getLeft().get());

                Route.vertxDBHelper.select(builder.toString(), result -> returnResponseWithCount(routingContext, HttpResponseStatus.OK, result, "SELECT count(*) FROM project"));
            });

        } else {
            String sql = "SELECT project_id, name, description, contact_person, startdate FROM project WHERE project_id IN (SELECT project_id FROM user_project INNER JOIN esysbio_user on user_project.user_id=esysbio_user.user_id WHERE esysbio_user.federated_id LIKE ?) order by name";
            Route.vertxDBHelper.select(sql, new JsonArray().add(federatedId), result -> returnResponse(routingContext, HttpResponseStatus.OK, result));
        }
    }

    public void doAction(RoutingContext routingContext) {

        validateRequest(routingContext, () -> {
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.METHOD);
            String methodName = jsonObject.getString(JsonKey.METHOD);

            if (methodName.trim().equalsIgnoreCase(MethodName.RE_POPULATE_PROJECT_DISK_USAGE)) {
                recomputeProjectDiskUsage(routingContext);
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, "Method " + methodName + " is not supported").encodePrettily());
            }
        });
    }

    public void createDataset(RoutingContext routingContext) {
        String federatedId = routingContext.request().getHeader(JsonKey.FEDERATED_ID_IN_HEADER);

        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            long projectId = map.get(UrlParam.PROJECT_ID);

            JsonObject body = validateRequestBody(routingContext, JsonKey.DATA_SET_TYPE_ID, JsonKey.NAME, JsonKey.DESCRIPTION);
            int dataSetTypeId = body.getInteger(JsonKey.DATA_SET_TYPE_ID);
            String name = body.getString(JsonKey.NAME);
            String description = body.getString(JsonKey.DESCRIPTION);

            String dataSetId = UUID.randomUUID().toString();

            boolean result = false;

            try {
                result = JdbcHelper.addDataSetToProject(dataSetId, federatedId, dataSetTypeId, projectId, name, description);
            } catch (SQLException | NullPointerException e) {
                logger.error(e.getLocalizedMessage());
            }

            if (result) {
                routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    public void getDataSets(RoutingContext routingContext) {

        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            long projectId = map.get(UrlParam.PROJECT_ID);

            String sql = "SELECT id, data_set_id, name, type, owner_name, description, created, locked FROM data_set WHERE project_policy IN (SELECT policy_id FROM project_policy INNER JOIN project ON project_policy.project_id=project.externalref WHERE project.project_id=?) order by name";
            Route.vertxDBHelper.select(sql, new JsonArray().add(projectId), result -> returnResponse(routingContext, HttpResponseStatus.OK, result));
        });
    }

    public void getDataSet(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID, UrlParam.DATASET_ID);
            String sql = "SELECT id, data_set_id, name, type, owner_name, description, created, locked FROM data_set WHERE id=? AND project_policy IN (SELECT policy_id FROM project_policy INNER JOIN project ON project_policy.project_id=project.externalref WHERE project.project_id=?) order by name";
            Route.vertxDBHelper.getOne(sql, new JsonArray().add(map.get(UrlParam.DATASET_ID)).add(map.get(UrlParam.PROJECT_ID)), result -> returnResponse(routingContext, HttpResponseStatus.OK, result));
        });
    }

    private void removeDataSetFromDb(RoutingContext routingContext, JsonArray subtypes, long dataSetId) {
        Route.vertxDBHelper.updateMultipleSqlInTransaction(finalResult -> {
            if (finalResult.succeeded()) {
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
            } else {
                logger.error("Cleaning up db failed", finalResult.cause());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        }, (connection, asyncResultHandler) -> {
            //delete resource sub entries
            String deleteEntriesSql = "DELETE FROM resource_subentries WHERE " + subtypes.stream().map(JsonObject.class::cast).map(subtype -> "resource_id = " + subtype.getLong("id")).collect(Collectors.joining(" OR "));
            connection.update(deleteEntriesSql, deleteEntriesResult -> {
                if (deleteEntriesResult.succeeded()) {
                    //delete resources
                    String deleteResourcesSql = "DELETE FROM resource WHERE " + subtypes.stream().map(JsonObject.class::cast).map(subtype -> "id = " + subtype.getLong("id")).collect(Collectors.joining(" OR "));
                    connection.update(deleteResourcesSql, deleteResourcesResult -> {
                        if (deleteResourcesResult.succeeded()) {
                            //delete data set
                            String deleteDataSet = "DELETE FROM data_set WHERE id=?";
                            connection.updateWithParams(deleteDataSet, new JsonArray().add(dataSetId), deleteDataSetResult -> {
                                if (deleteDataSetResult.succeeded()) {
                                    asyncResultHandler.handle(Future.succeededFuture());
                                } else {
                                    logger.error("Deleting data set failed", deleteDataSetResult.cause());
                                    asyncResultHandler.handle(Future.failedFuture(deleteDataSetResult.cause()));
                                }
                            });
                        } else {
                            logger.error("Deleting subtypes failed", deleteResourcesResult.cause());
                            asyncResultHandler.handle(Future.failedFuture(deleteResourcesResult.cause()));
                        }
                    });
                } else {
                    logger.error("Deleting sub entries failed", deleteEntriesResult.cause());
                    asyncResultHandler.handle(Future.failedFuture(deleteEntriesResult.cause()));
                }
            });
        });
    }

    public void deleteDataSet(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID, UrlParam.DATASET_ID);
            long projectId = map.get(UrlParam.PROJECT_ID);
            long dataSetId = map.get(UrlParam.DATASET_ID);

            String sql = "SELECT resource.id, resource.quota_id, resource.project_policy, resource.size FROM resource INNER JOIN data_set ON resource.dataset_id=data_set.id AND resource.project_policy=data_set.project_policy WHERE data_set.id=? AND data_set.project_policy IN (SELECT policy_id FROM project_policy INNER JOIN project ON project_policy.project_id=project.externalref WHERE project.project_id=?) order by resource.name";

            Route.vertxDBHelper.select(sql, new JsonArray().add(dataSetId).add(projectId), result -> {
                if (result.succeeded()) {
                    JsonArray subtypes = new JsonArray(result.result());
                    Route.vertxDBHelper.select("SELECT data_set_id from data_set where id = ?",new JsonArray().add(dataSetId), selectDatasetUUIDResult -> {
                        if (selectDatasetUUIDResult.succeeded()) {
                            JsonArray data_setUUID = new JsonArray(selectDatasetUUIDResult.result());


                            Route.vertxDBHelper.updateOne("UPDATE data_set SET locked=? WHERE id=?", new JsonArray().add(true).add(dataSetId), lockingResult -> {
                                if (lockingResult.succeeded()) {
                                    Optional<Long> totalSize = subtypes.stream().map(JsonObject.class::cast).map(subtype -> subtype.getLong("size")).reduce(Long::sum);

                                    IRODSAccount irodsAccount = new IRODSAccount(Config.valueOf(ConfigName.IRODS_HOST),
                                            Integer.parseInt(Config.valueOf(ConfigName.IRODS_PORT)),
                                            Config.valueOf(ConfigName.IRODS_USER),
                                            Config.valueOf(ConfigName.IRODS_PASSWORD),
                                            Config.valueOf(ConfigName.IRODS_HOME),
                                            Config.valueOf(ConfigName.IRODS_ZONE),
                                            Config.valueOf(ConfigName.DEFAULT_STORAGE_RESOURCE));
                                    IRODSAccessObjectFactory irodsAccessObjectFactory = null;

                                    try {
                                        irodsAccessObjectFactory = new IRODSAccessObjectFactoryImpl(new IRODSSession(new IRODSSimpleProtocolManager()));
                                        IRODSFile irodsFile = irodsAccessObjectFactory.getIRODSFileFactory(irodsAccount).instanceIRODSFile(Config.valueOf(ConfigName.IRODS_HOME) + FileSystems.getDefault().getSeparator() + data_setUUID.getJsonObject(0).getString("data_set_id"));
                                        if (irodsFile.exists()) {
                                            if (irodsFile.delete()) {
                                                removeDataSetFromDb(routingContext, subtypes, dataSetId);
                                            } else {
                                                logger.error("Deleting data in iRods failed");
                                                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end("Can't remove data in iRods server");

                                                Route.vertxDBHelper.updateOne("UPDATE data_set SET locked=? WHERE id=?", new JsonArray().add(false).add(dataSetId), unlockingResult -> {
                                                    if (unlockingResult.failed()) {
                                                        logger.error("Unlocking data set {} failed", unlockingResult.cause(), dataSetId);
                                                        //TODO how to unlock the data set if unlocking fails
                                                    }
                                                });
                                            }
                                        } else {
                                            logger.debug("Data set folder did not exist in irods");
                                            removeDataSetFromDb(routingContext, subtypes, dataSetId);
                                        }
                                    } catch (JargonException e) {
                                        logger.error(e.getMessage(), e.getCause());
                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                                    } finally {
                                        if (irodsAccessObjectFactory != null) {
                                            irodsAccessObjectFactory.closeSessionAndEatExceptions();
                                        }
                                    }
                                } else {
                                    logger.error("Locking data set {} failed", lockingResult.cause(), dataSetId);
                                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end("Can't lock the data set");
                                }
                            });
                        }
                    else{
                            logger.error("Selecting data_set_id failed", selectDatasetUUIDResult.cause());
                            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                        }});

                } else {
                    logger.error("Selecting subtypes failed", result.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            });
        });
    }

    public void getSubtypes(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID, UrlParam.DATASET_ID);

            String sql = "SELECT resource.id, resource.name, resource.size, resource.type, resource.created FROM resource INNER JOIN data_set ON resource.dataset_id=data_set.id AND resource.project_policy=data_set.project_policy WHERE data_set.id=? AND data_set.project_policy IN (SELECT policy_id FROM project_policy INNER JOIN project ON project_policy.project_id=project.externalref WHERE project.project_id=?) order by resource.name";

            Route.vertxDBHelper.select(sql, new JsonArray().add(map.get(UrlParam.DATASET_ID)).add(map.get(UrlParam.PROJECT_ID)), result -> returnResponse(routingContext, HttpResponseStatus.OK, result));
        });
    }

    public void getSubtype(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID, UrlParam.DATASET_ID, UrlParam.SUBTYPE_ID);

            String sql = "SELECT element FROM resource_subentries WHERE resource_id=? ";

            Route.vertxDBHelper.select(sql, new JsonArray().add(map.get(UrlParam.SUBTYPE_ID)), result -> returnResponseWithExtraFunction(routingContext, HttpResponseStatus.OK, result, sqlResult -> {
                JsonArray resultArray = new JsonArray(sqlResult);
                JsonObject returnJsonObjet = new JsonObject();
                JsonArray folderArray = new JsonArray();
                JsonObject fileJsonObject = new JsonObject();

                resultArray.stream().map(item -> (JsonObject) item).forEach(item -> {
                    String element = item.getString(JsonKey.ELEMENT);
                    int index = element.indexOf(",");
                    String size = element.substring(index + 1);
                    String path = element.substring(0, index);
                    String[] pathArray = path.split("/");

                    if (pathArray.length == 2) {
                        fileJsonObject.put(pathArray[1], size);
                    } else {
                        if (!folderArray.contains(pathArray[1])) {
                            folderArray.add(pathArray[1]);
                        }
                    }
                });
                returnJsonObjet.put(JsonKey.PATH, "");
                returnJsonObjet.put(JsonKey.FILES, fileJsonObject);
                returnJsonObjet.put(JsonKey.FOLDERS, folderArray);
                return returnJsonObjet.encode();
            }));
        });
    }

    public void getContent(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.PARAM0, UrlParam.PARAM1, UrlParam.PARAM2);
            String relativePath = routingContext.request().getParam(UrlParam.PARAM3);

            String sql = "SELECT element FROM resource_subentries WHERE resource_id=? AND element LIKE ?";

            Route.vertxDBHelper.select(sql, new JsonArray().add(map.get(UrlParam.PARAM2)).add(relativePath + "%"), result -> returnResponseWithExtraFunction(routingContext, HttpResponseStatus.OK, result, sqlResult -> {
                JsonArray resultArray = new JsonArray(sqlResult);
                JsonObject returnJsonObject = new JsonObject();
                returnJsonObject.put(JsonKey.PATH, relativePath);
                JsonArray folderArray = new JsonArray();
                JsonObject fileJsonObject = new JsonObject();

                List<String> elements = resultArray.stream().map(o -> JsonObject.class.cast(o).getString(JsonKey.ELEMENT)).collect(Collectors.toList());
                List<String> filteredElements = elements.stream().filter(e -> e.startsWith(relativePath + "/")).collect(Collectors.toList());
                filteredElements.forEach(element -> {
                    int index = element.indexOf(",");
                    String size = element.substring(index + 1);
                    String pathPart = element.substring(0, index);
                    String path = pathPart.replaceFirst(relativePath + "/", "");
                    String[] pathArray = path.split("/");
                    if (pathArray.length == 1) {
                        fileJsonObject.put(pathArray[0], size);
                    } else {
                        if (!folderArray.contains(pathArray[0])) {
                            folderArray.add(pathArray[0]);
                        }
                    }
                });
                returnJsonObject.put(JsonKey.FILES, fileJsonObject);
                returnJsonObject.put(JsonKey.FOLDERS, folderArray);
                return returnJsonObject.encode();
            }));
        });
    }

    private Future<String> updateDiskUsage(JsonObject o) {

        Future<String> future = Future.future();
        Route.vertxDBHelper.updateOne("update project set diskusage=?, diskusage_last_update=now() where project_id=?",
                new JsonArray().add(o.getLong(JsonKey.USED_SIZE))
                        .add(o.getLong(JsonKey.ID)), future.completer());

        return future;
    }

    private void recomputeProjectDiskUsage(RoutingContext routingContext) {

        Route.vertxDBHelper.select("SELECT project_id as id, externalref as external_ref, diskusage as disk_usage from project", asyncResult -> {

            if (asyncResult.succeeded()) {
                JsonArray r = new JsonArray(asyncResult.result());
                List<Long> failedLst = Collections.synchronizedList(new ArrayList<>());
                final List<Future<String>> updateDiskUsageFuture = r.stream().map(JsonObject.class::cast).map(o -> {
                    Long size = ResrouceFacade.getUsedSize(o.getString(JsonKey.EXTERNAL_REF));
                    return updateDiskUsage(o.put(JsonKey.USED_SIZE, size)).setHandler(ar -> {
                        if (ar.failed()) {
                            failedLst.add(new JsonObject(ar.result()).getJsonArray(no.nels.vertx.commons.constants.JsonKey.KEYS).getLong(0));
                        }
                    });
                }).collect(Collectors.toList());

                ParameterizedCompositeFuture.join(updateDiskUsageFuture).setHandler(ar -> {
                    if (ar.succeeded()) {
                        routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end();
                    } else {
                        routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(new JsonArray(failedLst).encode());
                    }
                });
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }

        });
    }

}
