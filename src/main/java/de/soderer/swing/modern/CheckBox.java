package de.soderer.swing.modern;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicRadioButtonUI;

/**
 * Check box UI in ModernUi style: rounded box with accent colored fill and white check mark,
 * hover and pressed feedback and a soft focus ring. The box is painted completely by this class,
 * so it looks the same with every look and feel. This especially fixes the GTK look and feel on
 * Linux (e.g. Ubuntu), where the native check mark is often hardly or not at all visible.
 *
 * <p>Installed for all check boxes by {@link #install()}, which is called by {@link ModernUi}
 * automatically. {@link RadioButtonUI} does the same for radio buttons.</p>
 */
public class CheckBox extends BasicCheckBoxUI {
	/** Box icon of this check box, created when the defaults are installed */
	private ToggleIcon toggleIcon;

	/**
	 * Creates a new check box UI. Usually called by {@link #createUI(JComponent)}.
	 */
	public CheckBox() {
		super();
	}

	/**
	 * Called by the UIManager via reflection.
	 *
	 * @param component the check box
	 * @return new UI instance
	 */
	public static ComponentUI createUI(final JComponent component) {
		return new CheckBox();
	}

	/**
	 * Registers this UI for all check boxes and {@link RadioButtonUI} for all radio buttons.
	 * Survives look-and-feel changes, because it is stored in the developer defaults of the UIManager.
	 */
	public static void install() {
		UIManager.put("CheckBoxUI", CheckBox.class.getName());
		// Store the classes themselves, so UIDefaults does not depend on its classloader lookup
		UIManager.put(CheckBox.class.getName(), CheckBox.class);
		UIManager.put("RadioButtonUI", RadioButtonUI.class.getName());
		UIManager.put(RadioButtonUI.class.getName(), RadioButtonUI.class);
	}

	@Override
	protected void installDefaults(final AbstractButton button) {
		super.installDefaults(button);
		installModernDefaults(button);
		toggleIcon = new ToggleIcon(false);
	}

	@Override
	public Icon getDefaultIcon() {
		return toggleIcon;
	}

	@Override
	protected void paintText(final Graphics graphics, final AbstractButton button, final Rectangle textRect, final String text) {
		paintModernText(graphics, button, textRect, text);
	}

	@Override
	protected void paintFocus(final Graphics graphics, final Rectangle textRect, final Dimension size) {
		// Focus is shown as ring around the box by ToggleIcon
	}

	/**
	 * Radio button UI in ModernUi style: round counterpart of {@link CheckBox}.
	 */
	public static class RadioButtonUI extends BasicRadioButtonUI {
		/** Circle icon of this radio button, created when the defaults are installed */
		private ToggleIcon toggleIcon;

		/**
		 * Creates a new radio button UI. Usually called by {@link #createUI(JComponent)}.
		 */
		public RadioButtonUI() {
			super();
		}

		/**
		 * Called by the UIManager via reflection.
		 *
		 * @param component the radio button
		 * @return new UI instance
		 */
		public static ComponentUI createUI(final JComponent component) {
			return new RadioButtonUI();
		}

		@Override
		protected void installDefaults(final AbstractButton button) {
			super.installDefaults(button);
			installModernDefaults(button);
			toggleIcon = new ToggleIcon(true);
		}

		@Override
		public Icon getDefaultIcon() {
			return toggleIcon;
		}

		@Override
		protected void paintText(final Graphics graphics, final AbstractButton button, final Rectangle textRect, final String text) {
			paintModernText(graphics, button, textRect, text);
		}

		@Override
		protected void paintFocus(final Graphics graphics, final Rectangle textRect, final Dimension size) {
			// Focus is shown as ring around the circle by ToggleIcon
		}
	}

	/**
	 * Replaces missing or look-and-feel provided values. Some look and feels (GTK/Synth) do not define
	 * the Basic defaults, so font and colors would otherwise be missing. Values set by the application are kept.
	 *
	 * @param button check box or radio button to configure
	 */
	static void installModernDefaults(final AbstractButton button) {
		if (button.getFont() == null || button.getFont() instanceof UIResource) {
			button.setFont(new FontUIResource(ModernUi.baseFont()));
		}
		if (button.getForeground() == null || button.getForeground() instanceof UIResource) {
			button.setForeground(new ColorUIResource(ModernUi.TEXT));
		}
		button.setRolloverEnabled(true);
		button.setIconTextGap(7);
	}

	/**
	 * Paints the (non HTML) text: normal foreground, or a flat grey when disabled,
	 * instead of the embossed disabled text of BasicButtonUI. The mnemonic is underlined while enabled.
	 *
	 * @param graphics graphics to paint on
	 * @param button   check box or radio button whose text is painted
	 * @param textRect area for the text, calculated by the layout of the button UI
	 * @param text     text to paint, possibly clipped with "..."
	 */
	static void paintModernText(final Graphics graphics, final AbstractButton button, final Rectangle textRect, final String text) {
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			final FontMetrics fontMetrics = button.getFontMetrics(button.getFont());
			g2.setFont(button.getFont());
			g2.setColor(button.getModel().isEnabled() ? button.getForeground() : ModernUi.DISABLED_TEXT);
			final int mnemonicIndex = button.getModel().isEnabled() ? button.getDisplayedMnemonicIndex() : -1;
			BasicGraphicsUtils.drawStringUnderlineCharAt(button, g2, text, mnemonicIndex, textRect.x, textRect.y + fontMetrics.getAscent());
		} finally {
			g2.dispose();
		}
	}

	/**
	 * Box (check box) or circle (radio button) that paints itself depending on the button's model state.
	 */
	public static class ToggleIcon implements Icon {
		/** Space around the box for the focus ring */
		private static final int FOCUS_MARGIN = 2;

		/** True for a circle (radio button), false for a rounded box (check box) */
		private final boolean round;
		/** Width and height of the box or circle in pixels, without the focus margin */
		private final int boxSize;

		/**
		 * Creates an icon sized relative to the ModernUi base font, so it scales with the desktop font size.
		 *
		 * @param round true for radio buttons (circle), false for check boxes (rounded box)
		 */
		public ToggleIcon(final boolean round) {
			this.round = round;
			boxSize = Math.max(14, Math.round(ModernUi.baseFont().getSize2D() * 1.25f));
		}

		@Override
		public int getIconWidth() {
			return boxSize + 2 * FOCUS_MARGIN;
		}

		@Override
		public int getIconHeight() {
			return boxSize + 2 * FOCUS_MARGIN;
		}

		@Override
		public void paintIcon(final Component component, final Graphics graphics, final int x, final int y) {
			final ButtonModel model = component instanceof final AbstractButton button ? button.getModel() : null;
			final boolean enabled = model == null || model.isEnabled();
			final boolean selected = model != null && model.isSelected();
			final boolean pressed = model != null && model.isArmed() && model.isPressed();
			final boolean rollover = model != null && model.isRollover();
			final boolean focused = component instanceof final AbstractButton button && button.isFocusPainted() && button.hasFocus();

			final Graphics2D g2 = ModernUi.prepare(graphics);
			try {
				final float boxX = x + FOCUS_MARGIN;
				final float boxY = y + FOCUS_MARGIN;
				final float size = boxSize;

				if (enabled && focused) {
					g2.setColor(ModernUi.withAlpha(ModernUi.ACCENT, 70));
					g2.fill(shape(boxX - FOCUS_MARGIN, boxY - FOCUS_MARGIN, size + 2 * FOCUS_MARGIN));
				}

				final Color fill;
				final Color border;
				if (!enabled) {
					fill = selected ? ModernUi.DISABLED_TEXT : ModernUi.NEUTRAL_LIGHT;
					border = selected ? ModernUi.DISABLED_TEXT : ModernUi.CARD_BORDER;
				} else if (selected) {
					fill = pressed ? ModernUi.ACCENT_PRESSED : rollover ? ModernUi.ACCENT_HOVER : ModernUi.ACCENT;
					border = fill;
				} else {
					fill = pressed ? ModernUi.ACCENT_LIGHT : ModernUi.CARD;
					border = rollover || pressed || focused ? ModernUi.ACCENT : ModernUi.mix(ModernUi.CARD_BORDER, ModernUi.MUTED, 0.45);
				}

				g2.setColor(fill);
				g2.fill(shape(boxX, boxY, size));
				g2.setColor(border);
				g2.setStroke(new BasicStroke(1f));
				g2.draw(shape(boxX + 0.5f, boxY + 0.5f, size - 1));

				if (selected) {
					g2.setColor(Color.WHITE);
					if (round) {
						final float dot = size * 0.4f;
						g2.fill(new Ellipse2D.Float(boxX + (size - dot) / 2, boxY + (size - dot) / 2, dot, dot));
					} else {
						final Path2D.Float checkMark = new Path2D.Float();
						checkMark.moveTo(boxX + size * 0.25f, boxY + size * 0.52f);
						checkMark.lineTo(boxX + size * 0.43f, boxY + size * 0.70f);
						checkMark.lineTo(boxX + size * 0.76f, boxY + size * 0.32f);
						g2.setStroke(new BasicStroke(Math.max(1.7f, size / 8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						g2.draw(checkMark);
					}
				}
			} finally {
				g2.dispose();
			}
		}

		/**
		 * Creates the outline shape of the icon: a circle for radio buttons, a rounded square for check boxes.
		 *
		 * @param x    left edge
		 * @param y    top edge
		 * @param size width and height
		 * @return the shape
		 */
		private Shape shape(final float x, final float y, final float size) {
			if (round) {
				return new Ellipse2D.Float(x, y, size, size);
			} else {
				final float arc = Math.max(5f, size * 0.32f);
				return new RoundRectangle2D.Float(x, y, size, size, arc, arc);
			}
		}
	}
}
