package no.nels.commons.constants;

/**
 * Created by weizhang on 6/3/16.
 */
public enum LogContextType {
    USER_LOGIN(100),
    USER_ADD(110),
    USER_DELETE(111),
    TERMS_OF_USAGE_ACCEPTED(112),
    PROJECT_CREATE(200),
    PROJECT_DELETE(201),
    PROJECT_NEWMEMBER_ADD(202),
    PROJECT_MEMBERSHIPCHANGE(203),
    PROJECT_NEWMEMBER_REMOVE(204),
    JOB_DELETE(300),
    SBI_QUOTA_CREATE(501),
    SBI_QUOTA_RECOMPUTE(502),
    SBI_QUOTA_UPDATE(503),
    SBI_QUOTA_DELETE(504),
    SBI_QUOTA_PROJECT_ADDED(505),
    SBI_QUOTA_PROJECT_REMOVED(506),
    SBI_BLOCK_QUOTA_UPDATE(507);

    private int contextType;

    LogContextType(int contextType) {
        this.contextType = contextType;
    }

    public int getValue() {
        return this.contextType;
    }

    public static LogContextType valueOf(int contextType) {
        switch (contextType) {
            case 100:
                return USER_LOGIN;
            case 110:
                return USER_ADD;
            case 111:
                return USER_DELETE;
            case 112:
                return TERMS_OF_USAGE_ACCEPTED;
            case 200:
                return PROJECT_CREATE;
            case 201:
                return PROJECT_DELETE;
            case 202:
                return PROJECT_NEWMEMBER_ADD;
            case 203:
                return PROJECT_MEMBERSHIPCHANGE;
            case 204:
                return PROJECT_NEWMEMBER_REMOVE;
            case 300:
                return JOB_DELETE;
            case 501:
                return SBI_QUOTA_CREATE;
            case 502:
                return SBI_QUOTA_RECOMPUTE;
            case 503:
                return SBI_QUOTA_UPDATE;
            case 504:
                return SBI_QUOTA_DELETE;
            case 505:
                return SBI_QUOTA_PROJECT_ADDED;
            case 506:
                return SBI_QUOTA_PROJECT_REMOVED;
            case 507:
                return SBI_BLOCK_QUOTA_UPDATE;
            default:
                throw new IllegalArgumentException();
        }
    }
}
