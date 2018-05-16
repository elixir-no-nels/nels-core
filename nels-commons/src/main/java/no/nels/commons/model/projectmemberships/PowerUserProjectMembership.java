package no.nels.commons.model.projectmemberships;

import no.nels.commons.abstracts.AProjectMembership;

public class PowerUserProjectMembership extends AProjectMembership {
    @Override
    public long getId() {
        return 2;
    }
    public String toString() { return "Power User";}
}
