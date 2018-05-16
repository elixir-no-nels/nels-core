package no.nels.portal.utilities;

import no.nels.commons.utilities.StringUtilities;
import no.nels.portal.facades.LoggingFacade;
import org.apache.commons.lang.StringUtils;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public final class GenericUtils {

	public static void sendPostRequest(String urlString, String[] keys,
			String[] values, boolean showResponse) throws Exception {

		if (keys.length == 0 || keys.length != values.length) {
			throw (new Exception("Invalid input"));
		}
		// Build parameter string
		String data = "";// "width=50&height=100";
		for (int i = 0; i < keys.length; i++) {
			data = StringUtilities.AppendWithDelimiter(data, keys[i] + "="
                    + values[i], "&");
		}
		try {

			// Send the request
			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(
					conn.getOutputStream());

			// write parameters
			writer.write(data);
			writer.flush();

			// Get the response
			StringBuffer answer = new StringBuffer();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				answer.append(line);
			}
			writer.close();
			reader.close();

			// Output the response
			if (showResponse) {
				LoggingFacade.logDebugInfo(answer);
			} else {
				LoggingFacade.logDebugInfo(answer);
			}

		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static String assembleRelativeURL(String path, Map<String, String> parameterMap) {
		String parameters = StringUtils.join(
				parameterMap.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(toList()),
				"&");
		return StringUtils.join(new String[] {path, "?", parameters});
	}

	public static String getClientIp() {
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		String forwardedFor = request.getHeader("X-Forwarded-For");

		if (!forwardedFor.isEmpty()) {
			return forwardedFor.split("\\s*,\\s*", 2)[0]; // It's a comma separated string: client,proxy1,proxy2,...
		}

		return request.getRemoteAddr();
	}
}
