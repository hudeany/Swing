package de.soderer.swing.modern;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import de.soderer.utilities.LangResources;

/**
 * Helpers for read-only texts in the modern dialogs.
 */
final class TextSupport {
	/** Duration of the check mark shown on a copy button after copying */
	private static final int COPY_FEEDBACK_MILLIS = 1500;

	/** Utility class, no instances */
	private TextSupport() {
	}

	/**
	 * Makes a text area a read-only, selectable text in the dialog font: text can be copied with CTRL+C or a
	 * context menu, and the Enter key is passed on to the dialog's default button.
	 *
	 * @param <T>      type of the text area
	 * @param textArea text area to set up
	 * @return the same text area
	 */
	static <T extends JTextArea> T install(final T textArea) {
		textArea.setEditable(false);
		// Proportional dialog font instead of the monospaced JTextArea default of some look and feels
		textArea.setFont(ModernUi.baseFont().deriveFont(Font.PLAIN));
		textArea.setForeground(ModernUi.TEXT);
		// An unbound key is not consumed, so Enter reaches the default button of the root pane
		textArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");

		final JPopupMenu contextMenu = new JPopupMenu();
		final JMenuItem copyItem = new JMenuItem(LangResources.get("copyToClipboard"));
		copyItem.addActionListener(event -> {
			if (textArea.getSelectionStart() != textArea.getSelectionEnd()) {
				textArea.copy();
			} else {
				copyToClipboard(textArea.getText(), null);
			}
		});
		contextMenu.add(copyItem);
		textArea.setComponentPopupMenu(contextMenu);
		return textArea;
	}

	/**
	 * Creates a transparent, borderless and not focusable scroll pane with a read-only text, e.g. for the
	 * explanation above the input fields of a dialog. The size is limited, longer texts can be scrolled.
	 *
	 * @param text      text to show
	 * @param maxWidth  maximum preferred width
	 * @param maxHeight maximum preferred height
	 * @return the scroll pane containing the text
	 */
	static JScrollPane createTextScrollPane(final String text, final int maxWidth, final int maxHeight) {
		final JTextArea textArea = install(new JTextArea(text));
		// Not focusable: it would otherwise get the initial focus
		textArea.setFocusable(false);
		textArea.setOpaque(false);
		textArea.setBorder(BorderFactory.createEmptyBorder());

		final JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		ModernUi.styleScrollBars(scrollPane, ModernUi.CARD);
		scrollPane.setPreferredSize(new Dimension(Math.min(scrollPane.getPreferredSize().width, maxWidth), Math.min(scrollPane.getPreferredSize().height, maxHeight)));
		return scrollPane;
	}

	/**
	 * Copies a text into the system clipboard and briefly shows a check mark on the button that triggered it.
	 *
	 * @param text   text to copy
	 * @param button button to show the feedback on, may be null
	 */
	static void copyToClipboard(final String text, final Button button) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text == null ? "" : text), null);
		if (button != null) {
			final Icon originalIcon = button.getIcon();
			button.setIcon(new ThemeIcon(IconKind.CHECK, 15, ModernUi.SUCCESS));
			final Timer restoreTimer = new Timer(COPY_FEEDBACK_MILLIS, event -> button.setIcon(originalIcon));
			restoreTimer.setRepeats(false);
			restoreTimer.start();
		}
	}
}
