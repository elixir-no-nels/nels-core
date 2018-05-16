package no.nels.portal.session;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import no.nels.portal.model.enumerations.ManagedBeanNames;
import no.nels.portal.utilities.JSFUtils;

@ManagedBean(name = ManagedBeanNames.session_popupBean)
@SessionScoped
public class PopUpBean {

	private boolean isJustClosed = false;
	private boolean hasPopup = false;
	private String url = JSFUtils.getApplicationRoot();
	private int width = 1000;
	private int height = 550;
	private String closeJs ="";

	public boolean hasPopup() {
		return hasPopup;
	}

	public void setHasPopup(boolean hasPopup) {
		this.hasPopup = hasPopup;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public boolean getIsJustClosed(){
		return isJustClosed;
	}
	
	public void setIsJustClosed(boolean isJustClosed){
		this.isJustClosed = isJustClosed;
	}
	public boolean show() {
		// return hasPopup and reset it since it's assumed to be shown by the
		// xhtml page calling this function
		if (hasPopup) {
			setHasPopup(false);
			return true;
		}
		return false;
	}
	
	public boolean close(){
		//return isJustClosed and reset it to false
		if(isJustClosed){
			setIsJustClosed(false);
			setCloseJs("");
			return true;
		}
		return false;
	}

	public String getCloseJs() {
		return closeJs;
	}

	public void setCloseJs(String closeJs) {
		this.closeJs = closeJs;
	}

}
