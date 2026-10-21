package de.soderer.swing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import de.soderer.utilities.LangResources;

/**
 * JFileChooser with an editable text field above the file list, which shows the
 * complete path of the current directory (or of the selected file/directory).
 * Entering a path and pressing ENTER navigates there. Optionally the directory
 * hierarchy combo box ("Look in" / "Save in") of the look and feel is hidden and
 * the navigation buttons (parent directory, new folder, view) are moved to the
 * right of the path field. A button (and the DELETE key) allows deleting an
 * empty folder, e.g. one created by mistake, after a confirmation.
 */
public class PathFieldFileChooser extends JFileChooser {
	private static final long serialVersionUID = -3624598101370315247L;

	/**
	 * UIManager keys (icon, tool tip) identifying the navigation buttons of the look and feel.
	 * Metal uses upFolder, homeFolder, newFolder, listView and detailsView, Windows uses upFolder, newFolder and viewMenu.
	 */
	private static final String[][] NAVIGATION_BUTTON_KEYS = {
			{ "FileChooser.upFolderIcon", "FileChooser.upFolderToolTipText" },
			{ "FileChooser.homeFolderIcon", "FileChooser.homeFolderToolTipText" },
			{ "FileChooser.newFolderIcon", "FileChooser.newFolderToolTipText" },
			{ "FileChooser.viewMenuIcon", "FileChooser.viewMenuButtonToolTipText" },
			{ "FileChooser.listViewIcon", "FileChooser.listViewButtonToolTipText" },
			{ "FileChooser.detailsViewIcon", "FileChooser.detailsViewButtonToolTipText" }
	};

	/** UIManager keys (icon, tool tip) of the new folder button, the delete folder button is placed behind it */
	private static final String[] NEW_FOLDER_BUTTON_KEYS = { "FileChooser.newFolderIcon", "FileChooser.newFolderToolTipText" };

	/** Action key of the DELETE key binding */
	private static final String DELETE_FOLDER_ACTION_KEY = "deleteSelectedFolder";

	/** Editable field above the file list, shows the complete path of the current directory or the selected file */
	private final JTextField pathField = new JTextField();

	/** Navigation buttons of the look and feel, which were removed from their original position */
	private final List<AbstractButton> navigationButtons = new ArrayList<>();

	/** Text of the label in front of the path field, null for no label */
	private String pathLabelText = null;

	/** Whether the directory hierarchy combo box of the look and feel is hidden when the dialog is created */
	private boolean hideDirectoryCombo = true;

	/** Whether the navigation buttons of the look and feel are moved to the right of the path field */
	private boolean moveNavigationButtons = true;

	/** Whether a button (and the DELETE key) for deleting the selected empty folder is offered */
	private boolean showDeleteFolderButton = true;

	/**
	 * Creates a file chooser showing the user's default directory.
	 */
	public PathFieldFileChooser() {
		this((File) null);
	}

	/**
	 * Creates a file chooser showing the given directory.
	 *
	 * @param currentDirectory
	 *            directory shown initially, null for the user's default directory
	 */
	public PathFieldFileChooser(final File currentDirectory) {
		super(currentDirectory);

		pathField.addActionListener(e -> navigateToEnteredPath());
		addPropertyChangeListener(JFileChooser.DIRECTORY_CHANGED_PROPERTY, e -> updatePathField());
		addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, e -> updatePathField());
		updatePathField();

		// DELETE key in the file list (text fields consume the key themselves)
		getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE_FOLDER_ACTION_KEY);
		getActionMap().put(DELETE_FOLDER_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 4118375318573150281L;

			@Override
			public void actionPerformed(final ActionEvent event) {
				if (showDeleteFolderButton) {
					deleteSelectedFolder();
				}
			}
		});
	}

	/**
	 * Sets the text of the label in front of the path field.
	 *
	 * @param newPathLabelText
	 *            text of the label, null for no label (default)
	 * @return this file chooser, for chaining
	 */
	public PathFieldFileChooser withPathLabelText(final String newPathLabelText) {
		pathLabelText = newPathLabelText;
		return this;
	}

	/**
	 * Sets whether the directory hierarchy combo box ("Look in" / "Save in") of the look and feel is hidden.
	 * Takes effect when the dialog is created, so it must be set before showing the dialog.
	 *
	 * @param newHideDirectoryCombo
	 *            true to hide the combo box (default), false to keep it
	 * @return this file chooser, for chaining
	 */
	public PathFieldFileChooser withDirectoryComboHidden(final boolean newHideDirectoryCombo) {
		hideDirectoryCombo = newHideDirectoryCombo;
		return this;
	}

	/**
	 * Sets whether the navigation buttons of the look and feel (parent directory, new folder, view, ...)
	 * are moved to the right of the path field.
	 * Takes effect when the dialog is created, so it must be set before showing the dialog.
	 *
	 * @param newMoveNavigationButtons
	 *            true to move the buttons (default), false to keep them at their original position
	 * @return this file chooser, for chaining
	 */
	public PathFieldFileChooser withNavigationButtonsMoved(final boolean newMoveNavigationButtons) {
		moveNavigationButtons = newMoveNavigationButtons;
		return this;
	}

	/**
	 * Sets whether a button (and the DELETE key) for deleting the selected empty folder is offered.
	 * Only empty folders are deleted, after a confirmation.
	 * The button takes effect when the dialog is created, so it must be set before showing the dialog.
	 *
	 * @param newShowDeleteFolderButton
	 *            true to offer the delete function (default), false to hide it
	 * @return this file chooser, for chaining
	 */
	public PathFieldFileChooser withDeleteFolderButton(final boolean newShowDeleteFolderButton) {
		showDeleteFolderButton = newShowDeleteFolderButton;
		return this;
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// The new UI creates new navigation buttons, the previously moved ones are obsolete.
		// Null check: updateUI() is called by the super constructor before the fields are initialized
		if (navigationButtons != null) {
			navigationButtons.clear();
		}
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

		if (hideDirectoryCombo) {
			hideDirectoryCombo(this);
		}

		final JToolBar buttonBar = new JToolBar();
		buttonBar.setFloatable(false);
		buttonBar.setRollover(true);
		buttonBar.setBorder(BorderFactory.createEmptyBorder());
		buttonBar.setOpaque(false);
		boolean deleteFolderButtonAdded = false;
		if (moveNavigationButtons) {
			// The buttons are detached only once, when the dialog is created again they are taken from the previous dialog
			if (navigationButtons.isEmpty()) {
				detachNavigationButtons();
			}
			for (final AbstractButton navigationButton : navigationButtons) {
				buttonBar.add(navigationButton);
				if (showDeleteFolderButton && matchesKeys(navigationButton, NEW_FOLDER_BUTTON_KEYS, getLocale())) {
					buttonBar.add(createDeleteFolderButton());
					deleteFolderButtonAdded = true;
				}
			}
		}
		if (showDeleteFolderButton && !deleteFolderButtonAdded) {
			buttonBar.add(createDeleteFolderButton());
		}
		if (buttonBar.getComponentCount() > 0) {
			pathPanel.add(buttonBar, BorderLayout.EAST);
		}

		// super.createDialog() puts the chooser into the CENTER of a BorderLayout content pane
		dialog.getContentPane().add(pathPanel, BorderLayout.NORTH);

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
	 * Removes the navigation buttons of the look and feel from their original
	 * containers and hides containers which became empty by this (e.g. the top
	 * panel of Metal or the tool bar of Windows, if the directory combo is hidden too).
	 * Depends on the look and feel; if no such buttons are found, nothing is changed.
	 */
	private void detachNavigationButtons() {
		collectNavigationButtons(this, navigationButtons);

		final Set<Container> formerParents = new LinkedHashSet<>();
		for (final AbstractButton navigationButton : navigationButtons) {
			final Container formerParent = navigationButton.getParent();
			if (formerParent != null) {
				formerParent.remove(navigationButton);
				formerParents.add(formerParent);
			}
		}
		for (final Container formerParent : formerParents) {
			hideEmptyContainers(formerParent);
		}
	}

	/**
	 * Collects the icon-only buttons recognized by the icons or tool tips of the look and feel's navigation buttons.
	 *
	 * @param container
	 *            container to search recursively
	 * @param foundButtons
	 *            list the found buttons are added to, in the order of the original layout
	 */
	private void collectNavigationButtons(final Container container, final List<AbstractButton> foundButtons) {
		for (final Component component : container.getComponents()) {
			if (component instanceof final AbstractButton button && isNavigationButton(button)) {
				foundButtons.add(button);
			} else if (component instanceof final Container childContainer) {
				collectNavigationButtons(childContainer, foundButtons);
			}
		}
	}

	/**
	 * Checks whether a button is one of the look and feel's navigation buttons.
	 *
	 * @param button
	 *            button to check
	 * @return true if the button is a navigation button
	 */
	private boolean isNavigationButton(final AbstractButton button) {
		for (final String[] keys : NAVIGATION_BUTTON_KEYS) {
			if (matchesKeys(button, keys, getLocale())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether an icon-only button matches the given UIManager icon or tool tip.
	 * Icons are compared first, tool tips are a fallback for look and feels creating new icon instances on each lookup.
	 *
	 * @param button
	 *            button to check
	 * @param keys
	 *            UIManager key of the icon and of the tool tip
	 * @param locale
	 *            locale for the tool tip lookup
	 * @return true if the button matches
	 */
	private static boolean matchesKeys(final AbstractButton button, final String[] keys, final Locale locale) {
		if (button.getText() != null && !button.getText().isEmpty()) {
			return false;
		}
		final Icon buttonIcon = button.getIcon();
		if (buttonIcon != null && buttonIcon.equals(UIManager.getIcon(keys[0], locale))) {
			return true;
		}
		final String buttonToolTip = button.getToolTipText();
		return buttonToolTip != null && buttonToolTip.equals(UIManager.getString(keys[1], locale));
	}

	/**
	 * Hides the given container and its ancestors (up to this file chooser) as long as they contain no visible content.
	 *
	 * @param container
	 *            container to start with
	 */
	private void hideEmptyContainers(final Container container) {
		Container currentContainer = container;
		while (currentContainer != null && currentContainer != this && !hasVisibleContent(currentContainer)) {
			currentContainer.setVisible(false);
			currentContainer = currentContainer.getParent();
		}
	}

	/**
	 * Checks whether a container has visible children other than spacers and separators.
	 *
	 * @param container
	 *            container to check
	 * @return true if visible content exists
	 */
	private static boolean hasVisibleContent(final Container container) {
		for (final Component child : container.getComponents()) {
			if (child.isVisible() && !(child instanceof Box.Filler) && !(child instanceof JSeparator)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates the button for deleting the selected empty folder, with an icon sized like the new folder icon.
	 *
	 * @return the new button
	 */
	private JButton createDeleteFolderButton() {
		final Icon newFolderIcon = UIManager.getIcon("FileChooser.newFolderIcon", getLocale());
		final int iconSize = newFolderIcon == null ? 16 : Math.max(newFolderIcon.getIconWidth(), newFolderIcon.getIconHeight());
		final String toolTipText = LangResources.get("deleteFolder");
		final JButton deleteFolderButton = new JButton(new DeleteIcon(iconSize));
		deleteFolderButton.setToolTipText(toolTipText);
		deleteFolderButton.getAccessibleContext().setAccessibleName(toolTipText);
		deleteFolderButton.setFocusable(false);
		deleteFolderButton.addActionListener(e -> deleteSelectedFolder());
		return deleteFolderButton;
	}

	/**
	 * Deletes the folder selected in the file view after a confirmation. Only empty folders are deleted.
	 */
	private void deleteSelectedFolder() {
		final File folder = getFolderSelectedInView();
		final String title = LangResources.get("deleteFolder");
		if (folder == null) {
			JOptionPane.showMessageDialog(this, LangResources.get("noFolderSelected"), title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		final Object[] options = { LangResources.get("yes"), LangResources.get("no") };
		// "No" is the default option, so an accidental ENTER does not delete anything
		final int answer = JOptionPane.showOptionDialog(this, LangResources.get("reallyDeleteFolder", folder.getAbsolutePath()), title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		if (answer != 0) {
			return;
		}

		try {
			// Files.delete() only deletes empty directories, so no content can be lost
			Files.delete(folder.toPath());
		} catch (@SuppressWarnings("unused") final DirectoryNotEmptyException e) {
			JOptionPane.showMessageDialog(this, LangResources.get("folderNotEmpty", folder.getAbsolutePath()), title, JOptionPane.WARNING_MESSAGE);
			return;
		} catch (final IOException | SecurityException e) {
			JOptionPane.showMessageDialog(this, LangResources.get("folderCouldNotBeDeleted", folder.getAbsolutePath(), e.getMessage()), LangResources.get("error"), JOptionPane.ERROR_MESSAGE);
			return;
		}

		final File currentDirectory = getCurrentDirectory();
		if (currentDirectory != null && currentDirectory.getAbsoluteFile().equals(folder.getAbsoluteFile())) {
			setCurrentDirectory(folder.getAbsoluteFile().getParentFile());
		}
		final File selectedFile = getSelectedFile();
		if (selectedFile != null && selectedFile.getAbsoluteFile().equals(folder.getAbsoluteFile())) {
			setSelectedFile(null);
		}
		rescanCurrentDirectory();
	}

	/**
	 * Determines the folder selected in the file view. The view selection is used because in FILES_ONLY
	 * mode a selected directory is not reported as selected file of the chooser.
	 *
	 * @return the selected folder, null if no folder is selected
	 */
	private File getFolderSelectedInView() {
		final File fileInView = findSelectedFileInView(this);
		final File candidate = fileInView != null ? fileInView : getSelectedFile();
		return candidate != null && candidate.isDirectory() ? candidate : null;
	}

	/**
	 * Searches the visible list or details table of the look and feel for a selected File.
	 *
	 * @param container
	 *            container to search recursively
	 * @return the selected file, null if none is found
	 */
	private static File findSelectedFileInView(final Container container) {
		for (final Component component : container.getComponents()) {
			if (component instanceof final JList<?> list) {
				if (list.isShowing() && list.getSelectedValue() instanceof final File file) {
					return file;
				}
			} else if (component instanceof final JTable table) {
				final int selectedRow = table.getSelectedRow();
				if (table.isShowing() && selectedRow >= 0 && table.getModel().getValueAt(table.convertRowIndexToModel(selectedRow), 0) instanceof final File file) {
					return file;
				}
			} else if (component instanceof final Container childContainer) {
				final File file = findSelectedFileInView(childContainer);
				if (file != null) {
					return file;
				}
			}
		}
		return null;
	}

	/**
	 * Hides the combo box with the directory hierarchy and its label. The combo is
	 * recognized by its File items, the file filter combo contains FileFilters.
	 * Depends on the look and feel; if no such combo is found, nothing is changed.
	 *
	 * @param container
	 *            container to search recursively
	 * @return true if the combo box was found and hidden
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

	/**
	 * Simple red cross icon for the delete folder button, grayed out when the button is disabled.
	 */
	private static final class DeleteIcon implements Icon {
		private final int size;

		private DeleteIcon(final int size) {
			this.size = size;
		}

		@Override
		public void paintIcon(final Component component, final Graphics graphics, final int x, final int y) {
			final Graphics2D graphics2D = (Graphics2D) graphics.create();
			try {
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				final Color disabledColor = UIManager.getColor("Label.disabledForeground");
				graphics2D.setColor(component.isEnabled() || disabledColor == null ? new Color(0xC0, 0x30, 0x30) : disabledColor);
				graphics2D.setStroke(new BasicStroke(Math.max(2f, size / 8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				final int margin = size / 4;
				graphics2D.drawLine(x + margin, y + margin, x + size - margin - 1, y + size - margin - 1);
				graphics2D.drawLine(x + size - margin - 1, y + margin, x + margin, y + size - margin - 1);
			} finally {
				graphics2D.dispose();
			}
		}

		@Override
		public int getIconWidth() {
			return size;
		}

		@Override
		public int getIconHeight() {
			return size;
		}
	}
}
