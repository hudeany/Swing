package de.soderer.swing.modern;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 * Flat rounded button with hover and pressed effect, painted completely by itself.
 */
public class Button extends JButton {
	/** Serialization version */
	private static final long serialVersionUID = 1L;

	/** Minimum height of all text buttons, equal for every style, so buttons in a row stay aligned */
	private static final int TEXT_BUTTON_MIN_HEIGHT = 36;

	/** Color variant */
	private final ButtonStyle style;

	/**
	 * Creates a button. Without text the button is square and shows only the icon, so it should get a tooltip.
	 *
	 * @param text     button text, null or empty for an icon-only button
	 * @param iconKind symbol shown left of the text in the text color, null for none
	 * @param style    color variant
	 */
	public Button(final String text, final IconKind iconKind, final ButtonStyle style) {
		super(text);
		this.style = style;
		if (iconKind != null) {
			setIcon(new ThemeIcon(iconKind, style == ButtonStyle.HEADER && (text == null || text.isEmpty()) ? 20 : 15, ModernUi.TEXT));
		}
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setOpaque(false);
		setRolloverEnabled(true);
		setIconTextGap(8);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		if (text == null || text.isEmpty()) {
			setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		} else if (style == ButtonStyle.PRIMARY) {
			setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 22));
		} else {
			setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
		}
		if (style == ButtonStyle.PRIMARY) {
			setFont(ModernUi.baseFont().deriveFont(Font.BOLD, ModernUi.baseFont().getSize2D() + 1f));
		} else {
			setFont(ModernUi.baseFont());
		}
	}

	/**
	 * Calculates the size from text, icon and padding (independent of the look and feel).
	 * Text buttons of all styles are at least {@link #TEXT_BUTTON_MIN_HEIGHT} pixels high, icon-only buttons are square.
	 * An explicitly set preferred size takes precedence.
	 *
	 * @return the preferred size
	 */
	@Override
	public Dimension getPreferredSize() {
		if (isPreferredSizeSet()) {
			return super.getPreferredSize();
		}
		final Insets insets = getInsets();
		final FontMetrics metrics = getFontMetrics(getFont());
		final String text = getText();
		final int textWidth = text == null || text.isEmpty() ? 0 : metrics.stringWidth(text);
		final Icon icon = getIcon();
		final int iconWidth = icon == null ? 0 : icon.getIconWidth();
		final int iconHeight = icon == null ? 0 : icon.getIconHeight();
		final int gap = textWidth > 0 && iconWidth > 0 ? getIconTextGap() : 0;
		final int width = insets.left + insets.right + textWidth + gap + iconWidth;
		final int height = insets.top + insets.bottom + Math.max(textWidth > 0 ? metrics.getHeight() : 0, iconHeight);
		if (textWidth == 0) {
			final int side = Math.max(width, height);
			return new Dimension(side, side);
		}
		// Before, primary buttons had a minimum of 36 and all others 32 pixels, so mixed buttons in a FlowLayout row were vertically offset
		return new Dimension(width, Math.max(height, TEXT_BUTTON_MIN_HEIGHT));
	}

	/**
	 * The button never shrinks below its preferred size, so text and icon stay readable.
	 *
	 * @return the preferred size
	 */
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	protected void paintComponent(final Graphics graphics) {
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			final ButtonModel buttonModel = getModel();
			final boolean enabled = isEnabled();
			final boolean pressed = buttonModel.isArmed() && buttonModel.isPressed();
			final boolean hover = buttonModel.isRollover() && enabled;
			final int width = getWidth();
			final int height = getHeight();
			final RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, width - 1.0, height - 1.0, ModernUi.BUTTON_ARC, ModernUi.BUTTON_ARC);

			final Color foreground;
			switch (style) {
				case PRIMARY -> {
					if (enabled) {
						final Color base = pressed ? ModernUi.ACCENT_PRESSED : hover ? ModernUi.ACCENT_HOVER : ModernUi.ACCENT;
						// Soft glow below the button
						g2.setColor(ModernUi.withAlpha(ModernUi.ACCENT, hover ? 60 : 35));
						g2.fill(new RoundRectangle2D.Double(1, 3, width - 2.0, height - 3.0, ModernUi.BUTTON_ARC, ModernUi.BUTTON_ARC));
						g2.setPaint(new GradientPaint(0, 0, ModernUi.mix(base, Color.WHITE, 0.14), 0, height, base));
						g2.fill(new RoundRectangle2D.Double(0.5, 0.5, width - 1.0, height - 2.5, ModernUi.BUTTON_ARC, ModernUi.BUTTON_ARC));
						foreground = Color.WHITE;
					} else {
						g2.setColor(ModernUi.DISABLED_BACKGROUND);
						g2.fill(new RoundRectangle2D.Double(0.5, 0.5, width - 1.0, height - 2.5, ModernUi.BUTTON_ARC, ModernUi.BUTTON_ARC));
						foreground = ModernUi.DISABLED_TEXT;
					}
				}
				case SECONDARY -> {
					g2.setColor(!enabled ? new Color(0xF6F7F9) : pressed ? new Color(0xE3E9F5) : hover ? new Color(0xF0F4FB) : ModernUi.CARD);
					g2.fill(shape);
					g2.setColor(hover ? ModernUi.withAlpha(ModernUi.ACCENT, 150) : ModernUi.CARD_BORDER);
					g2.draw(shape);
					foreground = enabled ? ModernUi.TEXT : ModernUi.DISABLED_TEXT;
				}
				case DANGEROUS -> {
					g2.setColor(!enabled ? new Color(0xF6F7F9) : pressed ? new Color(0xF9DADA) : hover ? ModernUi.DANGER_LIGHT : ModernUi.CARD);
					g2.fill(shape);
					g2.setColor(enabled ? ModernUi.withAlpha(ModernUi.DANGER, hover ? 200 : 140) : ModernUi.CARD_BORDER);
					g2.draw(shape);
					foreground = enabled ? ModernUi.DANGER : ModernUi.DISABLED_TEXT;
				}
				case TOOLBAR -> {
					if (enabled && (hover || pressed)) {
						g2.setColor(pressed ? ModernUi.mix(ModernUi.ACCENT_LIGHT, ModernUi.ACCENT, 0.15) : ModernUi.ACCENT_LIGHT);
						g2.fill(shape);
					}
					foreground = enabled ? ModernUi.TEXT : ModernUi.DISABLED_TEXT;
				}
				case HEADER -> {
					if (enabled && (hover || pressed)) {
						g2.setColor(ModernUi.withAlpha(Color.WHITE, pressed ? 80 : 45));
						g2.fill(shape);
					}
					foreground = enabled ? Color.WHITE : ModernUi.withAlpha(Color.WHITE, 110);
				}
				default -> {
					if (enabled && (hover || pressed)) {
						g2.setColor(ModernUi.withAlpha(Color.WHITE, pressed ? 80 : 45));
						g2.fill(shape);
					}
					foreground = enabled ? Color.WHITE : ModernUi.withAlpha(Color.WHITE, 110);
				}
			}

			paintContent(g2, foreground, width, height - (style == ButtonStyle.PRIMARY ? 2 : 0));

			if (isFocusOwner() && enabled) {
				g2.setStroke(new BasicStroke(2f));
				g2.setColor(style == ButtonStyle.HEADER ? ModernUi.withAlpha(Color.WHITE, 160) : ModernUi.withAlpha(ModernUi.ACCENT, style == ButtonStyle.PRIMARY ? 0 : 110));
				g2.draw(new RoundRectangle2D.Double(1.5, 1.5, width - 3.0, height - 3.0, ModernUi.BUTTON_ARC, ModernUi.BUTTON_ARC));
			}
		} finally {
			g2.dispose();
		}
	}

	/**
	 * Paints icon and text centered within the given size.
	 *
	 * @param g2         prepared graphics
	 * @param foreground color of icon and text
	 * @param width      available width
	 * @param height     available height
	 */
	private void paintContent(final Graphics2D g2, final Color foreground, final int width, final int height) {
		final FontMetrics metrics = g2.getFontMetrics(getFont());
		final String text = getText();
		final int textWidth = text == null || text.isEmpty() ? 0 : metrics.stringWidth(text);
		final Icon icon = getIcon();
		final int iconWidth = icon == null ? 0 : icon.getIconWidth();
		final int gap = textWidth > 0 && iconWidth > 0 ? getIconTextGap() : 0;
		int x = (width - (iconWidth + gap + textWidth)) / 2;
		if (icon != null) {
			final int iconY = (height - icon.getIconHeight()) / 2;
			if (icon instanceof final ThemeIcon themeIcon) {
				themeIcon.paint(g2, x, iconY, foreground);
			} else {
				final Graphics2D iconGraphics = (Graphics2D) g2.create();
				if (!isEnabled()) {
					iconGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
				}
				icon.paintIcon(this, iconGraphics, x, iconY);
				iconGraphics.dispose();
			}
			x += iconWidth + gap;
		}
		if (textWidth > 0) {
			g2.setFont(getFont());
			g2.setColor(foreground);
			g2.drawString(text, x, (height - metrics.getHeight()) / 2 + metrics.getAscent());
		}
	}
}
