package no.nels.portal.utilities;

public class FileUtils {

	public static String getDisplayString(long bytes) {
		String[] Q = new String[] { "bytes", "KB", "MB", "GB", "TB", "PB", "EB" };
		for (int i = 6; i > 0; i--) {
			double step = Math.pow(1024, i);
			if (bytes > step)
				return String.format("%3.1f %s", bytes / step, Q[i]);
		}
		return String.format("%d %s", bytes, Q[0]);
	}

}
