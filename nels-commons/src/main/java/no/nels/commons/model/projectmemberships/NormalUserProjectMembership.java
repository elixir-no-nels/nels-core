package no.nels.commons.model.projectmemberships;

import no.nels.commons.abstracts.AProjectMembership;

public class NormalUserProjectMembership extends AProjectMembership {
    @Override
    public long getId() {
        return 3;
    }
    public String toString() { return "Member";}
}
