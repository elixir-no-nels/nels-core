package no.nels.master.api.facades;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.client.ProjectApi;
import no.nels.commons.constants.JsonObjectKey;
import no.nels.commons.constants.ProjectMembership;
import no.nels.commons.constants.db.Project;
import no.nels.commons.constants.db.ProjectUsers;
import no.nels.master.api.constants.JsonKey;
import no.nels.master.api.constants.UrlParam;
import no.nels.master.api.db.DAOService;
import no.nels.commons.utilities.db.SqlUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by weizhang on 1/24/17.
 */
public class ProjectFacade {
    private static Logger logger = LoggerFactory.getLogger(ProjectFacade.class);

    public static void getProjectIds(RoutingContext routingContext) {
        String sql = "select id from project ORDER BY id DESC";
        DAOService.getInstance().getDbHelper().select(sql, result -> {
            if (result.succeeded()) {
                routingContext.response().end(result.result());
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    public static void countProjects(RoutingContext routingContext) {
        String sql = "select count(*) from project";
        DAOService.getInstance().getDbHelper().count(sql, result -> {
            if (result.succeeded()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.put(JsonObjectKey.COUNT, result.result());
                routingContext.response().end(jsonObject.encode());
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    public static void getAllUserIdsInProject(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParam.PROJECT_ID);
        if (StringUtils.isNumeric(projectId)) {
            String sql = "select user_id from project_users where project_id=?";
            DAOService.getInstance().getDbHelper().select(sql, new JsonArray().add(Integer.valueOf(projectId)), result -> {
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

    public static void getUserInProject(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParam.PROJECT_ID);
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);

        if (StringUtils.isNumeric(projectId) && StringUtils.isNumeric(nelsId)) {
            String sql = "select membership_type from project_users where project_id=? and user_id=?";
            JsonArray params = new JsonArray();
            params.add(Integer.valueOf(projectId));
            params.add(Integer.valueOf(nelsId));

            DAOService.getInstance().getDbHelper().getOne(sql, params, result -> {
                if (result.succeeded()) {
                    routingContext.response().end(result.result());
                } else {
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            });
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public static void updateUserInProject(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParam.PROJECT_ID);
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);
        if (StringUtils.isNumeric(projectId) && StringUtils.isNumeric(nelsId)) {
            JsonObject requestBody = routingContext.getBodyAsJson();
            int role = requestBody.getInteger(ProjectUsers.MEMBERSHIP_TYPE);
            if (ProjectMembership.checkMembership(role)) {
                String sql = "update project_users set membership_type=? where user_id=? and project_id=?";
                JsonArray params = new JsonArray();
                params.add(role);
                params.add(Integer.valueOf(nelsId));
                params.add(Integer.valueOf(projectId));

                DAOService.getInstance().getDbHelper().update(sql, params, result -> {
                    if (result.succeeded()) {
                        routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                    } else {
                        logger.error(result.cause());
                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                    }
                }, () -> {
                    Boolean isUpdated = false;
                    try {
                        isUpdated = ProjectApi.changeProjectMembershipType(Integer.valueOf(projectId), Integer.valueOf(nelsId), ProjectMembership.nameOf(role).getName());
                    } catch (RuntimeException e) {
                        logger.error(e);
                    }
                    return isUpdated;
                });
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
            }
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public static void deleteUserInProject(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParam.PROJECT_ID);
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);
        if (StringUtils.isNumeric(projectId) && StringUtils.isNumeric(nelsId)) {
            logger.debug("Receive request of removing user from project");
            String sql = "delete from project_users where project_id=? and user_id=?";
            JsonArray params = new JsonArray();
            params.add(Integer.valueOf(projectId));
            params.add(Integer.valueOf(nelsId));
            DAOService.getInstance().getDbHelper().delete(sql, params, result -> {
                if (result.succeeded()) {
                    routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                } else {
                    logger.error(result.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            }, () -> {
                Boolean isDeleted = false;
                try {
                    isDeleted = ProjectApi.removeUserFromProject(Integer.valueOf(projectId), Integer.valueOf(nelsId));
                } catch (RuntimeException e) {
                    logger.error(e);
                }
                return isDeleted;
            });
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public static void addUserToProject(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParam.PROJECT_ID);
        String nelsId = routingContext.request().getParam(UrlParam.NELS_ID);
        if (StringUtils.isNumeric(projectId) && StringUtils.isNumeric(nelsId)) {
            logger.debug("Receive request of adding user to a project");
            JsonObject requestBody = routingContext.getBodyAsJson();
            int role = requestBody.getInteger(ProjectUsers.MEMBERSHIP_TYPE);
            if (ProjectMembership.checkMembership(role)) {
                String sql = "insert into project_users (project_id, user_id, membership_type) values (?, ?, ?)";
                JsonArray params = new JsonArray();
                params.add(Integer.valueOf(projectId));
                params.add(Integer.valueOf(nelsId));
                params.add(role);
                DAOService.getInstance().getDbHelper().insert(sql, params, result -> {
                    if (result.succeeded()) {
                        routingContext.response().end();
                    } else {
                        logger.error(result.cause());
                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                    }
                }, () -> {
                    Boolean isAdded = false;
                    try {
                        isAdded = ProjectApi.addUserToProject(Integer.valueOf(projectId), Integer.valueOf(nelsId), ProjectMembership.nameOf(role).getName());
                    } catch (RuntimeException e) {
                        logger.error(e);
                    }
                    return isAdded;
                });
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
            }
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public static void searchHandler(RoutingContext routingContext) {

        if (routingContext.request().params().contains(UrlParam.OFFSET) &&
                routingContext.request().params().contains(UrlParam.LIMIT)) {
            searchProjectMoreInfo(routingContext);
        } else {
            searchProject(routingContext);
        }
    }

    private static void searchProjectById(RoutingContext routingContext) {
        String sql = "Select * from project where id=?";
        JsonArray params = new JsonArray();
        params.add(routingContext.getBodyAsJson().getLong(Project.ID));
        DAOService.getInstance().getDbHelper().select(sql, params, result -> {
            if (result.succeeded()) {
                routingContext.response().end(result.result());
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    private static void searchProject(RoutingContext routingContext) {
        JsonObject requestBody = routingContext.getBodyAsJson();
        if (requestBody.containsKey(Project.ID)) {
            searchProjectById(routingContext);
        } else {
            fuzzySearchProject(routingContext);
        }
    }

    private static void fuzzySearchProject(RoutingContext routingContext) {
        JsonObject requestBody = routingContext.getBodyAsJson();
        String sql = "select * from project";
        List<String> params = new ArrayList<>();
        if (requestBody.containsKey(Project.NAME)) {
            params.add("LOWER(name) LIKE LOWER ('%" + requestBody.getString(Project.NAME) + "%')");
        }
        if (requestBody.containsKey(Project.DESCRIPTION)) {
            params.add("LOWER(description) LIKE LOWER ('%" + requestBody.getString(Project.DESCRIPTION) + "%')");
        }

        if (params.size() != 0) {
            sql = sql + " where " + StringUtils.join(params, " and ");
        }
        DAOService.getInstance().getDbHelper().select(sql, result -> {
            if (result.succeeded()) {
                routingContext.response().end(result.result());
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    private static void searchProjectMoreInfo(RoutingContext routingContext) {


        String sql = "SELECT project.*, count(project_users.user_id) AS number_of_users FROM (\n" +
                "  SELECT * from (\n" +
                "  select project.*, statistics.value as disk_usage,statistics.statstime as stat_time from statistics inner join project on statistics.targetid = project.id\n" +
                "  where statistics.id in (select max(id) as id from statistics where statscontextid=200 group by statscontextid,targetid)\n" +
                "  union\n" +
                "  select project.*, 0 as disk_usage,now() as stat_time from project where id not in (\n" +
                "  select project.id from project inner join statistics on statistics.targetid = project.id\n" +
                "  where statistics.id in (select max(id) as id from statistics where statscontextid=200 group by statscontextid,targetid))) as t) as project\n" +
                "LEFT JOIN project_users ON project.id = project_users.project_id";
        sql += assembleSql(routingContext);
        String groupBy = " GROUP BY project.id, project.name, project.description, project.creation_date, project.disk_usage, project.stat_time";
        sql += groupBy;
        List<String> predicates = new ArrayList<>();
        if (routingContext.request().params().contains(UrlParam.SORT)) {
            predicates.addAll(SqlUtils.appendSorting(routingContext.request().getParam(UrlParam.SORT)));
        }
        if (routingContext.request().params().contains(UrlParam.OFFSET)) {
            predicates.addAll(SqlUtils.appendOffset(routingContext.request().getParam(UrlParam.OFFSET)));
        }

        if (routingContext.request().params().contains(UrlParam.LIMIT)) {
            predicates.addAll(SqlUtils.appendLimit(routingContext.request().getParam(UrlParam.LIMIT)));
        }

        sql += " " + String.join(" ", predicates);

        DAOService.getInstance().getDbHelper().select(sql, result -> {
            if (result.succeeded()) {
                String countSql = "SELECT count(*) FROM project";
                JsonObject requestBody = routingContext.getBodyAsJson();
                if (requestBody.containsKey(JsonObjectKey.QUERY)) {
                    String queryStr = requestBody.getString(JsonObjectKey.QUERY);
                    countSql += (" where LOWER(name) LIKE LOWER('%" + queryStr + "%')");
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
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }

    private static String assembleSql(RoutingContext routingContext) {
        JsonObject requestBody = routingContext.getBodyAsJson();
        String sql = "";
        if (requestBody.containsKey(JsonObjectKey.QUERY)) {
            String queryStr = requestBody.getString(JsonObjectKey.QUERY);
            sql += (" WHERE LOWER(name) LIKE LOWER('%" + queryStr + "%')");
        }
        return sql;
    }


    public static void getProject(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParam.PROJECT_ID);
        if (StringUtils.isNumeric(projectId)) {
            String sql = "select * from project where id=?";
            JsonArray params = new JsonArray();
            params.add(Integer.valueOf(projectId));
            DAOService.getInstance().getDbHelper().getOne(sql, params, result -> {
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

    public static void getProjects(RoutingContext routingContext) {

        String select = "SELECT project.*, count(project_users.user_id) AS number_of_users FROM (\n" +
                "  SELECT * from (\n" +
                "  select project.*, statistics.value as disk_usage,statistics.statstime as stat_time from statistics inner join project on statistics.targetid = project.id\n" +
                "  where statistics.id in (select max(id) as id from statistics where statscontextid=200 group by statscontextid,targetid)\n" +
                "  union\n" +
                "  select project.*, 0 as disk_usage,now() as stat_time from project where id not in (\n" +
                "  select project.id from project inner join statistics on statistics.targetid = project.id\n" +
                "  where statistics.id in (select max(id) as id from statistics where statscontextid=200 group by statscontextid,targetid))) as t) as project\n" +
                "LEFT JOIN project_users ON project.id = project_users.project_id\n" +
                "GROUP BY project.id, project.name, project.description, project.creation_date, project.disk_usage, project.stat_time";
        List<String> predicates = new ArrayList<>();
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

        DAOService.getInstance().getDbHelper().select(sql, result -> {
            if (result.succeeded()) {
                String countSsql = "select count(*) from project";
                DAOService.getInstance().getDbHelper().count(countSsql, countResult -> {
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

    public static void updateProject(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParam.PROJECT_ID);
        if (StringUtils.isNumeric(projectId)) {
            logger.debug("Receive update request for project");
            JsonObject requestBody = routingContext.getBodyAsJson();

            List<String> params = new ArrayList<>(requestBody.size());
            requestBody.stream().forEach(entry ->
                params.add(entry.getKey() + "='" + entry.getValue().toString() + "'")
            );

            StringBuilder builder = new StringBuilder();
            builder.append("update project set ");
            builder.append(StringUtils.join(params, ","));
            builder.append(" where id=");
            builder.append(projectId);

            if (requestBody.containsKey(Project.NAME)) {
                DAOService.getInstance().getDbHelper().update(builder.toString(), new JsonArray(), result -> {
                    if (result.succeeded()) {
                        routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                    } else {
                        logger.error(result.cause());
                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                    }
                }, () -> {
                    Boolean isUpdated = false;
                    try {
                        isUpdated = ProjectApi.renameProject(Integer.valueOf(projectId), requestBody.getString(Project.NAME));
                    } catch (RuntimeException e) {
                        logger.error(e);
                    }
                    return isUpdated;
                });
            } else {
                DAOService.getInstance().getDbHelper().update(builder.toString(), result -> {
                    if (result.succeeded()) {
                        routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                    } else {
                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                    }
                });
            }

        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public static void deleteProject(RoutingContext routingContext) {
        String projectId = routingContext.request().getParam(UrlParam.PROJECT_ID);
        if (StringUtils.isNumeric(projectId)) {
            logger.debug("Receive request of deleting a project");
            String sql = "delete from project where id=?";
            JsonArray params = new JsonArray();
            params.add(Integer.valueOf(projectId));
            DAOService.getInstance().getDbHelper().delete(sql, params, result -> {
                if (result.succeeded()) {
                    routingContext.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end();
                } else {
                    logger.error(result.cause());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                }
            }, () -> {
                Boolean isDeleted = false;
                try {
                    isDeleted = ProjectApi.deleteProject(Integer.valueOf(projectId));
                } catch (RuntimeException e) {
                    logger.error(e);
                }
                return isDeleted;
            });
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code()).end();
        }
    }

    public static void createProject(RoutingContext routingContext) {
        logger.debug("Receive request of creating a project");
        JsonObject jsonObject = routingContext.getBodyAsJson();

        String sql = "insert into project (name, description, creation_date) values (?, ?, to_timestamp(?, 'YYYY-MM-DD'))";
        JsonArray params = new JsonArray();
        params.add(jsonObject.getString(Project.NAME));
        params.add(jsonObject.getString(Project.DESCRIPTION));
        Calendar cal = Calendar.getInstance();
        params.add(cal.toInstant());

        DAOService.getInstance().getDbHelper().insert(sql, params, result -> {
            if (result.succeeded()) {
                long id = result.result();
                routingContext.vertx().executeBlocking(future -> {
                    try {
                        if (ProjectApi.createProject(id, jsonObject.getString(Project.NAME))) {
                            future.complete(true);
                        } else {
                            future.fail("Failed to create project in storage side");
                        }
                    } catch (Exception e) {
                        logger.error(e);
                        future.fail(e.getCause());
                    }
                }, false, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end(new JsonObject().put(Project.ID, id).encode());
                    } else {
                        String deleteSql = "delete from project where id=?";
                        DAOService.getInstance().getDbHelper().delete(deleteSql, new JsonArray().add(id), result1 -> {
                            if (result1.succeeded()) {
                                logger.debug("The project cannot be created in storage system");
                                routingContext.response().setStatusCode(HttpResponseStatus.FAILED_DEPENDENCY.code()).end();
                            } else {
                                //TODO can't delete the created project record in db, needs to delete it manually
                                logger.error(result1.cause());
                                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                            }
                        });
                    }
                });
            } else {
                logger.error(result.cause());
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });
    }
}
