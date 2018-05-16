package no.nels.vertx.commons.constants;

/**
 * Created by xiaxi on 26/04/16.
 */
public enum MqJobStatus {
    SUCCESS(101),
    FAILURE(102),
    SUBMITTED(100),
    PROCESSING(103);

    private int jobStatus;

    MqJobStatus(int jobStatus) {
        this.jobStatus = jobStatus;
    }

    public int getValue() {
        return this.jobStatus;
    }

    public static MqJobStatus valueOf(int jobStatus) {
        switch (jobStatus) {
            case 100:
                return SUBMITTED;
            case 101:
                return SUCCESS;
            case 102:
                return FAILURE;
            case 103:
                return PROCESSING;
            default:
                throw new IllegalArgumentException();
        }
    }
}
