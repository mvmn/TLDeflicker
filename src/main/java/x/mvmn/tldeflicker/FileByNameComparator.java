package x.mvmn.tldeflicker;

import java.io.File;
import java.util.Comparator;

public final class FileByNameComparator implements Comparator<File> {
	public static final FileByNameComparator INSTANCE = new FileByNameComparator();

	@Override
	public int compare(File o1, File o2) {
		return o1.getName().compareTo(o2.getName());
	}
}