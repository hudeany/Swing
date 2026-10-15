package de.soderer.swing;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import de.soderer.utilities.ExceptionUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.VisibleException;

/**
 * Modal dialog showing an error message, with buttons to show the stack trace
 * details and to send the error via the standard email client.
 *
 * <p>
 * The message shown is the visible message of a {@link VisibleException}, the
 * message of any other throwable, or a generic error text.
 * </p>
 *
 * <p>
 * The email is prepared as a "mailto:" link and opened with the system's
 * standard email client ({@link Desktop#mail(URI)}, on Linux also via
 * "xdg-open"). The email button is only shown if a recipient is given.
 * </p>
 */
public class ErrorDialog extends ModalDialog<Boolean> {
	private static final long serialVersionUID = 8028942277125473906L;

	/**
	 * Maximum length of the complete "mailto:" link. Longer links are cut off or
	 * rejected by some systems and email clients (e.g. Windows and Outlook at
	 * about 2000 characters), so the email body is shortened if necessary.
	 */
	private static final int MAX_MAILTO_LENGTH = 2000;

	/** Seconds to wait for "xdg-open" to report whether it could open the email client */
	private static final int XDG_OPEN_TIMEOUT_SECONDS = 10;

	/** Name of the application, shown in the title and the email subject */
	private final String applicationName;

	/** Version of the application, may be blank */
	private final String applicationVersion;

	/** Recipient address of the error email, may be blank to hide the email button */
	private final String emailRecipient;

	/** Error to show, may be null */
	private final Throwable throwable;

	/** Button toggling the details, its text shows whether the details are expanded */
	private JButton detailButton;

	/** Panel holding the summary and the stack trace while the details are shown */
	private JPanel detailPanel;

	/** Whether the details are currently shown */
	private boolean detailsShown = false;

	/**
	 * Creates an error dialog.
	 *
	 * @param parent owner window, may be null. Should be the window the error occurred in: if a modal
	 *        dialog is open, a dialog owned by a window behind it is blocked and hidden behind that dialog
	 * @param applicationName name of the application, shown in the title and the email subject
	 * @param applicationVersion version of the application, may be blank
	 * @param emailRecipient recipient address of the error email, may be blank to hide the email button
	 * @param throwable error to show, may be null
	 */
	public ErrorDialog(final Window parent, final String applicationName, final String applicationVersion, final String emailRecipient, final Throwable throwable) {
		super(parent, applicationName + (Utilities.isNotBlank(applicationVersion) ? " (" + applicationVersion + ")" : "") + " " + LangResources.get("error"));

		this.applicationName = applicationName;
		this.applicationVersion = applicationVersion;
		this.emailRecipient = emailRecipient;
		this.throwable = throwable;

		initUI();
	}

	private void initUI() {
		final int margin = 5;

		getContentPane().setLayout(new BorderLayout(margin, margin));
		((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));

		// The icon is packaged as "icons/exclamation.png" in this library, the old location is kept as fallback
		for (final String iconPath : new String[] { "icons/exclamation.png", "images/icons/exclamation.png" }) {
			try (InputStream is = ErrorDialog.class.getClassLoader().getResourceAsStream(iconPath)) {
				if (is != null) {
					final ImageIcon icon = new ImageIcon(is.readAllBytes());
					setIconImage(icon.getImage());
					break;
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Try the next location
			}
		}

		final JTextArea mainText = new JTextArea(getMainText());
		mainText.setEditable(false);
		mainText.setLineWrap(true);
		mainText.setWrapStyleWord(true);
		mainText.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));

		final int lineHeight = mainText.getFontMetrics(mainText.getFont()).getHeight();
		final int lineCount = Math.max(1, mainText.getLineCount());
		final int maxLines = 5;
		final int preferredHeight = lineHeight * Math.min(lineCount, maxLines) + 10;

		final JScrollPane scrollPane = new JScrollPane(mainText);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(500, preferredHeight));
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.NORTH);

		final JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		add(centerPanel, BorderLayout.CENTER);

		final JPanel buttonPanel = new JPanel(new BorderLayout(margin, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, margin, 0));

		final JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, margin, 0));
		if (Utilities.isNotBlank(emailRecipient)) {
			final JButton mailButton = new JButton(LangResources.get("openMail"));
			mailButton.addActionListener(e -> openMail(mailButton));
			leftButtons.add(mailButton);
		}

		detailButton = new JButton(LangResources.get("detailsMore"));
		detailButton.addActionListener(e -> toggleDetails());
		leftButtons.add(detailButton);

		buttonPanel.add(leftButtons, BorderLayout.WEST);

		final JButton closeButton = new JButton(LangResources.get("close"));
		closeButton.addActionListener(e -> dispose());
		buttonPanel.add(closeButton, BorderLayout.CENTER);

		centerPanel.add(buttonPanel);

		detailPanel = new JPanel();
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		detailPanel.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
		centerPanel.add(detailPanel);

		pack();
		setLocationRelativeTo(getParent());
	}

	private String getMainText() {
		if (throwable instanceof VisibleException) {
			return ((VisibleException) throwable).getVisibleMessage();
		} else if (throwable != null && throwable.getMessage() != null) {
			return throwable.getMessage();
		} else {
			return LangResources.get("errorOccurred");
		}
	}

	/**
	 * Opens the error email in the standard email client. Runs in a background
	 * thread, because starting the email client (especially via "xdg-open") may
	 * take a moment and must not block the user interface.
	 */
	private void openMail(final JButton mailButton) {
		mailButton.setEnabled(false);
		final String subject = "Error in " + applicationName + (Utilities.isNotBlank(applicationVersion) ? " (" + applicationVersion + ")" : "");
		final URI mailtoUri = createMailtoUri(emailRecipient, subject, getErrorDetails());

		final Thread mailThread = new Thread(() -> {
			final boolean success = openMailtoUri(mailtoUri);
			SwingUtilities.invokeLater(() -> {
				mailButton.setEnabled(true);
				if (!success) {
					JOptionPane.showMessageDialog(this,
							LangResources.get("cannotOpenMail"),
							getTitle(),
							JOptionPane.ERROR_MESSAGE);
				}
			});
		}, "ErrorDialog-OpenMail");
		mailThread.setDaemon(true);
		mailThread.start();
	}

	/**
	 * Creates a "mailto:" link (RFC 6068). The body is shortened, if the link
	 * would get too long for the email clients.
	 *
	 * @param recipient email address of the recipient
	 * @param subject subject of the email
	 * @param body text of the email
	 * @return mailto link
	 */
	static URI createMailtoUri(final String recipient, final String subject, final String body) {
		final String truncationNote = "\n\n[...] (truncated, please copy the complete details from the error dialog)";
		final String prefix = "mailto:" + encodeMailtoComponent(recipient.trim()).replace("%40", "@") + "?subject=" + encodeMailtoComponent(subject) + "&body=";

		final String bodyText = body == null ? "" : body;
		String encodedBody = encodeMailtoComponent(bodyText);
		if (prefix.length() + encodedBody.length() > MAX_MAILTO_LENGTH) {
			// Encoding may multiply the length (e.g. umlauts, line breaks), so shorten step by step on the unencoded text
			final int maxEncodedBodyLength = MAX_MAILTO_LENGTH - prefix.length();
			int keepLength = bodyText.length();
			do {
				keepLength = Math.max(0, keepLength - Math.max(50, (encodedBody.length() - maxEncodedBodyLength) / 3));
				encodedBody = encodeMailtoComponent(bodyText.substring(0, keepLength) + truncationNote);
			} while (keepLength > 0 && prefix.length() + encodedBody.length() > MAX_MAILTO_LENGTH);
		}

		return URI.create(prefix + encodedBody);
	}

	/**
	 * Percent-encodes a value for a "mailto:" link. {@link URLEncoder} encodes
	 * for HTML forms, so blanks ("+") and line breaks are adjusted to the mailto
	 * syntax, where line breaks must be "%0D%0A".
	 */
	private static String encodeMailtoComponent(final String value) {
		final String normalizedLineBreaks = value.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n");
		return URLEncoder.encode(normalizedLineBreaks, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/**
	 * Opens the mailto link with the standard email client: via the Desktop API,
	 * or on Linux, where the Desktop API is often not supported, via "xdg-open".
	 *
	 * @param mailtoUri mailto link
	 * @return true if the email client was started
	 */
	private static boolean openMailtoUri(final URI mailtoUri) {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
				Desktop.getDesktop().mail(mailtoUri);
				return true;
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Try the fallback below
		}

		if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")) {
			try {
				// Output is discarded by the OS instead of read via a pipe: an email client started by xdg-open inherits
				// the pipe, so reading it until EOF would block until the email client is closed
				final Process process = new ProcessBuilder("xdg-open", mailtoUri.toString())
						.redirectOutput(ProcessBuilder.Redirect.DISCARD)
						.redirectError(ProcessBuilder.Redirect.DISCARD)
						.start();
				if (process.waitFor(XDG_OPEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
					return process.exitValue() == 0;
				} else {
					// Still running: the email client was started in the foreground
					return true;
				}
			} catch (@SuppressWarnings("unused") final InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			} catch (@SuppressWarnings("unused") final Exception e) {
				return false;
			}
		}

		return false;
	}

	private String getErrorDetails() {
		return throwable == null ? getMainText() : ExceptionUtilities.throwableToString(throwable);
	}

	private void toggleDetails() {
		detailsShown = !detailsShown;
		detailButton.setText(detailsShown ? LangResources.get("detailsLess") : LangResources.get("detailsMore"));
		setupDetailPanel(detailsShown);
	}

	private void setupDetailPanel(final boolean show) {
		detailPanel.removeAll();

		if (show) {
			// The dialog may be created without a throwable, the details must not fail with a NullPointerException then
			final String text2 = throwable == null ? getMainText() : throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
			final String text3 = throwable == null ? null : ExceptionUtilities.throwableToString(throwable);

			final JTextArea summary = new JTextArea(text2);
			summary.setEditable(false);
			summary.setLineWrap(true);
			summary.setWrapStyleWord(true);
			summary.setBorder(BorderFactory.createTitledBorder("Summary"));

			detailPanel.add(summary);

			if (Utilities.isNotEmpty(text3)) {
				final JTextArea stackTrace = new JTextArea(text3);
				stackTrace.setEditable(false);
				stackTrace.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));

				final JScrollPane scroll = new JScrollPane(stackTrace);
				scroll.setPreferredSize(new Dimension(500, 300));

				detailPanel.add(scroll);
			}
		}

		detailPanel.revalidate();
		detailPanel.repaint();

		// Only resize, the dialog must not jump back to the center if the user moved it
		pack();
	}
}
