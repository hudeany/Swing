package de.soderer.swing;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.soderer.utilities.Credentials;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog asking for a username and/or a password.
 *
 * <p>
 * {@link #open()} returns the entered credentials, or null if the dialog was
 * canceled. The OK button is only enabled while a username is entered (if a
 * username is requested) and, if a password confirmation is requested, while
 * both password fields are equal. An optional checkbox lets the user choose to
 * remember the credentials (see {@link #isRememberCredentials()}), and an
 * optional "no password" button returns the credentials without password.
 * </p>
 *
 * <p>
 * The password confirmation field uses the language resource key
 * {@code passwordConfirmation} for its label.
 * </p>
 */
public class CredentialsDialog extends ModalDialog<Credentials> {
	private static final long serialVersionUID = -3159468071935512867L;

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

	/** Button closing the dialog and returning the credentials without password */
	private JButton enableNoPasswordButton;

	/** Text of the "remember credentials" checkbox, blank for no checkbox */
	private String rememberCredentialsText = null;

	/** Whether the password must be entered twice (e.g. when a new password is set) */
	private boolean passwordConfirmation = false;

	/** Content panel, rebuilt whenever an option affecting the layout changes */
	private JPanel panel;

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
	 * Creates a credentials dialog with the default texts of the language
	 * resources.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param text text shown above the input fields, may be blank
	 * @param acquireUsername true to ask for a username
	 * @param acquirePassword true to ask for a password
	 */
	public CredentialsDialog(final Window parent, final String title, final String text, final boolean acquireUsername, final boolean acquirePassword) {
		this(parent, title, text, acquireUsername, acquirePassword, LangResources.get("username"), LangResources.get("password"), LangResources.get("ok"), LangResources.get("cancel"));
	}

	/**
	 * Creates a credentials dialog with custom label and button texts.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param text text shown above the input fields, may be blank
	 * @param acquireUsername true to ask for a username
	 * @param acquirePassword true to ask for a password
	 * @param text_Username label of the username field
	 * @param text_Password label of the password field
	 * @param text_OK text of the OK button
	 * @param text_Cancel text of the cancel button
	 */
	public CredentialsDialog(final Window parent, final String title, final String text, final boolean acquireUsername, final boolean acquirePassword,
			final String text_Username, final String text_Password, final String text_OK, final String text_Cancel) {
		super(parent, title);

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

	private void createComponents() {
		if (panel != null) {
			remove(panel);
		}

		// Old password copies of a previous build must not stay in memory
		wipe(password);
		wipe(passwordConfirmationValue);
		userField = null;
		passwordField = null;

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		panel.add(Box.createRigidArea(new Dimension(0, 5)));

		if (StringUtilities.isNotBlank(text)) {
			final JTextArea textField = new JTextArea();
			final Font f = textField.getFont();
			textField.setFont(new Font(Font.DIALOG, f.getStyle(), f.getSize()));
			textField.setText(text);
			textField.setEditable(false);
			// Not focusable: it would otherwise get the initial focus and swallow the Enter key
			textField.setFocusable(false);
			textField.setBackground(panel.getBackground());

			final JScrollPane textScrollpane = new JScrollPane(textField);
			textScrollpane.setBorder(BorderFactory.createEmptyBorder());
			textScrollpane.setPreferredSize(new Dimension(textField.getPreferredSize().width + 5, textField.getPreferredSize().height + 5));

			// Text Panel
			final JPanel textPanel = new JPanel();
			textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			textPanel.add(textScrollpane);
			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			panel.add(textPanel);
		}

		if (acquireUsername) {
			// User Panel
			final JPanel userPanel = new JPanel();
			userPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			final JLabel userLabel = new JLabel(text_Username);
			userPanel.add(userLabel);
			final JTextField newUserField = new JTextField();
			newUserField.setPreferredSize(new Dimension(200, newUserField.getPreferredSize().height));
			newUserField.setText("");
			username = "";
			// A DocumentListener also covers input that is not typed, like pasting via mouse or drag and drop
			newUserField.getDocument().addDocumentListener(new FieldChangeListener() {
				@Override
				protected void fieldChanged() {
					username = newUserField.getText();
					checkButtonStatus();
				}
			});
			userPanel.add(newUserField);
			panel.add(userPanel);
			userField = newUserField;
		}

		if (acquirePassword) {
			// Password Panel
			final JPanel passwordPanel = new JPanel();
			passwordPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			final JLabel passwordLabel = new JLabel(text_Password);
			passwordPanel.add(passwordLabel);
			final JPasswordField newPasswordField = new JPasswordField();
			newPasswordField.setPreferredSize(new Dimension(200, newPasswordField.getPreferredSize().height));
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
			passwordPanel.add(newPasswordField);
			panel.add(passwordPanel);
			passwordField = newPasswordField;

			if (passwordConfirmation) {
				final JPanel confirmationPanel = new JPanel();
				confirmationPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
				confirmationPanel.add(new JLabel(LangResources.get("passwordConfirmation")));
				final JPasswordField confirmationField = new JPasswordField();
				confirmationField.setPreferredSize(new Dimension(200, confirmationField.getPreferredSize().height));
				passwordConfirmationValue = new char[0];
				confirmationField.getDocument().addDocumentListener(new FieldChangeListener() {
					@Override
					protected void fieldChanged() {
						wipe(passwordConfirmationValue);
						passwordConfirmationValue = confirmationField.getPassword();
						checkButtonStatus();
					}
				});
				confirmationPanel.add(confirmationField);
				panel.add(confirmationPanel);
			} else {
				passwordConfirmationValue = null;
			}
		}

		if (StringUtilities.isNotBlank(rememberCredentialsText)) {
			// RememberCredentials Panel
			final JPanel rememberCredentialsPanel = new JPanel();
			rememberCredentialsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			final JCheckBox rememberCredentialsCheckBox = new JCheckBox(rememberCredentialsText);
			rememberCredentialsCheckBox.setSelected(rememberCredentials);
			rememberCredentialsCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent event) {
					rememberCredentials = rememberCredentialsCheckBox.isSelected();
				}
			});
			rememberCredentialsPanel.add(rememberCredentialsCheckBox);
			panel.add(rememberCredentialsPanel);
		}

		// Button Panel
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

		if (enableNoPassword) {
			enableNoPasswordButton = new JButton(enableNoPasswordButtonText);
			enableNoPasswordButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent event) {
					final Credentials credentials = getCredentials();
					returnValue = credentials == null ? new Credentials() : credentials.withPassword(null);
					dispose();
				}
			});
			buttonPanel.add(enableNoPasswordButton);
		}

		// ok Button
		okButton = new JButton(text_OK);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				returnValue = getCredentials();
				dispose();
			}
		});
		buttonPanel.add(okButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		// Close Button
		final JButton closeButton = new JButton(text_Cancel);
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				returnValue = null;
				dispose();
			}
		});
		buttonPanel.add(closeButton);

		panel.add(Box.createRigidArea(new Dimension(5, 0)));
		panel.add(buttonPanel);
		getRootPane().setDefaultButton(okButton);

		panel.add(Box.createRigidArea(new Dimension(0, 5)));

		add(panel);

		pack();

		setLocationRelativeTo(getOwner());

		checkButtonStatus();
	}

	private void checkButtonStatus() {
		boolean enabled = !acquireUsername || StringUtilities.isNotEmpty(username);
		if (acquirePassword && passwordConfirmation) {
			enabled = enabled && Arrays.equals(password, passwordConfirmationValue);
		}
		okButton.setEnabled(enabled);
	}

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
	 * Sets the text of a "remember credentials" checkbox and returns this dialog
	 * for chaining.
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
	 * Sets the initial status of the "remember credentials" checkbox and returns
	 * this dialog for chaining.
	 *
	 * @param rememberCredentialsValue true to check the checkbox initially
	 * @return this dialog for chaining
	 */
	public CredentialsDialog withRememberCredentials(final boolean rememberCredentialsValue) {
		setRememberCredentials(rememberCredentialsValue);
		return this;
	}

	/**
	 * Requests the password to be entered twice. The OK button is only enabled
	 * while both entries are equal. Use this whenever a new password is set, so a
	 * typo cannot lock the user out.
	 *
	 * @param passwordConfirmation true to show a password confirmation field
	 */
	public void setPasswordConfirmation(final boolean passwordConfirmation) {
		this.passwordConfirmation = passwordConfirmation;
		createComponents();
	}

	/**
	 * Requests the password to be entered twice and returns this dialog for
	 * chaining.
	 *
	 * @param passwordConfirmationParam true to show a password confirmation field
	 * @return this dialog for chaining
	 */
	public CredentialsDialog withPasswordConfirmation(final boolean passwordConfirmationParam) {
		setPasswordConfirmation(passwordConfirmationParam);
		return this;
	}

	/**
	 * Returns the status of the "remember credentials" checkbox, also after the
	 * dialog was closed.
	 *
	 * @return true if the user wants the credentials to be remembered
	 */
	public boolean isRememberCredentials() {
		return rememberCredentials;
	}

	/**
	 * Returns the currently entered credentials. The password is a copy, the
	 * caller may wipe it after use.
	 *
	 * @return credentials with the requested parts (username and/or password),
	 *         null if neither username nor password is requested
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
	 * Shows or hides a "no password" button with the default text of the language
	 * resources. The button closes the dialog and returns the credentials without
	 * password.
	 *
	 * @param enable true to show the button
	 * @return this dialog for chaining
	 */
	public CredentialsDialog enableNoPassword(final boolean enable) {
		enableNoPassword(enable, LangResources.get("noPassword"));
		return this;
	}

	/**
	 * Shows or hides a "no password" button. The button closes the dialog and
	 * returns the credentials without password.
	 *
	 * @param enable true to show the button
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
	 * Wipes the entered passwords from memory when the dialog is closed. The
	 * credentials returned by {@link #open()} or {@link #getCredentials()} hold
	 * their own copy and are not affected.
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
