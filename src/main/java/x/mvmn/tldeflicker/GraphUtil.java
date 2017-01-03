package x.mvmn.tldeflicker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import x.mvmn.tldeflicker.ImageUtil.Pair;

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
		Set<String> fileExtensions = new HashSet<>();
		fileExtensions.add("jpg");
		fileExtensions.add("jpeg");
		showBrighnessGraph(Arrays.asList(args), fileExtensions, FileByNameComparator.INSTANCE);
	}

	public static void showBrighnessGraph(List<String> folders, Set<String> fileExtensions, FileByNameComparator fileComparator) {
		if (folders.size() > 0) {
			try {
				JPanel progressPanel = new JPanel(new GridLayout(2, 1));

				progressPanel.setBorder(BorderFactory.createTitledBorder("Progress"));
				JLabel lblFolderName = new JLabel("...");
				JLabel lblFileName = new JLabel("...");
				JProgressBar pbGeneral = new JProgressBar(0, folders.size());
				pbGeneral.setIndeterminate(false);
				JProgressBar pbFolder = new JProgressBar();
				pbFolder.setIndeterminate(true);
				JPanel p1 = new JPanel(new BorderLayout());
				p1.add(new JLabel("Folders: "), BorderLayout.WEST);
				p1.add(pbGeneral, BorderLayout.CENTER);
				p1.add(lblFolderName, BorderLayout.SOUTH);
				progressPanel.add(p1);
				JPanel p2 = new JPanel(new BorderLayout());
				p2.add(new JLabel("Files: "), BorderLayout.WEST);
				p2.add(pbFolder, BorderLayout.CENTER);
				p2.add(lblFileName, BorderLayout.SOUTH);
				progressPanel.add(p2);
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				BufferedImage bufImg = createGraphCanvas((int) (screenSize.width * 0.8), (int) (screenSize.height * 0.8), Color.WHITE);
				JFrame frame = new JFrame("Average brightnesses of images");
				frame.getContentPane().setLayout(new BorderLayout());
				frame.getContentPane().add(new JScrollPane(new JLabel(new ImageIcon(bufImg))), BorderLayout.CENTER);
				frame.getContentPane().add(progressPanel, BorderLayout.SOUTH);
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.pack();
				frame.setVisible(true);

				for (int argIndex = 0; argIndex < folders.size(); argIndex++) {
					final int argIndexFinal = argIndex;
					File dir = new File(folders.get(argIndex));
					if (dir.exists() && dir.isDirectory()) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								lblFolderName.setText(dir.getAbsolutePath());
							}
						});
						Graphics2D graph2d = bufImg.createGraphics();
						graph2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
						graph2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

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
						Arrays.sort(files, fileComparator);

						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								pbFolder.setIndeterminate(false);
								pbFolder.setMinimum(0);
								pbFolder.setMaximum(files.length);
								pbFolder.setValue(0);
							}
						});
						Map<String, Double> values = ImageUtil.calculateAverageBrightnesses(files, new Function<ImageUtil.Pair<File, Double>, Void>() {
							@Override
							public Void apply(Pair<File, Double> t) {
								SwingUtilities.invokeLater(new Runnable() {

									@Override
									public void run() {
										pbFolder.setValue(pbFolder.getValue() + 1);
										lblFileName.setText(t.getA().getAbsolutePath());
									}
								});
								return null;
							}
						});
						List<Double> valuesList = new ArrayList<>(files.length);
						for (File file : files) {
							valuesList.add(values.get(file.getAbsolutePath()));
						}

						Color color = Color.getHSBColor(((float) argIndex) / folders.size() * 0.8f, 1f, 0.5f);
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								paintGraphOver(bufImg, graph2d, color, valuesList, 0d, 100d, 10d);
								graph2d.setColor(color);
								graph2d.setFont(frame.getFont());
								graph2d.drawString(dir.getName(), 30f,
										bufImg.getHeight() - 10f - ((float) argIndexFinal) / folders.size() * (graph2d.getFontMetrics().getHeight() * 1.4f));

								pbGeneral.setValue(argIndexFinal + 1);
								pbFolder.setIndeterminate(true);
								frame.invalidate();
								frame.revalidate();
								frame.repaint();
							}
						});

					}
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						frame.getContentPane().remove(progressPanel);
						frame.invalidate();
						frame.revalidate();
						frame.repaint();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showConfirmDialog(null, "Error occurred: " + e.getClass().getName() + " " + e.getMessage());
			}
		}
	}
}
