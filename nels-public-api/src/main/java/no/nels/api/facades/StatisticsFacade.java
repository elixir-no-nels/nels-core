package no.nels.api.facades;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import no.nels.api.constants.SettingKeys;
import no.nels.client.Settings;
import no.nels.commons.constants.StatsContextType;
import no.nels.vertx.commons.db.DBHelper;

public class StatisticsFacade {
    private static io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(StatisticsFacade.class);

    public static long getNeLSDiskPeronsalAll() {
        String sql = "select value from statistics where statscontextid = " + StatsContextType.NELS_PERSONAL_DISK_USAGE_SUMMARY + " order by statstime desc limit 1";
        try {
            JsonObject jsonObject = DBHelper.getOne(sql, "value");
            return jsonObject.getLong("value");

        } catch (Exception ex) {
            logger.error(ex);
        }
        return 0L;
    }

    public static long getNeLSDiskProjectsAll() {
        String sql = "select value from statistics where statscontextid = " + StatsContextType.NELS_PROJECT_DISK_USAGE_SUMMARY + " order by statstime desc limit 1";
        try {
            JsonObject jsonObject = DBHelper.getOne(sql, "value");
            return jsonObject.getLong("value");

        } catch (Exception ex) {
            logger.error(ex);
        }
        return 0L;
    }

    public static long getLastNeLSDiskUsageUpdateTime() {
        String sql = "select max(statstime) as statstime from statistics where statscontextid = " + StatsContextType.NELS_PERSONAL_DISK_USAGE_SUMMARY + " or statscontextid=" + StatsContextType.NELS_PROJECT_DISK_USAGE_SUMMARY;
        try {
            JsonObject jsonObject = DBHelper.getOne(sql, "statstime");
            return jsonObject.getLong("statstime") / 1000;

        } catch (Exception ex) {
            logger.error(ex);
        }

        return 0L;
    }

    public static long getLastSbiDiskUsageUpdateTime() {
        return 0L;
    }

    public static long getNeLSDiskTotal() {
        return Settings.isSettingFound(SettingKeys.NELS_TOTAL_SIZE) ? Long.valueOf(Settings.getSetting(SettingKeys.NELS_TOTAL_SIZE)) : 0L;
    }
}
