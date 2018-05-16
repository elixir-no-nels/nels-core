package no.nels.master.api.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.master.api.db.model.Statistics;
import no.nels.master.api.db.model.StructuredLog;
import no.nels.vertx.commons.db.VertxDBHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by weizhang on 4/1/16.
 */
public final class DAOService {

    private static Logger logger = LoggerFactory.getLogger(DAOService.class);

    public VertxDBHelper getDbHelper() {
        return dbHelper;
    }

    private VertxDBHelper dbHelper;

    public static DAOService instance;

    private DAOService(Vertx vertx, String url, String user, String driver_class, String password, int max_pool_size) throws IOException {

        dbHelper = new VertxDBHelper(vertx, url, user, driver_class, password, max_pool_size);
    }

    public static void init(Vertx vertx, String url, String user, String driver_class, String password, int max_pool_size) throws IOException {
        if (instance == null) {
            instance = new DAOService(vertx, url, user, driver_class, password, max_pool_size);
        }
    }

    public static DAOService getInstance() {
        return instance;
    }


    public void insertLog(int contextId, int targetId, int operatorId, String text, Consumer<StructuredLog> consumer) {
        String sql = "INSERT INTO structured_log (logcontextid, targetid, operatorid, logtext, logtime)" +
                "VALUES (?, ?, ?, ?, to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'))";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        JsonArray params = new JsonArray().add(contextId).add(targetId).add(operatorId).add(text).add(timestamp);
        dbHelper.insert(sql, params, id -> {
            JsonArray param = new JsonArray().add(id.result());
            dbHelper.getOne("select * from structured_log where id=?", param, json -> {
                logger.debug("new log was added. id:" + id.result());
                ObjectMapper mapper = new ObjectMapper();
                StructuredLog structuredLog = null;
                try {
                    logger.debug("json result:" + json.result());
                    structuredLog = mapper.readValue(json.result(), StructuredLog.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                consumer.accept(structuredLog);
            });
        });
    }

    public void insertStat(long contextId, long targetId, Double value, Consumer<Statistics> consumer) {
        String sql = "INSERT INTO statistics (statscontextid, targetid, value, statstime)" +
                "VALUES (?, ?, ?, to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'))";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        JsonArray params = new JsonArray().add(contextId).add(targetId).add(value).add(timestamp);
        dbHelper.insert(sql, params, id -> {
            JsonArray param = new JsonArray().add(id.result());
            dbHelper.getOne("select * from statistics where id=?", param, json -> {
                logger.debug("new stat was added. id:" + id);
                ObjectMapper mapper = new ObjectMapper();
                Statistics stat = null;
                try {
                    stat = mapper.readValue(json.result(), Statistics.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                consumer.accept(stat);
            });
        });
    }

    public void insertJob(long nelsId, long jobTypeId, String paramJson, long state, Consumer<Long> consumer) {
        String sql = "INSERT INTO job (nelsid, jobtypeid, params, stateid, createtime, lastupdate)" +
                "VALUES (?, ?, ?, ?, to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'), to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'))";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        JsonArray params = new JsonArray().add(nelsId).add(jobTypeId).add(paramJson).add(state).add(timestamp).add(timestamp);
        dbHelper.insert(sql, params, id -> {
            JsonArray param = new JsonArray().add(id.result());
            dbHelper.existOne("select * from job where id=?", param, jobId -> {
                consumer.accept(jobId.result() == true ? id.result() : Long.valueOf(-1));
            });
        });
    }


    public void insertJobFeed(long jobId, String feedText, Consumer<String> consumer) {
        String sql = "INSERT INTO jobfeed (jobid, feedtext, feedtime)" +
                "VALUES (?, ?, to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS'))";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());

        JsonArray params = new JsonArray().add(jobId).add(feedText).add(timestamp);
        dbHelper.insert(sql, params, id -> {
            JsonArray param = new JsonArray().add(id.result());
            dbHelper.getOne("select * from jobfeed where id=?", param, result -> {
                consumer.accept(result.result());
            });
        });
    }

    public void querySettings(String key, Consumer<String> consumer) {
        String sql = "select id, setting_key, setting_value from setting where setting_key=?";
        dbHelper.select(sql, new JsonArray().add(key), s -> consumer.accept(s.result()));
    }

    public void queryLogs(long contextId, Optional<Long> since, Consumer<String> consumer) {
        String sql;
        JsonArray params = new JsonArray();
        params.add(contextId);
        if (since.isPresent()) {
            java.util.Date sinceTime = new java.util.Date(since.get() * 1000);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sinceParam = dateFormat.format(sinceTime);
            sql = "select * from structured_log where logcontextId=? and logtime >=to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS') ORDER BY id DESC";
            params.add(sinceParam);
        } else {
            sql = "select * from structured_log where logcontextId=? ORDER BY id DESC";
        }
        dbHelper.select(sql, params, s -> consumer.accept(s.result()));
    }

    public void queryJob(long jobId, Consumer<String> consumer) {
        String sql = "select * from job where id=?";
        JsonArray params = new JsonArray();
        params.add(jobId);
        dbHelper.getOne(sql, params, job -> {
            logger.debug("queryJob is:" + job.result());
            consumer.accept(job.result());
        });
    }

    public void queryFeeds(long jobId, Optional<Long> since, Consumer<String> consumer) {

        String sql;
        JsonArray params = new JsonArray();
        params.add(jobId);
        if (since.isPresent()) {
            java.util.Date sinceTime = new java.util.Date(since.get() * 1000);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sinceParam = dateFormat.format(sinceTime);
            sql = "select * from jobfeed where jobid=? and feedtime >=to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS') ORDER BY id ASC";
            params.add(sinceParam);
        } else {
            sql = "select * from jobfeed where jobid=? ORDER BY id ASC";
        }
        dbHelper.select(sql, params, s -> consumer.accept(s.result()));
    }


    public void userIds(Consumer<String> consumer) {
        String sql;
        sql = "select id from users ORDER BY id DESC";
        dbHelper.select(sql, s -> consumer.accept(s.result()));
    }

    public void settingIds(Consumer<String> consumer) {
        String sql;
        sql = "select id from setting ORDER BY id DESC";
        dbHelper.select(sql, s -> consumer.accept(s.result()));
    }
    public void settings(Consumer<String> consumer){
        String sql;
        sql = "select * from setting ORDER BY id DESC";
        dbHelper.select(sql, s -> consumer.accept(s.result()));
    }
    public void querySetting(long id, Consumer<String> consumer) {
        String sql = "select * from setting where id=?";
        JsonArray params = new JsonArray();
        params.add(id);
        dbHelper.getOne(sql, params, setting -> {
            consumer.accept(setting.result());
        });
    }

    public void queryStats(long contextId, Optional<Long> since, Consumer<String> consumer) {
        String sql;
        JsonArray params = new JsonArray();
        params.add(contextId);
        if (since.isPresent()) {
            java.util.Date sinceTime = new java.util.Date(since.get() * 1000);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sinceParam = dateFormat.format(sinceTime);
            sql = "select * from statistics where statscontextId=? and statstime >=to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS') ORDER BY id DESC ";
            params.add(sinceParam);
        } else {
            sql = "select * from statistics where statscontextId=? ORDER BY id DESC";
        }
        dbHelper.select(sql, params, s -> consumer.accept(s.result()));
    }

    public void queryJobs(long nelsId, Optional<Long> since, Consumer<String> consumer) {
        String sql;
        JsonArray params = new JsonArray();
        params.add(nelsId);
        if (since.isPresent()) {
            java.util.Date sinceTime = new java.util.Date(since.get() * 1000);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sinceParam = dateFormat.format(sinceTime);
            sql = "select * from job where nelsid=? and lastupdate >to_timestamp(?, 'YYYY-MM-DD HH24:MI:SS') ORDER BY lastupdate DESC ";
            params.add(sinceParam);
        } else {
            sql = "select * from job where nelsid=? ORDER BY id DESC";
        }
        dbHelper.select(sql, params, s -> consumer.accept(s.result()));
    }

    public void deleteJob(long jobId, Consumer<String> consumer) {
        String sql = "delete from job where id='" + jobId + "'";
        dbHelper.deleteOne(sql, job -> {
            logger.debug("deleted-job:" + job);
            consumer.accept(job.result());
        });
    }
    public void deleteSetting(long id, Consumer<String> consumer) {
        String sql = "delete from setting where id=" + id;
        dbHelper.deleteOne(sql, setting -> {
            logger.debug("deleted-setting:" + setting);
            consumer.accept(setting.result());
        });
    }
    public void updateSetting(long id, Optional<String> key,Optional<String> value, Consumer<String> consumer) {

        JsonArray params = new JsonArray();
        String sql = "update setting set ";
        if(key.isPresent()){
            sql += "setting_key=?,";
            params.add(key.get());
        }
        if(value.isPresent()) {
            sql += "setting_value=?,";
            params.add(value.get());
        }
        sql += "lastupdate=to_timestamp(?, 'YYYY-MM-DD') where id=?";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String timestamp = dateFormat.format(new Date());
        params.add(timestamp).add(id);
        dbHelper.updateOne(sql, params, r -> {
            consumer.accept(r.result());
        });
    }

    public void updateSetting(String key, String value, Consumer<String> consumer) {
        logger.debug("calling updateSetting");
        JsonArray params = new JsonArray();
        String sql = "update setting set setting_value=?, lastupdate=to_timestamp(?, 'YYYY-MM-DD') where setting_key=?";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String timestamp = dateFormat.format(new Date());
        params.add(value).add(timestamp).add(key);
        dbHelper.updateOne(sql, params, r -> {
            consumer.accept(r.result());
        });
    }

    public void deleteSetting(String key, Consumer<String> consumer) {

        String sql = "delete from setting where setting_key='" + key + "'";
        dbHelper.deleteOne(sql, r -> {
            consumer.accept(r.result());
        });
    }
    public void insertSetting(String key, String value, Consumer<Long> consumer) {
        String sql = "insert into setting (setting_key, setting_value, lastupdate) values (?, ?, to_timestamp(?, 'YYYY-MM-DD'))";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String timestamp = dateFormat.format(new Date());
        JsonArray params = new JsonArray().add(key).add(value).add(timestamp);
        dbHelper.insert(sql, params, id -> consumer.accept(id.result()));
    }
}
