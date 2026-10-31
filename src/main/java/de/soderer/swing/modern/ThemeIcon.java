package de.soderer.swing.modern;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/**
 * Simple line icon in a fixed color. {@link Button} repaints it in the button's text color.
 */
public class ThemeIcon implements Icon {
	/** Symbol to draw */
	private final IconKind kind;
	/** Width and height in pixels */
	private final int size;
	/** Color used by {@link #paintIcon(Component, Graphics, int, int)} */
	private final Color color;

	/**
	 * Creates an icon.
	 *
	 * @param kind  symbol to draw
	 * @param size  width and height in pixels
	 * @param color line and fill color
	 */
	public ThemeIcon(final IconKind kind, final int size, final Color color) {
		this.kind = kind;
		this.size = size;
		this.color = color;
	}

	/**
	 * Returns the symbol of this icon.
	 *
	 * @return the symbol
	 */
	public IconKind getKind() {
		return kind;
	}

	@Override
	public int getIconWidth() {
		return size;
	}

	@Override
	public int getIconHeight() {
		return size;
	}

	@Override
	public void paintIcon(final Component component, final Graphics graphics, final int x, final int y) {
		paint(graphics, x, y, color);
	}

	/**
	 * Paints the icon in another color than the one it was created with, e.g. in a button's text color.
	 *
	 * @param graphics   graphics to paint on
	 * @param x          left edge of the icon
	 * @param y          top edge of the icon
	 * @param paintColor line and fill color
	 */
	public void paint(final Graphics graphics, final int x, final int y, final Color paintColor) {
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			g2.translate(x, y);
			g2.setColor(paintColor);
			final double s = size;
			g2.setStroke(new BasicStroke((float) Math.max(1.4, s / 10.0), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			switch (kind) {
				case PHONE -> {
					g2.draw(new RoundRectangle2D.Double(s * 0.27, s * 0.06, s * 0.46, s * 0.88, s * 0.18, s * 0.18));
					g2.draw(new Line2D.Double(s * 0.44, s * 0.78, s * 0.56, s * 0.78));
				}
				case SERVER -> {
					g2.draw(new RoundRectangle2D.Double(s * 0.1, s * 0.14, s * 0.8, s * 0.3, s * 0.12, s * 0.12));
					g2.draw(new RoundRectangle2D.Double(s * 0.1, s * 0.56, s * 0.8, s * 0.3, s * 0.12, s * 0.12));
					final double dot = s * 0.12;
					g2.fill(new Ellipse2D.Double(s * 0.68, s * 0.29 - dot / 2, dot, dot));
					g2.fill(new Ellipse2D.Double(s * 0.68, s * 0.71 - dot / 2, dot, dot));
				}
				case REFRESH -> {
					final double radius = s * 0.33;
					final double center = s * 0.5;
					g2.draw(new Arc2D.Double(center - radius, center - radius, radius * 2, radius * 2, 80, 280, Arc2D.OPEN));
					// Arrow head at the end of the arc (angle 0, moving upwards)
					final Path2D arrow = new Path2D.Double();
					arrow.moveTo(center + radius - s * 0.16, center - s * 0.02);
					arrow.lineTo(center + radius + s * 0.15, center - s * 0.02);
					arrow.lineTo(center + radius, center - s * 0.22);
					arrow.closePath();
					g2.fill(arrow);
				}
				case PLAY -> {
					final Path2D triangle = new Path2D.Double();
					triangle.moveTo(s * 0.28, s * 0.16);
					triangle.lineTo(s * 0.28, s * 0.84);
					triangle.lineTo(s * 0.84, s * 0.5);
					triangle.closePath();
					g2.fill(triangle);
					g2.draw(triangle);
				}
				case STOP -> g2.fill(new RoundRectangle2D.Double(s * 0.22, s * 0.22, s * 0.56, s * 0.56, s * 0.14, s * 0.14));
				case CLOSE -> {
					g2.draw(new Line2D.Double(s * 0.26, s * 0.26, s * 0.74, s * 0.74));
					g2.draw(new Line2D.Double(s * 0.74, s * 0.26, s * 0.26, s * 0.74));
				}
				case LOG -> {
					g2.draw(new Line2D.Double(s * 0.18, s * 0.28, s * 0.82, s * 0.28));
					g2.draw(new Line2D.Double(s * 0.18, s * 0.5, s * 0.82, s * 0.5));
					g2.draw(new Line2D.Double(s * 0.18, s * 0.72, s * 0.6, s * 0.72));
				}
				case SEARCH -> {
					g2.draw(new Ellipse2D.Double(s * 0.14, s * 0.14, s * 0.5, s * 0.5));
					g2.draw(new Line2D.Double(s * 0.58, s * 0.58, s * 0.86, s * 0.86));
				}
				case FOLDER -> {
					final Path2D folder = new Path2D.Double();
					folder.moveTo(s * 0.08, s * 0.24);
					folder.lineTo(s * 0.38, s * 0.24);
					folder.lineTo(s * 0.48, s * 0.34);
					folder.lineTo(s * 0.92, s * 0.34);
					folder.lineTo(s * 0.92, s * 0.82);
					folder.lineTo(s * 0.08, s * 0.82);
					folder.closePath();
					g2.draw(folder);
				}
				case GEAR -> {
					final double center = s * 0.5;
					g2.draw(new Ellipse2D.Double(center - s * 0.2, center - s * 0.2, s * 0.4, s * 0.4));
					g2.setStroke(new BasicStroke((float) (s / 7.0), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					for (int tooth = 0; tooth < 8; tooth++) {
						final double angle = Math.PI / 4 * tooth;
						g2.draw(new Line2D.Double(center + Math.cos(angle) * s * 0.3, center + Math.sin(angle) * s * 0.3,
							center + Math.cos(angle) * s * 0.4, center + Math.sin(angle) * s * 0.4));
					}
				}
				case HELP -> {
					g2.draw(new Ellipse2D.Double(s * 0.08, s * 0.08, s * 0.84, s * 0.84));
					final Font font = ModernUi.baseFont().deriveFont(Font.BOLD, (float) (s * 0.62));
					g2.setFont(font);
					final FontMetrics metrics = g2.getFontMetrics();
					g2.drawString("?", (float) ((s - metrics.stringWidth("?")) / 2.0), (float) ((s - metrics.getHeight()) / 2.0 + metrics.getAscent()));
				}
				case SEND -> {
					final Path2D plane = new Path2D.Double();
					plane.moveTo(s * 0.08, s * 0.46);
					plane.lineTo(s * 0.92, s * 0.1);
					plane.lineTo(s * 0.62, s * 0.92);
					plane.lineTo(s * 0.46, s * 0.56);
					plane.closePath();
					g2.draw(plane);
					g2.draw(new Line2D.Double(s * 0.46, s * 0.56, s * 0.92, s * 0.1));
				}
				case EXPORT, IMPORT -> {
					final Path2D tray = new Path2D.Double();
					tray.moveTo(s * 0.14, s * 0.6);
					tray.lineTo(s * 0.14, s * 0.86);
					tray.lineTo(s * 0.86, s * 0.86);
					tray.lineTo(s * 0.86, s * 0.6);
					g2.draw(tray);
					g2.draw(new Line2D.Double(s * 0.5, s * 0.12, s * 0.5, s * 0.64));
					final Path2D arrowHead = new Path2D.Double();
					if (kind == IconKind.EXPORT) {
						arrowHead.moveTo(s * 0.3, s * 0.32);
						arrowHead.lineTo(s * 0.5, s * 0.12);
						arrowHead.lineTo(s * 0.7, s * 0.32);
					} else {
						arrowHead.moveTo(s * 0.3, s * 0.44);
						arrowHead.lineTo(s * 0.5, s * 0.64);
						arrowHead.lineTo(s * 0.7, s * 0.44);
					}
					g2.draw(arrowHead);
				}
				case TRASH -> {
					g2.draw(new Line2D.Double(s * 0.14, s * 0.26, s * 0.86, s * 0.26));
					final Path2D handle = new Path2D.Double();
					handle.moveTo(s * 0.38, s * 0.26);
					handle.lineTo(s * 0.38, s * 0.12);
					handle.lineTo(s * 0.62, s * 0.12);
					handle.lineTo(s * 0.62, s * 0.26);
					g2.draw(handle);
					final Path2D body = new Path2D.Double();
					body.moveTo(s * 0.24, s * 0.26);
					body.lineTo(s * 0.3, s * 0.88);
					body.lineTo(s * 0.7, s * 0.88);
					body.lineTo(s * 0.76, s * 0.26);
					g2.draw(body);
				}
				case FAST_FORWARD -> {
					for (final double offset : new double[] { 0.06, 0.46 }) {
						final Path2D triangle = new Path2D.Double();
						triangle.moveTo(s * offset, s * 0.22);
						triangle.lineTo(s * offset, s * 0.78);
						triangle.lineTo(s * (offset + 0.44), s * 0.5);
						triangle.closePath();
						g2.fill(triangle);
						g2.draw(triangle);
					}
				}
				case PLUS -> {
					g2.draw(new Line2D.Double(s * 0.5, s * 0.2, s * 0.5, s * 0.8));
					g2.draw(new Line2D.Double(s * 0.2, s * 0.5, s * 0.8, s * 0.5));
				}
				case FILE -> {
					final Path2D file = new Path2D.Double();
					file.moveTo(s * 0.22, s * 0.08);
					file.lineTo(s * 0.58, s * 0.08);
					file.lineTo(s * 0.8, s * 0.3);
					file.lineTo(s * 0.8, s * 0.92);
					file.lineTo(s * 0.22, s * 0.92);
					file.closePath();
					g2.draw(file);
					final Path2D fold = new Path2D.Double();
					fold.moveTo(s * 0.58, s * 0.08);
					fold.lineTo(s * 0.58, s * 0.3);
					fold.lineTo(s * 0.8, s * 0.3);
					g2.draw(fold);
				}
				case LOCK -> {
					g2.draw(new RoundRectangle2D.Double(s * 0.18, s * 0.44, s * 0.64, s * 0.46, s * 0.12, s * 0.12));
					g2.draw(new Arc2D.Double(s * 0.3, s * 0.1, s * 0.4, s * 0.5, 0, 180, Arc2D.OPEN));
					g2.draw(new Line2D.Double(s * 0.3, s * 0.35, s * 0.3, s * 0.44));
					g2.draw(new Line2D.Double(s * 0.7, s * 0.35, s * 0.7, s * 0.44));
				}
				case GLOBE -> {
					g2.draw(new Ellipse2D.Double(s * 0.1, s * 0.1, s * 0.8, s * 0.8));
					g2.draw(new Ellipse2D.Double(s * 0.32, s * 0.1, s * 0.36, s * 0.8));
					g2.draw(new Line2D.Double(s * 0.1, s * 0.5, s * 0.9, s * 0.5));
				}
				case INFO, ERROR -> {
					g2.draw(new Ellipse2D.Double(s * 0.08, s * 0.08, s * 0.84, s * 0.84));
					final double dot = s * 0.13;
					if (kind == IconKind.INFO) {
						g2.fill(new Ellipse2D.Double(s * 0.5 - dot / 2, s * 0.27 - dot / 2, dot, dot));
						g2.draw(new Line2D.Double(s * 0.5, s * 0.44, s * 0.5, s * 0.72));
					} else {
						g2.draw(new Line2D.Double(s * 0.5, s * 0.26, s * 0.5, s * 0.54));
						g2.fill(new Ellipse2D.Double(s * 0.5 - dot / 2, s * 0.71 - dot / 2, dot, dot));
					}
				}
				case WARNING -> {
					final Path2D triangle = new Path2D.Double();
					triangle.moveTo(s * 0.5, s * 0.08);
					triangle.lineTo(s * 0.94, s * 0.88);
					triangle.lineTo(s * 0.06, s * 0.88);
					triangle.closePath();
					g2.draw(triangle);
					g2.draw(new Line2D.Double(s * 0.5, s * 0.38, s * 0.5, s * 0.6));
					final double dot = s * 0.12;
					g2.fill(new Ellipse2D.Double(s * 0.5 - dot / 2, s * 0.74 - dot / 2, dot, dot));
				}
				case EDIT -> {
					final Path2D pencil = new Path2D.Double();
					pencil.moveTo(s * 0.64, s * 0.14);
					pencil.lineTo(s * 0.86, s * 0.36);
					pencil.lineTo(s * 0.36, s * 0.86);
					pencil.lineTo(s * 0.14, s * 0.86);
					pencil.lineTo(s * 0.14, s * 0.64);
					pencil.closePath();
					g2.draw(pencil);
					g2.draw(new Line2D.Double(s * 0.54, s * 0.24, s * 0.76, s * 0.46));
				}
				case COPY -> {
					g2.draw(new RoundRectangle2D.Double(s * 0.12, s * 0.3, s * 0.54, s * 0.6, s * 0.12, s * 0.12));
					final Path2D back = new Path2D.Double();
					back.moveTo(s * 0.34, s * 0.3);
					back.lineTo(s * 0.34, s * 0.1);
					back.lineTo(s * 0.88, s * 0.1);
					back.lineTo(s * 0.88, s * 0.7);
					back.lineTo(s * 0.66, s * 0.7);
					g2.draw(back);
				}
				case CHECK -> {
					final Path2D check = new Path2D.Double();
					check.moveTo(s * 0.16, s * 0.52);
					check.lineTo(s * 0.4, s * 0.76);
					check.lineTo(s * 0.86, s * 0.26);
					g2.draw(check);
				}
				case CHEVRON_DOWN -> {
					final Path2D chevron = new Path2D.Double();
					chevron.moveTo(s * 0.24, s * 0.38);
					chevron.lineTo(s * 0.5, s * 0.64);
					chevron.lineTo(s * 0.76, s * 0.38);
					g2.draw(chevron);
				}
				default -> {
					// nothing
				}
			}
		} finally {
			g2.dispose();
		}
	}
}
