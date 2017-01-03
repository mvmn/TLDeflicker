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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class GraphPanel extends JPanel {

	private static final long serialVersionUID = 345853583401944720L;
	protected final BufferedImage backingImage;
	protected final Graphics2D graphics2D;

	public GraphPanel(int width, int height, Color backgroundColor) {
		backingImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		graphics2D = backingImage.createGraphics();
		graphics2D.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
		graphics2D.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
		graphics2D.setPaint(backgroundColor);
		graphics2D.fillRect(0, 0, width, height);

		super.setLayout(new BorderLayout());
		super.add(new JLabel(new ImageIcon(backingImage)), BorderLayout.CENTER);
	}

	public BufferedImage getBackingImage() {
		return backingImage;
	}

	public Graphics2D getGraphics2D() {
		return graphics2D;
	}

	protected class Serie {
		protected final int size;
		protected final Color color;
		protected final Color transparentColor;
		protected final double min;
		protected final double max;
		protected final Double gridlineOffset;
		protected final int backingImageWidth;
		protected final int backingImageHeight;

		private final double valueXOffset;
		private final int topMargin;
		private final int effectiveHeight;
		private final double valueYMultiplier;

		protected final Double[] values;

		protected Serie(Color color, int size, double min, double max, Double gridlineOffset) {
			this.size = size;
			this.values = new Double[size];
			this.color = color;
			this.min = min;
			this.max = max;
			this.gridlineOffset = gridlineOffset;
			transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 64);

			int backingImageWidth = backingImage.getWidth();
			int backingImageHeight = backingImage.getHeight();
			this.backingImageWidth = backingImageWidth;
			this.backingImageHeight = backingImageHeight;

			this.topMargin = graphics2D.getFontMetrics().getHeight() + 10;
			this.effectiveHeight = backingImageHeight - topMargin;
			this.valueXOffset = ((double) backingImageWidth) / size;
			this.valueYMultiplier = effectiveHeight / (max - min);

			synchronized (graphics2D) {
				graphics2D.setColor(transparentColor);
				if (gridlineOffset != null) {
					double v = min;
					while (!(v > max + v / 2)) {
						int y = effectiveHeight - (int) (v * valueYMultiplier) + topMargin - 1;
						graphics2D.drawLine(0, y, backingImageWidth, y);
						int vint = (int) v * 100;
						int frac = vint % 100;
						vint = (vint - frac) / 100;
						graphics2D.drawString("" + vint + (frac != 0 ? "." + frac : ""), 6f, y - 6f);
						v += gridlineOffset;
					}
				}
			}
		}

		public void setValue(int index, double value) {
			Double prevVal = null;
			Double nextVal = null;
			synchronized (values) {
				values[index] = value;
				if (index > 0) {
					prevVal = values[index - 1];
				}
				if (index < values.length - 1) {
					nextVal = values[index + 1];
				}
			}
			int x = (int) (index * valueXOffset);
			int y = effectiveHeight - (int) ((value - min) * valueYMultiplier);
			synchronized (graphics2D) {
				graphics2D.setColor(transparentColor);
				graphics2D.fillOval(x, y - 1, 3, 3);
			}
			if (prevVal != null) {
				int x2 = (int) ((index - 1) * valueXOffset);
				int y2 = effectiveHeight - (int) ((prevVal - min) * valueYMultiplier);
				synchronized (graphics2D) {
					graphics2D.setColor(color);
					graphics2D.drawLine(x, y, x2, y2);
				}
			}

			if (nextVal != null) {
				int x2 = (int) ((index + 1) * valueXOffset);
				int y2 = effectiveHeight - (int) ((nextVal - min) * valueYMultiplier);
				synchronized (graphics2D) {
					graphics2D.setColor(color);
					graphics2D.drawLine(x, y, x2, y2);
				}
			}

			GraphPanel.this.invalidate();
			GraphPanel.this.revalidate();
			GraphPanel.this.repaint();
		}
	}

	public Serie createSerie(Color color, int size, double min, double max, Double gridlineOffset) {
		return new Serie(color, size, min, max, gridlineOffset);
	}

	public void fillSerie(Color graphColor, Collection<? extends Number> values, Double minOverride, Double maxOverride, Double gridlineOffset) {
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
		Serie serie = createSerie(graphColor, values.size(), min, max, gridlineOffset);

		for (int i = 0; i < valuesArray.length; i++) {
			serie.setValue(i, valuesArray[i]);
		}
	}

	public static void main(String args[]) throws Exception {
		Set<String> fileExtensions = Stream.of(javax.imageio.ImageIO.getReaderFormatNames()).map(String::toLowerCase).collect(Collectors.toSet());
		if (args.length > 0) {
			showBrighnessGraph(Arrays.asList(args).stream().map(File::new).filter(File::exists).filter(File::isDirectory).collect(Collectors.toList()),
					fileExtensions, FileByNameComparator.INSTANCE);
		} else {
			JFileChooser jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			jfc.setMultiSelectionEnabled(true);
			if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(null)) {
				showBrighnessGraph(Arrays.asList(jfc.getSelectedFiles()), fileExtensions, FileByNameComparator.INSTANCE);
			}
		}
	}

	public static void showBrighnessGraph(List<File> folders, Set<String> fileExtensions, FileByNameComparator fileComparator) {
		if (folders.size() > 0) {
			try {
				final JLabel lblProgress = new JLabel("Processing files...");
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				GraphPanel graphPanel = new GraphPanel((int) (screenSize.width * 0.8), (int) (screenSize.height * 0.8), Color.WHITE);
				JFrame frame = new JFrame("Average brightnesses of images");
				frame.getContentPane().setLayout(new BorderLayout());
				frame.getContentPane().add(new JScrollPane(graphPanel), BorderLayout.CENTER);
				frame.getContentPane().add(lblProgress, BorderLayout.NORTH);
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.pack();
				frame.setVisible(true);
				BufferedImage bufImg = graphPanel.getBackingImage();
				Graphics2D graph2d = graphPanel.getGraphics2D();

				final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
				for (int argIndex = 0; argIndex < folders.size(); argIndex++) {
					final int argIndexFinal = argIndex;
					executorService.submit(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							File dir = folders.get(argIndexFinal);
							if (dir.exists() && dir.isDirectory()) {
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
								Color color = Color.getHSBColor(((float) argIndexFinal) / folders.size() * 0.8f, 1f, 0.5f);
								final Serie serie = graphPanel.createSerie(color, files.length, 0d, 100d, 10d);

								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										synchronized (graph2d) {
											graph2d.setColor(color);
											graph2d.setFont(frame.getFont());
											graph2d.drawString(dir.getName(), 30f, bufImg.getHeight() - 10f
													- ((float) argIndexFinal) / folders.size() * (graph2d.getFontMetrics().getHeight() * 2f));
										}

										frame.invalidate();
										frame.revalidate();
										frame.repaint();
									}
								});
								ImageUtil.calculateAverageBrightnesses(files, new Function<Tuple<Integer, File, Double, Void, Void>, Void>() {
									@Override
									public Void apply(Tuple<Integer, File, Double, Void, Void> pairFileBrightness) {
										SwingUtilities.invokeLater(new Runnable() {
											@Override
											public void run() {
												serie.setValue(pairFileBrightness.getA(), pairFileBrightness.getC());
											}
										});
										return null;
									}
								});
							}
							return null;
						}
					});
				}
				executorService.shutdown();
				executorService.awaitTermination(365, TimeUnit.DAYS);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						frame.getContentPane().remove(lblProgress);
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
