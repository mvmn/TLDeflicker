package x.mvmn.tldeflicker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class GraphPanel extends JPanel {

	private static final long serialVersionUID = 345853583401944720L;
	protected final BufferedImage backingImage;
	protected final Graphics2D graphics2D;

	public GraphPanel() {
		this(UIManager.getColor("Panel.background"));
	}

	public GraphPanel(Color backgroundColor) {
		this((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.8), (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.8),
				backgroundColor);
	}

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

		protected Serie(Color color, int size, double min, double max, Double gridlineOffset, boolean gridlineValueRight) {
			this.size = size;
			this.values = new Double[size];
			this.color = color;
			this.min = min;
			if (max - min < 0.1) {
				max = min + 0.1d;
			}
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
				if (gridlineOffset != null && gridlineOffset > 0.01) {
					double v = min;
					while (!(v > max + v / 2)) {
						int y = effectiveHeight - (int) (v * valueYMultiplier) + topMargin - 1;
						graphics2D.drawLine(0, y, backingImageWidth, y);
						int vint = (int) (v * 1000);
						int frac = vint % 1000;
						vint = (vint - frac) / 1000;
						graphics2D.drawString("" + vint + (frac != 0 ? "." + String.format("%03d", frac) : ""),
								gridlineValueRight ? backingImageWidth - 50f : 6f, y - 6f);
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
			int x = Math.min((int) (index * valueXOffset), backingImageWidth - 1);
			int y = topMargin + effectiveHeight - (int) ((value - min) * valueYMultiplier);
			synchronized (graphics2D) {
				graphics2D.setColor(transparentColor);
				graphics2D.fillOval(x, y - 1, 3, 3);
			}
			if (prevVal != null) {
				int x2 = (int) ((index - 1) * valueXOffset);
				int y2 = topMargin + effectiveHeight - (int) ((prevVal - min) * valueYMultiplier);
				synchronized (graphics2D) {
					graphics2D.setColor(color);
					graphics2D.drawLine(x, y, x2, y2);
				}
			}

			if (nextVal != null) {
				int x2 = (int) ((index + 1) * valueXOffset);
				int y2 = topMargin + effectiveHeight - (int) ((nextVal - min) * valueYMultiplier);
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
		return createSerie(color, size, min, max, gridlineOffset, false);
	}

	public Serie createSerie(Color color, int size, double min, double max, Double gridlineOffset, boolean gridlineValueRight) {
		return new Serie(color, size, min, max, gridlineOffset, gridlineValueRight);
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

	public static void populateBrighnessGraph(final GraphPanel graphPanel, String graphName, int graphIndex, int totalGraphs, File[] files,
			Set<String> fileExtensions, FileByNameComparator fileComparator, Container uiContainer, int graphWidth, int graphHeight,
			double[] collectBrightnesses) {
		try {
			final JLabel lblProgress = new JLabel("Processing files...");
			BufferedImage bufImg = graphPanel.getBackingImage();
			Graphics2D graph2d = graphPanel.getGraphics2D();

			Color color = Color.getHSBColor(((float) graphIndex) / totalGraphs * 0.8f, 1f, 0.5f);
			final Serie serie = graphPanel.createSerie(color, files.length, 0d, 100d, 10d);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					uiContainer.add(lblProgress);
					uiContainer.invalidate();
					uiContainer.revalidate();
					uiContainer.repaint();

					synchronized (graph2d) {
						graph2d.setColor(color);
						graph2d.setFont(uiContainer.getFont());
						graph2d.drawString(graphName, 30f,
								bufImg.getHeight() - 10f - ((float) graphIndex) / totalGraphs * (graph2d.getFontMetrics().getHeight() * 2f));
					}

					graphPanel.invalidate();
					graphPanel.revalidate();
					graphPanel.repaint();
					uiContainer.invalidate();
					uiContainer.revalidate();
					uiContainer.repaint();
				}
			});
			ImageUtil.calculateAverageBrightnesses(files, new Function<Tuple<Integer, File, Double, Void, Void>, Void>() {
				@Override
				public Void apply(Tuple<Integer, File, Double, Void, Void> tupleIndexFileBrightness) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							serie.setValue(tupleIndexFileBrightness.getA(), tupleIndexFileBrightness.getC());
							if (collectBrightnesses != null) {
								collectBrightnesses[tupleIndexFileBrightness.getA()] = tupleIndexFileBrightness.getC();
							}
						}
					});
					return null;
				}
			});
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					uiContainer.remove(lblProgress);
					uiContainer.invalidate();
					uiContainer.revalidate();
					uiContainer.repaint();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error occurred: " + e.getClass().getName() + " " + e.getMessage());
		}
	}
}