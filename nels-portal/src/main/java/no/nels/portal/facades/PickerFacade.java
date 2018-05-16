package no.nels.portal.facades;

import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.Config;
import no.nels.portal.abstracts.ASecureBean;
import no.nels.portal.model.PickerModel;
import no.nels.portal.model.enumerations.PageModes;
import no.nels.portal.model.enumerations.SessionItemKeys;
import no.nels.portal.model.enumerations.URLParameterNames;

public class PickerFacade {

	private static String registerPickerForPopup(String pickerPageUrl,
			ASecureBean callerPageBean, boolean onlyOne,
			String purposeIdentifier, String closeJs) {
		// generate the pop-up url with all parameters
		String pickerURLKey = StringUtilities.getRandomString(Config
				.getUrlRandomKeyLength());
		String mode = (onlyOne) ? PageModes.PickOne : PageModes.PickMultiple;
		String popupUrl = StringUtilities.appendUrlParameter(pickerPageUrl,
				URLParameterNames.Mode, mode);
		popupUrl = StringUtilities.appendUrlParameter(popupUrl,
				URLParameterNames.PickerCallKey, pickerURLKey);

		// register the PICKER details in a session object
		PickerModel pickerModel = new PickerModel(purposeIdentifier,
				callerPageBean.getNelsParametrizedReferrerUrl());
		SessionFacade.setSessionObject(SessionItemKeys.PICKER, pickerModel);

		return popupUrl;
	}

	public static void launchPicker(String pickerPageUrl,
			ASecureBean callerPageBean, boolean onlyOne,
			String purposeIdentifier, String closeJs) {
		NavigationFacade.popPage(
				registerPickerForPopup(pickerPageUrl, callerPageBean, onlyOne,
						purposeIdentifier, closeJs), true, closeJs);
	}

	public static void launchPicker(String pickerPageUrl,
			ASecureBean callerPageBean, boolean onlyOne,
			String purposeIdentifier, String closeJs, int width, int height) {
		NavigationFacade.popPage(
				registerPickerForPopup(pickerPageUrl, callerPageBean, onlyOne,
						purposeIdentifier, closeJs), true, width, height,
				closeJs);
	}

	public static boolean hasPicker() {
		return hasPicker("");
	}

	public static boolean hasPicker(String purposeIdentifier) {
		// check for any PICKER
		if (SessionFacade.isSessionObjectSet(SessionItemKeys.PICKER)) {
			if (!purposeIdentifier.equalsIgnoreCase("")) {
				// check for a purposeIdentifier specific PICKER
				if (((PickerModel) SessionFacade
						.getSessionObject(SessionItemKeys.PICKER))
						.getPurposeIdentifier().equals(purposeIdentifier)) {
					return true;
				}
			}
			return true;
		}
		return false;
	}

	public static void clearPicker() {
		if (SessionFacade.isSessionObjectSet(SessionItemKeys.PICKER)) {
			SessionFacade.removeSessionObject(SessionItemKeys.PICKER);
		}
	}

	public static void cancelPicker() {
		clearPicker();
		NavigationFacade.closePopup();
	}

	public static void returnFromPicker(ASecureBean pickerPageBean,
			Object[] values) {
		if (hasPicker()) {
			// set result objects
			PickerModel model = (PickerModel) SessionFacade
					.getSessionObject(SessionItemKeys.PICKER);
			model.setValues(values);
			model.setReturned(true);
			// navigate back to source of PICKER initiation
			String url = model.getCallerUrl();
			NavigationFacade.showPage(url, false, true);
		}
	}

	public static boolean isReturnedFromPicker(String purposeIdentifier) {
		if (hasPicker()) {
			if (purposeIdentifier.equalsIgnoreCase("")
					|| (!purposeIdentifier.equalsIgnoreCase("") && hasPicker(purposeIdentifier))) {
				return ((PickerModel) SessionFacade
						.getSessionObject(SessionItemKeys.PICKER)).isReturned();
			}
		}
		return false;
	}

	public static boolean isReturnedFromPicker() {
		return isReturnedFromPicker("");
	}

	public static Object[] getPickerReturnedValues() {
		return getPickerReturnedValues("");
	}

	public static Object[] getPickerReturnedValues(String purposeIdentifier) {
		Object[] ret = null;
		if (hasPicker()) {
			if (purposeIdentifier.equalsIgnoreCase("")
					|| (!purposeIdentifier.equalsIgnoreCase("") && hasPicker(purposeIdentifier))) {
				ret = ((PickerModel) SessionFacade
						.getSessionObject(SessionItemKeys.PICKER)).getValues();
			}
		}
		// clear the PICKER information since result is pickd
		clearPicker();
		return ret;
	}
}
