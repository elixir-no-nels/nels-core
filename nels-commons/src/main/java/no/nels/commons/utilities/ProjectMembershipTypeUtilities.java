package no.nels.commons.utilities;

import no.nels.commons.abstracts.AProjectMembership;
import no.nels.commons.model.projectmemberships.NormalUserProjectMembership;
import no.nels.commons.model.projectmemberships.PIProjectMembership;
import no.nels.commons.model.projectmemberships.PowerUserProjectMembership;

public class ProjectMembershipTypeUtilities {
    public static AProjectMembership getProjectMembership(long id) {
        AProjectMembership ret = new NormalUserProjectMembership();
        if (ret.getId() == id) {
            return ret;
        }
        ret = new PowerUserProjectMembership();
        if (ret.getId() == id) {
            return ret;
        }
        ret = new PIProjectMembership();
        if (ret.getId() == id) {
            return ret;
        }
        return null;
    }
}
