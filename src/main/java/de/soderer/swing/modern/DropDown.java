package de.soderer.swing.modern;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;

/**
 * Combo with autocomplete in the {@link ModernUi} design: the input field and the dropdown button form one
 * rounded field with an accent colored outline while focused, the popup list has more room per entry, a light
 * accent selection and slim scroll bars.
 *
 * <p>
 * Behavior and API are those of {@link de.soderer.swing.DropDown}, only the look differs. The fluent
 * {@code with...} methods return this class, so {@code new DropDown().withItems(...)} can be assigned to a
 * variable of this type. The nested {@link de.soderer.swing.DropDown.MatchMode} is still imported from the base
 * class.
 * </p>
 */
public class DropDown extends de.soderer.swing.DropDown {
	/** Serialization version */
	private static final long serialVersionUID = -7120934681573309442L;

	/** Width of the dropdown button inside the field */
	private static final int BUTTON_WIDTH = 24;

	/** Color of the gray suggestion parts, the hint text and the disabled text */
	private static final Color SUGGESTION_COLOR = new Color(0xA3A9B6);

	/**
	 * Creates a DropDown.
	 */
	public DropDown() {
		super();

		setSuggestionColor(SUGGESTION_COLOR);
		setInvalidColor(ModernUi.DANGER);

		// The rounded outline of the whole field shows the focus of the input field
		final JTextPane textPane = getTextControl();
		textPane.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(final FocusEvent event) {
				repaint();
			}

			@Override
			public void focusLost(final FocusEvent event) {
				repaint();
			}
		});
	}

	/**
	 * Creates a flat button with a chevron, which sits inside the rounded field.
	 *
	 * @return the dropdown button
	 */
	@Override
	protected JButton createDropdownButton() {
		final Button button = new Button(null, null, ButtonStyle.TOOLBAR);
		button.setIcon(new ThemeIcon(IconKind.CHEVRON_DOWN, 14, ModernUi.MUTED));
		button.setBorder(BorderFactory.createEmptyBorder());
		// The height follows the input field (BorderLayout), only the width matters
		button.setPreferredSize(new Dimension(BUTTON_WIDTH, 10));
		return button;
	}

	/**
	 * Applies the modern field look instead of the look and feel's text field look.
	 */
	@Override
	protected void applyLookAndFeelDefaults() {
		final JTextPane textPane = getTextControl();

		final JScrollPane textScrollPane = getTextScrollPane();
		textScrollPane.setBorder(BorderFactory.createEmptyBorder());
		textScrollPane.setViewportBorder(null);

		// Together with the field border the same padding as ModernUi.styleTextField()
		textPane.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 2));
		// Same font as the text fields styled by ModernUi.styleTextField(), which keep the look and feel's font
		final Font textFieldFont = UIManager.getFont("TextField.font");
		textPane.setFont(textFieldFont != null ? textFieldFont : ModernUi.baseFont().deriveFont(Font.PLAIN));
		textPane.setForeground(ModernUi.TEXT);
		textPane.setCaretColor(ModernUi.TEXT);
		textPane.setSelectionColor(ModernUi.ACCENT_LIGHT);
		textPane.setSelectedTextColor(ModernUi.TEXT);
		textPane.setDisabledTextColor(ModernUi.DISABLED_TEXT);

		setOpaque(true);
		setBorder(new RoundedFieldBorder(2, 4, textPane));

		applyBackground();
	}

	/**
	 * White background while enabled, the read-only background while disabled.
	 */
	@Override
	protected void applyBackground() {
		final Color background = isEnabled() ? Color.WHITE : ModernUi.READ_ONLY_BACKGROUND;
		setBackground(background);
		getTextControl().setBackground(background);
		getTextScrollPane().getViewport().setBackground(background);
	}

	/**
	 * Gives the popup a thin outline, slim scroll bars, more room per entry and a light accent selection.
	 *
	 * @param popupContentScrollPane scroll pane around the popup list
	 * @param popupContentList       the popup list
	 */
	@Override
	protected void configurePopupContent(final JScrollPane popupContentScrollPane, final JList<String> popupContentList) {
		popupContentScrollPane.setBorder(BorderFactory.createLineBorder(ModernUi.CARD_BORDER));
		popupContentScrollPane.getViewport().setBackground(Color.WHITE);
		ModernUi.styleScrollBars(popupContentScrollPane, Color.WHITE);

		popupContentList.setBackground(Color.WHITE);
		popupContentList.setForeground(ModernUi.TEXT);
		popupContentList.setSelectionBackground(ModernUi.ACCENT_LIGHT);
		popupContentList.setSelectionForeground(ModernUi.TEXT);
		popupContentList.setCellRenderer(new DefaultListCellRenderer() {
			/** Serialization version */
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, false);
				// Left padding aligned with the text of the input field
				setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
				return this;
			}
		});
	}

	/**
	 * Marker color of drag&amp;drop reordering in the accent color.
	 *
	 * @return marker color
	 */
	@Override
	protected Color getInsertionMarkerColor() {
		return ModernUi.ACCENT;
	}

	// ---------------------------------------------------------------- fluent methods returning this class

	/**
	 * Sets the values of the suggestion list and returns this instance for chaining.
	 *
	 * @param values values, null for none
	 * @return this instance
	 */
	@Override
	public DropDown withItems(final String... values) {
		super.withItems(values);
		return this;
	}

	/**
	 * Sets the values of the suggestion list and returns this instance for chaining.
	 *
	 * @param values values, null for none
	 * @return this instance
	 */
	@Override
	public DropDown withItems(final List<String> values) {
		super.withItems(values);
		return this;
	}

	/**
	 * Sets the text and returns this instance for chaining.
	 *
	 * @param value text, null for an empty text
	 * @return this instance
	 */
	@Override
	public DropDown withText(final String value) {
		super.withText(value);
		return this;
	}

	/**
	 * Sets whether values are allowed that are not in the list and returns this instance for chaining.
	 *
	 * @param allowCustomValuesParam true to allow custom values
	 * @return this instance
	 */
	@Override
	public DropDown withAllowCustomValues(final boolean allowCustomValuesParam) {
		super.withAllowCustomValues(allowCustomValuesParam);
		return this;
	}

	/**
	 * Sets whether the search is case-sensitive and returns this instance for chaining.
	 *
	 * @param caseSensitiveValue true for a case-sensitive search
	 * @return this instance
	 */
	@Override
	public DropDown withCaseSensitive(final boolean caseSensitiveValue) {
		super.withCaseSensitive(caseSensitiveValue);
		return this;
	}

	/**
	 * Sets the search mode and returns this instance for chaining.
	 *
	 * @param matchModeValue search mode
	 * @return this instance
	 */
	@Override
	public DropDown withMatchMode(final MatchMode matchModeValue) {
		super.withMatchMode(matchModeValue);
		return this;
	}

	/**
	 * Sets whether entries can be reordered via drag&amp;drop and returns this instance for chaining.
	 *
	 * @param reorderableValue true to allow reordering
	 * @return this instance
	 */
	@Override
	public DropDown withReorderable(final boolean reorderableValue) {
		super.withReorderable(reorderableValue);
		return this;
	}

	/**
	 * Sets whether entries can be removed via the Delete key and returns this instance for chaining.
	 *
	 * @param deletableValue true to allow deleting
	 * @return this instance
	 */
	@Override
	public DropDown withDeletable(final boolean deletableValue) {
		super.withDeletable(deletableValue);
		return this;
	}

	/**
	 * Enables or disables this control and returns this instance for chaining.
	 *
	 * @param enabled true to enable the control
	 * @return this instance
	 */
	@Override
	public DropDown withEnabled(final boolean enabled) {
		super.withEnabled(enabled);
		return this;
	}

	/**
	 * Sets the hint text shown while the field is empty and returns this instance for chaining.
	 *
	 * @param message hint text, null for none
	 * @return this instance
	 */
	@Override
	public DropDown withMessage(final String message) {
		super.withMessage(message);
		return this;
	}

	/**
	 * Sets the preferred width in columns and returns this instance for chaining.
	 *
	 * @param columnsValue columns, 0 to size the field by the widest item
	 * @return this instance
	 */
	@Override
	public DropDown withColumns(final int columnsValue) {
		super.withColumns(columnsValue);
		return this;
	}

	/**
	 * Shows or hides the dropdown button and returns this instance for chaining.
	 *
	 * @param show true to show the button
	 * @return this instance
	 */
	@Override
	public DropDown withShowDropdownButton(final boolean show) {
		super.withShowDropdownButton(show);
		return this;
	}

	/**
	 * Sets the color of the suggestion parts, the hint text and the disabled text and returns this instance for
	 * chaining.
	 *
	 * @param suggestionColorValue suggestion color, not null
	 * @return this instance
	 */
	@Override
	public DropDown withSuggestionColor(final Color suggestionColorValue) {
		super.withSuggestionColor(suggestionColorValue);
		return this;
	}

	/**
	 * Sets the color of invalid text and returns this instance for chaining.
	 *
	 * @param invalidColorValue invalid color, not null
	 * @return this instance
	 */
	@Override
	public DropDown withInvalidColor(final Color invalidColorValue) {
		super.withInvalidColor(invalidColorValue);
		return this;
	}
}
