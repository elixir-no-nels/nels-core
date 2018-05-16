package no.nels.master.api.facades;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.client.Admin;
import no.nels.commons.constants.JsonObjectKey;
import no.nels.commons.constants.JsonObjectValue;
import no.nels.commons.constants.db.User;
import no.nels.master.api.constants.JsonKey;
import no.nels.master.api.constants.SqlErrorCode;
import no.nels.master.api.constants.UrlParam;
import no.nels.master.api.db.DAOService;
import no.nels.commons.utilities.db.SqlUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by weizhang on 1/24/17.
 */
public final class UserFacade {
    private static Logger logger = LoggerFactory.getLogger(UserFacade.class);

    public static void getUserIds(RoutingContext routingContext) {
        DAOService.getInstance().userIds(result -> routingContext.response().end(result));
    }

    public static void getUser(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);

        String sql = "Select * from users where id=?";
        JsonArray params = new JsonArray();
        params.add(Integer.valueOf(nelsId));
        DAOService.getInstance().getDbHelper().getOne(sql, params, result -> {
            if (result.succeeded()) {
                routingContext.response().end(result.result());
            } else {
                logger.error(result.cause());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    public static void getAllProjectIdsOfUser(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);
        if (StringUtils.isNumeric(nelsId)) {
            String sql = "select project_id from project_users where user_id=?";
            DAOService.getInstance().getDbHelper().select(sql, new JsonArray().add(Integer.valueOf(nelsId)), result -> {
                if (result.succeeded()) {
                    routingContext.response().end(result.result());
                } else {
                    logger.error(result.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            });
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public static void getProjectsOfUser(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);
        if (StringUtils.isNumeric(nelsId)) {
            String offsetParam = routingContext.request().getParam(UrlParam.OFFSET);
            String limitParam = routingContext.request().getParam(UrlParam.LIMIT);

            StringBuilder builder = new StringBuilder();
            builder.append("select * from project where id in (select project_id from project_users where user_id=?) order by name");

            if (!StringUtils.isEmpty(limitParam)) {
                if (StringUtils.isNumeric(limitParam)) {
                    builder.append(" limit ");
                    builder.append(limitParam);
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
                    return;
                }
            }

            if (!StringUtils.isEmpty(offsetParam)) {
                if (StringUtils.isNumeric(offsetParam)) {
                    builder.append(" offset ");
                    builder.append(offsetParam);
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
                    return;
                }
            }

            DAOService.getInstance().getDbHelper().select(builder.toString(), new JsonArray().add(Integer.valueOf(nelsId)), result -> {
                if (result.succeeded()) {
                    String sql = "select count(project_id) from project_users where user_id=?";
                    DAOService.getInstance().getDbHelper().count(sql, new JsonArray().add(Integer.valueOf(nelsId)), countResult -> {
                        if (countResult.succeeded()) {
                            int count = countResult.result();
                            routingContext.response().end(new JsonObject().put(JsonObjectKey.COUNT, count).put(JsonKey.DATA, new JsonArray(result.result())).encode());
                        } else {
                            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                        }
                    });
                } else {
                    logger.error(result.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            });
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public static void getUsers(RoutingContext routingContext) {

        List<String> predicates = new ArrayList<>();
        String select = "select * from (\n" +
                "select users.*, statistics.value as disk_usage,statistics.statstime as stat_time from statistics inner join users on statistics.targetid = users.id\n" +
                "where statistics.id in (select max(id) as id from statistics where statscontextid=100 group by statscontextid,targetid)\n" +
                "union \n" +
                "select users.*, 0 as disk_usage,now() as stat_time from users where id not in ( \n" +
                "select users.id from users inner join statistics on statistics.targetid = users.id\n" +
                "where statistics.id in (select max(id) as id from statistics where statscontextid=100 group by statscontextid,targetid))) as t ";

        if (routingContext.request().params().contains(UrlParam.SORT)) {
            String sort = routingContext.request().getParam(UrlParam.SORT);
            predicates.addAll(SqlUtils.appendSorting(sort));
        }
        if (routingContext.request().params().contains(UrlParam.OFFSET)) {
            String offset = routingContext.request().getParam(UrlParam.OFFSET);
            predicates.addAll(SqlUtils.appendOffset(offset));
        }

        if (routingContext.request().params().contains(UrlParam.LIMIT)) {
            String limit = routingContext.request().getParam(UrlParam.LIMIT);
            predicates.addAll(SqlUtils.appendLimit(limit));
        }

        String sql = select + " " + String.join(" ", predicates);

        logger.debug("getUsers. sql:" + sql);
        DAOService.getInstance().getDbHelper().select(sql, result -> {
            if (result.succeeded()) {
                String countSql = "select count(*) from users";
                DAOService.getInstance().getDbHelper().count(countSql, countResult -> {
                    if (countResult.succeeded()) {
                        int count = countResult.result();
                        routingContext.response().end(new JsonObject().put(JsonObjectKey.COUNT, count).put(JsonKey.DATA, new JsonArray(result.result())).encode());
                    } else {
                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                    }
                });
            } else {
                logger.error(result.cause());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    public static void actionHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);
        logger.debug("Receive request of updating user active statue ");
        JsonObject requestBody = routingContext.getBodyAsJson();
        String method = requestBody.getString(JsonObjectKey.METHOD);
        if (method.equals(JsonObjectValue.ACTIVATE)) {
            updateUserActiveStatue(nelsId, true, routingContext);
        } else if (method.equals(JsonObjectValue.DEACTIVATE)) {
            updateUserActiveStatue(nelsId, false, routingContext);
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        }
    }

    private static void updateUserActiveStatue(String nelsId, boolean statue, RoutingContext routingContext) {
        String sql = "update users set isactivie=? where id=?";
        JsonArray params = new JsonArray();
        params.add(statue);
        params.add(Integer.valueOf(nelsId));
        DAOService.getInstance().getDbHelper().updateOne(sql, params, result -> {
            if (result.succeeded()) {
                routingContext.response().end();
            } else {
                logger.error(result.cause());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    public static void countUsers(RoutingContext routingContext) {
        String sql = "select count(*) from users";
        DAOService.getInstance().getDbHelper().count(sql, result -> {
            if (result.succeeded()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.put(JsonObjectKey.COUNT, result.result());
                routingContext.response().end(jsonObject.encode());
            } else {
                logger.warn("failed to count users." + result.cause().getLocalizedMessage());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(result.cause().getLocalizedMessage());
            }
        });
    }

    public static void updateHandler(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);
        logger.debug("Receive update request for nels " + nelsId);
        JsonObject requestBody = routingContext.getBodyAsJson();

        logger.debug("Request body is " + requestBody.encode());
        List<String> params = new ArrayList<>(requestBody.size());
        requestBody.stream().forEach(entry -> {
            if (entry.getKey().equals(User.USER_TYPE)) {
                params.add(entry.getKey() + "=" + entry.getValue());
            } else {
                params.add(entry.getKey() + "='" + entry.getValue().toString() + "'");
            }
        });
        StringBuilder builder = new StringBuilder();
        builder.append("update users set ");
        builder.append(StringUtils.join(params, ","));
        builder.append(" where id=");
        builder.append(nelsId);
        logger.debug("sql is " + builder.toString());
        DAOService.getInstance().getDbHelper().update(builder.toString(), result -> {
            if (result.succeeded()) {
                routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    public static void deleteUser(RoutingContext routingContext) {
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);
        logger.debug("Receive request of deleting user " + nelsId);
        if (StringUtils.isNumeric(nelsId)) {
            Integer id = Integer.valueOf(nelsId);
            String sql = "delete from users where id=?";

            DAOService.getInstance().getDbHelper().delete(sql, new JsonArray().add(id), result -> {
                if (result.succeeded()) {
                    logger.debug("The user has been deleted");
                    routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                } else {
                    logger.error(result.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            }, () -> {
                Boolean isDeleted = false;
                try {
                    isDeleted = Admin.deleteUser(id);
                } catch (RuntimeException e) {
                    logger.error(e);
                }
                return isDeleted;
            });
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public static void registerUser(RoutingContext routingContext) {

        JsonObject requestBody = routingContext.getBodyAsJson();

        logger.debug("Register new user ");
        logger.debug(requestBody.encode());
        JsonArray params = new JsonArray();
        params.add(requestBody.getInteger(User.IDP_NUMBER));
        params.add(requestBody.getString(User.IDP_USER_NAME));
        params.add(requestBody.getInteger(User.USER_TYPE));
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        params.add(format.format(cal.getTime()));
        params.add(requestBody.getBoolean(User.IS_ACTIVE));
        params.add(requestBody.getString(User.NAME));
        params.add(requestBody.getString(User.EMAIL));
        params.add(requestBody.getString(User.AFFILIATION));
        String sql = "INSERT INTO users(idpnumber, idpusername, usertype, registrationdate, isactivie, name, email, affiliation) VALUES (?, ?, ?, to_timestamp(?, 'YYYY-MM-DD'), ?, ?, ?, ?)";
        DAOService.getInstance().getDbHelper().insert(sql, params, result -> {
            if (result.succeeded()) {
                logger.debug("The user info has been inserted into db");
                long id = result.result();
                logger.debug("The id of new user is " + id);

                routingContext.vertx().executeBlocking(future -> {
                    try {
                        if (Admin.createUser(id, id, requestBody.getString(User.NAME), requestBody.getInteger(User.USER_TYPE))) {
                            future.complete(true);
                        } else {
                            future.fail("Failed to create user in storage part");
                        }
                    } catch (Exception e) {
                        logger.error(e);
                        future.fail(e.getCause());
                    }
                }, false, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        logger.debug("The new user has been created in storage side");
                        JsonObject response = new JsonObject();
                        response.put(User.ID, id);
                        routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end(response.encode());
                    } else {
                        logger.error(asyncResult.cause());
                        String deleteSql = "delete from users where id=" + id;
                        DAOService.getInstance().getDbHelper().deleteOne(deleteSql, result1 -> {
                            if (result1.succeeded()) {
                                routingContext.response().setStatusCode(HttpResponseStatus.FAILED_DEPENDENCY.code()).end();
                            } else {
                                logger.error(result1.cause());
                                //TODO should log this event, because the new user record in db should be deleted
                                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                            }
                        });
                    }
                });
            } else {
                logger.error(result.cause());
                if (result.cause() instanceof SQLException) {
                    String stateCode = SQLException.class.cast(result.cause()).getSQLState();
                    if (stateCode.equals(SqlErrorCode.UNIQUE_VIOLATION)) {
                        routingContext.response().setStatusCode(HttpResponseStatus.CONFLICT.code()).end();
                    }
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            }
        });
    }

    public static void searchHandler(RoutingContext routingContext) {
        String select = "select * from (\n" +
                "select users.*, statistics.value as disk_usage,statistics.statstime as stat_time from statistics inner join users on statistics.targetid = users.id\n" +
                "where statistics.id in (select max(id) as id from statistics where statscontextid=100 group by statscontextid,targetid)\n" +
                "union \n" +
                "select users.*, 0 as disk_usage,now() as stat_time from users where id not in ( \n" +
                "select users.id from users inner join statistics on statistics.targetid = users.id\n" +
                "where statistics.id in (select max(id) as id from statistics where statscontextid=100 group by statscontextid,targetid))) as t ";
        String sql = assembleSql(routingContext, select);
        logger.debug("Receive user query request. sql:" + sql);

        DAOService.getInstance().getDbHelper().select(sql, result -> {
            if (result.succeeded()) {
                if (routingContext.request().params().contains(UrlParam.OFFSET) &&
                        routingContext.request().params().contains(UrlParam.LIMIT)) {
                    String countSql = "SELECT count(*) FROM users";
                    JsonObject requestBody = routingContext.getBodyAsJson();
                    if (requestBody.containsKey(JsonObjectKey.QUERY)) {
                        String queryStr = requestBody.getString(JsonObjectKey.QUERY);
                        countSql += (" where LOWER(name) LIKE LOWER('%" + queryStr + "%') OR LOWER(email) LIKE LOWER('%" + queryStr + "%')");
                    }

                    DAOService.getInstance().getDbHelper().count(countSql, countResult -> {
                        if (countResult.succeeded()) {
                            int count = countResult.result();
                            routingContext.response().end(new JsonObject().put(JsonKey.COUNT, count).put(JsonKey.DATA, new JsonArray(result.result())).encode());
                        } else {
                            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                        }
                    });
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(result.result());
                }

            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    private static String assembleSql(RoutingContext routingContext, String select) {
        JsonObject requestBody = routingContext.getBodyAsJson();
        StringBuilder builder = new StringBuilder();

        List<String> params = new ArrayList<>();
        if (requestBody.containsKey(User.ID)) {
            params.add("id=" + requestBody.getLong(User.ID));
        }
        if (requestBody.containsKey(User.IDP_NUMBER)) {
            params.add("idpnumber=" + requestBody.getInteger(User.IDP_NUMBER));
        }
        if (requestBody.containsKey(User.USER_TYPE)) {
            params.add("usertype=" + requestBody.getInteger(User.USER_TYPE));
        }
        if (requestBody.containsKey(User.IDP_USER_NAME)) {
            params.add("LOWER(idpusername) LIKE LOWER('%" + requestBody.getString(User.IDP_USER_NAME) + "%')");
        }
        if (requestBody.containsKey(User.NAME)) {
            params.add("LOWER(name) LIKE LOWER('%" + requestBody.getString(User.NAME) + "%')");
        }
        if (requestBody.containsKey(User.EMAIL)) {
            params.add("LOWER(email) LIKE LOWER('%" + requestBody.getString(User.EMAIL) + "%')");
        }
        if (requestBody.containsKey(User.AFFILIATION)) {
            params.add("LOWER(affiliation) LIKE LOWER('%" + requestBody.getString(User.AFFILIATION) + "%')");
        }
        if (requestBody.containsKey(User.IS_ACTIVE)) {
            params.add("isactivie='" + requestBody.getBoolean(User.IS_ACTIVE) + "'");
        }
        if (requestBody.containsKey(JsonObjectKey.QUERY)) {
            String queryStr = requestBody.getString(JsonObjectKey.QUERY);
            params.add("LOWER(name) LIKE LOWER('%" + queryStr + "%') OR LOWER(email) LIKE LOWER('%" + queryStr + "%')");
        }
        if (params.size() != 0) {
            select = select + " where " + StringUtils.join(params, " and ");
        }
        builder.append(select);
        List<String> sql = new ArrayList<>();

        if (routingContext.request().params().contains(UrlParam.SORT)) {
            String sort = routingContext.request().getParam(UrlParam.SORT);
            sql.addAll(SqlUtils.appendSorting(sort));
        }
        if (routingContext.request().params().contains(UrlParam.OFFSET)) {
            String offset = routingContext.request().getParam(UrlParam.OFFSET);
            sql.addAll(SqlUtils.appendOffset(offset));
        }

        if (routingContext.request().params().contains(UrlParam.LIMIT)) {
            String limit = routingContext.request().getParam(UrlParam.LIMIT);
            sql.addAll(SqlUtils.appendLimit(limit));
        }


        return builder.toString() + " " + String.join(" ", sql);
    }

}
