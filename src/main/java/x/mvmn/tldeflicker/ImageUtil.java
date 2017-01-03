package x.mvmn.tldeflicker;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

public class ImageUtil {

	public static enum ColorChannel {
		RED, GREEN, BLUE
	}

	public static int argbToRed(int argb) {
		return (argb >> 16) & 0xFF;
	}

	public static int argbToGreen(int argb) {
		return (argb >> 8) & 0xFF;
	}

	public static int argbToBlue(int argb) {
		return argb & 0xFF;
	}

	public static double calc(BufferedImage image) {
		long result = 0;
		int[] argbs = image.getRGB(0, 0, image.getWidth(), image.getHeight(), new int[image.getWidth() * image.getHeight()], 0, image.getWidth());
		for (int i = 0; i < argbs.length; i++) {
			int argb = argbs[i];
			result += argbToRed(argb);
			result += argbToGreen(argb);
			result += argbToBlue(argb);
		}
		return (result / argbs.length) / 7.65d;
	}

	public static double calc(BufferedImage image, ColorChannel channel) {
		long result = 0;
		int[] argbs = image.getRGB(0, 0, image.getWidth(), image.getHeight(), new int[image.getWidth() * image.getHeight()], 0, image.getWidth());
		for (int i = 0; i < argbs.length; i++) {
			int argb = argbs[i];
			switch (channel) {
				case RED:
					result += argbToRed(argb);
				break;
				case GREEN:
					result += argbToGreen(argb);
				break;
				case BLUE:
					result += argbToBlue(argb);
				break;
			}
		}
		return (result / argbs.length) / 2.56d;
	}

	public static void writeJpeg(BufferedImage image, File outputFile, float quality) throws Exception {
		ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		ImageWriteParam writeParam = jpegWriter.getDefaultWriteParam();
		writeParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
		writeParam.setCompressionQuality(quality);
		if (outputFile.exists()) {
			outputFile.delete();
		}
		try (FileImageOutputStream fileImageOutputStream = new FileImageOutputStream(outputFile)) {
			jpegWriter.setOutput(fileImageOutputStream);
			jpegWriter.write(null, new javax.imageio.IIOImage(image, null, null), writeParam);
		}
	}
}
