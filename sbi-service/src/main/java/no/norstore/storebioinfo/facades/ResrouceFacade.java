package no.norstore.storebioinfo.facades;

import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.vertx.commons.db.DBHelper;
import no.norstore.storebioinfo.constants.JsonKey;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ResrouceFacade {
    private static Logger logger = LoggerFactory.getLogger(ResrouceFacade.class);


    public static long getUsedSize(long quotaId) {
        String getResourceElementsSql = "select element from resource_subentries where resource_id in (select id from resource where quota_id = (select quota_id from quota where id =" + quotaId + "))";
        return sumSizeForElement(getResourceElementsSql);
    }

    public static long getUsedSize(String projectExternalRef) {
        String getResourceIdByProjectSql = "select element from resource_subentries where resource_id in (SELECT id FROM resource WHERE dataset_id IN (SELECT id FROM data_set where project_policy IN (SELECT policy_id from project_policy where project_id = '" + projectExternalRef + "')))";
        return sumSizeForElement(getResourceIdByProjectSql);
    }

    private static long sumSizeForElement(String sql) {
        JsonArray elements = new JsonArray();
        try {
            elements = DBHelper.select(sql, JsonKey.ELEMENT).getJsonArray(JsonKey.ELEMENT);
        } catch (SQLException e) {
            logger.error(e);
        }
        List<Long> size = new ArrayList<>();
        String s;
        for ( int i = 0; i < elements.size(); i++) {
            s = elements.getString(i);
            size.add(getSize(s));
        }
        return size.stream().mapToLong(Long::longValue).sum();

    }

    private static long getSize(String s) {
        List<String> items = Arrays.asList(s.split("\\s*,\\s*"));
        return Long.valueOf(items.get(items.size() - 1));
    }


}
