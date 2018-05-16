package no.nels.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by weizhang on 3/4/16.
 */
public class LogRequest {
    @JsonProperty("context_id")
    private int contextId;
    @JsonProperty("operator_id")
    private long operatorId;
    @JsonProperty("target_id")
    private long targetId;
    private String text;

    public LogRequest(int contextId, long operatorId, long targetId, String text) {
        this.contextId = contextId;
        this.operatorId = operatorId;
        this.targetId = targetId;
        this.text = text;
    }

    public int getContextId() {
        return contextId;
    }

    public void setContextId(int contextId) {
        this.contextId = contextId;
    }

    public long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(long operatorId) {
        this.operatorId = operatorId;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
