package no.nels.portal.model;

import no.nels.portal.utilities.JSFUtils;

public class PickerModel {
	String callerUrl = JSFUtils.getApplicationRoot();
	Object[] values = new Object[] {};
	String purposeIdentifier = "";
	boolean returned = false;

	public PickerModel(String purposeIdentifier, String callerUrl) {
		this.callerUrl = callerUrl;
		this.purposeIdentifier = purposeIdentifier;
	}

	public String getCallerUrl() {
		return callerUrl;
	}

	public String getPurposeIdentifier() {
		return purposeIdentifier;
	}

	public void setValues(Object[] values) {
		this.values = values;
	}

	public Object[] getValues() {
		return values;
	}

	public boolean isReturned() {
		return returned;
	}

	public void setReturned(boolean returned) {
		this.returned = returned;
	}
	

}
