package no.nels.eup.core;

import no.nels.eup.core.enumerations.EupSpringBeanNames;
import no.nels.eup.core.utilities.EupSpringUtilities;
import no.nels.commons.utilities.db.DBDAO;

public class Config {
	static DBDAO eupDBHelper = EupSpringUtilities.getSpringBean(EupSpringBeanNames.eupDBHelper);

	public static DBDAO getEUPDBhelper(){
		return eupDBHelper;
	}
}
