package no.nels.portal.facades;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import no.nels.portal.Config;

public class MailFacade {

	public static void sendMail(String from, String[] recipients, String[] ccs,
			String[] bccs, String subject, String messageBody, boolean isHtml)
			throws Exception {
		Properties properties = System.getProperties();
		// Setup mail server
		properties.setProperty("mail.smtp.host", Config.getMailHost());
		if (!Config.getMailUser().equalsIgnoreCase("")) {
			properties.setProperty("mail.user", Config.getMailUser());
		}
		if (!Config.getMailPassword().equalsIgnoreCase("")) {
			properties.setProperty("mail.password", Config.getMailPassword());
		}
		// Get the default Session object.
		Session session = Session.getDefaultInstance(properties);
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from));
		// recipients
		if (recipients != null) {
			for (String to : recipients) {
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
						to));
			}
		}
		if (ccs != null) {
			// cc
			for (String cc : ccs) {
				msg.addRecipient(Message.RecipientType.CC, new InternetAddress(
						cc));
			}
		}
		if (bccs != null) {
			// bcc
			for (String bcc : bccs) {
				msg.addRecipient(Message.RecipientType.BCC,
						new InternetAddress(bcc));
			}
		}
		msg.setSubject(subject);
		if (isHtml) {
			msg.setContent(messageBody, "text/html");
		} else {
			msg.setText(messageBody);
		}
		Transport.send(msg);
	}
}
