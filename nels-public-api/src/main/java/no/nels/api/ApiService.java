package no.nels.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.api.constants.ConfigName;
import no.nels.client.sbi.SbiConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public final class ApiService extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(ApiService.class);

    @Override
    public void start() throws Exception {
        try {
            Config.init();


            SbiConfig.setSbiRootUrl(Config.valueOf(ConfigName.SBI_URL));
            SbiConfig.setSbiApiUsername(Config.valueOf(ConfigName.SBI_USERNAME));
            SbiConfig.setSbiApiPassword(Config.valueOf(ConfigName.SBI_PASSWORD));

            no.nels.client.Config.setMasterApiUrl(Config.valueOf(ConfigName.MASTER_URL));
            no.nels.client.Config.setMasterApiUsername(Config.valueOf(ConfigName.MASTER_USERNAME));
            no.nels.client.Config.setMasterApiPassword(Config.valueOf(ConfigName.MASTER_PASSWORD));

            if (!StringUtils.isEmpty(Config.valueOf(ConfigName.HTTP_TIMEOUT))) {
                SbiConfig.setTimeout(Config.valueOf(ConfigName.HTTP_TIMEOUT));
                no.nels.client.Config.setTimeout(Config.valueOf(ConfigName.HTTP_TIMEOUT));
            }
            vertx.deployVerticle(Route.class.getName(), new DeploymentOptions().setInstances(Integer.valueOf(Config.valueOf(ConfigName.HTTP_VERTICLE_INSTANCE_NUMBER))));
        } catch (IOException e) {
            logger.error(e.getMessage(), e.getCause());
        }
    }
}
