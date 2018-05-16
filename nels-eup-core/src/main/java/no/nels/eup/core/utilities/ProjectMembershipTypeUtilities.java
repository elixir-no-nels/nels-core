package no.nels.eup.core.utilities;

import no.nels.eup.core.abstracts.AProjectMembership;
import no.nels.eup.core.enumerations.projectmemberships.NormalUserProjectMembership;
import no.nels.eup.core.enumerations.projectmemberships.PIProjectMembership;
import no.nels.eup.core.enumerations.projectmemberships.PowerUserProjectMembership;

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
