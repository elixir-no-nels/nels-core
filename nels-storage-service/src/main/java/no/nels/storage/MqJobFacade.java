package no.nels.storage;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.storage.constants.ConfigName;
import no.nels.storage.constants.JobName;
import no.nels.storage.constants.JsonConstant;
import no.nels.vertx.commons.constants.MqJobStatus;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class MqJobFacade {
    private static Logger logger = LoggerFactory.getLogger(MqJobFacade.class);

    private static String SEPARATOR = FileSystems.getDefault().getSeparator();

    public static void copy(JsonObject jsonObject, Consumer<byte[]> publishFeed, Consumer<byte[]> publishStatus) {
        doAction(jsonObject, JobName.COPY, publishFeed, publishStatus);
    }

    public static void move(JsonObject jsonObject, Consumer<byte[]> publishFeed, Consumer<byte[]> publishStatus) {
        doAction(jsonObject, JobName.MOVE, publishFeed, publishStatus);
    }

    private static void doAction(JsonObject jsonObject, String type, Consumer<byte[]> publishFeed, Consumer<byte[]> publishStatus) {
        publishStatus.accept(new JsonObject().put(JsonConstant.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonConstant.COMPLETION, 0.0).encode().getBytes());

        int nelsId = jsonObject.getInteger(JsonConstant.NELS_ID);

        JsonArray sourceArray = jsonObject.getJsonArray(JsonConstant.SOURCE);
        String destination = jsonObject.getString(JsonConstant.DESTINATION).endsWith(SEPARATOR) ?
                jsonObject.getString(JsonConstant.DESTINATION) :
                jsonObject.getString(JsonConstant.DESTINATION) + SEPARATOR;

        logger.debug("NeLS Id: " + nelsId);
        logger.debug("Destination: " + destination);
        logger.debug("Source: " + sourceArray.toString());

        String rootPath = Utils.getNelsUserRootPath(nelsId);
        String userName = rootPath.substring(rootPath.lastIndexOf(SEPARATOR) + 1);

        List<String> failedList = new ArrayList<>(sourceArray.size());
        ProcessBuilder processBuilder = new ProcessBuilder();

        JsonObject temp = new JsonObject();
        Object item;
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < sourceArray.size(); i++) {
            item = sourceArray.getValue(i);

            stringBuilder.setLength(0);
            stringBuilder.append(type).append(" ").append(item.toString()).append(" to ").append(destination);
            publishFeed.accept(stringBuilder.toString().getBytes());

            logger.debug(stringBuilder.toString());

            List<String> list = Arrays.asList(
                    Config.valueOf(ConfigName.SHUFFLE_CMD_PATH),
                    Config.valueOf(ConfigName.SHUFFLE_CMD),
                    "-o", type, "-u", userName, "-s",
                    StringUtils.join(rootPath, SEPARATOR, item.toString()), "-d",
                    StringUtils.join(rootPath, SEPARATOR, destination));
            logger.debug("Command: " + list.toString());
            processBuilder.command(list);

            try {
                Process process = processBuilder.start();
                String errorOutput = Utils.output(process.getErrorStream());
                if (!StringUtils.isEmpty(errorOutput)) {
                    logger.error(errorOutput);
                }
                String debugOutput = Utils.output(process.getInputStream());
                logger.debug(debugOutput);
                if (process.waitFor() != 0) {
                    failedList.add(item.toString());
                    publishFeed.accept("failed".getBytes());
                } else {
                    publishFeed.accept("successful".getBytes());
                }
            } catch (IOException | InterruptedException e) {
                logger.error(e);
                logger.error(e.getClass().getName() + " happens");
                logger.error(stringBuilder.toString() + " failed");
                failedList.add(item.toString());
                publishFeed.accept("failed".getBytes());
            }
            temp.clear();
            temp.put(JsonConstant.JOB_STATUS, MqJobStatus.PROCESSING.getValue()).put(JsonConstant.COMPLETION, (i + 1.0 - failedList.size()) / sourceArray.size());
            publishStatus.accept(temp.encode().getBytes());
        }


        temp.clear();
        if (failedList.size() == 0) {
            temp.put(JsonConstant.JOB_STATUS, MqJobStatus.SUCCESS.getValue()).put(JsonConstant.COMPLETION, 1.0);
        } else {
            temp.put(JsonConstant.JOB_STATUS, MqJobStatus.FAILURE.getValue()).put(JsonConstant.COMPLETION, (double) (sourceArray.size() - failedList.size()) / sourceArray.size());
        }
        publishStatus.accept(temp.encode().getBytes());

    }
}
