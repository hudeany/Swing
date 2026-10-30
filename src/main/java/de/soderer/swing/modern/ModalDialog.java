package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import de.soderer.swing.modern.ModernUi.ModernButton;
import de.soderer.swing.modern.ModernUi.ThemeIcon;

/**
 * Base class of document modal dialogs in the {@link ModernUi} design that return a value.
 *
 * <p>
 * Same contract as {@link de.soderer.swing.ModalDialog}: subclasses set {@link #returnValue} (e.g. in a button
 * listener) and dispose the dialog, {@link #open()} blocks until the dialog is closed and returns that value.
 * </p>
 *
 * <p>
 * The dialog consists of a gradient header showing an optional icon and the title, a white content area
 * ({@link #getContentArea()}) and a light button bar at the bottom with a left ({@link #getLeftButtonBar()})
 * and a right ({@link #getButtonBar()}) button group. Subclasses add their components to the content area
 * and their buttons via {@link #addButton(String, IconKind, ButtonStyle, ActionListener)}, then call
 * {@link #pack()}. The content pane must not be replaced.
 * </p>
 *
 * @param <T> type of the value returned by {@link #open()}
 */
public abstract class ModalDialog<T> extends JDialog {
	/** Serialization version */
	private static final long serialVersionUID = 6812749537420982167L;

	/** Minimum width of buttons in the button bar, so short texts like "OK" do not get tiny buttons */
	private static final int MIN_BUTTON_WIDTH = 96;

	/**
	 * Value returned by {@link #open()}. If it is null, the value of
	 * {@link #getDefaultReturnValue()} is returned instead.
	 */
	protected T returnValue;

	/** Gradient header with icon and title */
	private final ModernUi.GradientHeader header;
	/** Title text in the header, follows {@link #setTitle(String)} */
	private final JLabel headerLabel;
	/** White area for the dialog's components */
	private final JPanel contentArea;
	/** Bar at the bottom holding both button groups */
	private final JPanel buttonBarPanel;
	/** Left button group, e.g. for secondary actions like "copy" */
	private final JPanel leftButtonBar;
	/** Right button group for the closing actions like "OK" and "Cancel" */
	private final JPanel rightButtonBar;

	/**
	 * Creates a document modal dialog without header icon.
	 *
	 * @param owner owner window, may be null
	 * @param title title of the dialog, also shown in the header
	 */
	public ModalDialog(final Window owner, final String title) {
		this(owner, title, null);
	}

	/**
	 * Creates a document modal dialog.
	 *
	 * @param owner    owner window, may be null
	 * @param title    title of the dialog, also shown in the header
	 * @param iconKind symbol shown in the header left of the title, null for none
	 */
	public ModalDialog(final Window owner, final String title, final IconKind iconKind) {
		super(owner, title, ModalityType.DOCUMENT_MODAL);

		// JDialog default is HIDE_ON_CLOSE: a dialog closed via the window decoration would keep its native resources
		// and prevent the AWT auto shutdown (JVM would not exit after the last window was closed)
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		header = new ModernUi.GradientHeader();
		header.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
		headerLabel = new JLabel(title);
		headerLabel.setForeground(Color.WHITE);
		headerLabel.setFont(ModernUi.baseFont().deriveFont(Font.BOLD, ModernUi.baseFont().getSize2D() + 3f));
		headerLabel.setIconTextGap(10);
		header.add(headerLabel, BorderLayout.CENTER);
		setHeaderIcon(iconKind);

		contentArea = new JPanel(new BorderLayout(0, 10));
		contentArea.setBackground(ModernUi.CARD);
		contentArea.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

		leftButtonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		leftButtonBar.setOpaque(false);
		rightButtonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		rightButtonBar.setOpaque(false);
		buttonBarPanel = new JPanel(new BorderLayout(16, 0));
		buttonBarPanel.setBackground(ModernUi.READ_ONLY_BACKGROUND);
		buttonBarPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, ModernUi.INNER_BORDER),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		buttonBarPanel.add(leftButtonBar, BorderLayout.WEST);
		buttonBarPanel.add(rightButtonBar, BorderLayout.EAST);

		final JPanel rootPanel = new JPanel(new BorderLayout());
		rootPanel.setBackground(ModernUi.CARD);
		rootPanel.add(header, BorderLayout.NORTH);
		rootPanel.add(contentArea, BorderLayout.CENTER);
		rootPanel.add(buttonBarPanel, BorderLayout.SOUTH);
		setContentPane(rootPanel);
	}

	/**
	 * Sets the title of the window and of the header.
	 *
	 * @param title new title
	 */
	@Override
	public void setTitle(final String title) {
		super.setTitle(title);
		// Null while the super constructor runs
		if (headerLabel != null) {
			headerLabel.setText(title);
		}
	}

	/**
	 * Sets the symbol shown in the header left of the title.
	 *
	 * @param iconKind symbol, null for none
	 */
	protected void setHeaderIcon(final IconKind iconKind) {
		headerLabel.setIcon(iconKind == null ? null : new ThemeIcon(iconKind, 20, Color.WHITE));
	}

	/**
	 * Sets the color variant of the header.
	 *
	 * @param headerStyle color variant, null for {@link HeaderStyle#NORMAL}
	 */
	protected void setHeaderStyle(final HeaderStyle headerStyle) {
		if (headerStyle == HeaderStyle.DANGER) {
			header.setColors(ModernUi.DANGER_HEADER_START, ModernUi.DANGER_HEADER_END);
		} else if (headerStyle == HeaderStyle.WARNING) {
			header.setColors(ModernUi.WARNING_HEADER_START, ModernUi.WARNING_HEADER_END);
		} else {
			header.setColors(ModernUi.HEADER_START, ModernUi.HEADER_END);
		}
	}

	/**
	 * Returns the white area for the dialog's components. It has a BorderLayout with a vertical gap and padding.
	 * Components added to it should be transparent or white.
	 *
	 * @return the content area
	 */
	protected JPanel getContentArea() {
		return contentArea;
	}

	/**
	 * Returns the right button group of the button bar, which holds the closing actions.
	 *
	 * @return the right button group
	 */
	protected JPanel getButtonBar() {
		return rightButtonBar;
	}

	/**
	 * Returns the left button group of the button bar for secondary actions (e.g. "copy", "details").
	 *
	 * @return the left button group
	 */
	protected JPanel getLeftButtonBar() {
		return leftButtonBar;
	}

	/**
	 * Shows or hides the button bar, e.g. for dialogs whose buttons are part of the content.
	 *
	 * @param visible true to show the button bar
	 */
	protected void setButtonBarVisible(final boolean visible) {
		buttonBarPanel.setVisible(visible);
	}

	/**
	 * Creates a button and adds it to the right button group. Buttons get a minimum width.
	 *
	 * @param text           button text
	 * @param iconKind       symbol left of the text, null for none
	 * @param style          color variant, e.g. {@link ButtonStyle#PRIMARY} for the main action
	 * @param actionListener action of the button, may be null
	 * @return the new button
	 */
	protected ModernButton addButton(final String text, final IconKind iconKind, final ButtonStyle style, final ActionListener actionListener) {
		return addButton(rightButtonBar, text, iconKind, style, actionListener);
	}

	/**
	 * Creates a button and adds it to the left button group. Buttons get a minimum width.
	 *
	 * @param text           button text
	 * @param iconKind       symbol left of the text, null for none
	 * @param style          color variant
	 * @param actionListener action of the button, may be null
	 * @return the new button
	 */
	protected ModernButton addLeftButton(final String text, final IconKind iconKind, final ButtonStyle style, final ActionListener actionListener) {
		return addButton(leftButtonBar, text, iconKind, style, actionListener);
	}

	/**
	 * Creates a button with minimum width and adds it to a button group.
	 *
	 * @param buttonGroup    left or right button group
	 * @param text           button text
	 * @param iconKind       symbol left of the text, null for none
	 * @param style          color variant
	 * @param actionListener action of the button, may be null
	 * @return the new button
	 */
	private static ModernButton addButton(final JPanel buttonGroup, final String text, final IconKind iconKind, final ButtonStyle style, final ActionListener actionListener) {
		final ModernButton button = createButton(text, iconKind, style);
		if (actionListener != null) {
			button.addActionListener(actionListener);
		}
		buttonGroup.add(button);
		return button;
	}

	/**
	 * Creates a button with a minimum width of 96 pixels, e.g. for buttons placed in the content area.
	 *
	 * @param text     button text
	 * @param iconKind symbol left of the text, null for none
	 * @param style    color variant
	 * @return the new button
	 */
	public static ModernButton createButton(final String text, final IconKind iconKind, final ButtonStyle style) {
		final ModernButton button = new ModernButton(text, iconKind, style);
		final Dimension preferredSize = button.getPreferredSize();
		if (preferredSize.width < MIN_BUTTON_WIDTH) {
			button.setPreferredSize(new Dimension(MIN_BUTTON_WIDTH, preferredSize.height));
		}
		return button;
	}

	/**
	 * Removes all buttons of both button groups, e.g. before the dialog's components are rebuilt.
	 */
	protected void clearButtons() {
		leftButtonBar.removeAll();
		rightButtonBar.removeAll();
	}

	/**
	 * Shows the dialog and blocks until it is closed.
	 *
	 * @return the value set by the dialog, or {@link #getDefaultReturnValue()} if
	 *         none was set (e.g. when the dialog was closed via the window
	 *         decoration)
	 */
	public T open() {
		setVisible(true);

		if (returnValue == null) {
			return getDefaultReturnValue();
		} else {
			return returnValue;
		}
	}

	/**
	 * Returns the value returned by {@link #open()} if the dialog set no return
	 * value.
	 *
	 * @return default return value, null by default
	 */
	protected T getDefaultReturnValue() {
		return null;
	}
}
