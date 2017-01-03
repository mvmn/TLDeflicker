package x.mvmn.tldeflicker;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import x.mvmn.tldeflicker.ImageUtil.ColorChannel;

public class TLDeflicker {

	public static void main(String args[]) throws Exception {
		File path = new File(args[0]);
		File outPath = new File(path, "deflickered");
		if (!outPath.exists()) {
			outPath.mkdir();
		}
		File[] files = path.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				final String fnl = pathname.getName().toLowerCase();
				return fnl.endsWith(".jpg") || fnl.endsWith(".jpeg");
			}
		});

		List<Integer> exposureChangePoints = new ArrayList<>();
		String prevExposure = null;
		for (int i = 0; i < files.length; i++) {
			String exposure = ExifUtil.getValue(files[i], args[1], args[2]);
			if (prevExposure != null && !exposure.equals(prevExposure)) {
				exposureChangePoints.add(i);
			}
			prevExposure = exposure;
		}

		for (int i = 0; i <= exposureChangePoints.size(); i++) {
			System.out.println("Processing sequence " + (i + 1) + " of " + (exposureChangePoints.size() + 1));

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
			// TODO: What about cases when there's 1 pic for exposure? Shouldn't ever happen in my TLs, but in general?
			if (deviatedSeqSize > 1) {
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

				for (int k = start; k < end; k++) {
					int numberInSequence = k - start;
					BufferedImage biToAdjust = ImageIO.read(files[k]);
					BufferedImage biOutput;
					if (numberInSequence > 0) {
						float[] adjustments = new float[] { 1f + (float) (dRed * numberInSequence / deviatedSeqSize),
								1f + (float) (dGreen * numberInSequence / deviatedSeqSize), 1f + (float) (dBlue * numberInSequence / deviatedSeqSize) };
						biOutput = new BufferedImage(biToAdjust.getWidth(), biToAdjust.getHeight(), biToAdjust.getType());
						RescaleOp operation = new RescaleOp(adjustments, new float[3], null);
						operation.filter(biToAdjust, biOutput);
					} else {
						biOutput = biToAdjust;
					}
					ImageUtil.writeJpeg(biOutput, new File(outPath, files[k].getName()), 1f);
					System.out.println("Processed file " + k);
				}
				ImageUtil.writeJpeg(biLastDeviated, new File(outPath, files[end].getName()), 1f);
				System.out.println("Processed file " + end);
			}
		}
	}
}
