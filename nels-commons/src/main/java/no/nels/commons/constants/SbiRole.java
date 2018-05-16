package no.nels.commons.constants;

/**
 * Created by xiaxi on 16/06/2017.
 */
public enum SbiRole {
    USER("User", 10),
    ADMIN("Admin", 1);

    private int value;
    private String name;

    SbiRole(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static boolean checkRole(int value) {
        if (value != 1 && value != 10) {
            return false;
        } else {
            return true;
        }
    }

    public static SbiRole nameOf(int value) {
        if (value == 1) {
            return SbiRole.ADMIN;
        } else if (value == 10) {
            return SbiRole.USER;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
