package no.nels.master.api.facades;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import no.nels.commons.constants.LogContextType;
import no.nels.master.api.Sender;
import no.nels.master.api.db.DAOService;
import no.nels.vertx.commons.constants.MqJobStatus;
import no.nels.vertx.commons.constants.MqJobType;

import java.time.Instant;
import java.util.Optional;

import static no.nels.vertx.commons.constants.MqJobType.STORAGE_COPY;
import static no.nels.vertx.commons.constants.MqJobType.STORAGE_MOVE;

/**
 * Created by weizhang on 12/7/16.
 */
public class JobFacade {
    private static Logger logger = LoggerFactory.getLogger(JobFacade.class);
    public static void addJob(RoutingContext routingContext) {
        logger.debug("body length: " + routingContext.getBody().length());
        Optional<JsonObject> body = Optional.ofNullable(routingContext.getBodyAsJson());
        logger.debug("json body: " + body.map(x -> x.toString()));
        Optional<Long> nelsId = body.map(x -> x.getLong("nels_id"));
        Optional<Long> jobTypeId = body.map(x -> x.getLong("job_type_id"));
        Optional<JsonObject> parameters = body.map(x -> x.getJsonObject("parameters"));
        if (!nelsId.isPresent() ||
                !jobTypeId.isPresent() ||
                !parameters.isPresent()) {
            logger.error("Some json fields are missing.");
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        }
        Long jobType = jobTypeId.get();
        JsonObject param = parameters.get();
        JsonObject paramWithSensitiveInfo = param.copy();

        if (param.containsKey("ssh_key") && (jobType == MqJobType.SBI_PULL.getValue() ||
                jobType == MqJobType.SBI_PUSH.getValue() ||
                jobType == MqJobType.TSD_PULL.getValue() ||
                jobType == MqJobType.TSD_PUSH.getValue() ||
                jobType == MqJobType.NIRD_SBI_PUSH.getValue() ||
                jobType == MqJobType.NIRD_SBI_PULL.getValue())) {

            param.remove("ssh_key");

        }
        DAOService.getInstance().insertJob(nelsId.get(), jobTypeId.get(), param.encode(), MqJobStatus.SUBMITTED.getValue(), jobId -> {
            if (-1 != jobId) {
                //construct params
                JsonObject params = new JsonObject();

                if (jobType == STORAGE_COPY.getValue() || jobType == STORAGE_MOVE.getValue()) {
                    params.put("nels_id", nelsId.get());
                }
                params.mergeIn(paramWithSensitiveInfo);
                //send to rabbitmq
                Sender.send(params.encode(), jobType, jobId);

                routingContext.response().setStatusCode(HttpResponseStatus.CREATED.code()).end(new JsonObject().put("job_id", jobId).encode());
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
            }
        });

    }


    public static void deleteJob(RoutingContext routingContext) {

        long jobId = Long.valueOf(routingContext.request().getParam("jobId"));
        logger.debug("delete job. jobId:" + jobId);
        DAOService.getInstance().queryJob(jobId, job -> {
            JsonObject o = new JsonObject(job);
            long userId = o.getInteger("nelsid");
            long state = o.getLong("stateid");
            //long jobtypeid = o.getLong("jobtypeid");
            String params = o.getString("params");
            JsonObject json = new JsonObject(params);
            Instant lastUpdate = o.getInstant("lastupdate");
            json.put("lastupdate", lastUpdate);
            if (state == MqJobStatus.FAILURE.getValue() || state == MqJobStatus.SUCCESS.getValue()) {
                DAOService.getInstance().insertLog(LogContextType.JOB_DELETE.getValue(), Math.toIntExact(jobId), Math.toIntExact(userId), json.encode(), log -> {
                    DAOService.getInstance().deleteJob(jobId, res -> {
                        routingContext.response().setStatusCode(204).end();
                    });
                });

            } else {
                routingContext.response().setStatusCode(400).end();
            }
        });

    }



    public static void getJob(RoutingContext routingContext) {

        logger.debug("getJob got called");
        String jobId = routingContext.request().getParam("jobId");
        DAOService.getInstance().queryJob(Long.valueOf(jobId), routingContext.response().putHeader("content-type", "application/json")::end);

    }

    public static void getFeeds(RoutingContext routingContext) {

        String jobId = routingContext.request().getParam("jobId");
        Optional<String> sinceStr = Optional.ofNullable(routingContext.request().getParam("since"));
        DAOService.getInstance().queryFeeds(Long.valueOf(jobId), sinceStr.map(Long::valueOf), routingContext.response().putHeader("content-type", "application/json")::end);

    }


    public static void getJobs(RoutingContext routingContext) {

        String userId = routingContext.request().getParam("userId");
        Optional<String> sinceStr = Optional.ofNullable(routingContext.request().getParam("since"));
        logger.debug("getting jobs. nelsId:" + userId + "since:" + sinceStr);
        DAOService.getInstance().queryJobs(Long.valueOf(userId), sinceStr.map(Long::valueOf), routingContext.response().putHeader("content-type", "application/json")::end);

    }
}