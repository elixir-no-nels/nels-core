package no.nels.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import no.nels.api.constants.JsonKey;
import no.nels.client.ProjectApi;
import no.nels.client.StatisticsApi;
import no.nels.client.UserApi;
import no.nels.commons.constants.InvalidValues;
import no.nels.commons.constants.NelsUserType;
import no.nels.commons.constants.StatsContextType;
import no.nels.commons.constants.db.Project;
import no.nels.commons.constants.db.Statistics;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.Optional;

public final class NeLSHandler implements IHttpRequestHandler {
    private static Logger logger = LoggerFactory.getLogger(NeLSHandler.class);

    public void getUserInfo(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> true, () -> {
            User user = routingContext.user();
            JsonObject response = new JsonObject();
            response.put(JsonKey.NELS_ID, user.principal().getInteger(JsonKey.NELS_ID))
                    .put(JsonKey.NAME, user.principal().getString(JsonKey.NAME))
                    .put(JsonKey.USER_TYPE, user.principal().getString(JsonKey.USER_TYPE));

            return Pair.of(HttpResponseStatus.OK, Optional.of(response.encode()));
        });
    }

    public void getNelsUsers(RoutingContext routingContext) {
        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.REGISTRATION_DATE, JsonKey.DISK_USAGE);
            JsonObject requestBody = new JsonObject(UserApi.getNelsUsers(pair.getLeft(), pair.getRight(), sort));
            JsonArray userJsonArray = requestBody.getJsonArray(JsonKey.DATA);

            return Pair.of(HttpResponseStatus.OK, Optional.of(new JsonObject().put(JsonKey.COUNT, requestBody.getInteger(JsonKey.COUNT)).put(JsonKey.DATA, userJsonArray).encode()));
        });

    }

    public void getNelsAllProjects(RoutingContext routingContext) {

        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);

            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CREATION_DATE, JsonKey.NUMBER_OF_USERS, JsonKey.DISK_USAGE);

            JsonObject response = new JsonObject(ProjectApi.getProjects(pair.getLeft(), pair.getRight(), sort));
            JsonArray projectJsonArray = response.getJsonArray(JsonKey.DATA);
            return Pair.of(HttpResponseStatus.OK, Optional.of(new JsonObject().put(JsonKey.COUNT, response.getInteger(JsonKey.COUNT)).put(JsonKey.DATA, projectJsonArray).encode()));
        });
    }

    public void getNelsProjects(RoutingContext routingContext) {
        int nelsId = routingContext.user().principal().getInteger(JsonKey.NELS_ID);

        returnResponseFunction(routingContext, () -> true, () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);
            String response = UserApi.getProjects(nelsId, pair.getLeft(), pair.getRight());

            return Pair.of(HttpResponseStatus.OK, Optional.of(returnNelsProjects(new JsonObject(response))));
        });
    }

    public void searchNelsUsers(RoutingContext routingContext) {

        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.QUERY);
            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.REGISTRATION_DATE, JsonKey.DISK_USAGE);
            JsonObject requestBody = new JsonObject(UserApi.searchNelsUsers(jsonObject.getString(JsonKey.QUERY), pair.getLeft(), pair.getRight(), sort));

            JsonArray userJsonArray = requestBody.getJsonArray(JsonKey.DATA);

            return Pair.of(HttpResponseStatus.OK, Optional.of(new JsonObject().put(JsonKey.COUNT, requestBody.getInteger(JsonKey.COUNT)).put(JsonKey.DATA, userJsonArray).encode()));
        });

    }

    public void searchNelsProjects(RoutingContext routingContext) {

        returnResponseFunction(routingContext, () -> !routingContext.user().principal().getString(JsonKey.USER_TYPE).equalsIgnoreCase(NelsUserType.USER.getName()), () -> {
            Pair<Optional<String>, Optional<String>> pair = validateUrlParamForReturnPartialResult(routingContext);
            JsonObject jsonObject = validateRequestBody(routingContext, JsonKey.QUERY);
            Optional<String> sort = validateUrlParamForSorting(routingContext, JsonKey.ID, JsonKey.NAME, JsonKey.CREATION_DATE, JsonKey.DISK_USAGE, JsonKey.NUMBER_OF_USERS);
            JsonObject requestBody = new JsonObject(ProjectApi.searchNelsProjects(jsonObject.getString(JsonKey.QUERY), pair.getLeft(), pair.getRight(), sort));
            JsonArray projectJsonArray = requestBody.getJsonArray(JsonKey.DATA);
            return Pair.of(HttpResponseStatus.OK, Optional.of(new JsonObject().put(JsonKey.COUNT, requestBody.getInteger(JsonKey.COUNT)).put(JsonKey.DATA, projectJsonArray).encode()));
        });

    }

    private String returnNelsProjects(JsonObject response) {
        JsonArray projects = response.getJsonArray(JsonKey.DATA);
        JsonObject project;
        JsonArray statistics;
        JsonObject statistic;
        JsonArray newJsonArray = new JsonArray();
        for (Object object : projects) {
            project = JsonObject.class.cast(object);
            statistics = StatisticsApi.getStatsOfTarget(StatsContextType.NELS_PROJECT_DISK_USAGE, project.getInteger(Project.ID));
            if (statistics.size() != 0) {
                statistic = statistics.getJsonObject(0);
                project.put(JsonKey.DISK_USAGE, new JsonObject().put(JsonKey.SIZE, statistic.getInteger(Statistics.VALUE)).put(JsonKey.DATE, statistic.getInstant(Statistics.STATS_TIME)));
            } else {
                project.put(JsonKey.DISK_USAGE, new JsonObject().put(JsonKey.SIZE, InvalidValues.InvalidNumberValue).put(JsonKey.DATE, Instant.now()));
            }
            newJsonArray.add(project);
        }
        return new JsonObject().put(JsonKey.COUNT, response.getInteger(JsonKey.COUNT)).put(JsonKey.DATA, newJsonArray).encode();
    }
}
