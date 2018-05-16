package no.nels.portal.model.sbi;

public enum SbiTransferringMode {
    PUSH("push"),
    PULL("pull");

    private final String value;

    SbiTransferringMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
