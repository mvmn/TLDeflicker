package x.mvmn.tldeflicker;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

public class FileUtil {

	public static File[] listFilesByExtensions(File dir, Set<String> fileExtensions) {
		final File[] files = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				boolean result = false;
				String lcName = pathname.getName().toLowerCase();
				if (lcName.indexOf(".") >= 0) {
					int dotIdx = lcName.lastIndexOf(".");
					String lcCxtension = lcName.substring(dotIdx + 1);
					result = fileExtensions.contains(lcCxtension);
				}
				return result;
			}
		});
		return files;
	}

}
