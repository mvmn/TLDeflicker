package x.mvmn.tldeflicker;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import x.mvmn.tldeflicker.ImageUtil.ColorChannel;

public class TLDeflicker {

	public static void main(String args[]) throws Exception {
		Set<String> fileExtensions = Stream.of(javax.imageio.ImageIO.getReaderFormatNames()).map(String::toLowerCase).collect(Collectors.toSet());
		if (args.length > 2) {
			File path = new File(args[0]);
			if (path.exists() && path.isDirectory()) {
				File outPath = new File(path, "deflickered");
				if (!outPath.exists()) {
					outPath.mkdir();
				}
				File[] files = path.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						final String fnl = pathname.getName().toLowerCase();
						if (fnl.indexOf(".") >= 0) {
							return fileExtensions.contains(fnl.substring(fnl.lastIndexOf(".") + 1));
						} else {
							return false;
						}
					}
				});
				Arrays.sort(files, FileByNameComparator.INSTANCE);

				System.out.println("Readin exposure values from EXIF directory '" + args[1] + "' tag '" + args[2] + "'");
				Set<Integer> exposureChangePointsSet = new TreeSet<>();
				String prevExposure = null;
				for (int i = 0; i < files.length; i++) {
					String exposure = ExifUtil.getValue(files[i], args[1], args[2]);
					if (prevExposure != null && !exposure.equals(prevExposure)) {
						exposureChangePointsSet.add(i);
					}
					prevExposure = exposure;
				}

				final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

				List<Integer> exposureChangePoints = new ArrayList<>(exposureChangePointsSet);
				for (int i = 0; i <= exposureChangePoints.size(); i++) {
					System.out.println("Preparing tasks for sequence " + (i + 1) + " of " + (exposureChangePoints.size() + 1));

					File standard;
					int start;
					int end;
					if (i < exposureChangePoints.size()) {
						int newExposurePoint = exposureChangePoints.get(i);
						start = i > 0 ? exposureChangePoints.get(i - 1) : 0;
						end = newExposurePoint - 1;
						standard = files[newExposurePoint];
					} else {
						int lastExposureChangePoint = exposureChangePoints.get(exposureChangePoints.size() - 1);
						standard = files[lastExposureChangePoint];
						start = lastExposureChangePoint;
						end = files.length - 1;
					}
					int deviatedSeqSize = end - start;
					if (deviatedSeqSize > 1) {
						try {
							BufferedImage biStandard = ImageIO.read(standard);
							double stRed = ImageUtil.calc(biStandard, ColorChannel.RED);
							double stGreen = ImageUtil.calc(biStandard, ColorChannel.GREEN);
							double stBlue = ImageUtil.calc(biStandard, ColorChannel.BLUE);

							BufferedImage biLastDeviated = ImageIO.read(files[end]);

							double dvRed = ImageUtil.calc(biLastDeviated, ColorChannel.RED);
							double dvGreen = ImageUtil.calc(biLastDeviated, ColorChannel.GREEN);
							double dvBlue = ImageUtil.calc(biLastDeviated, ColorChannel.BLUE);

							double dRed = (stRed - dvRed) / dvRed;
							double dGreen = (stGreen - dvGreen) / dvGreen;
							double dBlue = (stBlue - dvBlue) / dvBlue;

							for (int k = start; k <= end; k++) {
								final int finalK = k;
								int numberInSequence = k - start;

								executorService.submit(new Callable<Void>() {
									public Void call() throws Exception {
										try {
											BufferedImage biToAdjust = ImageIO.read(files[finalK]);
											BufferedImage biOutput;
											if (numberInSequence > 0) {
												float[] adjustments = new float[] { 1f + (float) (dRed * numberInSequence / deviatedSeqSize),
														1f + (float) (dGreen * numberInSequence / deviatedSeqSize),
														1f + (float) (dBlue * numberInSequence / deviatedSeqSize) };
												biOutput = new BufferedImage(biToAdjust.getWidth(), biToAdjust.getHeight(), biToAdjust.getType());
												RescaleOp operation = new RescaleOp(adjustments, new float[3], null);
												operation.filter(biToAdjust, biOutput);
											} else {
												biOutput = biToAdjust;
											}
											ImageUtil.writeJpeg(biOutput, new File(outPath, files[finalK].getName()), 1f);
											System.out.println("Processed file #" + finalK + " " + files[finalK].getName());
										} catch (Exception e) {
											e.printStackTrace();
										}
										return null;
									}
								});
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				executorService.shutdown();
				executorService.awaitTermination(365, TimeUnit.DAYS);
				System.out.println("Finished successfully.");
			}
		}
	}
}
