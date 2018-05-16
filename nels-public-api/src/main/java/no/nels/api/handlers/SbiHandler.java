package no.nels.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.RoutingContext;
import no.nels.api.Config;
import no.nels.api.Route;
import no.nels.api.constants.ConfigName;
import no.nels.api.constants.JsonKey;
import no.nels.api.constants.Method;
import no.nels.api.constants.UrlParam;
import no.nels.client.sbi.SbiApiConsumer;
import no.nels.client.sbi.models.*;
import no.nels.commons.constants.JsonObjectKey;
import no.nels.commons.constants.LogContextType;
import no.nels.commons.constants.NelsUserType;
import no.nels.commons.utilities.StringUtilities;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public final class SbiHandler implements IHttpRequestHandler {
    private static Logger logger = LoggerFactory.getLogger(SbiHandler.class);

    public void createSbiQuota(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            JsonObject body = validateRequestBody(routingContext, JsonKey.NAME, JsonKey.DESCRIPTION, JsonKey.QUOTA_SIZE);
            body.put(JsonKey.FEDERATED_ID, routingContext.user().principal().getString(JsonKey.FEDERATED_ID));

            SbiApiConsumer.createQuota(body.encode());
            return Pair.of(HttpResponseStatus.CREATED, Optional.empty());
        });
    }

    public void updateSbiQuota(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.QUOTA_ID);
            long quotaId = params.get(UrlParam.QUOTA_ID);

            SbiApiConsumer.updateQuota(quotaId, routingContext.getBodyAsJson().put(JsonObjectKey.FEDERATED_ID, routingContext.user().principal().getString(JsonObjectKey.FEDERATED_ID)).encode());

            return Pair.of(HttpResponseStatus.NO_CONTENT, Optional.empty());
        });
    }

    public void deleteSbiQuota(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.QUOTA_ID);
            SbiApiConsumer.deleteQuota(params.get(UrlParam.QUOTA_ID), routingContext.user().principal().getString(JsonObjectKey.FEDERATED_ID));
            return Pair.of(HttpResponseStatus.NO_CONTENT, Optional.empty());
        });
    }

    public void deleteSbiProjectDataSet(RoutingContext routingContext) {
        String federatedId = routingContext.user().principal().getString(JsonKey.FEDERATED_ID);

        returnResponseFunction(routingContext, () -> routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.ADMINISTRATOR.getName()), () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID, UrlParam.DATASET_ID);
            SbiApiConsumer.deleteProjectDataSet(federatedId, params.get(UrlParam.PROJECT_ID), params.get(UrlParam.DATASET_ID));
            return Pair.of(HttpResponseStatus.NO_CONTENT, Optional.empty());
        });
    }

    public void getSbiQuota(RoutingContext routingContext) {
        validateRequest(routingContext,
                () -> true,
                () -> {

                    Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.QUOTA_ID);
                    long quotaId = params.get(UrlParam.QUOTA_ID);
                    SbiQuota quota = SbiApiConsumer.getQuota(quotaId);

                    JsonObject response = new JsonObject();
                    response.put(JsonKey.ID, quota.getId())
                            .put(JsonKey.NAME, quota.getName())
                            .put(JsonKey.QUOTA_SIZE, quota.getQuotaSize())
                            .put(JsonKey.USED_SIZE, quota.getUsedSize())
                            .put(JsonKey.DESCRIPTION, quota.getDescription())
                            .put(JsonKey.CREATION_DATE, quota.getCreationDate().toInstant()).put(JsonKey.QUOTA_ID, quota.getQuotaId());
                    Route.vertxDBHelper.select("SELECT structured_log.*,users.name from structured_log inner join users on users.id = structured_log.operatorid WHERE logcontextid >= 500 and logcontextid < 600 and targetid = ? order by id desc", new JsonArray().add(quotaId), stringAsyncResult -> {
                        if (stringAsyncResult.succeeded()) {
                            JsonArray dbJournals = new JsonArray(stringAsyncResult.result());
                            logger.debug("journals:" + dbJournals.encodePrettily());
                            List<JsonObject> journalEntries = new ArrayList<>();
                            dbJournals.stream().map(JsonObject.class::cast).forEach(dbJournal -> {
                                logger.debug("item:" + dbJournal.encodePrettily());
                                JsonObject journalEntry = new JsonObject();
                                journalEntry.put(JsonObjectKey.LOGTIME, dbJournal.getInstant(JsonObjectKey.LOGTIME));
                                journalEntry.put(JsonObjectKey.NAME, dbJournal.getString(JsonObjectKey.NAME));
                                journalEntry.mergeIn(new JsonObject(dbJournal.getString(JsonObjectKey.LOGTEXT)));
                                journalEntries.add(journalEntry);
                            });
                            response.put("journal", journalEntries);
                            routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(response.encode());
                        } else {
                            logger.error("", stringAsyncResult.cause());
                            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .end(new JsonObject().put(JsonKey.DESCRIPTION, stringAsyncResult.cause().getMessage()).encodePrettily());
                        }
                    });
                });

    }

    public void getSbiProjectsInQuota(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> true, () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.QUOTA_ID);
            long quotaId = params.get(UrlParam.QUOTA_ID);

            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CONTACT_PERSON, JsonKey.DISK_USAGE, JsonKey.CREATION_DATE);

            Map<String, String> parameters = new HashMap<>();
            sort.ifPresent(str -> parameters.put(UrlParam.SORT, str));
            pair.getLeft().ifPresent(str -> parameters.put(UrlParam.LIMIT, str));
            pair.getRight().ifPresent(str -> parameters.put(UrlParam.OFFSET, str));


            String response = SbiApiConsumer.getProjectsInQuota(quotaId, parameters);
            JsonObject jsonObject = new JsonObject(response);
            JsonArray jsonArray = jsonObject.getJsonArray(JsonKey.DATA);
            JsonArray newJsonArray = new JsonArray();

            jsonArray.stream().map(JsonObject.class::cast).forEach(item -> newJsonArray.add(
                    new JsonObject().put(JsonKey.ID, item.getInteger(JsonKey.PROJECT_ID))
                            .put(JsonKey.NAME, item.getString(JsonKey.NAME))
                            .put(JsonKey.DESCRIPTION, item.getString(JsonKey.DESCRIPTION))
                            .put(JsonKey.CREATION_DATE, item.getString(JsonKey.START_DATE))
                            .put(JsonKey.CONTACT_PERSON, item.getString(JsonKey.CONTACT_PERSON))
                            .put(JsonKey.RESERVED_QUOTA, item.getLong(JsonKey.QUOTA_SIZE))
                            .put(JsonKey.DISK_USAGE, item.getLong(JsonKey.DISK_USAGE))
                            .put(JsonKey.DISK_USAGE_LAST_UPDATE, item.getString(JsonKey.DISK_USAGE_LAST_UPDATE))
                            .put(JsonKey.QUOTA_NAME, item.getString(JsonKey.QUOTA_NAME))
                            .put(JsonKey.REMAINING_QUOTA, item.getLong(JsonKey.QUOTA_SIZE) - item.getLong(JsonKey.USED_SIZE))));

            return Pair.of(HttpResponseStatus.OK, Optional.of(new JsonObject().put(JsonKey.COUNT, jsonObject.getInteger(JsonKey.COUNT)).put(JsonKey.DATA, newJsonArray).encode()));
        });
    }

    public void downloadSbiSampleMetaData(RoutingContext routingContext) {

        String ref = routingContext.request().getParam(UrlParam.REF);
        //ref = new String(Base64.getDecoder().decode(ref));
        logger.info("REF: " + ref);
        if (ref.equals("404")) {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
        } else {
            String[] params = StringUtilities.base64Decode(ref).split(":");
            //params = [p-id, ds-id, sub-type-name]
            sendFileToUser(routingContext, getMetadataAbsolutePath(params[1], params[2]));
        }
    }

    private String getMetadataAbsolutePath(RoutingContext routingContext) {
        return getMetadataAbsolutePath(routingContext.request().getParam(UrlParam.DATASET_ID), routingContext.request().getParam(UrlParam.SUBTYPE_NAME));
    }

    private String getMetadataAbsolutePath(String datasetId, String subTypeName) {
        return Config.valueOf(ConfigName.SEEK_METADATA_FOLDER) + "/" + datasetId + "_" + subTypeName + ".xlsx";
    }

    public void doActionInSbiMetadata(RoutingContext routingContext) {
        JsonObject body = routingContext.getBodyAsJson();
        String method = body.getString(JsonKey.METHOD);
        if (method.equals(Method.EXIST)) {
            String filePath = getMetadataAbsolutePath(routingContext);
            if (Files.exists(Paths.get(filePath))) {
                routingContext.response().setStatusCode(HttpResponseStatus.FOUND.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
            }
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public void sendFileToUser(RoutingContext routingContext, String absolutePath) {
        if (Files.exists(Paths.get(absolutePath))) {
            routingContext.response().putHeader("Content-disposition", "attachment; filename=\"" + absolutePath.substring(absolutePath.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1) + "\"").sendFile(absolutePath);
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
        }
    }

    public void downloadSbiMetadata(RoutingContext routingContext) {
        sendFileToUser(routingContext, getMetadataAbsolutePath(routingContext));
    }

    public void deleteSbiMetadata(RoutingContext routingContext) {
        String absolutePath = getMetadataAbsolutePath(routingContext);

        if (Files.exists(Paths.get(absolutePath))) {
            try {
                Files.delete(Paths.get(absolutePath));
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
            } catch (IOException e) {
                logger.error("Can't delete the file " + absolutePath, e.getCause());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
        }
    }

    public void uploadSbiMetadata(RoutingContext routingContext) {
        routingContext.request().pause();

        String absolutePath = getMetadataAbsolutePath(routingContext);

        routingContext.vertx().fileSystem().open(absolutePath, new OpenOptions(), asyncResult -> {
            if (asyncResult.succeeded()) {
                final AsyncFile file = asyncResult.result();
                final Pump pump = Pump.pump(routingContext.request(), file);
                routingContext.request().endHandler(Void -> file.close(result -> {
                    if (result.succeeded()) {
                        routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end();
                    } else {
                        logger.error("Closing file has some problems", result.cause());
                    }
                }));
                routingContext.request().exceptionHandler(throwable -> {
                    logger.error("Exception is happened while uploading", throwable);
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                });
                pump.start();
                routingContext.request().resume();
            } else {
                logger.error("Can't open the file " + absolutePath, asyncResult.cause());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    public void getSbiQuotas(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);


            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CREATOR, JsonKey.NUM_OF_PROJECTS, JsonKey.QUOTA_SIZE, JsonKey.USED_SIZE, JsonKey.UTILIZATION, JsonKey.CREATED);


            String quotas = SbiApiConsumer.getAllQuotas(pair.getLeft().orElse(""), pair.getRight().orElse(""), sort.orElse(""));
            return Pair.of(HttpResponseStatus.OK, Optional.of(quotas));
        });
    }

    public void navigateInSubtype(RoutingContext routingContext) {
        String federatedId = routingContext.user().principal().getString(JsonKey.FEDERATED_ID);

        returnResponseFunction(routingContext, () -> true, () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PARAM0, UrlParam.PARAM1);

            JsonObject requestBody = validateRequestBody(routingContext, JsonKey.SUBTYPE_NAME);

            String subtypeName = requestBody.getString(JsonKey.SUBTYPE_NAME);
            String relativePath = requestBody.getString(JsonKey.RELATIVE_PATH);

            List<SbiSubtype> subtypes = SbiApiConsumer.getSubtypes(federatedId, params.get(UrlParam.PARAM0), params.get(UrlParam.PARAM1));
            Optional<SbiSubtype> optional = subtypes.stream().filter(subtype -> subtype.getName().equals(subtypeName)).findFirst();
            List<SbiData> sbiData;
            if (StringUtils.isEmpty(relativePath)) {
                sbiData = SbiApiConsumer.getSubtype(federatedId, params.get(UrlParam.PARAM0), params.get(UrlParam.PARAM1), Long.valueOf(optional.get().getId()));
            } else {
                sbiData = SbiApiConsumer.getContent(federatedId, params.get(UrlParam.PARAM0), params.get(UrlParam.PARAM1), Long.valueOf(optional.get().getId()), relativePath);
            }

            JsonArray files = new JsonArray();
            JsonArray folders = new JsonArray();
            sbiData.stream().forEach(data -> {
                if (data.isFolder()) {
                    folders.add(data.getName());
                } else {
                    files.add(new JsonObject().put(JsonKey.NAME, data.getName()).put(JsonKey.SIZE, data.getSize()));
                }
            });
            JsonObject response = new JsonObject();
            response.put(JsonKey.FILES, files).put(JsonKey.FOLDERS, folders);

            return Pair.of(HttpResponseStatus.OK, Optional.of(response.encode()));
        });
    }

    public void doAction(RoutingContext routingContext) {

        returnResponseFunction(routingContext, () -> true, () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PARAM0, UrlParam.PARAM1);

            JsonObject requestBody = validateRequestBody(routingContext, JsonKey.SUBTYPE_NAME, JsonKey.METHOD);

            if (!requestBody.getString(JsonKey.METHOD).equals("get_nels_url")) {
                return Pair.of(HttpResponseStatus.BAD_REQUEST, Optional.empty());
            } else {
                //project_id:dataset_id:subtype_name
                StringBuilder builder = new StringBuilder();
                builder.append(params.get(UrlParam.PARAM0)).append(":").append(params.get(UrlParam.PARAM1)).append(":").append(requestBody.getString(JsonKey.SUBTYPE_NAME));
                String ref = Base64.getEncoder().encodeToString(builder.toString().getBytes());
                return Pair.of(HttpResponseStatus.OK, Optional.of(new JsonObject().put(JsonKey.URL, Config.valueOf(ConfigName.NELS_SBI_URL) + "?ref=" + ref).encode()));
            }
        });
    }


    public void getSbiProjectDataset(RoutingContext routingContext) {
        String federatedId = routingContext.user().principal().getString(JsonKey.FEDERATED_ID);

        returnResponseFunction(routingContext, () -> true, () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID, UrlParam.DATASET_ID);

            SbiDataSet dataset = SbiApiConsumer.getDataSet(federatedId, params.get(UrlParam.PROJECT_ID), params.get(UrlParam.DATASET_ID));
            List<SbiSubtype> subtypes = SbiApiConsumer.getSubtypes(federatedId, params.get(UrlParam.PROJECT_ID), params.get(UrlParam.DATASET_ID));

            JsonArray jsonArray = new JsonArray();
            subtypes.stream().forEach(subtype -> jsonArray.add(new JsonObject().put(JsonKey.TYPE, subtype.getType()).put(JsonKey.SIZE, subtype.getSize())));
            JsonObject response = new JsonObject();
            response.put(JsonKey.CREATION_DATE, dataset.getCreationDate().toInstant())
                    .put(JsonKey.ID, dataset.getId())
                    .put(JsonKey.NAME, dataset.getName())
                    .put(JsonKey.TYPE, dataset.getType())
                    .put(JsonKey.OWNER_NAME, dataset.getOwner())
                    .put(JsonKey.DESCRIPTION, dataset.getDescription())
                    .put(JsonKey.SUBTYPES, jsonArray);

            return Pair.of(HttpResponseStatus.OK, Optional.of(response.encode()));
        });
    }

    public void getSbiProject(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParam.PROJECT_ID);

        if (projectId.equals(UrlParam.ALL)) {
            routingContext.next();
        } else {
            returnResponseFunction(routingContext, () -> true, () -> {
                Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);

                SbiProject project = SbiApiConsumer.getProject(params.get(UrlParam.PROJECT_ID));
                JsonObject response = new JsonObject();
                response.put(JsonKey.ID, project.getId())
                        .put(JsonKey.NAME, project.getName())
                        .put(JsonKey.DESCRIPTION, project.getDescription())
                        .put(JsonKey.CONTACT_PERSON, project.getContactPerson())
                        .put(JsonKey.CREATION_DATE, project.getCreationDate().toInstant())
                        .put(JsonKey.CONTACT_EMAIL, project.getContactEmail())
                        .put(JsonKey.CONTACT_AFFILIATION, project.getContactAffiliation())
                        .put(JsonKey.RESERVED_QUOTA, project.getQuotaSize())
                        .put(JsonKey.QUOTA_NAME, project.getQuotaName())
                        .put(JsonKey.REMAINING_QUOTA, project.getQuotaSize() - project.getUsedSize())
                        .put(JsonKey.DISK_USAGE, project.getDiskUsage());
                return Pair.of(HttpResponseStatus.OK, Optional.of(response.encode()));
            });
        }
    }

    public void deleteSbiProject(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            JsonObject body = new JsonObject().put(JsonKey.FEDERATED_ID, routingContext.user().principal().getString(JsonKey.FEDERATED_ID));
            SbiApiConsumer.deleteProject(params.get(UrlParam.PROJECT_ID), body.encode());
            return Pair.of(HttpResponseStatus.NO_CONTENT, Optional.empty());
        });
    }

    public void createSbiProject(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            JsonObject body = validateRequestBody(routingContext, JsonKey.NAME, JsonKey.DESCRIPTION, JsonKey.QUOTA_ID);
            body.put(JsonKey.FEDERATED_ID, routingContext.user().principal().getString(JsonKey.FEDERATED_ID));

            return Pair.of(HttpResponseStatus.CREATED, Optional.of(SbiApiConsumer.createProject(body.encode())));
        });
    }

    public void updateSbiProject(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            long projectId = params.get(UrlParam.PROJECT_ID);

            SbiApiConsumer.updateProject(projectId, routingContext.getBodyAsJson().encode());

            return Pair.of(HttpResponseStatus.NO_CONTENT, Optional.empty());
        });
    }

    public void getSbiProjects(RoutingContext routingContext) {
        String federatedId = routingContext.user().principal().getString(JsonKey.FEDERATED_ID);

        returnResponseFunction(routingContext, () -> true, () -> {
            List<SbiProject> projects = SbiApiConsumer.getProjects(federatedId);
            JsonArray response = new JsonArray();
            projects.stream().forEach(project -> response.add(new JsonObject().put(JsonKey.ID, project.getId()).put(JsonKey.NAME, project.getName()).put(JsonKey.DESCRIPTION, project.getDescription())
                    .put(JsonKey.CONTACT_PERSON, project.getContactPerson()).put(JsonKey.CREATION_DATE, project.getCreationDate().toInstant())));

            return Pair.of(HttpResponseStatus.OK, Optional.of(response.encode()));
        });
    }

    public void getSbiAllProjects(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CONTACT_PERSON, JsonKey.DISK_USAGE, JsonKey.CREATION_DATE, JsonKey.UTILIZATION);

            Map<String, String> parameters = new HashMap<>();
            sort.ifPresent(str -> parameters.put(UrlParam.SORT, str));
            pair.getLeft().ifPresent(str -> parameters.put(UrlParam.LIMIT, str));
            pair.getRight().ifPresent(str -> parameters.put(UrlParam.OFFSET, str));


            String response = SbiApiConsumer.getProjects(parameters);
            JsonObject jsonObject = new JsonObject(response);
            JsonArray jsonArray = jsonObject.getJsonArray(JsonKey.DATA);
            JsonArray newJsonArray = new JsonArray();

            jsonArray.stream().map(JsonObject.class::cast).forEach(item -> newJsonArray.add(
                    new JsonObject().put(JsonKey.ID, item.getInteger(JsonKey.PROJECT_ID))
                            .put(JsonKey.NAME, item.getString(JsonKey.NAME))
                            .put(JsonKey.DESCRIPTION, item.getString(JsonKey.DESCRIPTION))
                            .put(JsonKey.CREATION_DATE, item.getString(JsonKey.START_DATE))
                            .put(JsonKey.CONTACT_PERSON, item.getString(JsonKey.CONTACT_PERSON))
                            .put(JsonKey.RESERVED_QUOTA, item.getLong(JsonKey.QUOTA_SIZE))
                            .put(JsonKey.DISK_USAGE, item.getLong(JsonKey.DISK_USAGE))
                            .put(JsonKey.DISK_USAGE_LAST_UPDATE, item.getString(JsonKey.DISK_USAGE_LAST_UPDATE))
                            .put(JsonKey.QUOTA_NAME, item.getString(JsonKey.QUOTA_NAME))
                            .put(JsonKey.REMAINING_QUOTA, item.getLong(JsonKey.QUOTA_SIZE) - item.getLong(JsonKey.USED_SIZE))
                            .put(JsonKey.UTILIZATION, item.getFloat(JsonKey.UTILIZATION))));

            return Pair.of(HttpResponseStatus.OK, Optional.of(new JsonObject().put(JsonKey.COUNT, jsonObject.getInteger(JsonKey.COUNT)).put(JsonKey.DATA, newJsonArray).encode()));
        });
    }

    public void querySbiProjects(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);
            JsonObject body = validateRequestBody(routingContext, JsonKey.QUERY);
            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CONTACT_PERSON, JsonKey.DISK_USAGE, JsonKey.CREATION_DATE, JsonKey.UTILIZATION);

            Map<String, String> urlParameters = new HashMap<>();
            pair.getLeft().ifPresent(str -> urlParameters.put(UrlParam.LIMIT, str));
            pair.getRight().ifPresent(str -> urlParameters.put(UrlParam.OFFSET, str));
            sort.ifPresent(str -> urlParameters.put(UrlParam.SORT, str));

            String response = SbiApiConsumer.searchProjects(body.getString(JsonKey.QUERY), urlParameters);
            JsonObject jsonObject = new JsonObject(response);
            JsonArray jsonArray = jsonObject.getJsonArray(JsonKey.DATA);
            JsonArray newJsonArray = new JsonArray();

            jsonArray.stream().map(JsonObject.class::cast).forEach(item -> newJsonArray.add(
                    new JsonObject().put(JsonKey.ID, item.getInteger(JsonKey.PROJECT_ID))
                            .put(JsonKey.NAME, item.getString(JsonKey.NAME))
                            .put(JsonKey.DESCRIPTION, item.getString(JsonKey.DESCRIPTION))
                            .put(JsonKey.CREATION_DATE, item.getString(JsonKey.START_DATE))
                            .put(JsonKey.CONTACT_PERSON, item.getString(JsonKey.CONTACT_PERSON))
                            .put(JsonKey.RESERVED_QUOTA, item.getLong(JsonKey.QUOTA_SIZE))
                            .put(JsonKey.DISK_USAGE, item.getLong(JsonKey.DISK_USAGE))
                            .put(JsonKey.DISK_USAGE_LAST_UPDATE, item.getString(JsonKey.DISK_USAGE_LAST_UPDATE))
                            .put(JsonKey.QUOTA_NAME, item.getString(JsonKey.QUOTA_NAME))
                            .put(JsonKey.REMAINING_QUOTA, item.getLong(JsonKey.QUOTA_SIZE) - item.getLong(JsonKey.USED_SIZE))
                            .put(JsonKey.UTILIZATION, item.getFloat(JsonKey.UTILIZATION))));

            return Pair.of(HttpResponseStatus.OK, Optional.of(new JsonObject().put(JsonKey.COUNT, jsonObject.getInteger(JsonKey.COUNT)).put(JsonKey.DATA, newJsonArray).encode()));
        });
    }

    public void getSbiProjectDatasets(RoutingContext routingContext) {

        String federatedId = routingContext.user().principal().getString(JsonKey.FEDERATED_ID);

        returnResponseFunction(routingContext, () -> true, () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);

            List<SbiDataSet> dataSets = SbiApiConsumer.getDataSets(federatedId, params.get(UrlParam.PROJECT_ID));
            JsonArray response = new JsonArray();
            dataSets.stream().forEach(dataset -> response.add(
                    new JsonObject().put(JsonKey.CREATION_DATE, dataset.getCreationDate().toInstant()).put(JsonKey.OWNER_NAME, dataset.getOwner()).put(JsonKey.DESCRIPTION, dataset.getDescription()).put(JsonKey.ID, dataset.getId()).put(JsonKey.NAME, dataset.getName()).put(JsonKey.TYPE, dataset.getType())));

            return Pair.of(HttpResponseStatus.OK, Optional.of(response.encode()));
        });
    }

    public void querySbiUsers(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.FEDERATED_ID);
            JsonArray federatedIds = jsonObject.getJsonArray(JsonKey.FEDERATED_ID);
            List<String> federatedIdLst = new ArrayList<>();
            int size = federatedIds.size();
            for (int i = 0; i < size; i++) {
                federatedIdLst.add(federatedIds.getString(i));
            }
            JsonArray response = new JsonArray();
            List<SbiUser> sbiUsers = SbiApiConsumer.searchSbiUsers(federatedIdLst);
            sbiUsers.stream().forEach(user -> response.add(new JsonObject().put(JsonKey.ID, user.getId())
                    .put(JsonKey.EMAIL, user.getEmail())
                    .put(JsonKey.USERNAME, user.getUsername())
                    .put(JsonKey.FEDERATED_ID, user.getFederatedId())));

            return Pair.of(HttpResponseStatus.OK, Optional.of(response.encode()));
        });
    }

    public void createSbiUser(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.NAME, JsonKey.EMAIL, JsonKey.FEDERATED_ID);
            SbiApiConsumer.createUser(jsonObject.getString(JsonKey.NAME),
                    jsonObject.getString(JsonKey.EMAIL),
                    jsonObject.getString(JsonKey.FEDERATED_ID));

            return Pair.of(HttpResponseStatus.CREATED, Optional.empty());
        });
    }

    public void changeProjectMembers(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            long projectId = params.get(UrlParam.PROJECT_ID);

            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.METHOD, JsonKey.DATA);
            String method = jsonObject.getString(JsonKey.METHOD);

            switch (method) {
                case Method.ADD:
                    return Pair.of(HttpResponseStatus.OK, Optional.of(SbiApiConsumer.addUsersToProject(projectId, jsonObject.encode())));
                case Method.DELETE:
                    return Pair.of(HttpResponseStatus.OK, Optional.of(SbiApiConsumer.removeUsersFromProject(projectId, jsonObject.encode())));
                default:
                    return Pair.of(HttpResponseStatus.BAD_REQUEST, Optional.empty());
            }
        });
    }

    public void getProjectMembers(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.PROJECT_ID);
            long projectId = params.get(UrlParam.PROJECT_ID);
            return Pair.of(HttpResponseStatus.OK, Optional.of(SbiApiConsumer.getProjectMembers(projectId)));
        });
    }

    public void getSbiUsers(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);
            String response = SbiApiConsumer.getUsers(pair.getLeft(), pair.getRight());
            return Pair.of(HttpResponseStatus.OK, Optional.of(response));
        });
    }

    public void querySbiQuotas(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.QUERY);
            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CREATOR, JsonKey.NUM_OF_PROJECTS, JsonKey.QUOTA_SIZE, JsonKey.USED_SIZE, JsonKey.UTILIZATION, JsonKey.CREATED);


            return Pair.of(HttpResponseStatus.OK, Optional.of(SbiApiConsumer.searchQuotas(jsonObject.getString(JsonKey.QUERY), pair.getLeft(), pair.getRight(), sort)));
        });
    }

    public void getSbiDataSetTypes(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.NAME, JsonKey.CREATOR);

            Map<String, String> parameters = new HashMap<>();
            sort.ifPresent(str -> parameters.put(UrlParam.SORT, str));
            pair.getLeft().ifPresent(str -> parameters.put(UrlParam.LIMIT, str));
            pair.getRight().ifPresent(str -> parameters.put(UrlParam.OFFSET, str));

            return Pair.of(HttpResponseStatus.OK, Optional.of(SbiApiConsumer.getDataSetTypesAsString(parameters)));
        });
    }

    public void getSbiDataSetType(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> true, () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.DATASET_TYPE_ID);

            return Pair.of(HttpResponseStatus.OK, Optional.of(SbiApiConsumer.getDataSetType(params.get(UrlParam.DATASET_TYPE_ID))));
        });
    }

    public void deleteSbiDataSetType(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Map<String, Long> params = validateNumericParamOfUrlPath(routingContext, UrlParam.DATASET_TYPE_ID);
            SbiApiConsumer.deleteDataSetType(params.get(UrlParam.DATASET_TYPE_ID));
            return Pair.of(HttpResponseStatus.NO_CONTENT, Optional.empty());
        });
    }

    public void createSbiDataSetType(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.NAME, JsonKey.DESCRIPTION, JsonKey.SUBTYPE);

            SbiApiConsumer.createDataSetType(jsonObject.encode());

            return Pair.of(HttpResponseStatus.CREATED, Optional.empty());
        });
    }

    public void querySbiDataSetTypes(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);
            JsonObject body = validateRequestBody(routingContext, JsonKey.QUERY);
            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.NAME, JsonKey.CREATOR);

            Map<String, String> urlParameters = new HashMap<>();
            pair.getLeft().ifPresent(str -> urlParameters.put(UrlParam.LIMIT, str));
            pair.getRight().ifPresent(str -> urlParameters.put(UrlParam.OFFSET, str));
            sort.ifPresent(str -> urlParameters.put(UrlParam.SORT, str));

            return Pair.of(HttpResponseStatus.OK, Optional.of(SbiApiConsumer.searchDataSetTypes(body.encode(), urlParameters)));
        });
    }


}
