package x.mvmn.tldeflicker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import x.mvmn.tldeflicker.GraphPanel.Serie;
import x.mvmn.tldeflicker.ImageUtil.ColorChannel;

public class TLDeflicker {

	public static void main(String args[]) {
		if (args.length > 2) {
			try {
				process(new File(args[0]), new File(new File(args[0]), "deflickered"), args[1], args[2], args.length > 3 ? args[3] : "jpg",
						args.length > 4 ? Float.parseFloat(args[4].trim()) : 1f, false, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			JFrame frame = new JFrame("TLDeflicker");
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			JTextField txInputPath = new JTextField("");
			txInputPath.setEditable(false);
			JTextField txOutputPath = new JTextField("");
			txOutputPath.setEditable(false);
			JComboBox<String> outFormatCombo = new JComboBox<>(ImageIO.getWriterFormatNames());
			frame.getContentPane().setLayout(new GridLayout(5, 1));
			JButton chooseInputFolder = new JButton("...");
			chooseInputFolder.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser jfc = new JFileChooser();
					jfc.setMultiSelectionEnabled(false);
					jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(frame)) {
						txInputPath.setText(jfc.getSelectedFile().getAbsolutePath());
					}
				}
			});
			JButton chooseOutputFolder = new JButton("...");
			chooseOutputFolder.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser jfc = new JFileChooser();
					jfc.setMultiSelectionEnabled(false);
					jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if (JFileChooser.APPROVE_OPTION == jfc.showSaveDialog(frame)) {
						txOutputPath.setText(jfc.getSelectedFile().getAbsolutePath());
					}
				}
			});
			JTextField tfExifDirectory = new JTextField("Exif SubIFD");
			JTextField tfExifTag = new JTextField("Exposure Time");
			JButton lookupExif = new JButton("Lookup from image file");
			lookupExif.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent event) {
					JFileChooser jfc = new JFileChooser();
					jfc.setMultiSelectionEnabled(false);
					jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
					if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(frame)) {
						try {
							Metadata metadata = ImageMetadataReader.readMetadata(jfc.getSelectedFile());
							List<Tuple<String, String, String, Void, Void>> metaList = new ArrayList<>();
							for (Directory dir : metadata.getDirectories()) {
								for (Tag tag : dir.getTags()) {
									metaList.add(new Tuple<String, String, String, Void, Void>(dir.getName(), tag.getTagName(), dir.getString(tag.getTagType()),
											null, null));
								}
							}

							Object[][] rowData = new Object[metaList.size()][];
							int i = 0;
							for (Tuple<String, String, String, Void, Void> metaElem : metaList) {
								rowData[i++] = new Object[] { metaElem.getA(), metaElem.getB(), metaElem.getC() };
							}

							JTable table = new JTable(new DefaultTableModel(rowData, new String[] { "EXIF Directory", "EXIF Tag", "Value" }) {
								private static final long serialVersionUID = 1017210439624887330L;

								@Override
								public boolean isCellEditable(int row, int column) {
									return false;
								}
							});
							table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

							JFrame lookupFrame = new JFrame("EXIF of " + jfc.getSelectedFile().getAbsolutePath());
							lookupFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
							lookupFrame.getContentPane().setLayout(new BorderLayout());
							lookupFrame.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
							JButton btnChoose = new JButton("select");
							lookupFrame.getContentPane().add(btnChoose, BorderLayout.SOUTH);
							btnChoose.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									if (table.getSelectedRow() >= 0) {
										Object row[] = rowData[table.getSelectedRow()];
										tfExifDirectory.setText(row[0].toString());
										tfExifTag.setText(row[1].toString());
										lookupFrame.setVisible(false);
										lookupFrame.dispose();
									}
								}
							});
							lookupFrame.pack();
							lookupFrame.setVisible(true);
						} catch (Exception e) {
							e.printStackTrace();
							JOptionPane.showMessageDialog(frame, "Error occurred: " + e.getClass().getName() + " " + e.getMessage());
						}
					}
				}
			});
			JLabel lblQuality = new JLabel("100");
			JSlider sliderQuality = new JSlider(1, 100, 100);
			sliderQuality.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					lblQuality.setText(String.format("%03d", sliderQuality.getValue()));
				}
			});
			JCheckBox cbGraphBrightness = new JCheckBox("Show brightnesses graph (may slow things down)", true);
			JButton btnDoDeflicker = new JButton("Process images");
			btnDoDeflicker.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// Validate
					String validationError = null;
					if (txInputPath.getText().trim().isEmpty()) {
						validationError = "Choose input folder first";
					} else if (!new File(txInputPath.getText().trim()).exists()) {
						validationError = "Input folder does not exist";
					} else if (!new File(txInputPath.getText().trim()).isDirectory()) {
						validationError = "Input folder path points to a file - not a folder";
					} else if (txOutputPath.getText().trim().isEmpty()) {
						validationError = "Choose output folder first";
					} else if (new File(txOutputPath.getText().trim()).exists() && !new File(txOutputPath.getText().trim()).isDirectory()) {
						validationError = "Output folder path points to a file - not a folder";
					} else if (tfExifDirectory.getText().trim().isEmpty()) {
						validationError = "Specify EXIF directory for exposure tag";
					} else if (tfExifTag.getText().trim().isEmpty()) {
						validationError = "Specify EXIF tag for exposure";
					}
					if (validationError != null) {
						JOptionPane.showMessageDialog(frame, validationError);
					} else {
						new Thread() {
							public void run() {
								try {
									File dir = new File(txInputPath.getText());
									GraphPanel graphPanel = process(dir, new File(txOutputPath.getText()), tfExifDirectory.getText(), tfExifTag.getText(),
											outFormatCombo.getSelectedItem().toString(), ((float) sliderQuality.getValue()) / 100, true,
											cbGraphBrightness.isSelected());
									try {
										List<Double> values = ExifUtil.getValuesAsNumeric(
												Arrays.asList(FileUtil.listFilesByExtensions(dir, ImageUtil.getSupportedImageFormatExtensions())),
												tfExifDirectory.getText(), tfExifTag.getText(), 0d);
										if (!values.isEmpty()) {

											double max = values.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
											Serie exposureSerie = graphPanel.createSerie(Color.getHSBColor(0.8f, 0.8f, 0.4f), values.size(), 0d, max, max / 10,
													true);
											for (int i = 0; i < values.size(); i++) {
												exposureSerie.setValue(i, values.get(i));
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
										JOptionPane.showMessageDialog(frame,
												"Error occurred while plotting exposures: " + e.getClass().getName() + " " + e.getMessage());
									}
								} catch (Exception e) {
									e.printStackTrace();
									JOptionPane.showMessageDialog(frame, "Error occurred: " + e.getClass().getName() + " " + e.getMessage());
								}
							}
						}.start();
						frame.setVisible(false);
						frame.dispose();
					}
				}
			});
			frame.getContentPane().add(gridLayoutPanel("Paths", borderLayoutPanel(new JLabel("Input folder: "), txInputPath, chooseInputFolder, null, null),
					borderLayoutPanel(new JLabel("Output folder: "), txOutputPath, chooseOutputFolder, null, null)));
			frame.getContentPane().add(gridLayoutPanel("Output files", borderLayoutPanel(new JLabel("File format: "), outFormatCombo, null, null, null),
					borderLayoutPanel(new JLabel("Compression quality"), sliderQuality, lblQuality, null, null)));
			frame.getContentPane()
					.add(gridLayoutPanel("Exposure EXIF metadata", borderLayoutPanel(new JLabel("EXIF directory: "), tfExifDirectory, null, null, null),
							borderLayoutPanel(new JLabel("EXIF tag: "), tfExifTag, lookupExif, null, null)));
			JButton btnBuildGraph = new JButton("Build brightness graph for existing images");
			btnBuildGraph.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser jfc = new JFileChooser();
					jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					jfc.setMultiSelectionEnabled(true);
					if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(null)) {
						new Thread() {
							public void run() {
								JPanel progressPanel = new JPanel();
								JTabbedPane tabbedPane = new JTabbedPane();
								Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
								GraphPanel grafPanel = new GraphPanel((int) (screenSize.width * 0.8), (int) (screenSize.height * 0.8), Color.WHITE);

								JFrame frame = new JFrame("Average brightnesses of images");
								frame.getContentPane().setLayout(new BorderLayout());

								JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(grafPanel), tabbedPane);
								frame.getContentPane().add(jSplitPane, BorderLayout.CENTER);
								frame.getContentPane().add(progressPanel, BorderLayout.NORTH);
								frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
								frame.pack();
								frame.setVisible(true);

								for (int index = 0; index < jfc.getSelectedFiles().length; index++) {
									File dir = jfc.getSelectedFiles()[index];
									try {
										File[] files = FileUtil.listFilesByExtensions(dir, ImageUtil.getSupportedImageFormatExtensions());
										double[] brightnesses = new double[files.length];

										GraphPanel.populateBrighnessGraph(grafPanel, dir.getAbsolutePath(), index, jfc.getSelectedFiles().length, files,
												ImageUtil.getSupportedImageFormatExtensions(), FileByNameComparator.INSTANCE, progressPanel,
												(int) (screenSize.width * 0.8), (int) (screenSize.height * 0.8), brightnesses);

										List<Double> values = ExifUtil.getValuesAsNumeric(Arrays.asList(files), tfExifDirectory.getText(), tfExifTag.getText(),
												0d);
										double max = values.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
										Serie exposureSerie = grafPanel.createSerie(
												Color.getHSBColor(((float) index) / jfc.getSelectedFiles().length * 0.8f, 0.8f, 0.4f), values.size(), 0d, max,
												max / 10, true);
										DefaultTableModel tableModel = new DefaultTableModel(new String[] { "File", "Exposure", "Brightness" }, 0);
										JTable table = new JTable(tableModel);
										for (int i = 0; i < values.size(); i++) {
											exposureSerie.setValue(i, values.get(i));
											tableModel.addRow(new Object[] { files[i].getName(), String.format("%5f", values.get(i)),
													String.format("%5f", brightnesses[i]) });
										}
										tabbedPane.addTab(dir.getName(), new JScrollPane(table));

									} catch (Exception e) {
										e.printStackTrace();
										JOptionPane.showMessageDialog(frame,
												"Error occurred while plotting exposures: " + e.getClass().getName() + " " + e.getMessage());
									}
								}

							}
						}.start();
					}
				}
			});
			frame.getContentPane().add(gridLayoutPanel("", cbGraphBrightness, btnBuildGraph));
			frame.getContentPane().add(btnDoDeflicker);
			frame.pack();
			frame.setVisible(true);
		}
	}

	protected static JPanel gridLayoutPanel(String titledBorder, JComponent... children) {
		JPanel result = new JPanel(new GridLayout(children.length, 1));
		if (titledBorder != null) {
			result.setBorder(BorderFactory.createTitledBorder(titledBorder));
		}
		for (JComponent child : children) {
			result.add(child);
		}

		return result;

	}

	protected static JPanel borderLayoutPanel(JComponent west, JComponent center, JComponent east, JComponent north, JComponent south) {
		JPanel result = new JPanel(new BorderLayout());
		if (west != null) {
			result.add(west, BorderLayout.WEST);
		}
		if (center != null) {
			result.add(center, BorderLayout.CENTER);
		}
		if (east != null) {
			result.add(east, BorderLayout.EAST);
		}
		if (north != null) {
			result.add(north, BorderLayout.NORTH);
		}
		if (south != null) {
			result.add(south, BorderLayout.SOUTH);
		}
		return result;
	}

	public static GraphPanel process(File path, File outputPath, String exifDirectory, String exifTag, String imageOutputFormat, float quality,
			boolean guiProgressIndication, boolean chartBrightnesses) throws Exception {
		GraphPanel graphPanel = null;
		Set<String> fileExtensions = ImageUtil.getSupportedImageFormatExtensions();
		if (path.exists() && path.isDirectory()) {
			File outPath = outputPath == null ? new File(path, "deflickered") : outputPath;
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

			graphPanel = chartBrightnesses ? new GraphPanel(Color.WHITE) : null;
			Serie inputSerie = chartBrightnesses ? graphPanel.createSerie(Color.RED, files.length, 0d, 100d, 10d) : null;
			Serie outputSerie = chartBrightnesses ? graphPanel.createSerie(Color.GREEN, files.length, 0d, 100d, 10d) : null;
			final JLabel progressText;
			final JProgressBar progressBar;
			final JFrame frame;
			final JPanel progressPanel;
			if (guiProgressIndication) {
				frame = new JFrame("TLDeflicker: progress");
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				progressText = new JLabel("Initializing...");
				progressBar = new JProgressBar(0, files.length);
				progressBar.setValue(0);
				frame.getContentPane().setLayout(new BorderLayout());
				if (guiProgressIndication && chartBrightnesses) {
					frame.getContentPane().add(graphPanel, BorderLayout.CENTER);
				}
				progressPanel = borderLayoutPanel(null, progressText, null, null, progressBar);
				frame.getContentPane().add(progressPanel, BorderLayout.NORTH);
				frame.pack();
				frame.setVisible(true);
			} else {
				progressText = null;
				progressBar = null;
				frame = null;
				progressPanel = null;
			}
			{
				final String message = "Readin exposure values from EXIF directory '" + exifDirectory + "' tag '" + exifTag + "'";
				if (guiProgressIndication) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							progressText.setText(message);
							frame.invalidate();
							frame.revalidate();
							frame.repaint();
						}
					});
				} else {
					System.out.println(message);
				}
			}

			Set<Integer> exposureChangePointsSet = new TreeSet<>();
			String prevExposure = null;
			for (int i = 0; i < files.length; i++) {
				final int finalI = i;
				String exposure = ExifUtil.getValue(files[i], exifDirectory, exifTag);
				if (prevExposure != null && !exposure.equals(prevExposure)) {
					exposureChangePointsSet.add(i);
				}
				prevExposure = exposure;
				if (guiProgressIndication) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							progressBar.setValue(finalI + 1);
							frame.invalidate();
							frame.revalidate();
							frame.repaint();
						}
					});
				}
			}

			final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

			if (guiProgressIndication) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						progressBar.setValue(0);
						progressText.setText("Processing files...");
						frame.invalidate();
						frame.revalidate();
						frame.repaint();
					}
				});
			}
			List<Integer> exposureChangePoints = new ArrayList<>(exposureChangePointsSet);
			for (int i = 0; i <= exposureChangePoints.size(); i++) {
				if (!guiProgressIndication) {
					System.out.println("Preparing tasks for sequence " + (i + 1) + " of " + (exposureChangePoints.size() + 1));
				}

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
										double brightnesBeforAdjustment = 0d;
										if (chartBrightnesses) {
											brightnesBeforAdjustment = ImageUtil.calc(biToAdjust);
											inputSerie.setValue(finalK, brightnesBeforAdjustment);
										}
										BufferedImage biOutput;
										if (numberInSequence > 0) {
											float[] adjustments = new float[] { 1f + (float) (dRed * numberInSequence / deviatedSeqSize),
													1f + (float) (dGreen * numberInSequence / deviatedSeqSize),
													1f + (float) (dBlue * numberInSequence / deviatedSeqSize) };
											biOutput = new BufferedImage(biToAdjust.getWidth(), biToAdjust.getHeight(), biToAdjust.getType());
											RescaleOp operation = new RescaleOp(adjustments, new float[3], null);
											operation.filter(biToAdjust, biOutput);
											if (chartBrightnesses) {
												outputSerie.setValue(finalK, ImageUtil.calc(biOutput));
											}
										} else {
											biOutput = biToAdjust;
											if (chartBrightnesses) {
												outputSerie.setValue(finalK, brightnesBeforAdjustment);
											}
										}
										String fileName = files[finalK].getName();
										if (!fileName.toLowerCase().endsWith(imageOutputFormat.toLowerCase())) {
											fileName = fileName + "." + imageOutputFormat;
										}
										ImageUtil.writeImage(biOutput, new File(outPath, fileName), imageOutputFormat, quality);
										final String message = "Processed file #" + finalK + " " + files[finalK].getName();
										if (guiProgressIndication) {
											SwingUtilities.invokeLater(new Runnable() {
												@Override
												public void run() {
													progressBar.setValue(progressBar.getValue() + 1);
													progressText.setText(message);
													frame.invalidate();
													frame.revalidate();
													frame.repaint();
												}
											});
										} else {
											System.out.println(message);
										}
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
			if (guiProgressIndication) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (chartBrightnesses) {
							frame.getContentPane().remove(progressPanel);
						} else {
							progressText.setText("Processing finished.");
							progressBar.setVisible(false);
						}
						frame.invalidate();
						frame.revalidate();
						frame.repaint();
					}
				});
			} else {
				System.out.println("Processing finished.");
			}
		}
		return graphPanel;
	}
}
