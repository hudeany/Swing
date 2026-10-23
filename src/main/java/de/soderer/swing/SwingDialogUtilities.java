package de.soderer.swing;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.collection.UniqueFifoQueuedList;

/**
 * Small GUI helpers shared by the main window and its actions: file choosers,
 * recent lists and popup menu items
 */
public final class SwingDialogUtilities {
	private SwingDialogUtilities() {
		// Utility class, no instances
	}

	/**
	 * Index of the last entry of a recent list. The latest used entry is always
	 * moved to the end (see {@link #moveToEnd(UniqueFifoQueuedList, String)}), so
	 * this entry is preselected in the ComboSelectionDialog.
	 *
	 * @param recentEntries the list of recent entries, or {@code null}
	 * @return index of the last entry, or -1 if the list is empty or {@code null}
	 */
	public static int getLastEntryIndex(final UniqueFifoQueuedList<String> recentEntries) {
		return recentEntries == null ? -1 : recentEntries.size() - 1;
	}

	/**
	 * Moves an entry of a recent list to its end (or appends it, if it is new),
	 * so it counts as latest used and is preselected next time.
	 * The list is rebuilt explicitly, because adding an already contained entry
	 * to the unique list does not necessarily change its position. If the list
	 * is full, a new entry pushes out the first (oldest) entry.
	 *
	 * @param recentEntries the list of recent entries to update
	 * @param entry the entry to move to the end
	 */
	public static void moveToEnd(final UniqueFifoQueuedList<String> recentEntries, final String entry) {
		final List<String> entries = new ArrayList<>(recentEntries);
		entries.remove(entry);
		entries.add(entry);
		recentEntries.clear();
		recentEntries.addAll(entries);
	}

	/**
	 * The given path itself if it is an existing directory, otherwise its nearest
	 * existing parent directory. Handles file paths, paths that do not exist
	 * anymore and paths starting with "~".
	 *
	 * @param path the path to check
	 * @return the directory, or null if the path is blank or no parent exists
	 */
	public static File getNearestExistingDirectory(final String path) {
		if (Utilities.isBlank(path)) {
			return null;
		}
		File directory = new File(Utilities.replaceUsersHome(path)).getAbsoluteFile();
		while (directory != null && !directory.isDirectory()) {
			directory = directory.getParentFile();
		}
		return directory;
	}

	/**
	 * Shows a file chooser to open a file.
	 *
	 * @param parent the component over which the file chooser is shown
	 * @param title the dialog title
	 * @param initialDirectory the initial directory path
	 * @param fileExtensions optional extension filters (without dot), e.g. "xlsx"
	 * @return the selected file or null if canceled
	 */
	public static File chooseFileToOpen(final Component parent, final String title, final String initialDirectory, final String... fileExtensions) {
		final JFileChooser fileChooser = createFileChooser(title, fileExtensions);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		final File directory = getNearestExistingDirectory(initialDirectory);
		if (directory != null) {
			fileChooser.setCurrentDirectory(directory);
		}
		return fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
	}

	/**
	 * Shows a file chooser to select a file to save to.
	 *
	 * @param parent the component over which the file chooser is shown
	 * @param title the dialog title
	 * @param initialDirectory the initial directory path
	 * @param proposedFileName the proposed file name
	 * @param fileExtensions optional extension filters (without dot), e.g. "xlsx"
	 * @return the selected file or null if canceled
	 */
	public static File chooseFileToSave(final Component parent, final String title, final String initialDirectory, final String proposedFileName, final String... fileExtensions) {
		final JFileChooser fileChooser = createFileChooser(title, fileExtensions);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		final File directory = getNearestExistingDirectory(initialDirectory);
		if (directory != null) {
			fileChooser.setCurrentDirectory(directory);
		}
		if (Utilities.isNotBlank(proposedFileName)) {
			// Also shows the complete path of the proposed file in the path field
			fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), proposedFileName));
		}
		return fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
	}

	/**
	 * Shows a file chooser to select a directory. The initial directory is shown
	 * within its parent directory and is already selected, so choosing the same
	 * directory again only needs a click on the approve button.
	 *
	 * @param parent the component over which the file chooser is shown
	 * @param title the dialog title
	 * @param initialDirectory the initial directory path
	 * @return the selected directory or null if canceled
	 */
	public static File chooseDirectory(final Component parent, final String title, final String initialDirectory) {
		final JFileChooser fileChooser = createFileChooser(title);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		final File directory = getNearestExistingDirectory(initialDirectory);
		if (directory != null) {
			if (directory.getParentFile() != null) {
				fileChooser.setCurrentDirectory(directory.getParentFile());
				fileChooser.setSelectedFile(directory);
			} else {
				// A root directory has no parent to be shown in
				fileChooser.setCurrentDirectory(directory);
			}
		}
		return fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
	}

	/**
	 * Creates a file chooser that shows the complete path in an editable field
	 * instead of the directory hierarchy combo box.
	 *
	 * @param title the dialog title
	 * @param fileExtensions optional extension filters (without dot), e.g. "xlsx"
	 * @return the configured file chooser
	 */
	private static JFileChooser createFileChooser(final String title, final String... fileExtensions) {
		final JFileChooser fileChooser = new PathFieldFileChooser().withPathLabelText(LangResources.get("path"));
		fileChooser.setDialogTitle(title);

		if (fileExtensions != null && fileExtensions.length > 0) {
			final FileNameExtensionFilter filter = new FileNameExtensionFilter("*." + String.join(", *.", fileExtensions), fileExtensions);
			fileChooser.addChoosableFileFilter(filter);
			fileChooser.setFileFilter(filter);
		}

		return fileChooser;
	}
}
