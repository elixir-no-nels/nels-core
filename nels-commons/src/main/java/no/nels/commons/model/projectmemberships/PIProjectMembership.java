package no.nels.commons.model.projectmemberships;

import no.nels.commons.abstracts.AProjectMembership;

public class PIProjectMembership extends AProjectMembership {
    @Override
    public long getId() {
        return 1;
    }
    public String toString() { return "PI";}
}
