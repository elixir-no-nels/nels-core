package no.nels.master.api.db.model;

import java.util.Date;

/**
 * Created by weizhang on 4/1/16.
 */
public class StructuredLog {
    private long id;
    private long logcontextid;
    private long targetid;
    private long operatorid;
    private String logtext;
    private Date logtime;

    public StructuredLog() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLogcontextid() {
        return logcontextid;
    }

    public void setLogcontextid(long logcontextid) {
        this.logcontextid = logcontextid;
    }

    public long getTargetid() {
        return targetid;
    }

    public void setTargetid(long targetid) {
        this.targetid = targetid;
    }

    public long getOperatorid() {
        return operatorid;
    }

    public void setOperatorid(long operatorid) {
        this.operatorid = operatorid;
    }

    public String getLogtext() {
        return logtext;
    }

    public void setLogtext(String logtext) {
        this.logtext = logtext;
    }

    public Date getLogtime() {
        return logtime;
    }

    public void setLogtime(Date logtime) {
        this.logtime = logtime;
    }

    public StructuredLog(long id, long logcontextid, long targetid, long operatorid, String logtext, Date logtime) {
        this.id = id;
        this.logcontextid = logcontextid;
        this.targetid = targetid;
        this.operatorid = operatorid;
        this.logtext = logtext;
        this.logtime = logtime;
    }
}
