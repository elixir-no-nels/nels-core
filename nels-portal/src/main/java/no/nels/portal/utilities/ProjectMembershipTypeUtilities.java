package no.nels.portal.utilities;


import no.nels.commons.abstracts.AProjectMembership;
import no.nels.commons.model.projectmemberships.NormalUserProjectMembership;
import no.nels.commons.model.projectmemberships.PIProjectMembership;
import no.nels.commons.model.projectmemberships.PowerUserProjectMembership;

public class ProjectMembershipTypeUtilities {
    public static String getProjectMembershipTypeName(AProjectMembership membership) {
        if (membership instanceof PIProjectMembership) {
            return "PI";
        } else if (membership instanceof PowerUserProjectMembership) {
            return "Power User";
        } else if (membership instanceof NormalUserProjectMembership) {
            return "Normal User";
        } else {
            return "";
        }
    }
}
