package x.mvmn.tldeflicker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
