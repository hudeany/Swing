package de.soderer.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
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
import de.soderer.utilities.Utilities;

/**
 * Modal dialog with a text and a single input field.
 *
 * <p>
 * {@link #open()} returns the entered value, or null if the dialog was
 * canceled. The OK button is only enabled while the input is not empty. An
 * optional input pattern rejects every edit (typing, pasting, drag&amp;drop)
 * that would make the input not match the pattern.
 * </p>
 */
public class SimpleInputDialog extends ModalDialog<String> {
	private static final long serialVersionUID = 2945587731926296004L;

	/** Text shown above the input field, may be blank */
	private final String text;

	/** Content panel, refilled by createComponents() */
	private final JPanel panel;

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
	 * @param title title of the dialog
	 * @param text text shown above the input field, may be blank
	 */
	public SimpleInputDialog(final Window parent, final String title, final String text) {
		super(parent, title);

		this.text = text;

		setResizable(false);

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

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
	 * Sets the initial value of the input field. The value is not checked against
	 * the input pattern.
	 *
	 * @param defaultText initial value, may be null for an empty field
	 */
	public void setDefaultText(final String defaultText) {
		this.defaultText = defaultText;
		createComponents();
	}

	/**
	 * Sets the initial value of the input field and returns this dialog for
	 * chaining.
	 *
	 * @param defaultTextValue initial value, may be null for an empty field
	 * @return this dialog for chaining
	 */
	public SimpleInputDialog withDefaultText(final String defaultTextValue) {
		setDefaultText(defaultTextValue);
		return this;
	}

	/**
	 * Sets a regular expression the whole input must match while editing. Edits
	 * that make the input not match are rejected.
	 *
	 * @param patternString regular expression, blank for no restriction
	 */
	public void setInputPattern(final String patternString) {
		if (Utilities.isBlank(patternString)) {
			inputPattern = null;
		} else {
			inputPattern = Pattern.compile(patternString);
		}
	}

	/**
	 * Sets a regular expression the whole input must match while editing and
	 * returns this dialog for chaining.
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
		panel.removeAll();

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

		JScrollPane textScrollpane = null;
		if (Utilities.isNotBlank(text)) {
			final JTextArea textField = new JTextArea();
			final Font f = textField.getFont();
			textField.setFont(new Font(Font.DIALOG, f.getStyle(), f.getSize()));
			textField.setText(text);
			textField.setEditable(false);
			// Not focusable: it would otherwise get the initial focus and swallow the Enter key
			textField.setFocusable(false);
			textField.setBackground(panel.getBackground());

			textScrollpane = new JScrollPane(textField);
			textScrollpane.setBorder(BorderFactory.createEmptyBorder());
			textScrollpane.setPreferredSize(new Dimension(Math.min(textScrollpane.getPreferredSize().width, 600), Math.min(textScrollpane.getPreferredSize().height, 400)));

			// Text Panel
			final JPanel textPanel = new JPanel();
			textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			textPanel.add(textScrollpane);
			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			panel.add(textPanel);
		}

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

		inputField = new JTextField();
		final Font f = inputField.getFont();
		inputField.setFont(f.deriveFont(f.getSize2D() + 2));
		inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK));

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

		final JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

		textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		textPanel.add(inputField);
		textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		panel.add(textPanel);

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

		// Button Panel
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

		// ok Button (also triggered by Enter as default button, but only while it is enabled)
		okButton = new JButton(LangResources.get("ok"));
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				returnValue = inputField.getText();
				dispose();
			}
		});
		buttonPanel.add(okButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		// Close Button
		final JButton closeButton = new JButton(LangResources.get("cancel"));
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				returnValue = null;
				dispose();
			}
		});
		buttonPanel.add(closeButton);

		panel.add(Box.createRigidArea(new Dimension(5, 5)));
		panel.add(buttonPanel);
		getRootPane().setDefaultButton(okButton);

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

		add(panel);

		pack();

		if (textScrollpane != null && textScrollpane.getHorizontalScrollBar().isVisible()) {
			textScrollpane.setPreferredSize(new Dimension(textScrollpane.getPreferredSize().width, textScrollpane.getPreferredSize().height + 20));
			textScrollpane.setSize(new Dimension(textScrollpane.getPreferredSize().width, textScrollpane.getPreferredSize().height));
			pack();
		}

		setLocationRelativeTo(getOwner());

		checkButtonStatus();
	}

	private void checkButtonStatus() {
		if (okButton != null) {
			okButton.setEnabled(Utilities.isNotEmpty(inputField.getText()));
		}
	}

	/**
	 * Rejects every edit whose result would not match the input pattern.
	 */
	private final class PatternFilter extends DocumentFilter {
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
