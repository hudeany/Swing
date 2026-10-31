package de.soderer.swing.modern;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicProgressBarUI;

/** Paints the progress bar of {@link ModernUi#createProgressBar()} completely by itself */
class ProgressBar extends BasicProgressBarUI {
	/** Creates the UI */
	ProgressBar() {
		// nothing to initialize
	}

	@Override
	public void paint(final Graphics graphics, final JComponent component) {
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			final int width = progressBar.getWidth();
			final int height = progressBar.getHeight();
			final RoundRectangle2D track = new RoundRectangle2D.Double(0, 0, width, height, height, height);
			g2.setColor(new Color(0xE3E7EF));
			g2.fill(track);
			g2.clip(track);
			final GradientPaint fill = new GradientPaint(0, 0, ModernUi.ACCENT, width, 0, ModernUi.HEADER_END);
			if (progressBar.isIndeterminate()) {
				final Rectangle box = getBox(new Rectangle());
				if (box != null) {
					g2.setPaint(fill);
					g2.fill(new RoundRectangle2D.Double(box.x, 0, box.width, height, height, height));
				}
			} else {
				final double percent = progressBar.getPercentComplete();
				if (percent > 0) {
					final double fillWidth = Math.max(height, width * percent);
					g2.setPaint(fill);
					g2.fill(new RoundRectangle2D.Double(0, 0, fillWidth, height, height, height));
				}
			}
			final String text = progressBar.isStringPainted() ? progressBar.getString() : null;
			if (text != null && !text.isEmpty()) {
				g2.setFont(progressBar.getFont());
				final FontMetrics metrics = g2.getFontMetrics();
				final int textX = (width - metrics.stringWidth(text)) / 2;
				final int textY = (height - metrics.getHeight()) / 2 + metrics.getAscent();
				g2.setColor(ModernUi.TEXT);
				g2.drawString(text, textX, textY);
				if (!progressBar.isIndeterminate() && progressBar.getPercentComplete() > 0) {
					// The part of the text lying on the colored fill is drawn in white
					final Graphics2D fillTextGraphics = (Graphics2D) g2.create();
					fillTextGraphics.clip(new Rectangle(0, 0, (int) Math.max(height, width * progressBar.getPercentComplete()), height));
					fillTextGraphics.setColor(Color.WHITE);
					fillTextGraphics.drawString(text, textX, textY);
					fillTextGraphics.dispose();
				}
			}
		} finally {
			g2.dispose();
		}
	}

	@Override
	protected int getBoxLength(final int availableLength, final int otherDimension) {
		return Math.max(otherDimension * 3, availableLength / 4);
	}
}
