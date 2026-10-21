package de.soderer.swing;

import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import de.soderer.utilities.LangResources;

/**
 * Makes the text of read-only text components (e.g. messages in dialogs)
 * selectable and copyable:
 * <ul>
 * <li>Context menu with "Copy" (the selection, or the complete text if nothing
 * is selected) and "Select all"</li>
 * <li>ENTER is not consumed by the text component, so the default button of
 * the dialog still works while the text has the focus</li>
 * <li>TAB moves the focus to the next component instead of being consumed</li>
 * <li>The selection stays visible when the focus moves e.g. to a button</li>
 * </ul>
 * CTRL+C and CTRL+A work as usual for text components.
 */
public final class ReadOnlyTextSupport {
	private ReadOnlyTextSupport() {
		// Utility class
	}

	/**
	 * Installs the copy support on a read-only text component.
	 *
	 * @param textComponent
	 *            text component, should not be editable
	 * @return the given text component, for chaining
	 */
	public static <T extends JTextComponent> T install(final T textComponent) {
		final JPopupMenu popupMenu = new JPopupMenu();

		final JMenuItem copyItem = new JMenuItem(LangResources.get("copy"));
		copyItem.addActionListener(e -> copy(textComponent));
		popupMenu.add(copyItem);

		final JMenuItem selectAllItem = new JMenuItem(LangResources.get("selectAll"));
		selectAllItem.addActionListener(e -> {
			textComponent.requestFocusInWindow();
			textComponent.selectAll();
		});
		popupMenu.add(selectAllItem);

		textComponent.setComponentPopupMenu(popupMenu);

		// "none" removes the binding, so ENTER reaches the default button of the root pane
		textComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");

		// Null inherits the container's traversal keys (TAB / SHIFT+TAB), JTextArea would otherwise only react to CTRL+TAB
		textComponent.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
		textComponent.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

		// The caret hides the selection on focus loss, which is confusing when copying via the context menu or a button
		textComponent.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent event) {
				textComponent.getCaret().setSelectionVisible(true);
			}
		});

		return textComponent;
	}

	/**
	 * Copies the selected text, or the complete text if nothing is selected, to the system clipboard.
	 *
	 * @param textComponent
	 *            text component to copy from
	 */
	public static void copy(final JTextComponent textComponent) {
		if (textComponent.getSelectionStart() != textComponent.getSelectionEnd()) {
			textComponent.copy();
		} else {
			copyToClipboard(textComponent.getText(), textComponent);
		}
	}

	/**
	 * Copies a text to the system clipboard.
	 *
	 * @param text
	 *            text to copy, null for an empty text
	 * @param feedbackComponent
	 *            component for the error feedback if the clipboard is not available, may be null
	 * @return true if the text was copied
	 */
	public static boolean copyToClipboard(final String text, final java.awt.Component feedbackComponent) {
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text == null ? "" : text), null);
			return true;
		} catch (@SuppressWarnings("unused") final IllegalStateException e) {
			// Clipboard is currently used by another application
			UIManager.getLookAndFeel().provideErrorFeedback(feedbackComponent);
			return false;
		}
	}
}
