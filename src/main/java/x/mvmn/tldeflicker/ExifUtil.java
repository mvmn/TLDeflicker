package x.mvmn.tldeflicker;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
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
}
