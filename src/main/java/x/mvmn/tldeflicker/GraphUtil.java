package x.mvmn.tldeflicker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

public class GraphUtil {

	public static BufferedImage createGraphCanvas(int width, int height, Color backgroundColor) {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graph2d = result.createGraphics();
		graph2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
		graph2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
		graph2d.setPaint(backgroundColor);
		graph2d.fillRect(0, 0, width, height);
		return result;
	}

	public static void paintGraphOver(BufferedImage image, Graphics2D graph2d, Color graphColor, Collection<? extends Number> values, Double minOverride,
			Double maxOverride, Double gridlineOffset) {
		Color graphColorTransparent = new Color(graphColor.getRed(), graphColor.getGreen(), graphColor.getBlue(), 64);
		double[] valuesArray = new double[values.size()];
		double min = 0;
		double max = 0;
		{
			int i = 0;
			for (Number val : values) {
				double doubleVal = val.doubleValue();
				valuesArray[i++] = doubleVal;
				if (doubleVal < min) {
					min = doubleVal;
				}
				if (doubleVal > max) {
					max = doubleVal;
				}
			}
		}
		if (minOverride != null) {
			min = minOverride;
		}
		if (maxOverride != null) {
			max = maxOverride;
		}
		double valueXOffset = ((double) image.getWidth()) / valuesArray.length;
		int topMargin = graph2d.getFontMetrics().getHeight() + 10;
		int effectiveHeight = image.getHeight() - topMargin;
		double valueYMultiplier = effectiveHeight / (max - min);
		for (int i = 0; i < valuesArray.length - 1; i++) {
			double v1 = valuesArray[i];
			double v2 = valuesArray[i + 1];
			int x1 = (int) (i * valueXOffset);
			int y1 = effectiveHeight - (int) ((v1 - min) * valueYMultiplier);
			int x2 = (int) ((i + 1) * valueXOffset);
			int y2 = effectiveHeight - (int) ((v2 - min) * valueYMultiplier);
			graph2d.setColor(graphColor);
			graph2d.drawLine(x1, y1, x2, y2);
			graph2d.setColor(graphColorTransparent);
			graph2d.fillOval(x1 - 1, y1 - 1, 3, 3);
			if (i == valuesArray.length - 2) { // last point
				graph2d.fillOval(x2 - 1, y2 - 1, 3, 3);
			}
		}

		graph2d.setColor(graphColorTransparent);
		if (gridlineOffset != null) {
			double v = min;
			while (!(v > max + v / 2)) {
				int y = effectiveHeight - (int) (v * valueYMultiplier) + topMargin - 1;
				graph2d.drawLine(0, y, image.getWidth(), y);
				int vint = (int) v * 100;
				int frac = vint % 100;
				vint = (vint - frac) / 100;
				graph2d.drawString("" + vint + (frac != 0 ? "." + frac : ""), 6f, y - 6f);
				v += gridlineOffset;
			}
		}
	}

	public static void main(String args[]) throws Exception {
		if (args.length > 0) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			BufferedImage bufImg = createGraphCanvas((int) (screenSize.width * 0.8), (int) (screenSize.height * 0.8), Color.WHITE);
			JFrame frame = new JFrame("Average brightnesses of images");
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(new JScrollPane(new JLabel(new ImageIcon(bufImg))), BorderLayout.CENTER);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.pack();
			frame.setVisible(true);

			for (int argIndex = 0; argIndex < args.length; argIndex++) {
				File dir = new File(args[argIndex]);
				System.out.println("Processing " + dir.getAbsolutePath());
				if (dir.exists() && dir.isDirectory()) {
					Graphics2D graph2d = bufImg.createGraphics();
					graph2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
					graph2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

					final File[] files = dir.listFiles(new FileFilter() {
						@Override
						public boolean accept(File pathname) {
							String lcName = pathname.getName().toLowerCase();
							return lcName.endsWith("jpg") || lcName.endsWith("jpeg");
						}
					});
					Arrays.sort(files, FileByNameComparator.INSTANCE);
					Map<String, Double> values = ImageUtil.calculateAverageBrightnesses(files);
					List<Double> valuesList = new ArrayList<>(files.length);
					for (File file : files) {
						valuesList.add(values.get(file.getAbsolutePath()));
					}

					Color color = Color.getHSBColor(((float) argIndex) / args.length * 0.8f, 1f, 0.5f);
					paintGraphOver(bufImg, graph2d, color, valuesList, 0d, 100d, 10d);
					graph2d.setColor(color);
					graph2d.setFont(frame.getFont());
					graph2d.drawString(dir.getName(), 30f,
							bufImg.getHeight() - 10f - ((float) argIndex) / args.length * (graph2d.getFontMetrics().getHeight() * 1.4f));

					frame.invalidate();
					frame.revalidate();
					frame.repaint();

				}
			}
			System.out.println("All done.");
		}
	}
}
