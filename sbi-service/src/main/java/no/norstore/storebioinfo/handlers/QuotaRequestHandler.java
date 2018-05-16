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
import no.norstore.storebioinfo.Route;
import no.norstore.storebioinfo.constants.JsonKey;
import no.norstore.storebioinfo.constants.MethodName;
import no.norstore.storebioinfo.constants.SqlErrorCode;
import no.norstore.storebioinfo.constants.UrlParam;
import no.norstore.storebioinfo.facades.ResrouceFacade;
import no.norstore.storebioinfo.utils.ParameterizedCompositeFuture;
import no.norstore.storebioinfo.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by weizhang on 6/15/17.
 */
public final class QuotaRequestHandler implements HttpRequestHandler {
    private static Logger logger = LoggerFactory.getLogger(QuotaRequestHandler.class);

    public void getQuota(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.QUOTA_ID);
            long quotaId = map.get(UrlParam.QUOTA_ID);

            Route.vertxDBHelper.getOne("SELECT id, name, description, created::DATE, quota_size, used_size, owner_id, quota_id FROM quota2 WHERE id=?", new JsonArray().add(quotaId), result -> returnResponse(routingContext, HttpResponseStatus.OK, result));
        });
    }

    public void getProjectsInQuota(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.QUOTA_ID);
            long quotaId = map.get(UrlParam.QUOTA_ID);

            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CONTACT_PERSON, JsonKey.DISK_USAGE, JsonKey.CREATION_DATE);

            StringBuilder builder = new StringBuilder();
            builder.append("SELECT project.project_id, project.description, project.name, project.contact_person, project.startdate, project.diskusage as disk_usage, project.diskusage_last_update as disk_usage_last_update, quota2.quota_size, quota2.used_size, quota2.name as quota_name FROM ((project inner join quota on project.project_quota = quota.id) inner join quota2 on quota.name = quota2.name) WHERE quota2.id=? ");
            if (sort.isPresent()) {
                switch (sort.get()) {
                    case "-id":
                        builder.append(" order by project_id desc");
                        break;
                    case "id":
                        builder.append(" order by project_id asc");
                        break;
                    case "-name":
                        builder.append(" order by name desc");
                        break;
                    case "name":
                        builder.append(" order by name asc");
                        break;
                    case "-contact_person":
                        builder.append(" order by contact_person desc");
                        break;
                    case "contact_person":
                        builder.append(" order by contact_person asc");
                        break;
                    case "-disk_usage":
                        builder.append(" order by disk_usage desc");
                        break;
                    case "disk_usage":
                        builder.append(" order by disk_usage asc");
                        break;
                    case "-creation_date":
                        builder.append(" order by startdate desc");
                        break;
                    case "creation_date":
                        builder.append(" order by startdate asc");
                        break;
                    default:
                        builder.append(" order by name");
                }
            } else {
                builder.append(" order by name");
            }

            if (pair.getRight().isPresent()) {
                builder.append(" offset ").append(pair.getRight().get());
            }

            builder.append(" limit ").append(pair.getLeft().get());

            Route.vertxDBHelper.select(builder.toString(), new JsonArray().add(quotaId), result -> returnResponseWithCount(routingContext, HttpResponseStatus.OK, result, "SELECT count(*) FROM project WHERE project_quota IN (SELECT quota_id FROM quota2 WHERE id=" + quotaId + ")"));
        });
    }

    public void deleteQuota(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.QUOTA_ID);
            long quotaId = map.get(UrlParam.QUOTA_ID);
            String federatedId = routingContext.request().getHeader(JsonKey.FEDERATED_ID_IN_HEADER);
            JsonArray params = new JsonArray();
            params.add(quotaId);
            String sql = "SELECT count(*) FROM project WHERE project_quota IN (SELECT quota_id FROM quota2 WHERE id=?)";
            Route.vertxDBHelper.count(sql, params, countResult -> {
                if (countResult.succeeded()) {
                    int count = countResult.result();
                    if (count > 0) {
                        routingContext.response().setStatusCode(HttpResponseStatus.PRECONDITION_FAILED.code()).end(
                                new JsonObject().put(JsonKey.DESCRIPTION, "This quota includes existing projects, you need to remove them first.").encodePrettily());
                    } else {
                        Route.vertxDBHelper.getOne("SELECT name, quota FROM quota where id IN (SELECT quota_id FROM quota2 WHERE quota2.id=?)",
                                new JsonArray().add(quotaId), stringAsyncResult -> {
                                    if (stringAsyncResult.succeeded()) {
                                        JsonObject quotaInfo = new JsonObject(stringAsyncResult.result());
                                        String name = quotaInfo.getString(JsonKey.NAME);
                                        long quotaSize = quotaInfo.getLong(JsonKey.QUOTA);
                                        Route.vertxDBHelper.updateMultipleSqlInTransaction(finalResult -> {
                                                    if (finalResult.succeeded()) {
                                                        routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                                                    } else {
                                                        logger.error("Deleting quota " + quotaId + " failed.", finalResult.cause());
                                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                                .end(new JsonObject().put(JsonKey.DESCRIPTION, finalResult.cause().getMessage()).encodePrettily());
                                                    }
                                                }, (connection, asyncResultHandler) ->
                                                        connection.updateWithParams("DELETE FROM quota WHERE quota.id IN (SELECT quota2.quota_id FROM quota2 WHERE quota2.id=?)", params, deleteFromQuotaResult -> {
                                                            if (deleteFromQuotaResult.succeeded()) {
                                                                connection.updateWithParams("DELETE FROM quota2 WHERE id=?", params, deleteFromQuota2Result -> {
                                                                    if (deleteFromQuota2Result.succeeded()) {
                                                                        JsonArray fields = new JsonArray();
                                                                        JsonObject logText = new JsonObject();
                                                                        fields.add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonObjectKey.NAME)
                                                                                .put(JsonObjectKey.VALUE, name))
                                                                                .add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonObjectKey.QUOTA_SIZE)
                                                                                        .put(JsonObjectKey.VALUE, quotaSize));

                                                                        logText.put(JsonObjectKey.FIELDS, fields);
                                                                        sendJournal(routingContext, new JsonObject().put(JsonObjectKey.CONTEXT_ID, LogContextType.SBI_QUOTA_DELETE.getValue())
                                                                                .put(JsonObjectKey.OPERATOR_ID, -1)
                                                                                .put(JsonObjectKey.TARGET_ID, quotaId)
                                                                                .put(JsonObjectKey.TEXT, logText.encode())
                                                                                .put(JsonObjectKey.FEDERATED_ID, federatedId));
                                                                        asyncResultHandler.handle(Future.succeededFuture());
                                                                    } else {
                                                                        asyncResultHandler.handle(Future.failedFuture(deleteFromQuota2Result.cause()));
                                                                    }
                                                                });
                                                            } else {
                                                                asyncResultHandler.handle(Future.failedFuture(deleteFromQuotaResult.cause()));
                                                            }
                                                        })
                                        );
                                    } else {
                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                .end(new JsonObject().put(JsonKey.DESCRIPTION, stringAsyncResult.cause().getMessage()).encodePrettily());
                                    }
                                });

                    }
                } else {
                    logger.error("", countResult.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .end(new JsonObject().put(JsonKey.DESCRIPTION, countResult.cause().getMessage()).encodePrettily());
                }
            });
        });
    }

    public void updateQuota(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Map<String, Long> map = validateNumericParamOfUrlPath(routingContext, UrlParam.QUOTA_ID);

            long quotaId = map.get(UrlParam.QUOTA_ID);
            logger.debug("quotaId:" + quotaId);

            JsonObject body = validateRequestBody(routingContext, JsonKey.FEDERATED_ID);
            String federatedId = body.getString(JsonKey.FEDERATED_ID);
            List<String> paramsForQuota2 = new ArrayList<>();
            List<String> paramsForQuota = new ArrayList<>();
            JsonArray sqlParams = new JsonArray();
            JsonArray fields = new JsonArray();


            Route.vertxDBHelper.getOne("SELECT quota_id, decription, name, quota FROM quota where id IN (SELECT quota_id FROM quota2 WHERE quota2.id=?)",
                    new JsonArray().add(quotaId), stringAsyncResult -> {
                        if (stringAsyncResult.succeeded()) {
                            JsonObject quotaInfo = new JsonObject(stringAsyncResult.result());

                            String quotaGUID = quotaInfo.getString(JsonKey.QUOTA_ID);
                            String description = quotaInfo.getString(JsonKey.DESCRIPTION_TYPO);
                            String quotaName = quotaInfo.getString(JsonKey.NAME);
                            long quota = quotaInfo.getLong(JsonKey.QUOTA);
                            if (body.containsKey(JsonKey.NAME)) {
                                if (!quotaName.equalsIgnoreCase(body.getString(JsonKey.NAME))) {
                                    fields.add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonKey.NAME)
                                            .put(JsonObjectKey.VALUE, body.getString(JsonKey.NAME)));
                                }
                                paramsForQuota2.add("name=?");
                                paramsForQuota.add("name=?");
                                sqlParams.add(body.getString(JsonKey.NAME));
                            }

                            if (body.containsKey(JsonKey.DESCRIPTION)) {
                                if (!description.equalsIgnoreCase(body.getString(JsonKey.DESCRIPTION))) {
                                    fields.add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonKey.DESCRIPTION)
                                            .put(JsonObjectKey.VALUE, body.getString(JsonKey.DESCRIPTION)));
                                }
                                paramsForQuota2.add("description=?");
                                paramsForQuota.add("decription=?");
                                sqlParams.add(body.getString(JsonKey.DESCRIPTION));
                            }
                            if (body.containsKey(JsonKey.QUOTA_SIZE)) {
                                paramsForQuota2.add("quota_size=?");
                                paramsForQuota.add("quota=?");
                                sqlParams.add(body.getLong(JsonKey.QUOTA_SIZE));
                                sqlParams.add(quotaId);
                                if (quota != body.getLong(JsonKey.QUOTA_SIZE)) {
                                    fields.add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonKey.QUOTA_SIZE)
                                            .put(JsonObjectKey.VALUE, body.getLong(JsonKey.QUOTA_SIZE)));
                                }
                                String sqlForQuota2 = "UPDATE quota2 SET " + StringUtils.join(paramsForQuota2, ",") + " WHERE id=?";
                                String sqlForQuota = "UPDATE quota SET " + StringUtils.join(paramsForQuota, ",") + " WHERE id IN (SELECT quota_id FROM quota2 WHERE quota2.id=?)";
                                Route.vertxDBHelper.getOne("SELECT disk_quota, coalesce(original_size, 0) FROM disk_quota where quota_id = ?", new JsonArray().add(quotaGUID), diskQuotaResult -> {
                                    if (diskQuotaResult.succeeded()) {
                                        JsonObject result1 = new JsonObject(diskQuotaResult.result());
                                        long diskQuota = result1.getLong(JsonKey.DISK_QUOTA);
                                        long originalQuotaSize = result1.getLong(JsonKey.ORIGINAL_SIZE, 0L);
                                        Route.vertxDBHelper.updateMultipleSqlInTransaction(finalResult -> {
                                            if (finalResult.succeeded()) {
                                                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                                            } else {
                                                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                        .end(new JsonObject().put(JsonKey.DESCRIPTION, finalResult.cause().getMessage()).encodePrettily());
                                            }
                                        }, ((sqlConnection, asyncResultHandler) -> {

                                            sqlConnection.updateWithParams(sqlForQuota2, sqlParams, updateQuota2Result -> {
                                                if (updateQuota2Result.succeeded()) {
                                                    sqlConnection.updateWithParams(sqlForQuota, sqlParams, updateQuotaResult -> {
                                                        long newQuotaSize = body.getLong(JsonKey.QUOTA_SIZE);
                                                        if (updateQuotaResult.succeeded()) {
                                                            sqlConnection.updateWithParams("UPDATE disk_quota SET disk_quota = ?, original_size = ? WHERE quota_id = ?", new JsonArray().add(diskQuota + (newQuotaSize - originalQuotaSize)).add(newQuotaSize).add(quotaGUID), result -> {
                                                                if (result.succeeded()) {
                                                                    if (fields.size() != 0) {
                                                                        JsonObject text = new JsonObject().put(JsonObjectKey.FIELDS, fields);
                                                                        sendJournal(routingContext, new JsonObject().put(JsonObjectKey.CONTEXT_ID, LogContextType.SBI_QUOTA_UPDATE.getValue())
                                                                                .put(JsonObjectKey.OPERATOR_ID, -1)
                                                                                .put(JsonObjectKey.TARGET_ID, quotaId)
                                                                                .put(JsonObjectKey.TEXT, text.encode())
                                                                                .put(JsonObjectKey.FEDERATED_ID, federatedId));
                                                                    }
                                                                    asyncResultHandler.handle(Future.succeededFuture());
                                                                } else {
                                                                    asyncResultHandler.handle(Future.failedFuture(result.cause()));
                                                                }
                                                            });
                                                        } else {
                                                            asyncResultHandler.handle(Future.failedFuture(updateQuotaResult.cause()));
                                                        }
                                                    });

                                                } else {
                                                    asyncResultHandler.handle(Future.failedFuture(updateQuota2Result.cause()));
                                                }
                                            });
                                        }));
                                    } else {
                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                .end(new JsonObject().put(JsonKey.DESCRIPTION, diskQuotaResult.cause().getMessage()).encodePrettily());
                                    }
                                });
                            } else {
                                String sqlForQuota2 = "UPDATE quota2 SET " + StringUtils.join(paramsForQuota2, ",") + " WHERE id=?";
                                String sqlForQuota = "UPDATE quota SET " + StringUtils.join(paramsForQuota, ",") + " WHERE id IN (SELECT quota_id FROM quota2 WHERE quota2.id=?)";
                                Route.vertxDBHelper.updateMultipleSqlInTransaction(finalResult -> {
                                            if (finalResult.succeeded()) {
                                                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                                            } else {
                                                logger.error("Updating quota " + quotaId + " failed", finalResult.cause());
                                                if (finalResult.cause() instanceof SQLException) {
                                                    String stateCode = SQLException.class.cast(finalResult.cause()).getSQLState();
                                                    if (stateCode.equals(SqlErrorCode.UNIQUE_VIOLATION)) {
                                                        routingContext.response().setStatusCode(HttpResponseStatus.CONFLICT.code()).end("Quota name is already used.");
                                                    } else {
                                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                                .end(new JsonObject().put(JsonKey.DESCRIPTION, finalResult.cause().getMessage()).encodePrettily());
                                                    }
                                                } else {
                                                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                            .end(new JsonObject().put(JsonKey.DESCRIPTION, finalResult.cause().getMessage()).encodePrettily());
                                                }
                                            }
                                        }, (connection, asyncResultHandler) ->
                                                connection.updateWithParams(sqlForQuota2, sqlParams, updateQuota2Result -> {
                                                    if (updateQuota2Result.succeeded()) {
                                                        connection.updateWithParams(sqlForQuota, sqlParams, updateQuotaResult -> {
                                                            if (updateQuotaResult.succeeded()) {
                                                                if (fields.size() != 0) {
                                                                    JsonObject text = new JsonObject().put(JsonObjectKey.FIELDS, fields);
                                                                    sendJournal(routingContext, new JsonObject().put(JsonObjectKey.CONTEXT_ID, LogContextType.SBI_QUOTA_UPDATE.getValue())
                                                                            .put(JsonObjectKey.OPERATOR_ID, -1)
                                                                            .put(JsonObjectKey.TARGET_ID, quotaId)
                                                                            .put(JsonObjectKey.TEXT, text.encode())
                                                                            .put(JsonObjectKey.FEDERATED_ID, federatedId));
                                                                }
                                                                asyncResultHandler.handle(Future.succeededFuture());
                                                            } else {
                                                                asyncResultHandler.handle(Future.failedFuture(updateQuotaResult.cause()));
                                                            }
                                                        });
                                                    } else {
                                                        asyncResultHandler.handle(Future.failedFuture(updateQuota2Result.cause()));
                                                    }
                                                })
                                );
                            }
                        } else {
                            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .end(new JsonObject().put(JsonKey.DESCRIPTION, stringAsyncResult.cause().getMessage()).encodePrettily());
                        }
                    });
        });
    }

    public void createQuota(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            JsonObject body = validateRequestBody(routingContext, JsonKey.NAME, JsonKey.DESCRIPTION, JsonKey.QUOTA_SIZE, JsonKey.FEDERATED_ID);

            String name = body.getString(JsonKey.NAME);
            String description = body.getString(JsonKey.DESCRIPTION);
            long quota = body.getLong(JsonKey.QUOTA_SIZE);
            String federatedId = body.getString(JsonKey.FEDERATED_ID);

            Instant startDate = Instant.now();
            String quotaUUID = UUID.randomUUID().toString();

            Route.vertxDBHelper.select("SELECT user_id FROM esysbio_user WHERE federated_id=?", new JsonArray().add(federatedId), userIdResult -> {
                if (userIdResult.succeeded()) {
                    JsonArray jsonArray = new JsonArray(userIdResult.result());
                    logger.debug("federatedId:" + federatedId + ",userIdResult:" + jsonArray.encodePrettily());
                    if (jsonArray.size() >= 1) {

                        Route.vertxDBHelper.updateMultipleSqlInTransaction(finalResult -> {
                            if (finalResult.succeeded()) {
                                routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
                            } else {
                                logger.error("Creating quota failed", finalResult.cause());
                                if (finalResult.cause() instanceof SQLException) {
                                    String stateCode = SQLException.class.cast(finalResult.cause()).getSQLState();
                                    if (stateCode.equals(SqlErrorCode.UNIQUE_VIOLATION)) {
                                        routingContext.response().setStatusCode(HttpResponseStatus.CONFLICT.code()).end("Quota name is already used.");
                                    } else {
                                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                                .end(new JsonObject().put(JsonKey.DESCRIPTION, finalResult.cause().getMessage()).encodePrettily());
                                    }
                                } else {
                                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                            .end(new JsonObject().put(JsonKey.DESCRIPTION, finalResult.cause().getMessage()).encodePrettily());
                                }
                            }
                        }, (connection, asyncResultHandler) ->
                                connection.updateWithParams(
                                        "INSERT INTO quota (name, decription, created, quota, quota_id, owner_id) VALUES (?, ?, ?, ?, ?, ?)",
                                        new JsonArray().add(name).add(description).add(startDate).add(quota).add(quotaUUID).add(jsonArray.getJsonObject(0).getInteger(JsonKey.USER_ID)),
                                        insertQuotaResult -> {
                                            if (insertQuotaResult.succeeded()) {
                                                long quotaId = insertQuotaResult.result().getKeys().getLong(0);
                                                connection.updateWithParams(
                                                        "INSERT INTO quota2 (name, description, created, quota_size, owner_id, quota_id, used_size) VALUES (?, ?, ?, ?, ?, ?, ?)",
                                                        new JsonArray().add(name).add(description).add(startDate).add(quota).add(jsonArray.getJsonObject(0).getInteger(JsonKey.USER_ID)).add(quotaId).add(0),
                                                        insertQuota2Result -> {
                                                            if (insertQuota2Result.succeeded()) {
                                                                long quota2Id = insertQuota2Result.result().getKeys().getLong(0);
                                                                connection.updateWithParams("INSERT INTO disk_quota (disk_quota, quota_id, in_use, original_size) VALUES (?, ?, ?, ?)",
                                                                        new JsonArray().add(quota).add(quotaUUID).add(true).add(quota),
                                                                        insertDiskQuotaResult -> {
                                                                            if (insertDiskQuotaResult.succeeded()) {
                                                                                logger.info("sending journal upon quota creation");
                                                                                JsonArray fields = new JsonArray();
                                                                                fields.add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonObjectKey.NAME)
                                                                                        .put(JsonObjectKey.VALUE, name))
                                                                                        .add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonObjectKey.QUOTA_SIZE)
                                                                                                .put(JsonObjectKey.VALUE, quota))
                                                                                        .add(new JsonObject().put(JsonObjectKey.FIELD_NAME, JsonObjectKey.DESCRIPTION)
                                                                                                .put(JsonObjectKey.VALUE, description));

                                                                                JsonObject text = new JsonObject().put(JsonObjectKey.FIELDS, fields);
                                                                                sendJournal(routingContext, new JsonObject().put(JsonObjectKey.CONTEXT_ID, LogContextType.SBI_QUOTA_CREATE.getValue())
                                                                                        .put(JsonObjectKey.OPERATOR_ID, -1)
                                                                                        .put(JsonObjectKey.TARGET_ID, quota2Id)
                                                                                        .put(JsonObjectKey.TEXT, text.encode())
                                                                                        .put(JsonKey.FEDERATED_ID, federatedId));

                                                                                asyncResultHandler.handle(Future.succeededFuture());
                                                                            } else {
                                                                                asyncResultHandler.handle(Future.failedFuture(insertDiskQuotaResult.cause()));
                                                                            }
                                                                        });
                                                            } else {
                                                                asyncResultHandler.handle(Future.failedFuture(insertQuota2Result.cause()));
                                                            }


                                                        });
                                            } else {
                                                asyncResultHandler.handle(Future.failedFuture(insertQuotaResult.cause()));
                                            }
                                        }));


                    } else {
                        routingContext.response().setStatusCode(HttpResponseStatus.PRECONDITION_FAILED.code()).end("The creator doesn't have sbi profile.");
                    }
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .end(new JsonObject().put(JsonKey.DESCRIPTION, userIdResult.cause().getMessage()).encodePrettily());
                }
            });
        });
    }

    public void getAllQuotas(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);
            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CREATOR, JsonKey.NUM_OF_PROJECTS, JsonKey.QUOTA_SIZE, JsonKey.USED_SIZE, JsonKey.UTILIZATION, JsonKey.CREATED);
            String sql = "SELECT quota2.id, quota2.created, quota2.name, quota2.quota_size, quota2.used_size,cast(used_size as FLOAT)/NULLIF(quota_size, 0) AS utilization,  quota2.quota_id, quota2.description, esysbio_user.firstname, esysbio_user.surname, count(project.project_id) as num_of_projects FROM (quota2 INNER JOIN esysbio_user ON esysbio_user.user_id = quota2.owner_id inner join quota on quota2.name = quota.name left join project on quota.id = project.project_quota) group by quota2.id, quota2.created, quota2.name, quota2.quota_size, quota2.used_size,utilization, quota2.quota_id,  quota2.description, esysbio_user.firstname, esysbio_user.surname";
            sql = "SELECT * FROM (" + sql + ") AS quota ";
            List<String> predicates = new ArrayList<>();
            sort.ifPresent(s -> {
                if (s.contains(JsonKey.CREATOR)) {
                    predicates.addAll(SqlUtils.appendSorting(s.replace(JsonKey.CREATOR, JsonKey.FIRST_NAME)));
                } else {
                    predicates.addAll(SqlUtils.appendSorting(s));
                }
            });
            pair.getLeft().ifPresent(l -> predicates.addAll(SqlUtils.appendLimit(l)));
            pair.getRight().ifPresent(o -> predicates.addAll(SqlUtils.appendOffset(o)));
            sql = sql + String.join(" ", predicates);

            Route.vertxDBHelper.select(sql, result -> returnResponseWithCount(routingContext, HttpResponseStatus.OK, result, "SELECT count(*) FROM quota2"));

        });
    }

    public void doQuotas(RoutingContext routingContext) {

        validateRequest(routingContext, () -> {
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.METHOD);
            String methodName = jsonObject.getString(JsonKey.METHOD);

            if (methodName.trim().equalsIgnoreCase(MethodName.RE_POPULATE_QUOTA2)) {
                recomputeQuotas(routingContext);
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, "Method " + methodName + " is not supported").encodePrettily());
            }
        });
    }

    public void search(RoutingContext routingContext) {
        validateRequest(routingContext, () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);
            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CREATOR, JsonKey.NUM_OF_PROJECTS, JsonKey.QUOTA_SIZE, JsonKey.USED_SIZE, JsonKey.UTILIZATION, JsonKey.CREATED);
            String select = "SELECT quota2.id, quota2.created, quota2.name, quota2.quota_size, quota2.used_size, cast(used_size as FLOAT)/NULLIF(quota_size, 0) AS utilization, quota2.quota_id, quota2.description, esysbio_user.firstname, esysbio_user.surname, count(project.project_id) as num_of_projects FROM (quota2 INNER JOIN esysbio_user ON esysbio_user.user_id = quota2.owner_id inner join quota on quota2.name = quota.name left join project on quota.id = project.project_quota) group by quota2.id, quota2.created, quota2.name, quota2.quota_size,quota2.used_size,utilization, quota2.quota_id,  quota2.description, esysbio_user.firstname, esysbio_user.surname";
            String sql = select;
            String where = "";
            JsonObject requestBody = routingContext.getBodyAsJson();
            if (requestBody.containsKey(JsonKey.QUERY)) {
                String queryText = requestBody.getString(JsonKey.QUERY);
                where = "WHERE LOWER(name) LIKE LOWER('%" + queryText + "%') OR LOWER(description) LIKE ('%" + queryText + "%') OR LOWER(firstname) LIKE LOWER('%" + queryText + "%') OR LOWER(surname) LIKE LOWER('%" + queryText + "%')";
                sql = "SELECT * FROM (" + select + ") AS quota " + where;
            }
            String countSql = "SELECT count(*) from (" + select + ") AS quota " + where;
            List<String> predicates = new ArrayList<>();
            sort.ifPresent(s -> {
                if (s.contains(JsonKey.CREATOR)) {
                    predicates.addAll(SqlUtils.appendSorting(s.replace(JsonKey.CREATOR, JsonKey.FIRST_NAME)));
                } else {
                    predicates.addAll(SqlUtils.appendSorting(s));
                }
            });
            pair.getLeft().ifPresent(l -> predicates.addAll(SqlUtils.appendLimit(l)));
            pair.getRight().ifPresent(o -> predicates.addAll(SqlUtils.appendOffset(o)));

            sql = sql + " " + String.join(" ", predicates);
            Route.vertxDBHelper.select(sql, result -> returnResponseWithCount(routingContext, HttpResponseStatus.OK, result, countSql));

        });
    }

    private void recomputeQuotas(RoutingContext routingContext) {

        if (routingContext.request().headers().contains(JsonKey.FEDERATED_ID_IN_HEADER)) {

            Route.vertxDBHelper.select("SELECT * from quota order by id", quotaLst -> {

                if (quotaLst.failed()) {
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                    return;
                }

                Route.vertxDBHelper.delete("delete from quota2", voidAsyncResult -> {

                    if (voidAsyncResult.failed()) {
                        logger.error("delete from quota2." + voidAsyncResult.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                        return;
                    }
                    logger.error("delete from quota2.");
                    JsonArray quotaJsonArray = new JsonArray(quotaLst.result());

                    List<Long> failedLst = Collections.synchronizedList(new ArrayList<>());
                    final List<Future<Long>> updateQuota2Future = quotaJsonArray.stream().map(JsonObject.class::cast).map(o -> {
                        Long size = ResrouceFacade.getUsedSize(o.getLong(JsonKey.ID));
                        return insertQuota2(o.put(JsonKey.USED_SIZE, size)).setHandler(ar -> {
                            if (ar.failed()) {
                                failedLst.add(o.getLong(JsonKey.ID));
                            }
                        });
                    }).collect(Collectors.toList());

                    ParameterizedCompositeFuture.join(updateQuota2Future).setHandler(ar -> {
                        if (ar.succeeded()) {
                            JsonObject journal = new JsonObject().put(JsonObjectKey.CONTEXT_ID, LogContextType.SBI_QUOTA_RECOMPUTE.getValue())
                                    .put(JsonObjectKey.OPERATOR_ID, -1)
                                    .put(JsonObjectKey.TARGET_ID, -1) // invalid target id
                                    .put(JsonObjectKey.TEXT, "")
                                    .put(JsonObjectKey.FEDERATED_ID, routingContext.request().getHeader(JsonKey.FEDERATED_ID_IN_HEADER));
                            sendJournal(routingContext, journal);
                        } else {
                            routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(new JsonArray(failedLst).encode());
                        }
                    });

                });
            });
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end(new JsonObject().put(JsonKey.DESCRIPTION, JsonKey.FEDERATED_ID_IN_HEADER + " is missing in the header.").encodePrettily());
        }


    }

    private Future<Long> insertQuota2(JsonObject quota) {

        Future<Long> future = Future.future();

        Route.vertxDBHelper.insert("insert into quota2 (name, description, created, quota_size, used_size, owner_id, quota_id) values (?, ?, to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, ?, ?)",
                new JsonArray().add(quota.getString(JsonKey.NAME))
                        .add(quota.getString(JsonKey.DESCRIPTION_TYPO))
                        .add(quota.getString(JsonKey.CREATED))
                        .add(quota.getLong(JsonKey.QUOTA))
                        .add(quota.getLong(JsonKey.USED_SIZE))
                        .add(quota.getLong(JsonKey.OWNER_ID))
                        .add(quota.getLong(JsonKey.ID)), future.completer());

        return future;
    }


}
