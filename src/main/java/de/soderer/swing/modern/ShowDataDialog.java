package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.soderer.swing.modern.ModernUi.Button;
import de.soderer.utilities.LangResources;

/**
 * Modal dialog in the {@link ModernUi} design showing a (possibly long) read-only text, e.g. a report, with
 * buttons to copy the text to the clipboard and to close the dialog. The text can also be selected and copied
 * partially (CTRL+C or context menu). Same API as {@link de.soderer.swing.ShowDataDialog}.
 */
public class ShowDataDialog extends ModalDialog<Boolean> {
	/** Serialization version */
	private static final long serialVersionUID = -6620913548730185529L;

	/** Maximum initial width of the text area */
	private static final int DEFAULT_MAX_WIDTH = 800;
	/** Maximum initial height of the text area */
	private static final int DEFAULT_MAX_HEIGHT = 500;
	/** Minimum width of the text area */
	private static final int MIN_WIDTH = 400;
	/** Minimum height of the text area */
	private static final int MIN_HEIGHT = 200;

	/** Read-only text area showing the text */
	private final JTextArea textArea;

	/**
	 * Creates a dialog showing a text.
	 *
	 * @param parent owner window, may be null
	 * @param title  title of the dialog
	 * @param text   text to show, may be null for an empty text
	 */
	public ShowDataDialog(final Window parent, final String title, final String text) {
		super(parent, title, IconKind.LOG);

		textArea = TextSupport.install(new JTextArea(text == null ? "" : text));
		textArea.setCaretPosition(0);
		textArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		textArea.setBackground(ModernUi.READ_ONLY_BACKGROUND);

		final JScrollPane textScrollPane = new JScrollPane(textArea);
		ModernUi.styleScrollPane(textScrollPane, ModernUi.READ_ONLY_BACKGROUND);
		getContentArea().add(textScrollPane, BorderLayout.CENTER);

		// Always copies the complete text, a partial selection is copied via CTRL+C or the context menu
		final Button copyButton = addLeftButton(LangResources.get("copyToClipboard"), IconKind.COPY, ButtonStyle.SECONDARY, null);
		copyButton.addActionListener(event -> TextSupport.copyToClipboard(textArea.getText(), copyButton));

		final JButton closeButton = addButton(LangResources.get("close"), null, ButtonStyle.PRIMARY, event -> {
			returnValue = true;
			dispose();
		});
		getRootPane().setDefaultButton(closeButton);

		setResizable(false);

		// Size to the text, but never larger than a sensible maximum (long reports scroll instead)
		final Dimension preferredTextSize = textScrollPane.getPreferredSize();
		textScrollPane.setPreferredSize(new Dimension(
			Math.max(MIN_WIDTH, Math.min(preferredTextSize.width + 20, DEFAULT_MAX_WIDTH)),
			Math.max(MIN_HEIGHT, Math.min(preferredTextSize.height + 20, DEFAULT_MAX_HEIGHT))));

		pack();

		setLocationRelativeTo(parent);
	}

	/**
	 * Allows or forbids the user to resize the dialog (e.g. for long reports).
	 *
	 * @param resizable true to make the dialog resizable
	 * @return this dialog for chaining
	 */
	public ShowDataDialog withResizable(final boolean resizable) {
		setResizable(resizable);
		return this;
	}

	/**
	 * Switches between a monospaced font (e.g. for column aligned data) and the default dialog font.
	 *
	 * @param monospaced true for a monospaced font
	 * @return this dialog for chaining
	 */
	public ShowDataDialog withMonospacedFont(final boolean monospaced) {
		final Font currentFont = textArea.getFont();
		textArea.setFont(new Font(monospaced ? Font.MONOSPACED : Font.DIALOG, currentFont.getStyle(), currentFont.getSize()));
		return this;
	}

	/**
	 * Sets an explicit dialog size and centers the dialog on its owner again.
	 *
	 * @param width  new width in pixels, a value &lt;= 0 keeps the current width
	 * @param height new height in pixels, a value &lt;= 0 keeps the current height
	 * @return this dialog for chaining
	 */
	public ShowDataDialog withSize(final int width, final int height) {
		setSize(width > 0 ? width : getWidth(), height > 0 ? height : getHeight());
		setLocationRelativeTo(getOwner());
		return this;
	}
}
