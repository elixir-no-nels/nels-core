package no.nels.eup.core.enumerations.projectmemberships;


import no.nels.eup.core.abstracts.AProjectMembership;

public class PIProjectMembership extends AProjectMembership{
    @Override
    public long getId() {
        return 1;
    }
    public String toString() { return "PI";}
}
