package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog in the {@link ModernUi} design with a text and a single input field.
 * Same API as {@link de.soderer.swing.SimpleInputDialog}.
 *
 * <p>
 * {@link #open()} returns the entered value, or null if the dialog was canceled. The OK button is only enabled
 * while the input is not empty. An optional input pattern rejects every edit (typing, pasting, drag&amp;drop)
 * that would make the input not match the pattern.
 * </p>
 */
public class SimpleInputDialog extends ModalDialog<String> {
	/** Serialization version */
	private static final long serialVersionUID = -2091846612365079243L;

	/** Minimum width of the input field */
	private static final int MIN_FIELD_WIDTH = 320;

	/** Text shown above the input field, may be blank */
	private final String text;

	/** Input field of the value */
	private JTextField inputField;

	/** OK button, only enabled while the input is not empty */
	private JButton okButton;

	/** Pattern the whole input must match, null for no restriction */
	private Pattern inputPattern = null;

	/** Suppresses the pattern check while the field is filled programmatically */
	private boolean isTechnicalChange = false;

	/** Initial value of the input field, null for an empty field */
	private String defaultText = null;

	/**
	 * Creates a simple input dialog.
	 *
	 * @param parent owner window, may be null
	 * @param title  title of the dialog
	 * @param text   text shown above the input field, may be blank
	 */
	public SimpleInputDialog(final Window parent, final String title, final String text) {
		super(parent, title, IconKind.EDIT);

		this.text = text;

		setResizable(false);

		createComponents();

		// requestFocus() before the dialog is visible has no effect, so focus the input field once it is shown
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(final WindowEvent event) {
				inputField.requestFocusInWindow();
			}
		});

		returnValue = null;
	}

	/**
	 * Sets the initial value of the input field. The value is not checked against the input pattern.
	 *
	 * @param defaultText initial value, may be null for an empty field
	 */
	public void setDefaultText(final String defaultText) {
		this.defaultText = defaultText;
		createComponents();
	}

	/**
	 * Sets the initial value of the input field and returns this dialog for chaining.
	 *
	 * @param defaultTextValue initial value, may be null for an empty field
	 * @return this dialog for chaining
	 */
	public SimpleInputDialog withDefaultText(final String defaultTextValue) {
		setDefaultText(defaultTextValue);
		return this;
	}

	/**
	 * Sets a regular expression the whole input must match while editing. Edits that make the input not match are
	 * rejected.
	 *
	 * @param patternString regular expression, blank for no restriction
	 */
	public void setInputPattern(final String patternString) {
		if (StringUtilities.isBlank(patternString)) {
			inputPattern = null;
		} else {
			inputPattern = Pattern.compile(patternString);
		}
	}

	/**
	 * Sets a regular expression the whole input must match while editing and returns this dialog for chaining.
	 *
	 * @param patternString regular expression, blank for no restriction
	 * @return this dialog for chaining
	 */
	public SimpleInputDialog withInputPattern(final String patternString) {
		setInputPattern(patternString);
		return this;
	}

	/**
	 * (Re)creates all components of the dialog from the current settings.
	 */
	protected void createComponents() {
		getContentArea().removeAll();
		clearButtons();

		JScrollPane textScrollpane = null;
		if (StringUtilities.isNotBlank(text)) {
			final JTextArea textField = TextSupport.install(new JTextArea(text));
			// Not focusable: it would otherwise get the initial focus
			textField.setFocusable(false);
			textField.setOpaque(false);
			textField.setBorder(BorderFactory.createEmptyBorder());

			textScrollpane = new JScrollPane(textField);
			textScrollpane.setOpaque(false);
			textScrollpane.getViewport().setOpaque(false);
			textScrollpane.setBorder(BorderFactory.createEmptyBorder());
			ModernUi.styleScrollBars(textScrollpane, ModernUi.CARD);
			textScrollpane.setPreferredSize(new Dimension(Math.min(textScrollpane.getPreferredSize().width, 600), Math.min(textScrollpane.getPreferredSize().height, 400)));
			getContentArea().add(textScrollpane, BorderLayout.NORTH);
		}

		inputField = new JTextField();
		ModernUi.styleTextField(inputField, false);
		final Font font = inputField.getFont();
		inputField.setFont(font.deriveFont(font.getSize2D() + 1));
		inputField.setPreferredSize(new Dimension(Math.max(MIN_FIELD_WIDTH, inputField.getPreferredSize().width), inputField.getPreferredSize().height));

		final Document document = inputField.getDocument();
		if (document instanceof AbstractDocument) {
			// A DocumentFilter also covers pasting, drag&drop and key repeat, unlike a KeyListener
			((AbstractDocument) document).setDocumentFilter(new PatternFilter());
		}
		document.addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(final DocumentEvent event) {
				checkButtonStatus();
			}

			@Override
			public void removeUpdate(final DocumentEvent event) {
				checkButtonStatus();
			}

			@Override
			public void changedUpdate(final DocumentEvent event) {
				// Attribute changes only, no text change
			}
		});

		isTechnicalChange = true;
		try {
			inputField.setText(defaultText != null ? defaultText : "");
		} finally {
			isTechnicalChange = false;
		}
		getContentArea().add(inputField, BorderLayout.CENTER);

		// OK button, also triggered by Enter as default button, but only while it is enabled
		okButton = addButton(LangResources.get("ok"), null, ButtonStyle.PRIMARY, event -> {
			returnValue = inputField.getText();
			dispose();
		});
		addButton(LangResources.get("cancel"), null, ButtonStyle.SECONDARY, event -> {
			returnValue = null;
			dispose();
		});
		getRootPane().setDefaultButton(okButton);

		pack();

		if (textScrollpane != null && textScrollpane.getHorizontalScrollBar().isVisible()) {
			textScrollpane.setPreferredSize(new Dimension(textScrollpane.getPreferredSize().width, textScrollpane.getPreferredSize().height + 20));
			pack();
		}

		setLocationRelativeTo(getOwner());

		checkButtonStatus();
	}

	/**
	 * Enables the OK button only while the input is not empty.
	 */
	private void checkButtonStatus() {
		if (okButton != null) {
			okButton.setEnabled(StringUtilities.isNotEmpty(inputField.getText()));
		}
	}

	/**
	 * Rejects every edit whose result would not match the input pattern.
	 */
	private final class PatternFilter extends DocumentFilter {
		/** Creates the filter */
		PatternFilter() {
			// nothing to initialize
		}

		@Override
		public void insertString(final FilterBypass filterBypass, final int offset, final String string, final AttributeSet attributes) throws BadLocationException {
			replace(filterBypass, offset, 0, string, attributes);
		}

		@Override
		public void remove(final FilterBypass filterBypass, final int offset, final int length) throws BadLocationException {
			replace(filterBypass, offset, length, "", null);
		}

		@Override
		public void replace(final FilterBypass filterBypass, final int offset, final int length, final String newText, final AttributeSet attributes) throws BadLocationException {
			if (isTechnicalChange || inputPattern == null) {
				filterBypass.replace(offset, length, newText, attributes);
				return;
			}

			final Document document = filterBypass.getDocument();
			final String currentText = document.getText(0, document.getLength());
			final String resultText = currentText.substring(0, offset) + (newText == null ? "" : newText) + currentText.substring(offset + length);
			if (inputPattern.matcher(resultText).matches()) {
				filterBypass.replace(offset, length, newText, attributes);
			} else {
				inputField.getToolkit().beep();
			}
		}
	}
}
