package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog in the {@link ModernUi} design showing a (possibly multi-line) text with a row of answer
 * buttons and an optional checkbox. The text can be selected and copied (CTRL+C or context menu).
 * Same API as {@link de.soderer.swing.QuestionDialog}.
 *
 * <p>
 * {@link #open()} returns the index of the pressed button, or null if the dialog was closed via the window
 * decoration or (without button texts) via the close button.
 * </p>
 *
 * <p>
 * The header icon and color follow the {@link DialogStyle}. Without an explicit style it is chosen
 * automatically: {@link DialogStyle#ERROR} for a reddish background color (e.g.
 * {@link de.soderer.swing.SwingColor#LightRed}, as used for error messages), {@link DialogStyle#QUESTION} for
 * several buttons, otherwise {@link DialogStyle#INFORMATION}.
 * </p>
 */
public class QuestionDialog extends ModalDialog<Integer> {
	/** Serialization version */
	private static final long serialVersionUID = 4471923567819023045L;

	/** Maximum size of the text area before it scrolls */
	private static final int MAX_TEXT_WIDTH = 600;
	/** Maximum height of the text area before it scrolls */
	private static final int MAX_TEXT_HEIGHT = 400;
	/** Minimum width of the text area, so short texts do not result in a tiny dialog */
	private static final int MIN_TEXT_WIDTH = 280;

	/** Text to show */
	private final String text;

	/** Background color of the text, null for none */
	private Color backgroundcolor = null;

	/** Texts of the answer buttons; if none are given, a single "close" button is shown */
	private final String[] buttonTexts;

	/** Text of the additional checkbox, blank for no checkbox */
	private String checkboxText = null;

	/** Status of the checkbox, also available after the dialog was closed */
	private boolean checkboxStatus = false;

	/** Explicitly set kind of message, null for automatic */
	private DialogStyle dialogStyle = null;

	/** Default button, which gets the initial focus when the dialog is opened */
	private JButton defaultButton;

	/**
	 * Creates a question dialog. The first button is the default button.
	 *
	 * @param parent      owner window, may be null
	 * @param title       title of the dialog
	 * @param text        text to show
	 * @param buttonTexts texts of the answer buttons; if none are given, a single "close" button is shown
	 */
	public QuestionDialog(final Window parent, final String title, final String text, final String... buttonTexts) {
		super(parent, title);

		this.text = text;
		this.buttonTexts = buttonTexts;

		setResizable(false);

		createComponents();

		// The text area is focusable for selecting and copying, but the initial focus belongs to the default button
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(final WindowEvent event) {
				if (defaultButton != null) {
					defaultButton.requestFocusInWindow();
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

		final DialogStyle effectiveStyle = getEffectiveDialogStyle();
		switch (effectiveStyle) {
			case ERROR -> {
				setHeaderIcon(IconKind.ERROR);
				setHeaderStyle(HeaderStyle.DANGER);
			}
			case WARNING -> {
				setHeaderIcon(IconKind.WARNING);
				setHeaderStyle(HeaderStyle.WARNING);
			}
			case QUESTION -> {
				setHeaderIcon(IconKind.HELP);
				setHeaderStyle(HeaderStyle.NORMAL);
			}
			case INFORMATION -> {
				setHeaderIcon(IconKind.INFO);
				setHeaderStyle(HeaderStyle.NORMAL);
			}
			default -> {
				setHeaderIcon(IconKind.INFO);
				setHeaderStyle(HeaderStyle.NORMAL);
			}
		}

		final JTextArea textArea = TextSupport.install(new JTextArea(text));
		textArea.setCaretPosition(0);
		final JScrollPane textScrollpane = new JScrollPane(textArea);
		if (backgroundcolor != null) {
			// Tinted, rounded box around the text
			textArea.setBackground(backgroundcolor);
			textArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
			ModernUi.styleScrollPane(textScrollpane, backgroundcolor);
		} else {
			textArea.setOpaque(false);
			textArea.setBorder(BorderFactory.createEmptyBorder());
			textScrollpane.setOpaque(false);
			textScrollpane.getViewport().setOpaque(false);
			textScrollpane.setBorder(BorderFactory.createEmptyBorder());
			ModernUi.styleScrollBars(textScrollpane, ModernUi.CARD);
		}
		final Dimension preferredTextSize = textScrollpane.getPreferredSize();
		textScrollpane.setPreferredSize(new Dimension(
			Math.max(MIN_TEXT_WIDTH, Math.min(preferredTextSize.width, MAX_TEXT_WIDTH)),
			Math.min(preferredTextSize.height, MAX_TEXT_HEIGHT)));
		getContentArea().add(textScrollpane, BorderLayout.CENTER);

		if (StringUtilities.isNotBlank(checkboxText)) {
			final JCheckBox checkboxCheckBox = new JCheckBox(checkboxText);
			checkboxCheckBox.setOpaque(false);
			checkboxCheckBox.setSelected(checkboxStatus);
			checkboxCheckBox.addActionListener(event -> checkboxStatus = checkboxCheckBox.isSelected());
			getContentArea().add(checkboxCheckBox, BorderLayout.SOUTH);
		}

		JButton buttonToSelect = null;
		if (buttonTexts != null && buttonTexts.length > 0) {
			for (int i = 0; i < buttonTexts.length; i++) {
				final int answerButtonIndex = i;
				// The first button is the default answer and therefore highlighted
				final JButton answerButton = addButton(buttonTexts[i], null, i == 0 ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY, event -> {
					returnValue = answerButtonIndex;
					dispose();
				});
				if (i == 0) {
					buttonToSelect = answerButton;
				}
			}
		} else {
			buttonToSelect = addButton(LangResources.get("close"), null, ButtonStyle.PRIMARY, event -> dispose());
		}

		pack();

		if (textScrollpane.getHorizontalScrollBar().isVisible()) {
			// Room for the horizontal scroll bar, so the last text line is not hidden behind it
			textScrollpane.setPreferredSize(new Dimension(textScrollpane.getPreferredSize().width, textScrollpane.getPreferredSize().height + 20));
			pack();
		}

		setLocationRelativeTo(getOwner());

		defaultButton = buttonToSelect;
		getRootPane().setDefaultButton(buttonToSelect);
	}

	/**
	 * Returns the explicitly set kind of message, or the automatically chosen one.
	 *
	 * @return the effective kind of message
	 */
	private DialogStyle getEffectiveDialogStyle() {
		if (dialogStyle != null) {
			return dialogStyle;
		} else if (isReddish(backgroundcolor)) {
			return DialogStyle.ERROR;
		} else if (buttonTexts != null && buttonTexts.length > 1) {
			return DialogStyle.QUESTION;
		} else {
			return DialogStyle.INFORMATION;
		}
	}

	/**
	 * Whether a color is clearly red, like the background colors used for error messages.
	 *
	 * @param color color to check, may be null
	 * @return true for a reddish color
	 */
	private static boolean isReddish(final Color color) {
		return color != null && color.getRed() > color.getGreen() + 30 && color.getRed() > color.getBlue() + 30;
	}

	/**
	 * Sets the background color of the text, e.g. {@link de.soderer.swing.SwingColor#LightRed} for error messages
	 * (which also switches to the error style, unless a style was set explicitly).
	 *
	 * @param backgroundcolor background color, null for none
	 */
	public void setBackgroundColor(final Color backgroundcolor) {
		this.backgroundcolor = backgroundcolor;
		createComponents();
	}

	/**
	 * Sets the background color of the text and returns this dialog for chaining.
	 *
	 * @param backgroundcolorValue background color, null for none
	 * @return this dialog for chaining
	 */
	public QuestionDialog withBackgroundColor(final Color backgroundcolorValue) {
		setBackgroundColor(backgroundcolorValue);
		return this;
	}

	/**
	 * Sets the kind of message, which decides about header icon and color.
	 *
	 * @param dialogStyle kind of message, null for automatic
	 */
	public void setDialogStyle(final DialogStyle dialogStyle) {
		this.dialogStyle = dialogStyle;
		createComponents();
	}

	/**
	 * Sets the kind of message and returns this dialog for chaining.
	 *
	 * @param dialogStyleValue kind of message, null for automatic
	 * @return this dialog for chaining
	 */
	public QuestionDialog withDialogStyle(final DialogStyle dialogStyleValue) {
		setDialogStyle(dialogStyleValue);
		return this;
	}

	/**
	 * Sets the text of an additional checkbox shown below the text.
	 *
	 * @param checkboxText text of the checkbox, blank for no checkbox
	 */
	public void setCheckboxText(final String checkboxText) {
		this.checkboxText = checkboxText;
		createComponents();
	}

	/**
	 * Sets the text of an additional checkbox and returns this dialog for chaining.
	 *
	 * @param checkboxTextValue text of the checkbox, blank for no checkbox
	 * @return this dialog for chaining
	 */
	public QuestionDialog withCheckboxText(final String checkboxTextValue) {
		setCheckboxText(checkboxTextValue);
		return this;
	}

	/**
	 * Sets the initial status of the checkbox.
	 *
	 * @param checkboxStatus true to check the checkbox initially
	 */
	public void setCheckboxStatus(final boolean checkboxStatus) {
		this.checkboxStatus = checkboxStatus;
		createComponents();
	}

	/**
	 * Sets the initial status of the checkbox and returns this dialog for chaining.
	 *
	 * @param checkboxStatusValue true to check the checkbox initially
	 * @return this dialog for chaining
	 */
	public QuestionDialog withCheckboxStatus(final boolean checkboxStatusValue) {
		setCheckboxStatus(checkboxStatusValue);
		return this;
	}

	/**
	 * Returns the status of the checkbox, also after the dialog was closed.
	 *
	 * @return true if the checkbox is checked
	 */
	public boolean getCheckboxStatus() {
		return checkboxStatus;
	}
}
