package no.nels.commons.utilities;

import no.nels.commons.abstracts.AIDp;
import no.nels.commons.model.idps.NeLSIdp;
import no.nels.commons.model.idps.NonNeLSIdp;

public final class IDpUtilities {
    public static AIDp getIDp(long idpNumber) {
        if (idpNumber == new NonNeLSIdp().getId()) {
            return new NonNeLSIdp();
        }
        else if (idpNumber == new NeLSIdp().getId()) {
            return new NeLSIdp();
        }
        return null;
    }
}
