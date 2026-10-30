package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.soderer.utilities.Credentials;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog in the {@link ModernUi} design asking for a username and/or a password.
 * Same API as {@link de.soderer.swing.CredentialsDialog}.
 *
 * <p>
 * {@link #open()} returns the entered credentials, or null if the dialog was canceled. The OK button is only
 * enabled while a username is entered (if a username is requested) and, if a password confirmation is
 * requested, while both password fields are equal. An optional checkbox lets the user choose to remember the
 * credentials (see {@link #isRememberCredentials()}), and an optional "no password" button returns the
 * credentials without password.
 * </p>
 *
 * <p>
 * The password confirmation field uses the language resource key {@code passwordConfirmation} for its label.
 * </p>
 */
public class CredentialsDialog extends ModalDialog<Credentials> {
	/** Serialization version */
	private static final long serialVersionUID = 7316024485910263788L;

	/** Width of the input fields */
	private static final int FIELD_WIDTH = 240;

	/** Text shown above the input fields, may be blank */
	private final String text;

	/** Label of the username field */
	private final String text_Username;

	/** Label of the password field */
	private final String text_Password;

	/** Text of the OK button */
	private final String text_OK;

	/** Text of the cancel button */
	private final String text_Cancel;

	/** Whether a "no password" button is shown */
	private boolean enableNoPassword = false;

	/** Text of the "no password" button */
	private String enableNoPasswordButtonText;

	/** Text of the "remember credentials" checkbox, blank for no checkbox */
	private String rememberCredentialsText = null;

	/** Whether the password must be entered twice (e.g. when a new password is set) */
	private boolean passwordConfirmation = false;

	/** OK button, only enabled while the input is complete */
	private JButton okButton;

	/** Whether the dialog asks for a username */
	private final boolean acquireUsername;

	/** Whether the dialog asks for a password */
	private final boolean acquirePassword;

	/** Currently entered username, null if no username is requested */
	private String username = null;

	/** Currently entered password, null if no password is requested (transient: never serialized) */
	private transient char[] password = null;

	/** Currently entered password confirmation, null if no confirmation is requested (transient: never serialized) */
	private transient char[] passwordConfirmationValue = null;

	/** Input field of the username, null if no username is requested */
	private JTextField userField;

	/** Input field of the password, null if no password is requested */
	private JPasswordField passwordField;

	/** Status of the "remember credentials" checkbox */
	private boolean rememberCredentials = false;

	/**
	 * Creates a credentials dialog with the default texts of the language resources.
	 *
	 * @param parent          owner window, may be null
	 * @param title           title of the dialog
	 * @param text            text shown above the input fields, may be blank
	 * @param acquireUsername true to ask for a username
	 * @param acquirePassword true to ask for a password
	 */
	public CredentialsDialog(final Window parent, final String title, final String text, final boolean acquireUsername, final boolean acquirePassword) {
		this(parent, title, text, acquireUsername, acquirePassword, LangResources.get("username"), LangResources.get("password"), LangResources.get("ok"), LangResources.get("cancel"));
	}

	/**
	 * Creates a credentials dialog with custom label and button texts.
	 *
	 * @param parent          owner window, may be null
	 * @param title           title of the dialog
	 * @param text            text shown above the input fields, may be blank
	 * @param acquireUsername true to ask for a username
	 * @param acquirePassword true to ask for a password
	 * @param text_Username   label of the username field
	 * @param text_Password   label of the password field
	 * @param text_OK         text of the OK button
	 * @param text_Cancel     text of the cancel button
	 */
	public CredentialsDialog(final Window parent, final String title, final String text, final boolean acquireUsername, final boolean acquirePassword,
			final String text_Username, final String text_Password, final String text_OK, final String text_Cancel) {
		super(parent, title, IconKind.LOCK);

		this.text = text;
		this.text_Username = text_Username;
		this.text_Password = text_Password;
		this.text_OK = text_OK;
		this.text_Cancel = text_Cancel;

		this.acquireUsername = acquireUsername;
		this.acquirePassword = acquirePassword;

		setResizable(false);

		createComponents();

		// requestFocus() before the dialog is visible has no effect, so focus the first input field once it is shown
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(final WindowEvent event) {
				if (userField != null) {
					userField.requestFocusInWindow();
				} else if (passwordField != null) {
					passwordField.requestFocusInWindow();
				}
			}
		});
	}

	/**
	 * (Re)creates all components from the current settings.
	 */
	private void createComponents() {
		getContentArea().removeAll();
		clearButtons();

		// Old password copies of a previous build must not stay in memory
		wipe(password);
		wipe(passwordConfirmationValue);
		userField = null;
		passwordField = null;

		if (StringUtilities.isNotBlank(text)) {
			final JTextArea textField = TextSupport.install(new JTextArea(text));
			// Not focusable: it would otherwise get the initial focus and swallow the Enter key
			textField.setFocusable(false);
			textField.setOpaque(false);
			textField.setBorder(BorderFactory.createEmptyBorder());
			getContentArea().add(textField, BorderLayout.NORTH);
		}

		final JPanel fieldsPanel = new JPanel(new GridBagLayout());
		fieldsPanel.setOpaque(false);
		int row = 0;

		if (acquireUsername) {
			final JTextField newUserField = new JTextField();
			ModernUi.styleTextField(newUserField, false);
			username = "";
			// A DocumentListener also covers input that is not typed, like pasting via mouse or drag and drop
			newUserField.getDocument().addDocumentListener(new FieldChangeListener() {
				@Override
				protected void fieldChanged() {
					username = newUserField.getText();
					checkButtonStatus();
				}
			});
			addFieldRow(fieldsPanel, row++, text_Username, newUserField);
			userField = newUserField;
		}

		if (acquirePassword) {
			final JPasswordField newPasswordField = new JPasswordField();
			ModernUi.styleTextField(newPasswordField, false);
			password = new char[0];
			newPasswordField.getDocument().addDocumentListener(new FieldChangeListener() {
				@Override
				protected void fieldChanged() {
					// getPassword() avoids an immutable String copy of the password, the previous copy is wiped
					wipe(password);
					password = newPasswordField.getPassword();
					checkButtonStatus();
				}
			});
			addFieldRow(fieldsPanel, row++, text_Password, newPasswordField);
			passwordField = newPasswordField;

			if (passwordConfirmation) {
				final JPasswordField confirmationField = new JPasswordField();
				ModernUi.styleTextField(confirmationField, false);
				passwordConfirmationValue = new char[0];
				confirmationField.getDocument().addDocumentListener(new FieldChangeListener() {
					@Override
					protected void fieldChanged() {
						wipe(passwordConfirmationValue);
						passwordConfirmationValue = confirmationField.getPassword();
						checkButtonStatus();
					}
				});
				addFieldRow(fieldsPanel, row++, LangResources.get("passwordConfirmation"), confirmationField);
			} else {
				passwordConfirmationValue = null;
			}
		}

		if (StringUtilities.isNotBlank(rememberCredentialsText)) {
			final JCheckBox rememberCredentialsCheckBox = new JCheckBox(rememberCredentialsText);
			rememberCredentialsCheckBox.setOpaque(false);
			rememberCredentialsCheckBox.setSelected(rememberCredentials);
			rememberCredentialsCheckBox.addActionListener(event -> rememberCredentials = rememberCredentialsCheckBox.isSelected());
			final GridBagConstraints checkboxConstraints = new GridBagConstraints();
			checkboxConstraints.gridx = 1;
			checkboxConstraints.gridy = row;
			checkboxConstraints.anchor = GridBagConstraints.WEST;
			checkboxConstraints.insets = new Insets(6, 0, 0, 0);
			fieldsPanel.add(rememberCredentialsCheckBox, checkboxConstraints);
		}

		getContentArea().add(fieldsPanel, BorderLayout.CENTER);

		if (enableNoPassword) {
			addLeftButton(enableNoPasswordButtonText, null, ButtonStyle.SECONDARY, event -> {
				final Credentials credentials = getCredentials();
				returnValue = credentials == null ? new Credentials() : credentials.withPassword(null);
				dispose();
			});
		}

		okButton = addButton(text_OK, null, ButtonStyle.PRIMARY, event -> {
			returnValue = getCredentials();
			dispose();
		});
		addButton(text_Cancel, null, ButtonStyle.SECONDARY, event -> {
			returnValue = null;
			dispose();
		});
		getRootPane().setDefaultButton(okButton);

		pack();

		setLocationRelativeTo(getOwner());

		checkButtonStatus();
	}

	/**
	 * Adds a row with a grey label and an input field.
	 *
	 * @param fieldsPanel panel with GridBagLayout
	 * @param row         row index
	 * @param labelText   text of the label
	 * @param field       input field
	 */
	private static void addFieldRow(final JPanel fieldsPanel, final int row, final String labelText, final JTextField field) {
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.insets = new Insets(4, 0, 4, 12);
		fieldsPanel.add(ModernUi.createMutedLabel(labelText), labelConstraints);

		field.setPreferredSize(new Dimension(FIELD_WIDTH, field.getPreferredSize().height));
		final GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridx = 1;
		fieldConstraints.gridy = row;
		fieldConstraints.weightx = 1;
		fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.insets = new Insets(4, 0, 4, 0);
		fieldsPanel.add(field, fieldConstraints);
	}

	/**
	 * Enables the OK button only while the input is complete.
	 */
	private void checkButtonStatus() {
		boolean enabled = !acquireUsername || StringUtilities.isNotEmpty(username);
		if (acquirePassword && passwordConfirmation) {
			enabled = enabled && Arrays.equals(password, passwordConfirmationValue);
		}
		okButton.setEnabled(enabled);
	}

	/**
	 * Overwrites a password copy in memory.
	 *
	 * @param value password copy, may be null
	 */
	private static void wipe(final char[] value) {
		if (value != null) {
			Arrays.fill(value, '\0');
		}
	}

	/**
	 * Sets the text of a "remember credentials" checkbox.
	 *
	 * @param rememberCredentialsText text of the checkbox, blank for no checkbox
	 */
	public void setRememberCredentialsText(final String rememberCredentialsText) {
		this.rememberCredentialsText = rememberCredentialsText;
		createComponents();
	}

	/**
	 * Sets the text of a "remember credentials" checkbox and returns this dialog for chaining.
	 *
	 * @param rememberCredentialsTextValue text of the checkbox, blank for no checkbox
	 * @return this dialog for chaining
	 */
	public CredentialsDialog withRememberCredentialsText(final String rememberCredentialsTextValue) {
		setRememberCredentialsText(rememberCredentialsTextValue);
		return this;
	}

	/**
	 * Sets the initial status of the "remember credentials" checkbox.
	 *
	 * @param rememberCredentials true to check the checkbox initially
	 */
	public void setRememberCredentials(final boolean rememberCredentials) {
		this.rememberCredentials = rememberCredentials;
		createComponents();
	}

	/**
	 * Sets the initial status of the "remember credentials" checkbox and returns this dialog for chaining.
	 *
	 * @param rememberCredentialsValue true to check the checkbox initially
	 * @return this dialog for chaining
	 */
	public CredentialsDialog withRememberCredentials(final boolean rememberCredentialsValue) {
		setRememberCredentials(rememberCredentialsValue);
		return this;
	}

	/**
	 * Requests the password to be entered twice. The OK button is only enabled while both entries are equal. Use
	 * this whenever a new password is set, so a typo cannot lock the user out.
	 *
	 * @param passwordConfirmation true to show a password confirmation field
	 */
	public void setPasswordConfirmation(final boolean passwordConfirmation) {
		this.passwordConfirmation = passwordConfirmation;
		createComponents();
	}

	/**
	 * Requests the password to be entered twice and returns this dialog for chaining.
	 *
	 * @param passwordConfirmationParam true to show a password confirmation field
	 * @return this dialog for chaining
	 */
	public CredentialsDialog withPasswordConfirmation(final boolean passwordConfirmationParam) {
		setPasswordConfirmation(passwordConfirmationParam);
		return this;
	}

	/**
	 * Returns the status of the "remember credentials" checkbox, also after the dialog was closed.
	 *
	 * @return true if the user wants the credentials to be remembered
	 */
	public boolean isRememberCredentials() {
		return rememberCredentials;
	}

	/**
	 * Returns the currently entered credentials. The password is a copy, the caller may wipe it after use.
	 *
	 * @return credentials with the requested parts (username and/or password), null if neither username nor
	 *         password is requested
	 */
	public Credentials getCredentials() {
		final char[] passwordCopy = password == null ? null : Arrays.copyOf(password, password.length);
		if (acquireUsername && acquirePassword) {
			if (username != null) {
				return new Credentials(username, passwordCopy);
			} else {
				return null;
			}
		} else if (acquireUsername) {
			if (username != null) {
				return new Credentials(username);
			} else {
				return null;
			}
		} else if (acquirePassword) {
			return new Credentials(passwordCopy);
		} else {
			return null;
		}
	}

	/**
	 * Shows or hides a "no password" button with the default text of the language resources. The button closes
	 * the dialog and returns the credentials without password.
	 *
	 * @param enable true to show the button
	 * @return this dialog for chaining
	 */
	public CredentialsDialog enableNoPassword(final boolean enable) {
		enableNoPassword(enable, LangResources.get("noPassword"));
		return this;
	}

	/**
	 * Shows or hides a "no password" button. The button closes the dialog and returns the credentials without
	 * password.
	 *
	 * @param enable                          true to show the button
	 * @param enableNoPasswordButtonTextParam text of the button
	 * @return this dialog for chaining
	 */
	public CredentialsDialog enableNoPassword(final boolean enable, final String enableNoPasswordButtonTextParam) {
		enableNoPassword = enable;
		enableNoPasswordButtonText = enableNoPasswordButtonTextParam;
		// Rebuild the components, otherwise the button would only appear if another setter is called afterwards
		createComponents();
		return this;
	}

	/**
	 * Wipes the entered passwords from memory when the dialog is closed. The credentials returned by
	 * {@link #open()} or {@link #getCredentials()} hold their own copy and are not affected.
	 */
	@Override
	public void dispose() {
		if (returnValue == null && passwordField != null) {
			// Canceled: nothing needs the entered password anymore
			wipe(password);
			password = new char[0];
		}
		wipe(passwordConfirmationValue);
		super.dispose();
	}

	/**
	 * Calls {@link #fieldChanged()} on every change of a text field's document.
	 */
	private abstract static class FieldChangeListener implements DocumentListener {
		/** Creates the listener */
		FieldChangeListener() {
			// nothing to initialize
		}

		/** Called on every text change of the field */
		protected abstract void fieldChanged();

		@Override
		public void insertUpdate(final DocumentEvent event) {
			fieldChanged();
		}

		@Override
		public void removeUpdate(final DocumentEvent event) {
			fieldChanged();
		}

		@Override
		public void changedUpdate(final DocumentEvent event) {
			// Attribute changes only, no text change
		}
	}
}
