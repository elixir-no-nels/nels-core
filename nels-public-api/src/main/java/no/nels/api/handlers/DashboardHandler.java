package no.nels.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.api.constants.JsonKey;
import no.nels.api.facades.StatisticsFacade;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public final class DashboardHandler implements IHttpRequestHandler {
    private static Logger logger = LoggerFactory.getLogger(DashboardHandler.class);

    public void getDashboardInfo(RoutingContext routingContext) {
        JsonObject response = new JsonObject();
        returnResponseFunction(routingContext, () -> true, () -> {
            response.put(JsonKey.NELS_DISK_PERONAL_ALL, StatisticsFacade.getNeLSDiskPeronsalAll())
                    .put(JsonKey.NELS_DISK_PROJECTS_ALL, StatisticsFacade.getNeLSDiskProjectsAll())
                    .put(JsonKey.NELS_DISK_USAGE_LAST_UPDATE, StatisticsFacade.getLastNeLSDiskUsageUpdateTime())
                    .put(JsonKey.DISK_USAGE_LAST_UPDATE, StatisticsFacade.getLastSbiDiskUsageUpdateTime())
                    .put(JsonKey.NELS_DISK_TOTAL, StatisticsFacade.getNeLSDiskTotal());

            return Pair.of(HttpResponseStatus.OK, Optional.of(response.encode()));
        });
    }

}
