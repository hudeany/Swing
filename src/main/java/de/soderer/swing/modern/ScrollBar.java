package de.soderer.swing.modern;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Scroll bar without arrow buttons and with a slim rounded thumb */
class ScrollBar extends BasicScrollBarUI {
	/** Background of the track */
	private final Color barTrackColor;

	/**
	 * Creates the UI with the given track color.
	 *
	 * @param barTrackColor background of the track
	 */
	ScrollBar(final Color barTrackColor) {
		this.barTrackColor = barTrackColor;
	}

	@Override
	protected JButton createDecreaseButton(final int orientation) {
		return createInvisibleButton();
	}

	@Override
	protected JButton createIncreaseButton(final int orientation) {
		return createInvisibleButton();
	}

	/**
	 * Creates a zero sized replacement for the arrow buttons.
	 *
	 * @return the invisible button
	 */
	private static JButton createInvisibleButton() {
		final JButton button = new JButton();
		button.setPreferredSize(new Dimension(0, 0));
		button.setMinimumSize(new Dimension(0, 0));
		button.setMaximumSize(new Dimension(0, 0));
		return button;
	}

	@Override
	protected void paintTrack(final Graphics graphics, final JComponent component, final Rectangle trackBounds) {
		graphics.setColor(barTrackColor);
		graphics.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
	}

	@Override
	protected void paintThumb(final Graphics graphics, final JComponent component, final Rectangle thumbBounds) {
		if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
			return;
		}
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			final boolean vertical = scrollbar.getOrientation() == Adjustable.VERTICAL;
			final double thickness = 6;
			final double x = vertical ? thumbBounds.x + (thumbBounds.width - thickness) / 2 : thumbBounds.x + 2;
			final double y = vertical ? thumbBounds.y + 2 : thumbBounds.y + (thumbBounds.height - thickness) / 2;
			final double width = vertical ? thickness : thumbBounds.width - 4;
			final double height = vertical ? thumbBounds.height - 4 : thickness;
			g2.setColor(isDragging || isThumbRollover() ? new Color(0x9AA3B5) : new Color(0xC6CCD8));
			g2.fill(new RoundRectangle2D.Double(x, y, width, height, thickness, thickness));
		} finally {
			g2.dispose();
		}
	}
}
