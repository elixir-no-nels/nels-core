package no.nels.commons.constants;

public enum  NelsUserType {
    USER("Normal User", 3),
    HELPDESK("Help Desk", 2),
    ADMINISTRATOR("Administrator", 1);

    private int value;
    private String name;

    NelsUserType(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static boolean checkUserType(int value) {
        if (value != 1 && value != 2 && value != 3) {
            return false;
        } else {
            return true;
        }
    }

    public static NelsUserType nameOf(int value) {
        if (value == 1) {
            return NelsUserType.ADMINISTRATOR;
        } else if (value == 2) {
            return NelsUserType.HELPDESK;
        } else if (value == 3) {
            return NelsUserType.USER;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
