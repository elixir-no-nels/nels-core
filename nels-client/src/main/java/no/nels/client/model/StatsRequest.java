package no.nels.client.model;

/**
 * Created by weizhang on 3/29/16.
 */
public class StatsRequest {
    private long targetId;
    private long contextId;
    private double value;

    public long getContextId() {
        return contextId;
    }

    public void setContextId(long contextId) {
        this.contextId = contextId;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public StatsRequest(long targetId, long contextId, double value) {
        this.targetId = targetId;
        this.contextId = contextId;
        this.value = value;
    }
}
