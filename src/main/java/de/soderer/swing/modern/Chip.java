package de.soderer.swing.modern;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Pill shaped label for compact information like "Model: Pixel 8".
 */
public class Chip extends JLabel {
	/** Serialization version */
	private static final long serialVersionUID = 1L;
	/** Pill background color */
	private final Color background;

	/**
	 * Creates a chip showing an optional grey caption followed by the bold value.
	 *
	 * @param caption grey text in front of the value, null or empty for none
	 * @param value   bold value text
	 * @param style   color variant
	 */
	public Chip(final String caption, final String value, final ChipStyle style) {
		final Color foreground = switch (style) {
			case ACCENTUATED -> ModernUi.ACCENT_PRESSED;
			case SUCCESSFUL -> ModernUi.SUCCESS;
			case ALERT -> ModernUi.WARNING;
			case DANGEROUS -> ModernUi.DANGER;
			case NEUTRAL -> ModernUi.TEXT;
			default -> ModernUi.TEXT;
		};
		background = switch (style) {
			case ACCENTUATED -> ModernUi.ACCENT_LIGHT;
			case SUCCESSFUL -> ModernUi.SUCCESS_LIGHT;
			case ALERT -> ModernUi.WARNING_LIGHT;
			case DANGEROUS -> ModernUi.DANGER_LIGHT;
			case NEUTRAL -> ModernUi.NEUTRAL_LIGHT;
			default -> ModernUi.NEUTRAL_LIGHT;
		};
		final StringBuilder html = new StringBuilder("<html><span style='color:").append(ModernUi.toHex(ModernUi.MUTED)).append("'>");
		if (caption != null && !caption.isEmpty()) {
			html.append(ModernUi.escapeHtml(caption)).append("&nbsp;</span>");
		} else {
			html.append("</span>");
		}
		html.append("<b style='color:").append(ModernUi.toHex(foreground)).append("'>").append(ModernUi.escapeHtml(value)).append("</b></html>");
		setText(html.toString());
		setFont(ModernUi.baseFont());
		setOpaque(false);
		setHorizontalAlignment(SwingConstants.CENTER);
		setBorder(BorderFactory.createEmptyBorder(4, 11, 4, 11));
	}

	@Override
	protected void paintComponent(final Graphics graphics) {
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			g2.setColor(background);
			g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
		} finally {
			g2.dispose();
		}
		super.paintComponent(graphics);
	}
}
