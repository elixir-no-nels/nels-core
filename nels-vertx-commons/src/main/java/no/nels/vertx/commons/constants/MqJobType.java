package no.nels.vertx.commons.constants;

/**
 * Created by xiaxi on 26/04/16.
 */
public enum MqJobType {
    STORAGE_COPY(100),
    STORAGE_MOVE(101),
    SBI_PUSH(102),
    SBI_PULL(103),
    TSD_PUSH(104),
    TSD_PULL(105),
    NIRD_SBI_PUSH(106),
    NIRD_SBI_PULL(107);

    private int typeId;
    MqJobType(int typeId) {
        this.typeId = typeId;
    }

    public int getValue() {
        return this.typeId;
    }

    public static MqJobType valueOf(int typeId) {
        switch (typeId) {
            case 100:
                return STORAGE_COPY;
            case 101:
                return STORAGE_MOVE;
            case 102:
                return SBI_PUSH;
            case 103:
                return SBI_PULL;
            case 104:
                return TSD_PUSH;
            case 105:
                return TSD_PULL;
            case 106:
                return NIRD_SBI_PUSH;
            case 107:
                return NIRD_SBI_PULL;
            default:
                throw new IllegalArgumentException();
        }
    }
}
