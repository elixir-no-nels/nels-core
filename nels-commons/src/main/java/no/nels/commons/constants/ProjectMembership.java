package no.nels.commons.constants;

/**
 * Created by xiaxi on 07/03/2017.
 */
public enum ProjectMembership {
    NORMAL_USER("member", 3),
    PI("admin", 1),
    POWER_USER("poweruser", 2);

    private int value;
    private String name;

    ProjectMembership(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static boolean checkMembership(int value) {
        if (value != 1 && value != 2 && value != 3) {
            return false;
        } else {
            return true;
        }
    }

    public static ProjectMembership nameOf(int value) {
        if (value == 1) {
            return ProjectMembership.PI;
        } else if (value == 2) {
            return ProjectMembership.POWER_USER;
        } else if (value == 3) {
            return ProjectMembership.NORMAL_USER;
        } else {
            throw new IllegalArgumentException();
        }
    }
}
