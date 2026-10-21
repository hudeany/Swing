package de.soderer.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.HeadlessException;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * JFileChooser with an editable text field above the file list, which shows the
 * complete path of the current directory (or of the selected file/directory).
 * Entering a path and pressing ENTER navigates there. Optionally the directory
 * hierarchy combo box ("Look in" / "Save in") of the look and feel is hidden.
 */
public class PathFieldFileChooser extends JFileChooser {
	private static final long serialVersionUID = -3624598101370315247L;

	private final JTextField pathField = new JTextField();
	private String pathLabelText = null;
	private boolean hideDirectoryCombo = true;

	public PathFieldFileChooser() {
		this((File) null);
	}

	/**
	 * @param currentDirectory
	 *            directory shown initially, null for the user's default directory
	 */
	public PathFieldFileChooser(final File currentDirectory) {
		super(currentDirectory);

		pathField.addActionListener(e -> navigateToEnteredPath());
		addPropertyChangeListener(JFileChooser.DIRECTORY_CHANGED_PROPERTY, e -> updatePathField());
		addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, e -> updatePathField());
		updatePathField();
	}

	/**
	 * @param pathLabelText
	 *            text of the label in front of the path field, null for no label
	 */
	public PathFieldFileChooser withPathLabelText(final String newPathLabelText) {
		pathLabelText = newPathLabelText;
		return this;
	}

	/**
	 * @param hideDirectoryCombo
	 *            whether the directory hierarchy combo box of the look and feel is hidden (default: true)
	 */
	public PathFieldFileChooser withDirectoryComboHidden(final boolean newHideDirectoryCombo) {
		hideDirectoryCombo = newHideDirectoryCombo;
		return this;
	}

	@Override
	protected JDialog createDialog(final Component parent) throws HeadlessException {
		final JDialog dialog = super.createDialog(parent);

		final JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
		pathPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
		if (pathLabelText != null) {
			final JLabel pathLabel = new JLabel(pathLabelText);
			pathLabel.setLabelFor(pathField);
			pathPanel.add(pathLabel, BorderLayout.WEST);
		}
		pathPanel.add(pathField, BorderLayout.CENTER);

		// super.createDialog() puts the chooser into the CENTER of a BorderLayout content pane
		dialog.getContentPane().add(pathPanel, BorderLayout.NORTH);

		if (hideDirectoryCombo) {
			hideDirectoryCombo(this);
		}

		// super.createDialog() already packed and positioned the dialog without the path field
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		return dialog;
	}

	/**
	 * Shows the selected file or directory if it is located in the current directory, otherwise the current directory
	 */
	private void updatePathField() {
		final File selectedFile = getSelectedFile();
		final File currentDirectory = getCurrentDirectory();
		final File fileToShow;
		if (selectedFile != null && (currentDirectory == null || currentDirectory.equals(selectedFile.getAbsoluteFile().getParentFile()))) {
			fileToShow = selectedFile;
		} else {
			fileToShow = currentDirectory;
		}
		pathField.setText(fileToShow == null ? "" : fileToShow.getAbsolutePath());
	}

	/**
	 * Navigates to the path entered in the path field. A directory is shown and
	 * selected, a (possibly not yet existing) file in an existing directory is
	 * selected, so it can be confirmed directly in save dialogs.
	 */
	private void navigateToEnteredPath() {
		String enteredPath = pathField.getText().trim();
		if (enteredPath.isEmpty()) {
			return;
		}
		if (enteredPath.equals("~") || enteredPath.startsWith("~/") || enteredPath.startsWith("~\\")) {
			enteredPath = System.getProperty("user.home") + enteredPath.substring(1);
		}

		final File enteredFile = new File(enteredPath).getAbsoluteFile();
		final File parentDirectory = enteredFile.getParentFile();
		if (enteredFile.isDirectory()) {
			if (getFileSelectionMode() == FILES_ONLY || parentDirectory == null) {
				setCurrentDirectory(enteredFile);
			} else {
				// Shows the directory within its parent and selects it, so it can be confirmed directly
				setCurrentDirectory(parentDirectory);
				setSelectedFile(enteredFile);
			}
		} else if (parentDirectory != null && parentDirectory.isDirectory() && getFileSelectionMode() != DIRECTORIES_ONLY) {
			setCurrentDirectory(parentDirectory);
			setSelectedFile(enteredFile);
		} else {
			UIManager.getLookAndFeel().provideErrorFeedback(pathField);
		}
	}

	/**
	 * Hides the combo box with the directory hierarchy and its label. The combo is
	 * recognized by its File items, the file filter combo contains FileFilters.
	 * Depends on the look and feel; if no such combo is found, nothing is changed.
	 */
	private static boolean hideDirectoryCombo(final Container container) {
		for (final Component component : container.getComponents()) {
			if (component instanceof final JComboBox<?> comboBox && comboBox.getItemCount() > 0 && comboBox.getItemAt(0) instanceof File) {
				comboBox.setVisible(false);
				for (final Component sibling : container.getComponents()) {
					if (sibling instanceof final JLabel label && label.getLabelFor() == comboBox) {
						label.setVisible(false);
					}
				}
				return true;
			} else if (component instanceof final Container childContainer && hideDirectoryCombo(childContainer)) {
				return true;
			}
		}
		return false;
	}
}
