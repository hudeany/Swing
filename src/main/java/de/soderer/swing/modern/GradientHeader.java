package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Header band with a horizontal color gradient and some decorative translucent circles.
 */
public class GradientHeader extends JPanel {
	/** Serialization version */
	private static final long serialVersionUID = 1L;

	/** Left color of the gradient */
	private Color startColor = ModernUi.HEADER_START;
	/** Right color of the gradient */
	private Color endColor = ModernUi.HEADER_END;

	/**
	 * Creates the header with a BorderLayout (e.g. logo WEST, title CENTER, buttons EAST) and inner padding.
	 */
	public GradientHeader() {
		super(new BorderLayout(14, 0));
		setOpaque(true);
		setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 14));
	}

	/**
	 * Changes the gradient colors, e.g. to {@link ModernUi#DANGER_HEADER_START} and {@link ModernUi#DANGER_HEADER_END} for errors.
	 *
	 * @param start left color of the gradient
	 * @param end   right color of the gradient
	 */
	public void setColors(final Color start, final Color end) {
		startColor = start;
		endColor = end;
		repaint();
	}

	@Override
	protected void paintComponent(final Graphics graphics) {
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			final int width = getWidth();
			final int height = getHeight();
			g2.setPaint(new GradientPaint(0, 0, startColor, width, height, endColor));
			g2.fillRect(0, 0, width, height);
			g2.setColor(ModernUi.withAlpha(Color.WHITE, 18));
			g2.fill(new Ellipse2D.Double(width * 0.62, -height * 1.2, height * 2.6, height * 2.6));
			g2.fill(new Ellipse2D.Double(width * 0.80, height * 0.25, height * 1.6, height * 1.6));
			g2.setColor(ModernUi.withAlpha(Color.WHITE, 12));
			g2.fill(new Ellipse2D.Double(width * 0.45, height * 0.55, height * 1.1, height * 1.1));
		} finally {
			g2.dispose();
		}
	}
}
