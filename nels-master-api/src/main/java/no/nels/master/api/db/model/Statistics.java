package no.nels.master.api.db.model;

import java.util.Date;

/**
 * Created by weizhang on 4/4/16.
 */
public class Statistics {
    private long id;
    private long statscontextid;
    private long targetid;
    private Double value;
    private Date statstime;

    public Statistics() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStatscontextid() {
        return statscontextid;
    }

    public void setStatscontextid(long statscontextid) {
        this.statscontextid = statscontextid;
    }

    public long getTargetid() {
        return targetid;
    }

    public void setTargetid(long targetid) {
        this.targetid = targetid;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Date getStatstime() {
        return statstime;
    }

    public void setStatstime(Date statstime) {
        this.statstime = statstime;
    }
}
