package no.nels.master.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import no.nels.master.api.constants.ConfigName;
import no.nels.master.api.db.DAOService;
import no.nels.master.api.facades.*;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

public final class MasterService extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(MasterService.class);

    public void routeGeneric(Router router){
        router.route("/").produces(MediaType.APPLICATION_JSON).handler(this::getVersion);
    }

    public void routeSetting( Router router ){
        router.route(HttpMethod.GET, "/settings/ids").produces(MediaType.APPLICATION_JSON).handler(SettingFacade::getIds);
        router.route(HttpMethod.GET, "/settings/count").produces(MediaType.APPLICATION_JSON).handler(SettingFacade::getCount);
        router.route(HttpMethod.POST, "/settings/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(SettingFacade::getSettings);
        router.route(HttpMethod.GET, "/settings/:id").produces(MediaType.APPLICATION_JSON).handler(SettingFacade::getSettingById);
        router.route(HttpMethod.PATCH, "/settings/:id").consumes(MediaType.APPLICATION_JSON).handler(SettingFacade::updateSettingById);
        router.route(HttpMethod.DELETE, "/settings/:id").handler(SettingFacade::deleteSettingById);
        router.route(HttpMethod.POST, "/settings").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(SettingFacade::addSetting);
        router.route(HttpMethod.POST, "/settings/do").produces(MediaType.APPLICATION_JSON).handler(SettingFacade::manipulateSetting);
        router.route(HttpMethod.GET, "/settings").produces(MediaType.APPLICATION_JSON).handler(SettingFacade::getAllSettings);
    }

    @Override
    public void start() throws Exception {
        try {
            Config.init();
            no.nels.client.Config.setExtraApiUrl(Config.valueOf(ConfigName.STORAGE_API));
            no.nels.client.Config.setExtraApiUsername(Config.valueOf(ConfigName.STORAGE_USERNAME));
            no.nels.client.Config.setExtraApiPassword(Config.valueOf(ConfigName.STORAGE_PASSWORD));
            DAOService.init(vertx, Config.valueOf(ConfigName.DB_URL), Config.valueOf(ConfigName.DB_USER), Config.valueOf(ConfigName.DB_DRIVER_CLASS),
                    Config.valueOf(ConfigName.DB_PASSWORD), 30);
            VertxOptions options = new VertxOptions();
            logger.debug("pool size: " + options.getEventLoopPoolSize());
            vertx.deployVerticle(MqConsumerVerticle.class.getName());

            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());
            router.route().handler(BasicAuthHandler.create(new BasicHttpAuthProvider()));


            routeGeneric(router);

            //settings
            routeSetting(router);

            //users
            router.route(HttpMethod.GET, "/users/:nelsId/projects/ids").produces(MediaType.APPLICATION_JSON).handler(UserFacade::getAllProjectIdsOfUser);
            router.route(HttpMethod.GET, "/users/:nelsId/projects").produces(MediaType.APPLICATION_JSON).handler(UserFacade::getProjectsOfUser);
            router.route(HttpMethod.GET, "/users/ids").produces(MediaType.APPLICATION_JSON).handler(UserFacade::getUserIds);
            router.route(HttpMethod.GET, "/users/count").produces(MediaType.APPLICATION_JSON).handler(UserFacade::countUsers);
            router.route(HttpMethod.GET, "/users/:nelsId").produces(MediaType.APPLICATION_JSON).handler(UserFacade::getUser);
            router.route(HttpMethod.POST, "/users/:nelsId/do").consumes(MediaType.APPLICATION_JSON).handler(UserFacade::actionHandler);
            router.route(HttpMethod.PATCH, "/users/:nelsId").handler(UserFacade::updateHandler);
            router.route(HttpMethod.POST, "/users/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(UserFacade::searchHandler);
            router.route(HttpMethod.POST, "/users").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).blockingHandler(UserFacade::registerUser, false);
            router.route(HttpMethod.GET, "/users").produces(MediaType.APPLICATION_JSON).handler(UserFacade::getUsers);
            router.route(HttpMethod.DELETE, "/users/:nelsId").blockingHandler(UserFacade::deleteUser, false);

            //projects
            router.route(HttpMethod.GET, "/projects/:projectId/users/ids").produces(MediaType.APPLICATION_JSON).handler(ProjectFacade::getAllUserIdsInProject);
            router.route(HttpMethod.GET, "/projects/:projectId/users/:nelsId").produces(MediaType.APPLICATION_JSON).handler(ProjectFacade::getUserInProject);
            router.route(HttpMethod.POST, "/projects/:projectId/users/:nelsId").consumes(MediaType.APPLICATION_JSON).blockingHandler(ProjectFacade::addUserToProject);
            router.route(HttpMethod.DELETE, "/projects/:projectId/users/:nelsId").blockingHandler(ProjectFacade::deleteUserInProject, false);
            router.route(HttpMethod.PATCH, "/projects/:projectId/users/:nelsId").consumes(MediaType.APPLICATION_JSON).blockingHandler(ProjectFacade::updateUserInProject, false);
            router.route(HttpMethod.GET, "/projects/ids").produces(MediaType.APPLICATION_JSON).handler(ProjectFacade::getProjectIds);
            router.route(HttpMethod.GET, "/projects/count").produces(MediaType.APPLICATION_JSON).handler(ProjectFacade::countProjects);
            router.route(HttpMethod.GET, "/projects/:projectId").produces(MediaType.APPLICATION_JSON).handler(ProjectFacade::getProject);
            router.route(HttpMethod.PATCH, "/projects/:projectId").consumes(MediaType.APPLICATION_JSON).blockingHandler(ProjectFacade::updateProject, false);
            router.route(HttpMethod.POST, "/projects/query").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(ProjectFacade::searchHandler);
            router.route(HttpMethod.DELETE, "/projects/:projectId").blockingHandler(ProjectFacade::deleteProject, false);
            router.route(HttpMethod.GET, "/projects").produces(MediaType.APPLICATION_JSON).handler(ProjectFacade::getProjects);
            router.route(HttpMethod.POST, "/projects").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).blockingHandler(ProjectFacade::createProject, false);



            router.route(HttpMethod.GET, "/stats/contexts/:contextId").produces(MediaType.APPLICATION_JSON).handler(StatFacade::getStats);
            router.route(HttpMethod.GET, "/stats/contexts/:contextId/targets/:targetId").produces(MediaType.APPLICATION_JSON).handler(StatFacade::getStatsTarget);
            router.route(HttpMethod.POST, "/stats/add").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(StatFacade::addStat);

            router.route(HttpMethod.GET, "/logs/contexts/:contextId").produces(MediaType.APPLICATION_JSON).handler(LogFacade::getLogs);
            router.route(HttpMethod.POST, "/logs/add").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(LogFacade::addLog);

            router.route(HttpMethod.POST, "/jobs/add").consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).handler(JobFacade::addJob);
            router.route(HttpMethod.GET, "/jobs/:jobId").produces(MediaType.APPLICATION_JSON).handler(JobFacade::getJob);
            router.route(HttpMethod.GET, "/jobs/user/:userId").produces(MediaType.APPLICATION_JSON).handler(JobFacade::getJobs);
            router.route(HttpMethod.GET, "/jobs/:jobId/feeds").produces(MediaType.APPLICATION_JSON).handler(JobFacade::getFeeds);
            router.route(HttpMethod.DELETE, "/jobs/:jobId").handler(JobFacade::deleteJob);

            vertx.createHttpServer().requestHandler(router::accept).listen(
                    Integer.parseInt(Config.valueOf(ConfigName.PORT)), Config.valueOf(ConfigName.HOST));

        } catch (IOException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    private void getVersion(RoutingContext routingContext) {

        HttpServerResponse response = routingContext.response();
        JsonObject object = new JsonObject();
        object.put("version", Config.valueOf(ConfigName.VERSION));
        response.end(object.encodePrettily());

    }



}
