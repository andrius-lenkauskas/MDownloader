package gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;

import data.Controller;
import data.event.gui.StatusUpdateEvent;
import data.event.gui.StatusUpdateListener;

public class DownloaderPanel extends JPanel implements ActionListener, StatusUpdateListener {
	private JTextArea textArea;
	private JScrollPane scrollPaneForTextArea, scrollPaneForTable;
	private JButton importButton, chooserButton, startButton, stopButton, clearButton, scanButton;
	private JTable table;
	private JLabel superPhotoCheckBoxLabel, companyLabel, directoryLabel, statusLabel, checkIfIsModniqueCheckBoxLabel;
	private JRadioButton bidzJRadioButton, modJRadioButton;
	private ButtonGroup companyButtonGroup;
	private JFileChooser chooser;
	private JTextField textField;
	private JCheckBox superPhotoCheckBox, checkIfIsModniqueCheckBox;
	private static final Insets WEST_INSETS = new Insets(1, 0, 1, 5);
	private static final Insets EAST_INSETS = new Insets(1, 5, 1, 0);
	private String status = " ";
	private boolean stoping = false;

	public DownloaderPanel() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		UpperPanel upperPanel = new UpperPanel();
		LowerPanel lowerPanel = new LowerPanel();
		Box horizontalBox = Box.createHorizontalBox();
		horizontalBox.add(lowerPanel);
		horizontalBox.add(Box.createGlue());
		add(upperPanel);
		add(horizontalBox);
		importButton.addActionListener(this);
		chooserButton.addActionListener(this);
		startButton.addActionListener(this);
		stopButton.addActionListener(this);
		clearButton.addActionListener(this);
		scanButton.addActionListener(this);
		Controller.getInstance().addStatusUpdateListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == scanButton) {
			if (chooser.getSelectedFile() != null) {
				disableAllButtons();
				stopButton.setEnabled(true);
				DefaultTableModel model = (DefaultTableModel) table.getModel();
				Map<String, String> files = new HashMap<String, String>();
				String fileName, cleanedFileName;
				for (int i = 0; i < model.getRowCount(); i++) {
					if (model.getValueAt(i, 0) == Boolean.FALSE && !((String) model.getValueAt(i, 1)).isEmpty()) {
						fileName = (String) model.getValueAt(i, 1);
						cleanedFileName = Controller.getInstance().getFileNameWithoutNumber(fileName);
						files.put(cleanedFileName, fileName);
					}
				}
				Controller.getInstance().startScanning(files, chooser.getSelectedFile().getAbsolutePath());
			} else
				JOptionPane.showMessageDialog(null, "Please select download directory", "Info", JOptionPane.INFORMATION_MESSAGE);
		} else if (event.getSource() == clearButton) {
			textArea.setText("");
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			int rc = model.getRowCount();
			for (int i = 0; i < rc; i++)
				model.removeRow(0);
			model.addRow(new Object[] { Boolean.FALSE, "", "" });
			status = " ";
			repaint();
		} else if (event.getSource() == stopButton) {
			stoping = true;
			status = "Status: Stoping...";
			repaint();
			Controller.getInstance().stopDownloading();
			Controller.getInstance().stopScanning();
		} else if (event.getSource() == startButton) {
			if (chooser.getSelectedFile() != null) {
				disableAllButtons();
				stopButton.setEnabled(true);
				DefaultTableModel model = (DefaultTableModel) table.getModel();
				Map<String, String> allLinks = new HashMap<String, String>();
				for (int i = 0; i < model.getRowCount(); i++) {
					if (model.getValueAt(i, 0) == Boolean.FALSE && !((String) model.getValueAt(i, 1)).isEmpty()) {
						allLinks.put(chooser.getSelectedFile().getAbsolutePath() + File.separator + (String) model.getValueAt(i, 1),
								(String) model.getValueAt(i, 2));
					}
				}
				if (!Controller.getInstance().startDownloading(allLinks, superPhotoCheckBox.isSelected()))
					enableAllButtons();
			} else
				JOptionPane.showMessageDialog(null, "Please select download directory", "Info", JOptionPane.INFORMATION_MESSAGE);
		} else if (event.getSource() == importButton) {
			String text = textArea.getText();
			int start = 0;
			int end = 0;
			int j = 0;
			String[] row = null;
			String line = "";
			boolean wasDuplicates = false;
			//AbstractButton companyButton = getSelectedCompanyButton(companyButtonGroup);
			Controller controller = Controller.getInstance();
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			Map<String, String> map = new HashMap<String, String>();
			for (int i = 0; i < textArea.getLineCount(); i++) {
				start = 0;
				end = 0;
				line = "";
				try {
					start = textArea.getLineStartOffset(i);
					end = textArea.getLineEndOffset(i);
				} catch (BadLocationException e) {
					e.printStackTrace();
					line = "";
				}
				line = text.substring(start, end);
				row = controller.convertToFileName(line, checkIfIsModniqueCheckBox.isSelected());
				if (row != null) {
					if (map.get(row[0]) == null) {
						map.put(row[0], row[1]);
						j++;
					} else
						wasDuplicates = true;
				}
			}
			int rc = model.getRowCount();
			for (int i = 0; i < rc; i++)
				model.removeRow(0);
			for (Map.Entry<String, String> entry : map.entrySet()) {
				model.addRow(new Object[] { Boolean.FALSE, entry.getKey(), entry.getValue() });
			}
			status = j > 0 ? "Status: Imported " + j + " lines" : " ";
			repaint();
			if (wasDuplicates)
				JOptionPane.showMessageDialog(null, "Lines with duplicated file names wasn't add", "Info", JOptionPane.INFORMATION_MESSAGE);
		} else if (event.getSource() == chooserButton) {
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
				textField.setText(chooser.getSelectedFile().getPath());
		}
	}

	public AbstractButton getSelectedCompanyButton(ButtonGroup buttonGroup) {
		for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
			AbstractButton button = buttons.nextElement();
			if (button.isSelected()) {
				return button;
			}
		}
		return null;
	}

	private class LowerPanel extends JPanel {
		public LowerPanel() {
			setLayout(new GridBagLayout());
			GridBagConstraints gbc;
			//
			/*companyLabel = new JLabel("Company:");
			gbc = createGbc(0, 0);
			gbc.anchor = GridBagConstraints.NORTH;
			add(companyLabel, gbc);
			bidzJRadioButton = new JRadioButton("Bidz");
			bidzJRadioButton.setSelected(true);
			modJRadioButton = new JRadioButton("Modnique");
			companyButtonGroup = new ButtonGroup();
			companyButtonGroup.add(bidzJRadioButton);
			companyButtonGroup.add(modJRadioButton);
			JPanel radioPanel = new JPanel(new GridLayout(0, 1));
			radioPanel.add(bidzJRadioButton);
			radioPanel.add(modJRadioButton);
			add(radioPanel, createGbc(1, 0));*/
			//
			superPhotoCheckBoxLabel = new JLabel("Download super photo:");
			add(superPhotoCheckBoxLabel, createGbc(0, 1));
			superPhotoCheckBox = new JCheckBox();
			superPhotoCheckBox.setSelected(true);
			add(superPhotoCheckBox, createGbc(1, 1));
			//
			checkIfIsModniqueCheckBoxLabel = new JLabel("File name from URL:");
			checkIfIsModniqueCheckBoxLabel.setToolTipText("Assign file name from URL for Modnique's items");
			add(checkIfIsModniqueCheckBoxLabel, createGbc(0, 2));
			checkIfIsModniqueCheckBox = new JCheckBox();
			checkIfIsModniqueCheckBox.setSelected(true);
			add(checkIfIsModniqueCheckBox, createGbc(1, 2));
			//
			chooser = new JFileChooser("Choose Directory") {
				@Override
				public void approveSelection() {
					getSelectedFile().mkdirs();
					super.approveSelection();
				}
			};
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			directoryLabel = new JLabel("Download directory:");
			add(directoryLabel, createGbc(0, 3));
			chooserButton = new JButton("Choose Directory");
			gbc = createGbc(1, 3);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			add(chooserButton, gbc);

			//
			textField = new JTextField(26);
			textField.setEditable(false);
			Dimension d = textField.getPreferredSize();
			d.height = chooserButton.getPreferredSize().height;
			textField.setPreferredSize(d);
			gbc = createGbc(0, 4);
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = EAST_INSETS;
			add(textField, gbc);
			//
			startButton = new JButton("Start");
			stopButton = new JButton("Stop");
			scanButton = new JButton("Scan");
			scanButton.setToolTipText("Scanns selected directory for already downloaded files");
			JPanel stopStartScanPanel = new JPanel();
			stopStartScanPanel.add(startButton);
			stopStartScanPanel.add(stopButton);
			stopStartScanPanel.add(scanButton);
			gbc = createGbc(0, 5);
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			add(stopStartScanPanel, gbc);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(280, 140);
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		@Override
		public Dimension getMinimumSize() {
			return getPreferredSize();
		}
	}

	private class UpperPanel extends JPanel {
		public UpperPanel() {
			setLayout(new GridBagLayout());
			GridBagConstraints position = new GridBagConstraints();
			//
			textArea = new JTextArea();
			textArea.setFont(new Font("Verdana", Font.PLAIN, 12));
			scrollPaneForTextArea = new JScrollPane(textArea);
			scrollPaneForTextArea.setPreferredSize(new Dimension(400, 600));
			scrollPaneForTextArea.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPaneForTextArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			position.gridy = 0;
			position.gridx = 0;
			position.weightx = 1;
			position.weighty = 1;
			position.fill = GridBagConstraints.BOTH;
			add(scrollPaneForTextArea, position);
			//
			importButton = new JButton("Import");
			try {
				Image img = ImageIO.read(getClass().getResource("/next.png"));
				importButton.setIcon(new ImageIcon(img));
				importButton.setHorizontalTextPosition(JButton.LEFT);
			} catch (IOException ex) {
			}
			clearButton = new JButton("Clear");
			try {
				Image img = ImageIO.read(getClass().getResource("/clear.png"));
				clearButton.setIcon(new ImageIcon(img));
				clearButton.setHorizontalTextPosition(JButton.LEFT);
			} catch (IOException ex) {
			}
			clearButton.setPreferredSize(importButton.getPreferredSize());
			clearButton.setMaximumSize(importButton.getMaximumSize());
			clearButton.setMinimumSize(importButton.getMinimumSize());
			JPanel imClButtonsPanel = new JPanel();
			imClButtonsPanel.setLayout(new BoxLayout(imClButtonsPanel, BoxLayout.PAGE_AXIS));
			imClButtonsPanel.add(importButton);
			imClButtonsPanel.add(new JLabel(" "));
			imClButtonsPanel.add(clearButton);

			position.gridy = 0;
			position.gridx = 1;
			position.weightx = 0;
			position.weighty = 0;
			position.fill = GridBagConstraints.NONE;
			add(imClButtonsPanel, position);
			//
			final String[] columns = { "Is Downloaded", "File Name", "File URL" };
			final Object[][] data = { { Boolean.FALSE, "", "" } };
			table = new JTable(new DefaultTableModel(data, columns) {
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					return data[0][columnIndex].getClass();
				}

				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			}) {
				@Override
				public boolean getScrollableTracksViewportWidth() {
					return getPreferredSize().width < getParent().getWidth();
				}
			};
			table.setAutoCreateRowSorter(true);
			scrollPaneForTable = new JScrollPane(table);
			scrollPaneForTable.setPreferredSize(scrollPaneForTextArea.getPreferredSize());
			scrollPaneForTable.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPaneForTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			table.setFillsViewportHeight(true);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

			position.gridy = 0;
			position.gridx = 2;
			position.weightx = 1;
			position.weighty = 1;
			position.fill = GridBagConstraints.BOTH;
			add(scrollPaneForTable, position);
			//
			statusLabel = new JLabel(status);
			position.gridy = 1;
			position.gridx = 2;
			position.weightx = 0;
			position.weighty = 0;
			add(statusLabel, position);
		}
	}

	private GridBagConstraints createGbc(int x, int y) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;

		// gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
		gbc.anchor = (x == 0) ? GridBagConstraints.EAST : GridBagConstraints.WEST;
		// gbc.fill = (x == 0) ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;

		gbc.insets = (x == 0) ? WEST_INSETS : EAST_INSETS;
		gbc.weightx = (x == 0) ? 0.1 : 1.0;
		gbc.weighty = 1.0;
		return gbc;
	}

	@Override
	public void updateStatus(StatusUpdateEvent event) {
		if (!stoping)
			status = event.getMessage();
		String fileName = event.getFileName();
		if (!fileName.isEmpty()) {
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			for (int i = 0; i < model.getRowCount(); i++) {
				if (((String) model.getValueAt(i, 1)).equals(fileName)) {
					model.setValueAt(Boolean.TRUE, i, 0);
					break;
				}
			}
		}
		if (event.isCompleted()) {
			enableAllButtons();
			status = event.getMessage();
			stoping = false;
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		statusLabel.setText(status);
	}

	private void disableAllButtons() {
		importButton.setEnabled(false);
		chooserButton.setEnabled(false);
		startButton.setEnabled(false);
		stopButton.setEnabled(false);
		clearButton.setEnabled(false);
		scanButton.setEnabled(false);
	}

	private void enableAllButtons() {
		importButton.setEnabled(true);
		chooserButton.setEnabled(true);
		startButton.setEnabled(true);
		stopButton.setEnabled(true);
		clearButton.setEnabled(true);
		scanButton.setEnabled(true);
	}
}
