package x.mvmn.tldeflicker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class ExifUtil {

	public static Metadata readImageExif(File file) throws ImageProcessingException, IOException {
		return ImageMetadataReader.readMetadata(file);
	}

	public static String getValue(File file, String directoryName, String tagName) throws ImageProcessingException, IOException {
		Metadata meta = readImageExif(file);
		Optional<Directory> directory = StreamSupport.stream(meta.getDirectories().spliterator(), false).filter(it -> it.getName().equals(directoryName))
				.findAny();
		if (directory.isPresent()) {
			Optional<Tag> tag = directory.get().getTags().stream().filter(it -> it.getTagName().equals(tagName)).findAny();
			if (tag.isPresent()) {
				return directory.get().getString(tag.get().getTagType());
			}
		}
		return null;
	}

	public static List<Double> getValuesAsNumeric(Collection<File> files, String directoryName, String tagName, Double defaultVal)
			throws ImageProcessingException, IOException {
		List<String> strValues = getValues(files, directoryName, tagName, String.valueOf(defaultVal));
		return strValues.stream().map(new Function<String, Double>() {
			@Override
			public Double apply(String strVal) {
				Double result = defaultVal;
				if (strVal != null) {
					int idx = strVal.indexOf("/");
					try {
						if (idx > 0) {
							result = Double.parseDouble(strVal.substring(0, idx)) / Double.parseDouble(strVal.substring(idx + 1));
						} else {
							result = Double.parseDouble(strVal);
						}
					} catch (NumberFormatException e) {
						result = defaultVal;
					}
				}
				return result;
			}
		}).collect(Collectors.toList());
	}

	public static List<String> getValues(Collection<File> files, String directoryName, String tagName, String defaultValue)
			throws ImageProcessingException, IOException {
		List<String> result = new ArrayList<>(files.size());

		for (File file : files) {
			String value = getValue(file, directoryName, tagName);
			result.add(value == null ? defaultValue : value);
		}

		return result;
	}
}
