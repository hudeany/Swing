package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import de.soderer.swing.modern.ModernUi.ModernButton;
import de.soderer.utilities.ExceptionUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.StringUtilities;
import de.soderer.utilities.VisibleException;

/**
 * Modal dialog in the {@link ModernUi} design showing an error message, with buttons to copy the error to the
 * clipboard, to show the stack trace details and to send the error via the standard email client.
 * Same API as {@link de.soderer.swing.ErrorDialog}.
 *
 * <p>
 * The message shown is the visible message of a {@link VisibleException}, the message of any other throwable,
 * or a generic error text. All texts can be selected and copied (CTRL+C or context menu).
 * </p>
 *
 * <p>
 * The email is prepared as a "mailto:" link and opened with the system's standard email client
 * ({@link Desktop#mail(URI)}, on Linux also via "xdg-open"). The email button is only shown if a recipient is
 * given.
 * </p>
 */
public class ErrorDialog extends ModalDialogModern<Boolean> {
	/** Serialization version */
	private static final long serialVersionUID = -4729150836604217397L;

	/**
	 * Maximum length of the complete "mailto:" link. Longer links are cut off or rejected by some systems and
	 * email clients (e.g. Windows and Outlook at about 2000 characters), so the email body is shortened if
	 * necessary.
	 */
	private static final int MAX_MAILTO_LENGTH = 2000;

	/** Seconds to wait for "xdg-open" to report whether it could open the email client */
	private static final int XDG_OPEN_TIMEOUT_SECONDS = 10;

	/** Width of the message area */
	private static final int MESSAGE_WIDTH = 500;

	/** Name of the application, shown in the title and the email subject */
	private final String applicationName;

	/** Version of the application, may be blank */
	private final String applicationVersion;

	/** Recipient address of the error email, may be blank to hide the email button */
	private final String emailRecipient;

	/** Error to show, may be null */
	private final transient Throwable throwable;

	/** Button toggling the details, its text shows whether the details are expanded */
	private ModernButton detailButton;

	/** Panel holding the summary and the stack trace while the details are shown */
	private JPanel detailPanel;

	/** Whether the details are currently shown */
	private boolean detailsShown = false;

	/**
	 * Creates an error dialog.
	 *
	 * @param parent             owner window, may be null. Should be the window the error occurred in: if a modal
	 *                           dialog is open, a dialog owned by a window behind it is blocked and hidden behind
	 *                           that dialog
	 * @param applicationName    name of the application, shown in the title and the email subject
	 * @param applicationVersion version of the application, may be blank
	 * @param emailRecipient     recipient address of the error email, may be blank to hide the email button
	 * @param throwable          error to show, may be null
	 */
	public ErrorDialog(final Window parent, final String applicationName, final String applicationVersion, final String emailRecipient, final Throwable throwable) {
		super(parent, applicationName + (StringUtilities.isNotBlank(applicationVersion) ? " (" + applicationVersion + ")" : "") + " " + LangResources.get("error"), IconKind.ERROR);

		this.applicationName = applicationName;
		this.applicationVersion = applicationVersion;
		this.emailRecipient = emailRecipient;
		this.throwable = throwable;

		initUI();
	}

	/**
	 * Creates the components.
	 */
	private void initUI() {
		setHeaderStyle(HeaderStyle.DANGER);

		// The icon is packaged as "icons/exclamation.png" in this library, the old location is kept as fallback
		for (final String iconPath : new String[] { "icons/exclamation.png", "images/icons/exclamation.png" }) {
			try (InputStream is = ErrorDialog.class.getClassLoader().getResourceAsStream(iconPath)) {
				if (is != null) {
					setIconImage(new ImageIcon(is.readAllBytes()).getImage());
					break;
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Try the next location
			}
		}

		final JTextArea mainText = TextSupport.install(new JTextArea(getMainText()));
		mainText.setLineWrap(true);
		mainText.setWrapStyleWord(true);
		mainText.setBackground(ModernUi.DANGER_LIGHT);
		mainText.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

		final int lineHeight = mainText.getFontMetrics(mainText.getFont()).getHeight();
		final int maxLines = 5;
		// Line count of the wrapped text is only known after layout, so it is estimated from the text length
		final int estimatedLines = Math.max(1, Math.min(maxLines, mainText.getPreferredSize().width / (MESSAGE_WIDTH - 40) + mainText.getLineCount()));
		// Plus the padding of the text area (2 x 8) and of the rounded border (2 x 4) and some spare room
		final int preferredHeight = lineHeight * estimatedLines + 30;

		final JScrollPane scrollPane = new JScrollPane(mainText);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		ModernUi.styleScrollPane(scrollPane, ModernUi.DANGER_LIGHT);
		scrollPane.setPreferredSize(new Dimension(MESSAGE_WIDTH, preferredHeight));
		getContentArea().add(scrollPane, BorderLayout.NORTH);

		detailPanel = new JPanel();
		detailPanel.setOpaque(false);
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		getContentArea().add(detailPanel, BorderLayout.CENTER);

		if (StringUtilities.isNotBlank(emailRecipient)) {
			final ModernButton mailButton = addLeftButton(LangResources.get("openMail"), IconKind.SEND, ButtonStyle.SECONDARY, null);
			mailButton.addActionListener(event -> openMail(mailButton));
		}

		// Copies the complete error (title, message and stack trace), e.g. for pasting into a text editor or a bug report
		final ModernButton copyButton = addLeftButton(LangResources.get("copyToClipboard"), IconKind.COPY, ButtonStyle.SECONDARY, null);
		copyButton.addActionListener(event -> TextSupport.copyToClipboard(getClipboardText(), copyButton));

		detailButton = addLeftButton(LangResources.get("detailsMore"), IconKind.LOG, ButtonStyle.SECONDARY, event -> toggleDetails());

		final ModernButton closeButton = addButton(LangResources.get("close"), null, ButtonStyle.PRIMARY, event -> dispose());
		getRootPane().setDefaultButton(closeButton);

		pack();
		setLocationRelativeTo(getParent());
	}

	/**
	 * Returns the message shown at the top of the dialog.
	 *
	 * @return visible message, message of the throwable or a generic error text
	 */
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
	 * Complete error text for the clipboard: dialog title, visible message (if it differs from the exception
	 * message) and stack trace.
	 *
	 * @return clipboard text
	 */
	private String getClipboardText() {
		final StringBuilder clipboardText = new StringBuilder(getTitle()).append("\n\n");
		if (throwable == null) {
			clipboardText.append(getMainText());
		} else {
			if (throwable instanceof VisibleException) {
				clipboardText.append(getMainText()).append("\n\n");
			}
			clipboardText.append(ExceptionUtilities.throwableToString(throwable));
		}
		return clipboardText.toString();
	}

	/**
	 * Opens the error email in the standard email client. Runs in a background thread, because starting the
	 * email client (especially via "xdg-open") may take a moment and must not block the user interface.
	 *
	 * @param mailButton button that triggered the email, disabled meanwhile
	 */
	private void openMail(final ModernButton mailButton) {
		mailButton.setEnabled(false);
		final String subject = "Error in " + applicationName + (StringUtilities.isNotBlank(applicationVersion) ? " (" + applicationVersion + ")" : "");
		final URI mailtoUri = createMailtoUri(emailRecipient, subject, getErrorDetails());

		final Thread mailThread = new Thread(() -> {
			final boolean success = openMailtoUri(mailtoUri);
			SwingUtilities.invokeLater(() -> {
				mailButton.setEnabled(true);
				if (!success) {
					new QuestionDialog(this, getTitle(), LangResources.get("cannotOpenMail"), LangResources.get("ok"))
							.withDialogStyle(DialogStyle.ERROR)
							.open();
				}
			});
		}, "ErrorDialog-OpenMail");
		mailThread.setDaemon(true);
		mailThread.start();
	}

	/**
	 * Creates a "mailto:" link (RFC 6068). The body is shortened, if the link would get too long for the email
	 * clients.
	 *
	 * @param recipient email address of the recipient
	 * @param subject   subject of the email
	 * @param body      text of the email
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
	 * Percent-encodes a value for a "mailto:" link. {@link URLEncoder} encodes for HTML forms, so blanks ("+") and
	 * line breaks are adjusted to the mailto syntax, where line breaks must be "%0D%0A".
	 *
	 * @param value value to encode
	 * @return encoded value
	 */
	private static String encodeMailtoComponent(final String value) {
		final String normalizedLineBreaks = value.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n");
		return URLEncoder.encode(normalizedLineBreaks, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/**
	 * Opens the mailto link with the standard email client: via the Desktop API, or on Linux, where the Desktop API
	 * is often not supported, via "xdg-open".
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

	/**
	 * Returns the text of the error email.
	 *
	 * @return stack trace, or the main text without throwable
	 */
	private String getErrorDetails() {
		return throwable == null ? getMainText() : ExceptionUtilities.throwableToString(throwable);
	}

	/**
	 * Shows or hides the details.
	 */
	private void toggleDetails() {
		detailsShown = !detailsShown;
		detailButton.setText(detailsShown ? LangResources.get("detailsLess") : LangResources.get("detailsMore"));
		setupDetailPanel(detailsShown);
	}

	/**
	 * Fills or clears the details panel and resizes the dialog.
	 *
	 * @param show true to show the details
	 */
	private void setupDetailPanel(final boolean show) {
		detailPanel.removeAll();

		if (show) {
			// The dialog may be created without a throwable, the details must not fail with a NullPointerException then
			final String summaryText = throwable == null ? getMainText() : throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
			final String stackTraceText = throwable == null ? null : ExceptionUtilities.throwableToString(throwable);

			detailPanel.add(createDetailTitle("Summary"));
			final JTextArea summary = TextSupport.install(new JTextArea(summaryText));
			summary.setLineWrap(true);
			summary.setWrapStyleWord(true);
			summary.setOpaque(false);
			summary.setBorder(BorderFactory.createEmptyBorder(2, 0, 8, 0));
			summary.setAlignmentX(LEFT_ALIGNMENT);
			detailPanel.add(summary);

			if (StringUtilities.isNotEmpty(stackTraceText)) {
				detailPanel.add(createDetailTitle("Stack trace"));
				final JTextArea stackTrace = TextSupport.install(new JTextArea(stackTraceText));
				stackTrace.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.max(10, ModernUi.baseFont().getSize() - 1)));
				stackTrace.setBackground(ModernUi.READ_ONLY_BACKGROUND);
				stackTrace.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
				stackTrace.setCaretPosition(0);

				final JScrollPane scroll = new JScrollPane(stackTrace);
				ModernUi.styleScrollPane(scroll, ModernUi.READ_ONLY_BACKGROUND);
				scroll.setPreferredSize(new Dimension(MESSAGE_WIDTH, 300));
				scroll.setAlignmentX(LEFT_ALIGNMENT);
				detailPanel.add(scroll);
			}
		}

		detailPanel.revalidate();
		detailPanel.repaint();

		// Only resize, the dialog must not jump back to the center if the user moved it
		pack();
	}

	/**
	 * Creates a bold title above a detail section.
	 *
	 * @param text title text
	 * @return the title label
	 */
	private static JComponent createDetailTitle(final String text) {
		final JLabel label = ModernUi.createSectionTitle(text);
		label.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}
}
