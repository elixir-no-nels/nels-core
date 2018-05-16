package no.nels.eup.core.enumerations.projectmemberships;

import no.nels.eup.core.abstracts.AProjectMembership;

public class PowerUserProjectMembership extends AProjectMembership{
    @Override
    public long getId() {
        return 2;
    }
    public String toString() { return "Power User";}
}
