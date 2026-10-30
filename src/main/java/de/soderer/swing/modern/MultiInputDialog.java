package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog in the {@link ModernUi} design with a text and one labeled input field per label text. Same API as
 * {@link de.soderer.swing.MultiInputDialog}.
 *
 * <p>
 * {@link #open()} returns the entered values in the order of the labels, or null if the dialog was canceled.
 * </p>
 */
public class MultiInputDialog extends ModalDialog<List<String>> {
	/** Serialization version */
	private static final long serialVersionUID = 6427839520873311025L;

	/** Preferred width of the input fields */
	private static final int FIELD_WIDTH = 280;

	/** Text shown above the input fields, may be blank */
	private final String text;

	/** Labels of the input fields, one input field per label */
	private final String[] labelTexts;

	/** Initial values of the input fields in the order of the labels, null for empty fields */
	private String[] defaultTexts = null;

	/** Input fields in the order of the labels */
	private final List<JTextField> textFields = new ArrayList<>();

	/**
	 * Creates a multi input dialog.
	 *
	 * @param parent     owner window, may be null
	 * @param title      title of the dialog
	 * @param text       text shown above the input fields, may be blank
	 * @param labelTexts labels of the input fields, one input field per label
	 */
	public MultiInputDialog(final Window parent, final String title, final String text, final String... labelTexts) {
		super(parent, title, IconKind.EDIT);

		this.text = text;
		this.labelTexts = labelTexts == null ? new String[0] : labelTexts;

		setResizable(false);

		createComponents();

		// requestFocus() before the dialog is visible has no effect, so focus the first input field once it is shown
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(final WindowEvent event) {
				// A dialog without labels has no input field to focus
				if (!textFields.isEmpty()) {
					textFields.get(0).requestFocusInWindow();
				}
			}
		});

		returnValue = null;
	}

	/**
	 * Sets the initial values of the input fields.
	 *
	 * @param defaultTexts initial values in the order of the labels; missing or null entries leave the field empty
	 */
	public void setDefaultTexts(final String[] defaultTexts) {
		this.defaultTexts = defaultTexts;
		createComponents();
	}

	/**
	 * Sets the initial values of the input fields and returns this dialog for chaining.
	 *
	 * @param defaultTextsValue initial values in the order of the labels; missing or null entries leave the field
	 *                          empty
	 * @return this dialog for chaining
	 */
	public MultiInputDialog withDefaultTexts(final String... defaultTextsValue) {
		setDefaultTexts(defaultTextsValue);
		return this;
	}

	/**
	 * (Re)creates all components of the dialog from the current settings.
	 */
	protected void createComponents() {
		getContentArea().removeAll();
		clearButtons();
		textFields.clear();

		if (StringUtilities.isNotBlank(text)) {
			getContentArea().add(TextSupport.createTextScrollPane(text, 600, 300), BorderLayout.NORTH);
		}

		// Form: muted label left, input field right
		final JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setOpaque(false);
		int index = 0;
		for (final String labelText : labelTexts) {
			final JLabel label = ModernUi.createMutedLabel(labelText);

			final GridBagConstraints labelConstraints = new GridBagConstraints();
			labelConstraints.anchor = GridBagConstraints.LINE_START;
			labelConstraints.gridx = 0;
			labelConstraints.gridy = index;
			labelConstraints.insets = new Insets(index == 0 ? 0 : 8, 0, 0, 14);
			formPanel.add(label, labelConstraints);

			final JTextField inputField = new JTextField();
			ModernUi.styleTextField(inputField, false);
			final Font font = inputField.getFont();
			inputField.setFont(font.deriveFont(font.getSize2D() + 1));
			inputField.setPreferredSize(new Dimension(FIELD_WIDTH, inputField.getPreferredSize().height));
			if (defaultTexts != null && defaultTexts.length > index && defaultTexts[index] != null) {
				inputField.setText(defaultTexts[index]);
				inputField.setCaretPosition(0);
			}

			final GridBagConstraints fieldConstraints = new GridBagConstraints();
			fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			fieldConstraints.weightx = 1;
			fieldConstraints.gridx = 1;
			fieldConstraints.gridy = index;
			fieldConstraints.insets = new Insets(index == 0 ? 0 : 8, 0, 0, 0);
			formPanel.add(inputField, fieldConstraints);

			textFields.add(inputField);
			index++;
		}
		getContentArea().add(formPanel, BorderLayout.CENTER);

		// OK button, also triggered by Enter as default button
		final JButton okButton = addButton(LangResources.get("ok"), null, ButtonStyle.PRIMARY, event -> {
			final List<String> returnValueList = new ArrayList<>();
			for (final JTextField textFieldForReturnValue : textFields) {
				returnValueList.add(textFieldForReturnValue.getText());
			}
			returnValue = returnValueList;
			dispose();
		});

		addButton(LangResources.get("cancel"), null, ButtonStyle.SECONDARY, event -> {
			returnValue = null;
			dispose();
		});

		getRootPane().setDefaultButton(okButton);

		pack();

		setLocationRelativeTo(getOwner());
	}
}
