package x.mvmn.tldeflicker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	public static Set<String> getSupportedImageFormatExtensions() {
		return Stream.of(javax.imageio.ImageIO.getReaderFileSuffixes()).map(String::toLowerCase).collect(Collectors.toSet());
	}

	public static void writeImage(BufferedImage image, File outputFile, String format, Float compressionQuality) throws Exception {
		ImageWriter imageWriter = ImageIO.getImageWritersByFormatName(format).next();
		ImageWriteParam writeParam = null;
		if (compressionQuality != null) {
			writeParam = imageWriter.getDefaultWriteParam();
			writeParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
			writeParam.setCompressionQuality(compressionQuality);
		}
		if (outputFile.exists()) {
			outputFile.delete();
		}
		try (FileImageOutputStream fileImageOutputStream = new FileImageOutputStream(outputFile)) {
			imageWriter.setOutput(fileImageOutputStream);
			imageWriter.write(null, new javax.imageio.IIOImage(image, null, null), writeParam);
		}
	}

	public static Map<String, Double> calculateAverageBrightnesses(File[] files, Function<Tuple<Integer, File, Double, Void, Void>, Void> callback)
			throws Exception {
		final Map<String, Double> values = Collections.synchronizedMap(new HashMap<>());
		final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		for (int i = 0; i < files.length; i++) {
			final int index = i;
			final File file = files[i];
			executorService.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					BufferedImage bufferedImage = ImageIO.read(file);
					Double brightnessVal = calc(bufferedImage);
					values.put(file.getAbsolutePath(), brightnessVal);
					if (callback != null) {
						try {
							callback.apply(new Tuple<Integer, File, Double, Void, Void>(index, file, brightnessVal, null, null));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					return null;
				}
			});
		}
		executorService.shutdown();
		executorService.awaitTermination(365, TimeUnit.DAYS);
		return new TreeMap<>(values);
	}
}
