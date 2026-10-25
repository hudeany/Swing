package de.soderer.swing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.collection.UniqueFifoQueuedList;

/**
 * Headless tests of the small helper classes and of the static helpers of the dialogs.
 */
@SuppressWarnings("static-method")
public class SwingHelpersTest {
	@TempDir
	public File tempDirectory;

	/**
	 * Runs code with captured System.out and System.err, so expected error output does not show up in the test log.
	 */
	private static String captureConsole(final Runnable runnable) {
		final PrintStream originalOut = System.out;
		final PrintStream originalErr = System.err;
		final ByteArrayOutputStream captured = new ByteArrayOutputStream();
		final PrintStream capturingStream = new PrintStream(captured, true, StandardCharsets.UTF_8);
		System.setOut(capturingStream);
		System.setErr(capturingStream);
		try {
			runnable.run();
		} finally {
			System.setOut(originalOut);
			System.setErr(originalErr);
		}
		return new String(captured.toByteArray(), StandardCharsets.UTF_8);
	}

	@Test
	public void testSwingColor() {
		assertEquals(new Color(255, 127, 127), SwingColor.LightRed);
		assertEquals(Color.yellow, SwingColor.Yellow);
		assertEquals(Color.green, SwingColor.Green);
	}

	@Test
	public void testRecentEntries() {
		assertEquals(-1, SwingDialogUtilities.getLastEntryIndex(null));
		final UniqueFifoQueuedList<String> recentEntries = new UniqueFifoQueuedList<>(3);
		assertEquals(-1, SwingDialogUtilities.getLastEntryIndex(recentEntries));
		recentEntries.addAll(Arrays.asList("a", "b", "c"));
		assertEquals(2, SwingDialogUtilities.getLastEntryIndex(recentEntries));

		SwingDialogUtilities.moveToEnd(recentEntries, "a");
		assertArrayEquals(new Object[] { "b", "c", "a" }, recentEntries.toArray());
		// A new entry pushes out the oldest one
		SwingDialogUtilities.moveToEnd(recentEntries, "d");
		assertArrayEquals(new Object[] { "c", "a", "d" }, recentEntries.toArray());
	}

	@Test
	public void testNearestExistingDirectory() throws Exception {
		assertNull(SwingDialogUtilities.getNearestExistingDirectory(null));
		assertNull(SwingDialogUtilities.getNearestExistingDirectory("  "));
		assertEquals(tempDirectory.getAbsoluteFile(), SwingDialogUtilities.getNearestExistingDirectory(tempDirectory.getAbsolutePath()));
		final File file = new File(tempDirectory, "file.txt");
		assertTrue(file.createNewFile());
		assertEquals(tempDirectory.getAbsoluteFile(), SwingDialogUtilities.getNearestExistingDirectory(file.getAbsolutePath()));
		assertEquals(tempDirectory.getAbsoluteFile(), SwingDialogUtilities.getNearestExistingDirectory(new File(tempDirectory, "a/b/c").getAbsolutePath()));
		assertEquals(new File(System.getProperty("user.home")).getAbsoluteFile(), SwingDialogUtilities.getNearestExistingDirectory("~"));
	}

	@Test
	public void testFileChoosersNeedDisplay() throws Exception {
		if (!GraphicsEnvironment.isHeadless()) {
			// Would show modal dialogs
			return;
		}
		final File file = new File(tempDirectory, "file.txt");
		assertTrue(file.createNewFile());
		final String directory = tempDirectory.getAbsolutePath();
		SwingTestSupport.onEdt(() -> {
			assertThrows(HeadlessException.class, () -> SwingDialogUtilities.chooseFileToOpen(null, "Open", directory, "txt", "csv"));
			assertThrows(HeadlessException.class, () -> SwingDialogUtilities.chooseFileToOpen(null, "Open", null));
			assertThrows(HeadlessException.class, () -> SwingDialogUtilities.chooseFileToSave(null, "Save", directory, "new.txt", "txt"));
			assertThrows(HeadlessException.class, () -> SwingDialogUtilities.chooseFileToSave(null, "Save", null, null));
			assertThrows(HeadlessException.class, () -> SwingDialogUtilities.chooseDirectory(null, "Directory", directory));
			assertThrows(HeadlessException.class, () -> SwingDialogUtilities.chooseDirectory(null, "Directory", File.listRoots()[0].getAbsolutePath()));
			assertThrows(HeadlessException.class, () -> SwingDialogUtilities.chooseDirectory(null, "Directory", ""));
		});
	}

	public static class ConstructorTarget {
		public static int instances = 0;

		public ConstructorTarget(final boolean booleanValue, final byte byteValue, final short shortValue, final int intValue, final long longValue, final float floatValue, final double doubleValue, final char charValue, final String stringValue) {
			assertFalse(booleanValue);
			assertEquals(0, byteValue + shortValue + intValue + longValue);
			assertEquals(0D, floatValue + doubleValue, 0D);
			assertEquals('x', charValue);
			assertNull(stringValue);
			instances++;
		}
	}

	public static class FailingConstructorTarget {
		public FailingConstructorTarget(final String value) {
			throw new IllegalArgumentException(value);
		}
	}

	public abstract static class AbstractConstructorTarget {
		public AbstractConstructorTarget() {
			// Cannot be instantiated
		}
	}

	public static final class NoPublicConstructorTarget {
		private NoPublicConstructorTarget() {
			// Not used
		}
	}

	@Test
	public void testForceConstructorInit() {
		final int instancesBefore = ConstructorTarget.instances;
		SwingUtilities.forceConstructorInit(ConstructorTarget.class);
		assertEquals(instancesBefore + 1, ConstructorTarget.instances);
		// Exceptions of the constructor are ignored
		SwingUtilities.forceConstructorInit(FailingConstructorTarget.class);
		SwingUtilities.forceConstructorInit(NoPublicConstructorTarget.class);
		final String output = captureConsole(() -> SwingUtilities.forceConstructorInit(AbstractConstructorTarget.class));
		assertTrue(output.contains("forceConstructorInit failed for class: " + AbstractConstructorTarget.class.getName()), output);

		// Components: without display only the class is loaded
		SwingUtilities.forceConstructorInit(JPanel.class);
		SwingUtilities.forceConstructorInit(DropDown.class);
	}

	@Test
	public void testSetAwtWmClassAndLookAndFeel() throws Exception {
		// Fails silently without the required "--add-opens" option or display
		captureConsole(() -> SwingUtilities.setAwtWmClass("TestApplication"));

		final LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		try {
			captureConsole(SwingUtilities::setSystemLookAndFeel);
			assertNotNull(UIManager.getLookAndFeel());
		} finally {
			UIManager.setLookAndFeel(lookAndFeel);
		}
	}

	@Test
	public void testReadOnlyTextSupport() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final JTextArea textArea = new JTextArea("Some text");
			textArea.setEditable(false);
			assertSame(textArea, ReadOnlyTextSupport.install(textArea));
			assertEquals("none", textArea.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)));
			assertFalse(textArea.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS).isEmpty());

			final JPopupMenu popupMenu = textArea.getComponentPopupMenu();
			assertEquals(2, popupMenu.getComponentCount());
			final JMenuItem copyItem = (JMenuItem) popupMenu.getComponent(0);
			final JMenuItem selectAllItem = (JMenuItem) popupMenu.getComponent(1);
			assertEquals(LangResources.get("copy"), copyItem.getText());
			assertEquals(LangResources.get("selectAll"), selectAllItem.getText());

			selectAllItem.doClick(0);
			assertEquals("Some text", textArea.getSelectedText());
			// Copies the selection via the text component's transfer handler
			copyItem.doClick(0);

			// The selection stays visible after focus loss
			textArea.getCaret().setSelectionVisible(false);
			for (final FocusListener listener : textArea.getFocusListeners()) {
				if (listener.getClass().getEnclosingClass() == ReadOnlyTextSupport.class) {
					listener.focusLost(new FocusEvent(textArea, FocusEvent.FOCUS_LOST));
				}
			}
			assertTrue(textArea.getCaret().isSelectionVisible());

			// Without selection the complete text is copied to the system clipboard
			textArea.select(0, 0);
			if (GraphicsEnvironment.isHeadless()) {
				// There is no system clipboard without display
				assertThrows(HeadlessException.class, () -> copyItem.doClick(0));
				assertThrows(HeadlessException.class, () -> ReadOnlyTextSupport.copyToClipboard(null, textArea));
			} else {
				ReadOnlyTextSupport.copy(textArea);
				assertTrue(ReadOnlyTextSupport.copyToClipboard(null, textArea));
			}
		});
	}

	@Test
	public void testProgressHelpers() {
		assertEquals(0, ProgressDialog.calculatePercentage(0, 5));
		assertEquals(0, ProgressDialog.calculatePercentage(-1, 5));
		assertEquals(50, ProgressDialog.calculatePercentage(10, 5));
		assertEquals(100, ProgressDialog.calculatePercentage(10, 10));

		assertEquals("", ProgressDialog.formatComment(null, "1", "2", "3"));
		assertEquals("1 of 2 (3%)", ProgressDialog.formatComment("{0} of {1} ({2}%)", "1", "2", "3"));
		assertEquals("no placeholders", ProgressDialog.formatComment("no placeholders", "1", "2", "3"));

		final JProgressBar progressBar = new JProgressBar(0, 100);
		progressBar.setIndeterminate(true);
		ProgressDialog.updateProgressBar(progressBar, LocalDateTime.now().minusSeconds(10), 100, 25);
		assertFalse(progressBar.isIndeterminate());
		assertEquals(25, progressBar.getValue());
		assertEquals("25%", progressBar.getString());
		assertTrue(progressBar.isStringPainted());
		assertTrue(progressBar.getToolTipText().startsWith("ETA: "));

		// Values are limited to 0..100
		ProgressDialog.updateProgressBar(progressBar, LocalDateTime.now().minusSeconds(10), 10, 20);
		assertEquals(100, progressBar.getValue());
		ProgressDialog.updateProgressBar(progressBar, LocalDateTime.now().minusSeconds(10), 10, -5);
		assertEquals(0, progressBar.getValue());
	}

	@Test
	public void testMailtoUri() {
		final URI uri = ErrorDialog.createMailtoUri(" support@example.com ", "Error in App (1.0)", "Line 1\nLine 2\r\nÄ & ?");
		assertEquals("mailto", uri.getScheme());
		final String text = uri.toString();
		assertTrue(text.startsWith("mailto:support@example.com?subject=Error%20in%20App%20%281.0%29&body="), text);
		assertTrue(text.endsWith("Line%201%0D%0ALine%202%0D%0A%C3%84%20%26%20%3F"), text);
		assertTrue(ErrorDialog.createMailtoUri("a@b.c", "s", null).toString().endsWith("&body="));

		// Long bodies are truncated with a note
		final StringBuilder longBody = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			longBody.append("Ü line ").append(i).append('\n');
		}
		final String truncated = ErrorDialog.createMailtoUri("a@b.c", "s", longBody.toString()).toString();
		assertTrue(truncated.length() <= 2000, String.valueOf(truncated.length()));
		assertTrue(truncated.endsWith("%28truncated%2C%20please%20copy%20the%20complete%20details%20from%20the%20error%20dialog%29"), truncated);
	}

	@Test
	public void testCredentialsWipe() throws Exception {
		final char[] password = "secret".toCharArray();
		SwingTestSupport.invokePrivate(null, CredentialsDialog.class, "wipe", new Class<?>[] { char[].class }, password);
		assertArrayEquals(new char[6], password);
		SwingTestSupport.invokePrivate(null, CredentialsDialog.class, "wipe", new Class<?>[] { char[].class }, (Object) null);
	}

	@Test
	public void testDialogsNeedDisplay() throws Exception {
		if (GraphicsEnvironment.isHeadless()) {
			SwingTestSupport.onEdt(() -> {
				assertThrows(HeadlessException.class, () -> new ErrorDialog(null, "App", "1.0", "support@example.com", new Exception("x")));
			});
		}
	}
}
