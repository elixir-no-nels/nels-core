package no.nels.client.model;

/**
 * Created by weizhang on 3/29/16.
 */
public class StatsInfo {

    private long id;
    private double value;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public StatsInfo(long id, double value) {
        this.id = id;
        this.value = value;
    }
}
