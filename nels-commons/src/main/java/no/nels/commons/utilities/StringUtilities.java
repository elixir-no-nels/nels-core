package no.nels.commons.utilities;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class StringUtilities {

	public static String CombineString(String original, String toadd, String inBetween, String atStart, String atEnd)
	{
		return atStart + AppendWithDelimiter(original, toadd, inBetween) + atEnd;
	}

	public static String AppendAndString(String original, String toadd)
	{
		return CombineString(original, toadd, " AND ", "(", ")");
	}

	public static String AppendOrString(String original, String toadd)
	{
		return CombineString(original, toadd, " OR ", "(", ")");
	}

	public static String AppendWithDelimiter(String original, String toAdd,
			String delimiter) {
		return original.equalsIgnoreCase("") ? toAdd : original + delimiter
				+ toAdd;
	}

	public static String appendUrlParameter(String url, String key, String value) {
		if (!url.contains(key+"=")) {
			return url.contains("?") ? url + "&" + key + "=" + value : url
					+ "?" + key + "=" + value;
		} else {
			String urlBeforeKey = StringUtils
					.substringBefore(url, key);
			String urlAfterkeyValue = StringUtils
					.substringAfter(url, key);
			urlAfterkeyValue = urlAfterkeyValue.contains("&") ? "&"
					+ StringUtils.substringAfter(
							urlAfterkeyValue, "&") : "";
			url = urlBeforeKey + key + "=" + value + urlAfterkeyValue;
		}
		return url;
	}

	public static String getRandomString(int length) {
		return RandomStringUtils.random(length,
				"0123456789abcdefghijklmnopqrstuvwxyz");
	}



	public static boolean isValidEmailAddress(String email) {
		boolean result = true;
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException e) {
            e.printStackTrace();
        }
        return result;
	}

	public static boolean isValidFileFolderName(String name) {
		java.util.regex.Pattern p = java.util.regex.Pattern
				.compile("/*?([a-zA-Z_\\-\\.0-9]+)/*?");
		java.util.regex.Matcher m = p.matcher(name);
		boolean matchFound = m.matches();
		return matchFound;
	}

	public static String base64Encode(String plainString) {
		return new String(new Base64().encode(plainString.getBytes()));
	}

	public static String base64Decode(String encodedString) {
		return new String(new Base64().decode(encodedString.getBytes()));
	}

	public static String EncryptSimple(String plain,String salt) {
		String cipher = plain + salt;
		Base64 encoder = new Base64();
		return new String(encoder.encode(StringUtils
                .reverse(cipher).getBytes()));
	}

	public static String DecryptSimple(String cypher,String salt) {
		Base64 decoder = new Base64();
		return StringUtils.reverse(new String(decoder
				.decode(cypher.getBytes()))).replace(salt,"");
	}

	public static boolean containsDigit(String s) {
		return s.matches(".*\\d+.*");
	}

	public static boolean containsLowercaseLetter(String s) {
		return s.matches(".*[a-z]+.*");
	}

    public static boolean containsUppercaseLetter(String s) {
        return s.matches(".*[A-Z]+.*");
    }

	public static String bytesToHex(byte[] in) {
		final StringBuilder builder = new StringBuilder();
		for(byte b : in) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}

	public static byte[] hexToBytes(final String encoded) {
		if ((encoded.length() % 2) != 0)
			throw new IllegalArgumentException("Input string must contain an even number of characters");

		final byte result[] = new byte[encoded.length()/2];
		final char enc[] = encoded.toCharArray();
		for (int i = 0; i < enc.length; i += 2) {
			StringBuilder curr = new StringBuilder(2);
			curr.append(enc[i]).append(enc[i + 1]);
			result[i/2] = (byte) Integer.parseInt(curr.toString(), 16);
		}
		return result;
	}

}
