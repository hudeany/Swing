package de.soderer.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.soderer.utilities.LangResources;

/**
 * Headless tests of {@link PathFieldFileChooser}. Its dialog cannot be created
 * without a display, so the helpers used by the dialog are called directly.
 */
@SuppressWarnings("static-method")
public class PathFieldFileChooserTest {
	@TempDir
	public File tempDirectory;

	private static JTextField pathField(final PathFieldFileChooser chooser) throws Exception {
		return (JTextField) SwingTestSupport.getPrivateField(chooser, PathFieldFileChooser.class, "pathField");
	}

	private static void enterPath(final PathFieldFileChooser chooser, final String path) throws Exception {
		final JTextField pathField = pathField(chooser);
		pathField.setText(path);
		pathField.postActionEvent();
	}

	private static Object invoke(final PathFieldFileChooser chooser, final String methodName) throws Exception {
		return SwingTestSupport.invokePrivate(chooser, PathFieldFileChooser.class, methodName, new Class<?>[0]);
	}

	private static Object invokeStatic(final String methodName, final Class<?>[] parameterTypes, final Object... parameters) throws Exception {
		return SwingTestSupport.invokePrivate(null, PathFieldFileChooser.class, methodName, parameterTypes, parameters);
	}

	private static List<Component> findAll(final Container container, final Class<?> type) {
		final List<Component> result = new ArrayList<>();
		for (final Component component : container.getComponents()) {
			if (type.isInstance(component)) {
				result.add(component);
			}
			if (component instanceof Container) {
				result.addAll(findAll((Container) component, type));
			}
		}
		return result;
	}

	@Test
	public void testPathFieldFollowsDirectoryAndSelection() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final File file = new File(tempDirectory, "file.txt");
			assertTrue(file.createNewFile());
			final PathFieldFileChooser chooser = new PathFieldFileChooser(tempDirectory);
			assertEquals(tempDirectory.getAbsolutePath(), pathField(chooser).getText());

			chooser.setSelectedFile(file);
			assertEquals(file.getAbsolutePath(), pathField(chooser).getText());
			// A selected file outside the current directory is not shown
			chooser.setCurrentDirectory(tempDirectory.getParentFile());
			assertEquals(file, chooser.getSelectedFile());
			assertEquals(tempDirectory.getParentFile().getAbsolutePath(), pathField(chooser).getText());

			assertNotNull(new PathFieldFileChooser().getCurrentDirectory());
		});
	}

	@Test
	public void testFluentSetters() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final PathFieldFileChooser chooser = new PathFieldFileChooser();
			assertSame(chooser, chooser.withPathLabelText("Path"));
			assertSame(chooser, chooser.withDirectoryComboHidden(false));
			assertSame(chooser, chooser.withNavigationButtonsMoved(false));
			assertSame(chooser, chooser.withDeleteFolderButton(false));
			assertEquals("Path", SwingTestSupport.getPrivateField(chooser, PathFieldFileChooser.class, "pathLabelText"));
			assertEquals(Boolean.FALSE, SwingTestSupport.getPrivateField(chooser, PathFieldFileChooser.class, "hideDirectoryCombo"));
			assertEquals(Boolean.FALSE, SwingTestSupport.getPrivateField(chooser, PathFieldFileChooser.class, "moveNavigationButtons"));
			assertEquals(Boolean.FALSE, SwingTestSupport.getPrivateField(chooser, PathFieldFileChooser.class, "showDeleteFolderButton"));
		});
	}

	@Test
	public void testNavigateToEnteredPath() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final File subDirectory = new File(tempDirectory, "sub");
			assertTrue(subDirectory.mkdir());
			final File file = new File(subDirectory, "file.txt");
			assertTrue(file.createNewFile());
			final PathFieldFileChooser chooser = new PathFieldFileChooser(tempDirectory);

			// Blank input is ignored
			enterPath(chooser, "  ");
			assertEquals(tempDirectory.getAbsoluteFile(), chooser.getCurrentDirectory().getAbsoluteFile());

			// FILES_ONLY: a directory is opened
			enterPath(chooser, subDirectory.getAbsolutePath());
			assertEquals(subDirectory.getAbsoluteFile(), chooser.getCurrentDirectory().getAbsoluteFile());

			// A file is selected in its parent directory
			enterPath(chooser, file.getAbsolutePath());
			assertEquals(file.getAbsoluteFile(), chooser.getSelectedFile().getAbsoluteFile());

			// A not existing file in an existing directory can be entered, e.g. for saving
			final File newFile = new File(tempDirectory, "new.txt");
			enterPath(chooser, newFile.getAbsolutePath());
			assertEquals(newFile.getAbsoluteFile(), chooser.getSelectedFile().getAbsoluteFile());

			// Not existing parent: only error feedback
			enterPath(chooser, new File(tempDirectory, "missing/new.txt").getAbsolutePath());
			assertEquals(newFile.getAbsoluteFile(), chooser.getSelectedFile().getAbsoluteFile());

			// Directory selection: a directory is selected within its parent, files are rejected
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			enterPath(chooser, subDirectory.getAbsolutePath());
			assertEquals(tempDirectory.getAbsoluteFile(), chooser.getCurrentDirectory().getAbsoluteFile());
			assertEquals(subDirectory.getAbsoluteFile(), chooser.getSelectedFile().getAbsoluteFile());
			enterPath(chooser, file.getAbsolutePath());
			assertEquals(subDirectory.getAbsoluteFile(), chooser.getSelectedFile().getAbsoluteFile());

			// A root directory has no parent
			final File root = File.listRoots()[0];
			enterPath(chooser, root.getAbsolutePath());
			assertEquals(root.getAbsoluteFile(), chooser.getCurrentDirectory().getAbsoluteFile());

			// "~" is the user's home
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			enterPath(chooser, "~");
			assertEquals(new File(System.getProperty("user.home")).getAbsoluteFile(), chooser.getCurrentDirectory().getAbsoluteFile());
			enterPath(chooser, "~" + File.separator + "surely-not-existing-subdirectory" + File.separator + "x");
			assertEquals(new File(System.getProperty("user.home")).getAbsoluteFile(), chooser.getCurrentDirectory().getAbsoluteFile());
		});
	}

	@Test
	public void testNavigationButtonsAreDetached() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final PathFieldFileChooser chooser = new PathFieldFileChooser();
			final int buttonCountBefore = findAll(chooser, AbstractButton.class).size();
			invoke(chooser, "detachNavigationButtons");
			@SuppressWarnings("unchecked")
			final List<AbstractButton> navigationButtons = (List<AbstractButton>) SwingTestSupport.getPrivateField(chooser, PathFieldFileChooser.class, "navigationButtons");
			assertFalse(navigationButtons.isEmpty());
			assertEquals(buttonCountBefore - navigationButtons.size(), findAll(chooser, AbstractButton.class).size());
			for (final AbstractButton navigationButton : navigationButtons) {
				assertNull(navigationButton.getParent());
			}
			// A new UI creates new buttons, the detached ones are forgotten
			chooser.updateUI();
			assertTrue(navigationButtons.isEmpty());
		});
	}

	@Test
	public void testDirectoryComboIsHidden() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final PathFieldFileChooser chooser = new PathFieldFileChooser();
			assertEquals(Boolean.TRUE, invokeStatic("hideDirectoryCombo", new Class<?>[] { Container.class }, chooser));
			boolean hiddenComboFound = false;
			for (final Component component : findAll(chooser, JComboBox.class)) {
				final JComboBox<?> comboBox = (JComboBox<?>) component;
				if (comboBox.getItemCount() > 0 && comboBox.getItemAt(0) instanceof File) {
					assertFalse(comboBox.isVisible());
					hiddenComboFound = true;
				}
			}
			assertTrue(hiddenComboFound);

			// Containers without such a combo box
			final JPanel panel = new JPanel();
			panel.add(new JComboBox<>(new String[] { "x" }));
			panel.add(new JComboBox<>());
			panel.add(new JPanel());
			assertEquals(Boolean.FALSE, invokeStatic("hideDirectoryCombo", new Class<?>[] { Container.class }, panel));
		});
	}

	@Test
	public void testDirectoryComboLabelIsHidden() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final JPanel panel = new JPanel();
			final JComboBox<File> comboBox = new JComboBox<>(new File[] { new File("x") });
			final JLabel label = new JLabel("Look in:");
			label.setLabelFor(comboBox);
			final JLabel otherLabel = new JLabel("Other");
			panel.add(otherLabel);
			panel.add(label);
			panel.add(comboBox);
			assertEquals(Boolean.TRUE, invokeStatic("hideDirectoryCombo", new Class<?>[] { Container.class }, panel));
			assertFalse(comboBox.isVisible());
			assertFalse(label.isVisible());
			assertTrue(otherLabel.isVisible());
		});
	}

	@Test
	public void testMatchesKeysAndVisibleContent() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final Class<?>[] matchesKeysTypes = new Class<?>[] { AbstractButton.class, String[].class, Locale.class };
			final String[] keys = { "FileChooser.upFolderIcon", "FileChooser.upFolderToolTipText" };
			final Locale locale = Locale.getDefault();

			assertEquals(Boolean.FALSE, invokeStatic("matchesKeys", matchesKeysTypes, new JButton("Text"), keys, locale));
			assertEquals(Boolean.FALSE, invokeStatic("matchesKeys", matchesKeysTypes, new JButton(), keys, locale));
			final JButton iconButton = new JButton(UIManager.getIcon("FileChooser.upFolderIcon", locale));
			assertEquals(Boolean.TRUE, invokeStatic("matchesKeys", matchesKeysTypes, iconButton, keys, locale));
			final JButton toolTipButton = new JButton(UIManager.getIcon("FileChooser.homeFolderIcon", locale));
			toolTipButton.setText("");
			toolTipButton.setToolTipText(UIManager.getString("FileChooser.upFolderToolTipText", locale));
			assertEquals(Boolean.TRUE, invokeStatic("matchesKeys", matchesKeysTypes, toolTipButton, keys, locale));

			final Class<?>[] containerType = new Class<?>[] { Container.class };
			final JPanel panel = new JPanel();
			assertEquals(Boolean.FALSE, invokeStatic("hasVisibleContent", containerType, panel));
			panel.add(Box.createHorizontalStrut(5));
			panel.add(new JSeparator());
			final JButton hiddenButton = new JButton();
			hiddenButton.setVisible(false);
			panel.add(hiddenButton);
			assertEquals(Boolean.FALSE, invokeStatic("hasVisibleContent", containerType, panel));
			panel.add(new JButton());
			assertEquals(Boolean.TRUE, invokeStatic("hasVisibleContent", containerType, panel));
		});
	}

	@Test
	public void testEmptyContainersAreHidden() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final PathFieldFileChooser chooser = new PathFieldFileChooser();
			final JPanel outer = new JPanel();
			final JPanel inner = new JPanel();
			outer.add(inner);
			outer.add(new JButton("visible"));
			chooser.add(outer);
			SwingTestSupport.invokePrivate(chooser, PathFieldFileChooser.class, "hideEmptyContainers", new Class<?>[] { Container.class }, inner);
			assertFalse(inner.isVisible());
			assertTrue(outer.isVisible());
			assertTrue(chooser.isVisible());
		});
	}

	@Test
	public void testFindSelectedFileInView() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final Class<?>[] containerType = new Class<?>[] { Container.class };
			final File file = new File(tempDirectory, "x");

			final JList<Object> hiddenList = new JList<>(new Object[] { file });
			hiddenList.setSelectedIndex(0);
			final JPanel panel = new JPanel();
			panel.add(hiddenList);
			// Not showing
			assertNull(invokeStatic("findSelectedFileInView", containerType, panel));

			final JList<Object> list = new JList<Object>(new Object[] { "no file", file }) {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean isShowing() {
					return true;
				}
			};
			final JPanel nested = new JPanel();
			nested.add(list);
			panel.add(nested);
			list.setSelectedIndex(0);
			assertNull(invokeStatic("findSelectedFileInView", containerType, panel));
			list.setSelectedIndex(1);
			assertEquals(file, invokeStatic("findSelectedFileInView", containerType, panel));

			final DefaultTableModel model = new DefaultTableModel(new Object[][] { { "no file" }, { file } }, new Object[] { "Name" });
			final JTable table = new JTable(model) {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean isShowing() {
					return true;
				}
			};
			final JPanel tablePanel = new JPanel();
			tablePanel.add(table);
			assertNull(invokeStatic("findSelectedFileInView", containerType, tablePanel));
			table.setRowSelectionInterval(0, 0);
			assertNull(invokeStatic("findSelectedFileInView", containerType, tablePanel));
			table.setRowSelectionInterval(1, 1);
			assertEquals(file, invokeStatic("findSelectedFileInView", containerType, tablePanel));
			final JTable hiddenTable = new JTable(model);
			hiddenTable.setRowSelectionInterval(1, 1);
			final JPanel hiddenTablePanel = new JPanel();
			hiddenTablePanel.add(hiddenTable);
			assertNull(invokeStatic("findSelectedFileInView", containerType, hiddenTablePanel));
		});
	}

	@Test
	public void testFolderSelectedInView() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final File file = new File(tempDirectory, "file.txt");
			assertTrue(file.createNewFile());
			final PathFieldFileChooser chooser = new PathFieldFileChooser(tempDirectory);
			assertNull(invoke(chooser, "getFolderSelectedInView"));
			chooser.setSelectedFile(file);
			assertNull(invoke(chooser, "getFolderSelectedInView"));
			chooser.setSelectedFile(tempDirectory);
			assertEquals(tempDirectory, invoke(chooser, "getFolderSelectedInView"));
		});
	}

	@Test
	public void testDeleteFolderButtonAndIcon() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final PathFieldFileChooser chooser = new PathFieldFileChooser();
			final JButton deleteButton = (JButton) invoke(chooser, "createDeleteFolderButton");
			assertEquals(LangResources.get("deleteFolder"), deleteButton.getToolTipText());
			assertFalse(deleteButton.isFocusable());
			final Icon icon = deleteButton.getIcon();
			assertTrue(icon.getIconWidth() > 0);
			assertEquals(icon.getIconWidth(), icon.getIconHeight());

			final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
			final Graphics2D graphics = image.createGraphics();
			try {
				icon.paintIcon(deleteButton, graphics, 0, 0);
				assertTrue(hasPaintedPixels(image));
				deleteButton.setEnabled(false);
				icon.paintIcon(deleteButton, graphics, 0, 0);
				// Without a disabled color the enabled color is used
				SwingTestSupport.withoutUiDefaults(() -> icon.paintIcon(deleteButton, graphics, 0, 0), "Label.disabledForeground");
			} finally {
				graphics.dispose();
			}

			// Without a new folder icon the default size is used
			SwingTestSupport.withoutUiDefaults(() -> assertEquals(16, ((JButton) invoke(chooser, "createDeleteFolderButton")).getIcon().getIconWidth()), "FileChooser.newFolderIcon");

			if (GraphicsEnvironment.isHeadless()) {
				// The confirmation needs a dialog
				deleteButton.setEnabled(true);
				assertThrows(HeadlessException.class, () -> deleteButton.doClick(0));
			}
		});
	}

	private static boolean hasPaintedPixels(final BufferedImage image) {
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				if (image.getRGB(x, y) != 0) {
					return true;
				}
			}
		}
		return false;
	}

	@Test
	public void testDeleteKeyAction() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final PathFieldFileChooser chooser = new PathFieldFileChooser(tempDirectory);
			final ActionEvent event = new ActionEvent(chooser, ActionEvent.ACTION_PERFORMED, "delete");
			if (GraphicsEnvironment.isHeadless()) {
				// Messages and confirmation need a dialog
				assertThrows(HeadlessException.class, () -> chooser.getActionMap().get("deleteSelectedFolder").actionPerformed(event));
				chooser.setSelectedFile(tempDirectory);
				assertThrows(HeadlessException.class, () -> chooser.getActionMap().get("deleteSelectedFolder").actionPerformed(event));
			}
			// Disabled delete function: nothing happens
			chooser.withDeleteFolderButton(false);
			chooser.getActionMap().get("deleteSelectedFolder").actionPerformed(event);
			assertTrue(tempDirectory.isDirectory());
		});
	}

	@Test
	public void testDialogNeedsDisplay() throws Exception {
		if (GraphicsEnvironment.isHeadless()) {
			SwingTestSupport.onEdt(() -> {
				final PathFieldFileChooser chooser = new PathFieldFileChooser().withPathLabelText("Path");
				assertThrows(HeadlessException.class, () -> chooser.showOpenDialog(null));
				assertNotNull(chooser.getPreferredSize());
				assertTrue(chooser.getPreferredSize().width > new Dimension(0, 0).width);
			});
		}
	}
}
