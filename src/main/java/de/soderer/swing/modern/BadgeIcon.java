package de.soderer.swing.modern;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/** Icon in a light accent colored rounded square, used for card titles */
class BadgeIcon implements Icon {
	/** Width and height of the badge in pixels */
	private static final int SIZE = 26;
	/** Symbol painted in the center of the badge */
	private final ThemeIcon icon;

	/**
	 * Creates the badge for the given symbol.
	 *
	 * @param kind symbol to show
	 */
	BadgeIcon(final IconKind kind) {
		icon = new ThemeIcon(kind, 16, ModernUi.ACCENT);
	}

	@Override
	public int getIconWidth() {
		return SIZE;
	}

	@Override
	public int getIconHeight() {
		return SIZE;
	}

	@Override
	public void paintIcon(final Component component, final Graphics graphics, final int x, final int y) {
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			g2.setColor(ModernUi.ACCENT_LIGHT);
			g2.fill(new RoundRectangle2D.Double(x, y, SIZE, SIZE, 9, 9));
		} finally {
			g2.dispose();
		}
		icon.paintIcon(component, graphics, x + (SIZE - 16) / 2, y + (SIZE - 16) / 2);
	}
}
