package de.soderer.swing.modern;

import java.awt.Adjustable;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;

/**
 * Small, dependency-free toolkit for a modern look of application main windows:
 * gradient header, rounded "cards" with soft shadow, flat rounded buttons, pill shaped info chips,
 * a rounded progress bar, thin scroll bars and simple vector icons.
 *
 * <p>
 * All components paint themselves completely, so they look the same with every look and feel
 * (Windows, GTK, Metal). Native components like combo boxes and spinners are left untouched.
 * </p>
 *
 * <p>
 * Typical usage: put a {@link GradientHeader} at the top of the window, place the content in {@link Card}s
 * on a {@link #BACKGROUND} colored panel, use {@link Button}s for actions and call
 * {@link #blendIntoCard(Component)} for existing components placed inside a card.
 * </p>
 */
public final class ModernUi {
	/** Window background behind the cards */
	public static final Color BACKGROUND = new Color(0xF2F4F8);
	/** Background of cards and of the components inside them */
	public static final Color CARD = Color.WHITE;
	/** Outline of cards, buttons and input fields */
	public static final Color CARD_BORDER = new Color(0xDDE2EA);
	/** Thin separator lines inside cards */
	public static final Color INNER_BORDER = new Color(0xE6E9F0);
	/** Default text color */
	public static final Color TEXT = new Color(0x1F2633);
	/** Grey text color for labels and secondary information */
	public static final Color MUTED = new Color(0x6B7385);
	/** Main accent color, e.g. for primary buttons and focused input fields */
	public static final Color ACCENT = new Color(0x3B6FE0);
	/** Accent color while the mouse is over a primary button */
	public static final Color ACCENT_HOVER = new Color(0x4A7DEC);
	/** Darker accent color for pressed primary buttons and accent colored text */
	public static final Color ACCENT_PRESSED = new Color(0x2C55B8);
	/** Light accent color for backgrounds, e.g. of title icons and accent chips */
	public static final Color ACCENT_LIGHT = new Color(0xE8EFFD);
	/** Left color of the header gradient */
	public static final Color HEADER_START = new Color(0x2F5BD3);
	/** Right color of the header gradient, also the end color of the progress bar fill */
	public static final Color HEADER_END = new Color(0x7B4FD9);
	/** Text color for success information */
	public static final Color SUCCESS = new Color(0x2E9E5B);
	/** Background color for success information */
	public static final Color SUCCESS_LIGHT = new Color(0xE3F5EA);
	/** Text color for warnings */
	public static final Color WARNING = new Color(0xC97A12);
	/** Background color for warnings */
	public static final Color WARNING_LIGHT = new Color(0xFDF1DF);
	/** Text color for errors and dangerous actions */
	public static final Color DANGER = new Color(0xD64545);
	/** Background color for errors and dangerous actions */
	public static final Color DANGER_LIGHT = new Color(0xFDECEC);
	/** Background color of neutral chips */
	public static final Color NEUTRAL_LIGHT = new Color(0xEEF1F6);
	/** Background of disabled primary buttons */
	public static final Color DISABLED_BACKGROUND = new Color(0xE4E7ED);
	/** Text color of disabled buttons */
	public static final Color DISABLED_TEXT = new Color(0xA3A9B6);
	/** Background of log and output areas */
	public static final Color LOG_BACKGROUND = new Color(0xFAFBFD);
	/** Background of read-only input fields */
	public static final Color READ_ONLY_BACKGROUND = new Color(0xF6F7FA);
	/** Left color of the header gradient for errors */
	public static final Color DANGER_HEADER_START = new Color(0xD64545);
	/** Right color of the header gradient for errors */
	public static final Color DANGER_HEADER_END = new Color(0xB03A78);
	/** Left color of the header gradient for warnings */
	public static final Color WARNING_HEADER_START = new Color(0xD9822B);
	/** Right color of the header gradient for warnings */
	public static final Color WARNING_HEADER_END = new Color(0xC2562F);
	/** Background of every second table row */
	public static final Color TABLE_ALTERNATE_ROW = new Color(0xF8F9FC);
	/** Background of table headers */
	public static final Color TABLE_HEADER_BACKGROUND = new Color(0xF3F5F9);

	/** Corner diameter of cards */
	private static final int CARD_ARC = 16;
	/** Corner diameter of buttons */
	private static final int BUTTON_ARC = 10;

	/** Utility class, no instances */
	private ModernUi() {
		// Utility class
	}

	// ---------------------------------------------------------------- fonts and rendering

	/**
	 * Returns the default label font of the current look and feel.
	 *
	 * @return the label font, or a plain 12 point sans serif font if the look and feel defines none
	 */
	public static Font baseFont() {
		final Font font = UIManager.getFont("Label.font");
		return font != null ? font : new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	}

	/**
	 * Returns the font for card titles: bold and slightly larger than {@link #baseFont()}.
	 *
	 * @return the title font
	 */
	public static Font titleFont() {
		return baseFont().deriveFont(Font.BOLD, baseFont().getSize2D() + 1.5f);
	}

	/**
	 * Creates a graphics copy with antialiasing and the desktop's text rendering hints.
	 * The caller has to dispose the returned copy.
	 *
	 * @param graphics graphics to copy
	 * @return prepared copy of the graphics
	 */
	static Graphics2D prepare(final Graphics graphics) {
		final Graphics2D g2 = (Graphics2D) graphics.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		final Object desktopHints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
		if (desktopHints instanceof final Map<?, ?> hints) {
			g2.addRenderingHints(hints);
		} else {
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		return g2;
	}

	/**
	 * Returns the given color with another alpha value.
	 *
	 * @param color base color
	 * @param alpha alpha value from 0 (transparent) to 255 (opaque)
	 * @return new color with the given alpha
	 */
	static Color withAlpha(final Color color, final int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	/**
	 * Mixes two colors.
	 *
	 * @param color      base color
	 * @param other      color mixed in
	 * @param otherShare share of the other color from 0.0 (only base color) to 1.0 (only other color)
	 * @return mixed opaque color
	 */
	static Color mix(final Color color, final Color other, final double otherShare) {
		final double share = 1 - otherShare;
		return new Color(
			(int) Math.round(color.getRed() * share + other.getRed() * otherShare),
			(int) Math.round(color.getGreen() * share + other.getGreen() * otherShare),
			(int) Math.round(color.getBlue() * share + other.getBlue() * otherShare));
	}

	/**
	 * Escapes the HTML special characters of a text, e.g. for texts shown in HTML labels.
	 *
	 * @param text text to escape, may be null
	 * @return escaped text, empty for null
	 */
	public static String escapeHtml(final String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/**
	 * Converts a color to CSS hex notation like "#3b6fe0", used in HTML labels.
	 *
	 * @param color color to convert
	 * @return hex notation without alpha
	 */
	private static String toHex(final Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	// ---------------------------------------------------------------- icons

	/**
	 * Simple line icon in a fixed color. {@link Button} repaints it in the button's text color.
	 */
	public static class ThemeIcon implements Icon {
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
			final Graphics2D g2 = prepare(graphics);
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
						final Font font = baseFont().deriveFont(Font.BOLD, (float) (s * 0.62));
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

	// ---------------------------------------------------------------- buttons

	/**
	 * Flat rounded button with hover and pressed effect, painted completely by itself.
	 */
	public static class Button extends JButton {
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
				setIcon(new ThemeIcon(iconKind, style == ButtonStyle.HEADER && (text == null || text.isEmpty()) ? 20 : 15, TEXT));
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
				setFont(baseFont().deriveFont(Font.BOLD, baseFont().getSize2D() + 1f));
			} else {
				setFont(baseFont());
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
			final Graphics2D g2 = prepare(graphics);
			try {
				final ButtonModel buttonModel = getModel();
				final boolean enabled = isEnabled();
				final boolean pressed = buttonModel.isArmed() && buttonModel.isPressed();
				final boolean hover = buttonModel.isRollover() && enabled;
				final int width = getWidth();
				final int height = getHeight();
				final RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, width - 1.0, height - 1.0, BUTTON_ARC, BUTTON_ARC);

				final Color foreground;
				switch (style) {
					case PRIMARY -> {
						if (enabled) {
							final Color base = pressed ? ACCENT_PRESSED : hover ? ACCENT_HOVER : ACCENT;
							// Soft glow below the button
							g2.setColor(withAlpha(ACCENT, hover ? 60 : 35));
							g2.fill(new RoundRectangle2D.Double(1, 3, width - 2.0, height - 3.0, BUTTON_ARC, BUTTON_ARC));
							g2.setPaint(new GradientPaint(0, 0, mix(base, Color.WHITE, 0.14), 0, height, base));
							g2.fill(new RoundRectangle2D.Double(0.5, 0.5, width - 1.0, height - 2.5, BUTTON_ARC, BUTTON_ARC));
							foreground = Color.WHITE;
						} else {
							g2.setColor(DISABLED_BACKGROUND);
							g2.fill(new RoundRectangle2D.Double(0.5, 0.5, width - 1.0, height - 2.5, BUTTON_ARC, BUTTON_ARC));
							foreground = DISABLED_TEXT;
						}
					}
					case SECONDARY -> {
						g2.setColor(!enabled ? new Color(0xF6F7F9) : pressed ? new Color(0xE3E9F5) : hover ? new Color(0xF0F4FB) : CARD);
						g2.fill(shape);
						g2.setColor(hover ? withAlpha(ACCENT, 150) : CARD_BORDER);
						g2.draw(shape);
						foreground = enabled ? TEXT : DISABLED_TEXT;
					}
					case DANGEROUS -> {
						g2.setColor(!enabled ? new Color(0xF6F7F9) : pressed ? new Color(0xF9DADA) : hover ? DANGER_LIGHT : CARD);
						g2.fill(shape);
						g2.setColor(enabled ? withAlpha(DANGER, hover ? 200 : 140) : CARD_BORDER);
						g2.draw(shape);
						foreground = enabled ? DANGER : DISABLED_TEXT;
					}
					case TOOLBAR -> {
						if (enabled && (hover || pressed)) {
							g2.setColor(pressed ? mix(ACCENT_LIGHT, ACCENT, 0.15) : ACCENT_LIGHT);
							g2.fill(shape);
						}
						foreground = enabled ? TEXT : DISABLED_TEXT;
					}
					case HEADER -> {
						if (enabled && (hover || pressed)) {
							g2.setColor(withAlpha(Color.WHITE, pressed ? 80 : 45));
							g2.fill(shape);
						}
						foreground = enabled ? Color.WHITE : withAlpha(Color.WHITE, 110);
					}
					default -> {
						if (enabled && (hover || pressed)) {
							g2.setColor(withAlpha(Color.WHITE, pressed ? 80 : 45));
							g2.fill(shape);
						}
						foreground = enabled ? Color.WHITE : withAlpha(Color.WHITE, 110);
					}
				}

				paintContent(g2, foreground, width, height - (style == ButtonStyle.PRIMARY ? 2 : 0));

				if (isFocusOwner() && enabled) {
					g2.setStroke(new BasicStroke(2f));
					g2.setColor(style == ButtonStyle.HEADER ? withAlpha(Color.WHITE, 160) : withAlpha(ACCENT, style == ButtonStyle.PRIMARY ? 0 : 110));
					g2.draw(new RoundRectangle2D.Double(1.5, 1.5, width - 3.0, height - 3.0, BUTTON_ARC, BUTTON_ARC));
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

	// ---------------------------------------------------------------- header

	/**
	 * Header band with a horizontal color gradient and some decorative translucent circles.
	 */
	public static class GradientHeader extends JPanel {
		/** Serialization version */
		private static final long serialVersionUID = 1L;

		/** Left color of the gradient */
		private Color startColor = HEADER_START;
		/** Right color of the gradient */
		private Color endColor = HEADER_END;

		/**
		 * Creates the header with a BorderLayout (e.g. logo WEST, title CENTER, buttons EAST) and inner padding.
		 */
		public GradientHeader() {
			super(new BorderLayout(14, 0));
			setOpaque(true);
			setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 14));
		}

		/**
		 * Changes the gradient colors, e.g. to {@link #DANGER_HEADER_START} and {@link #DANGER_HEADER_END} for errors.
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
			final Graphics2D g2 = prepare(graphics);
			try {
				final int width = getWidth();
				final int height = getHeight();
				g2.setPaint(new GradientPaint(0, 0, startColor, width, height, endColor));
				g2.fillRect(0, 0, width, height);
				g2.setColor(withAlpha(Color.WHITE, 18));
				g2.fill(new Ellipse2D.Double(width * 0.62, -height * 1.2, height * 2.6, height * 2.6));
				g2.fill(new Ellipse2D.Double(width * 0.80, height * 0.25, height * 1.6, height * 1.6));
				g2.setColor(withAlpha(Color.WHITE, 12));
				g2.fill(new Ellipse2D.Double(width * 0.45, height * 0.55, height * 1.1, height * 1.1));
			} finally {
				g2.dispose();
			}
		}
	}

	/**
	 * White round badge with the application icon (or a phone symbol if no icon is available) for the header.
	 *
	 * @param applicationIcon icon scaled into the badge, may be null
	 * @return the logo component of 44 x 44 pixels
	 */
	public static JComponent createLogo(final Icon applicationIcon) {
		return new JComponent() {
			/** Serialization version */
		private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(44, 44);
			}

			@Override
			protected void paintComponent(final Graphics graphics) {
				final Graphics2D g2 = prepare(graphics);
				try {
					final int size = Math.min(getWidth(), getHeight());
					g2.setColor(withAlpha(Color.WHITE, 235));
					g2.fill(new RoundRectangle2D.Double(0, 0, size, size, 14, 14));
					if (applicationIcon != null && applicationIcon.getIconWidth() > 0) {
						final int iconSize = size - 12;
						final double scale = Math.min((double) iconSize / applicationIcon.getIconWidth(), (double) iconSize / applicationIcon.getIconHeight());
						final Graphics2D iconGraphics = (Graphics2D) g2.create();
						iconGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
						iconGraphics.translate((size - applicationIcon.getIconWidth() * scale) / 2, (size - applicationIcon.getIconHeight() * scale) / 2);
						iconGraphics.scale(scale, scale);
						applicationIcon.paintIcon(this, iconGraphics, 0, 0);
						iconGraphics.dispose();
					} else {
						new ThemeIcon(IconKind.PHONE, size - 14, HEADER_START).paintIcon(this, g2, 7, 7);
					}
				} finally {
					g2.dispose();
				}
			}
		};
	}

	// ---------------------------------------------------------------- cards

	/**
	 * White rounded panel with soft shadow and an optional title row.
	 * The content is added with {@link #setContent(Component)}, additional components for the right side of the
	 * title row with {@link #addHeaderComponent(Component)}.
	 */
	public static class Card extends JPanel {
		/** Serialization version */
		private static final long serialVersionUID = 1L;
		/** Width of the shadow around the card in pixels */
		private static final int SHADOW = 5;

		/** Container at the right side of the title row */
		private final JPanel headerAccessories = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

		/**
		 * Creates a card.
		 *
		 * @param title    title shown in the first row, null for a card without title row
		 * @param iconKind symbol shown left of the title, null for none
		 * @param padding  inner distance between the card's edge and its content in pixels
		 */
		public Card(final String title, final IconKind iconKind, final int padding) {
			super(new BorderLayout(0, 10));
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(SHADOW - 2 + padding, SHADOW + padding, SHADOW + 2 + padding, SHADOW + padding));
			if (title != null) {
				final JPanel header = new JPanel(new BorderLayout(10, 0));
				header.setOpaque(false);
				header.add(createTitleLabel(title, iconKind), BorderLayout.WEST);
				headerAccessories.setOpaque(false);
				header.add(headerAccessories, BorderLayout.EAST);
				add(header, BorderLayout.NORTH);
			}
		}

		/**
		 * Sets the content of the card, which takes all space below the title row.
		 *
		 * @param content component to show
		 */
		public void setContent(final Component content) {
			add(content, BorderLayout.CENTER);
		}

		/**
		 * Adds a component (e.g. a small button) at the right side of the title row.
		 * Only shown for cards with a title.
		 *
		 * @param component component to add
		 */
		public void addHeaderComponent(final Component component) {
			headerAccessories.add(component);
		}

		@Override
		protected void paintComponent(final Graphics graphics) {
			final Graphics2D g2 = prepare(graphics);
			try {
				final int width = getWidth();
				final int height = getHeight();
				// Soft shadow, slightly shifted downwards
				for (int layer = 0; layer < SHADOW; layer++) {
					g2.setColor(new Color(20, 30, 60, 4 + layer * 2));
					final double inset = layer;
					g2.fill(new RoundRectangle2D.Double(inset, inset + 2, width - inset * 2, height - inset * 2 - 1, CARD_ARC + SHADOW - layer, CARD_ARC + SHADOW - layer));
				}
				final RoundRectangle2D shape = new RoundRectangle2D.Double(SHADOW, SHADOW - 1, width - SHADOW * 2.0, height - SHADOW * 2.0, CARD_ARC, CARD_ARC);
				g2.setColor(CARD);
				g2.fill(shape);
				g2.setColor(CARD_BORDER);
				g2.draw(shape);
			} finally {
				g2.dispose();
			}
		}
	}

	/**
	 * Bold title with a small accent colored icon in a soft square.
	 *
	 * @param text     title text
	 * @param iconKind symbol shown left of the text, null for none
	 * @return the title label
	 */
	public static JLabel createTitleLabel(final String text, final IconKind iconKind) {
		final JLabel label = new JLabel(text);
		label.setFont(titleFont());
		label.setForeground(TEXT);
		if (iconKind != null) {
			label.setIcon(new BadgeIcon(iconKind));
			label.setIconTextGap(9);
		}
		return label;
	}

	/** Icon in a light accent colored rounded square, used for card titles */
	private static class BadgeIcon implements Icon {
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
			icon = new ThemeIcon(kind, 16, ACCENT);
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
			final Graphics2D g2 = prepare(graphics);
			try {
				g2.setColor(ACCENT_LIGHT);
				g2.fill(new RoundRectangle2D.Double(x, y, SIZE, SIZE, 9, 9));
			} finally {
				g2.dispose();
			}
			icon.paintIcon(component, graphics, x + (SIZE - 16) / 2, y + (SIZE - 16) / 2);
		}
	}

	/**
	 * Bold title of a section inside a card, e.g. "Request body".
	 *
	 * @param text title text
	 * @return the title label
	 */
	public static JLabel createSectionTitle(final String text) {
		final JLabel label = new JLabel(text);
		label.setFont(baseFont().deriveFont(Font.BOLD));
		label.setForeground(TEXT);
		return label;
	}

	/**
	 * Grey text, e.g. for labels in front of input fields.
	 *
	 * @param text label text
	 * @return the label
	 */
	public static JLabel createMutedLabel(final String text) {
		final JLabel label = new JLabel(text);
		label.setForeground(MUTED);
		return label;
	}

	/**
	 * Creates a thin vertical line separating groups of toolbar buttons.
	 *
	 * @return the separator component, 9 pixels wide and 24 pixels high
	 */
	public static JComponent createToolbarSeparator() {
		return new JComponent() {
			/** Serialization version */
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(9, 24);
			}

			@Override
			protected void paintComponent(final Graphics graphics) {
				graphics.setColor(CARD_BORDER);
				final int x = getWidth() / 2;
				graphics.drawLine(x, 4, x, getHeight() - 5);
			}
		};
	}

	/**
	 * Creates a transparent square toolbar button showing an image icon, e.g. from the application's resources.
	 * The text is used as tooltip and accessible name.
	 *
	 * @param icon icon to show, painted translucent while the button is disabled
	 * @param text tooltip and accessible name
	 * @param size width and height in pixels
	 * @return the button
	 */
	public static Button createToolbarButton(final Icon icon, final String text, final int size) {
		final Button button = new Button(null, null, ButtonStyle.TOOLBAR);
		button.setIcon(icon);
		button.setToolTipText(text);
		button.getAccessibleContext().setAccessibleName(text);
		button.setPreferredSize(new Dimension(size, size));
		return button;
	}

	// ---------------------------------------------------------------- chips

	/** Color variants of {@link Chip} */
	public enum ChipStyle {
		/** Grey background, normal text */
		NEUTRAL,
		/** Light accent background, accent colored text */
		ACCENTUATED,
		/** Light green background, green text */
		SUCCESSFUL,
		/** Light orange background, orange text */
		ALERT,
		/** Light red background, red text */
		DANGEROUS
	}

	/**
	 * Pill shaped label for compact information like "Model: Pixel 8".
	 */
	public static class Chip extends JLabel {
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
				case ACCENTUATED -> ACCENT_PRESSED;
				case SUCCESSFUL -> SUCCESS;
				case ALERT -> WARNING;
				case DANGEROUS -> DANGER;
				case NEUTRAL -> TEXT;
				default -> TEXT;
			};
			background = switch (style) {
				case ACCENTUATED -> ACCENT_LIGHT;
				case SUCCESSFUL -> SUCCESS_LIGHT;
				case ALERT -> WARNING_LIGHT;
				case DANGEROUS -> DANGER_LIGHT;
				case NEUTRAL -> NEUTRAL_LIGHT;
				default -> NEUTRAL_LIGHT;
			};
			final StringBuilder html = new StringBuilder("<html><span style='color:").append(toHex(MUTED)).append("'>");
			if (caption != null && !caption.isEmpty()) {
				html.append(escapeHtml(caption)).append("&nbsp;</span>");
			} else {
				html.append("</span>");
			}
			html.append("<b style='color:").append(toHex(foreground)).append("'>").append(escapeHtml(value)).append("</b></html>");
			setText(html.toString());
			setFont(baseFont());
			setOpaque(false);
			setHorizontalAlignment(SwingConstants.CENTER);
			setBorder(BorderFactory.createEmptyBorder(4, 11, 4, 11));
		}

		@Override
		protected void paintComponent(final Graphics graphics) {
			final Graphics2D g2 = prepare(graphics);
			try {
				g2.setColor(background);
				g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
			} finally {
				g2.dispose();
			}
			super.paintComponent(graphics);
		}
	}

	// ---------------------------------------------------------------- progress bar

	/**
	 * Creates a progress bar with rounded track, gradient fill and centered text.
	 * The part of the text lying on the fill is drawn in white.
	 *
	 * @return the progress bar; its height should be set via the preferred size (e.g. 20 pixels)
	 */
	public static JProgressBar createProgressBar() {
		final JProgressBar progressBar = new JProgressBar() {
			/** Serialization version */
		private static final long serialVersionUID = 1L;

			@Override
			public void updateUI() {
				setUI(new ModernProgressBarUI());
			}
		};
		progressBar.setOpaque(false);
		progressBar.setBorderPainted(false);
		progressBar.setBorder(BorderFactory.createEmptyBorder());
		progressBar.setFont(baseFont().deriveFont(Font.BOLD, Math.max(10f, baseFont().getSize2D() - 1f)));
		return progressBar;
	}

	/** Paints the progress bar of {@link #createProgressBar()} completely by itself */
	private static class ModernProgressBarUI extends BasicProgressBarUI {
		/** Creates the UI */
		ModernProgressBarUI() {
			// nothing to initialize
		}

		@Override
		public void paint(final Graphics graphics, final JComponent component) {
			final Graphics2D g2 = prepare(graphics);
			try {
				final int width = progressBar.getWidth();
				final int height = progressBar.getHeight();
				final RoundRectangle2D track = new RoundRectangle2D.Double(0, 0, width, height, height, height);
				g2.setColor(new Color(0xE3E7EF));
				g2.fill(track);
				g2.clip(track);
				final GradientPaint fill = new GradientPaint(0, 0, ACCENT, width, 0, HEADER_END);
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
					g2.setColor(TEXT);
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

	// ---------------------------------------------------------------- scroll and split panes

	/**
	 * Gives a scroll pane a rounded border, the given viewport background and slim rounded scroll bars.
	 *
	 * @param scrollPane         scroll pane to style
	 * @param viewportBackground background of the viewport and the scroll bar tracks
	 */
	public static void styleScrollPane(final JScrollPane scrollPane, final Color viewportBackground) {
		scrollPane.setBorder(new RoundedFieldBorder(4, 4));
		scrollPane.setViewportBorder(null);
		scrollPane.getViewport().setBackground(viewportBackground);
		scrollPane.setBackground(viewportBackground);
		styleScrollBars(scrollPane, viewportBackground);
	}

	/**
	 * Gives a scroll pane slim rounded scroll bars only, its border is kept.
	 *
	 * @param scrollPane scroll pane to style
	 * @param trackColor background of the scroll bar tracks
	 */
	public static void styleScrollBars(final JScrollPane scrollPane, final Color trackColor) {
		styleScrollBar(scrollPane.getVerticalScrollBar(), trackColor);
		styleScrollBar(scrollPane.getHorizontalScrollBar(), trackColor);
	}

	/**
	 * Installs the slim scroll bar UI, a faster mouse wheel step and a width of 11 pixels.
	 *
	 * @param scrollBar  scroll bar to style
	 * @param trackColor background of the track
	 */
	private static void styleScrollBar(final JScrollBar scrollBar, final Color trackColor) {
		scrollBar.setUI(new SlimScrollBarUI(trackColor));
		scrollBar.setOpaque(true);
		scrollBar.setBackground(trackColor);
		scrollBar.setUnitIncrement(16);
		if (scrollBar.getOrientation() == Adjustable.VERTICAL) {
			scrollBar.setPreferredSize(new Dimension(11, 0));
		} else {
			scrollBar.setPreferredSize(new Dimension(0, 11));
		}
	}

	/** Scroll bar without arrow buttons and with a slim rounded thumb */
	private static class SlimScrollBarUI extends BasicScrollBarUI {
		/** Background of the track */
		private final Color barTrackColor;

		/**
		 * Creates the UI with the given track color.
		 *
		 * @param barTrackColor background of the track
		 */
		SlimScrollBarUI(final Color barTrackColor) {
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
			final Graphics2D g2 = prepare(graphics);
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

	/**
	 * Makes a split pane transparent with an invisible divider, so the cards seem to float on the background.
	 * Replaces the split pane's UI, so this has to be called again after a look and feel change.
	 *
	 * @param splitPane split pane to style
	 */
	public static void styleSplitPane(final JSplitPane splitPane) {
		// Plain divider without grips or 3D effects of the look and feel
		splitPane.setUI(new BasicSplitPaneUI() {
			@Override
			public BasicSplitPaneDivider createDefaultDivider() {
				return new BasicSplitPaneDivider(this) {
					/** Serialization version */
		private static final long serialVersionUID = 1L;

					@Override
					public void paint(final Graphics graphics) {
						// transparent
					}
				};
			}
		});
		// Set after the UI, because installing the UI restores the look and feel's border
		splitPane.setBorder(null);
		splitPane.setOpaque(false);
		splitPane.setBackground(BACKGROUND);
		splitPane.setDividerSize(8);
		if (splitPane.getUI() instanceof final BasicSplitPaneUI splitPaneUI) {
			splitPaneUI.getDivider().setBorder(null);
		}
	}

	/**
	 * Gives all panels inside a card the card's white background, so existing (not yet restyled) components
	 * do not show the look and feel's grey panel color. Only default backgrounds of the look and feel are
	 * replaced; colors set explicitly by the application are kept. Check boxes and radio buttons are made
	 * transparent. Components added later are not affected, so call this after the content was created.
	 *
	 * @param component component to adjust, including all its children
	 */
	public static void blendIntoCard(final Component component) {
		if ((component instanceof JPanel || component instanceof JViewport) && component.isOpaque() && component.getBackground() instanceof UIResource) {
			component.setBackground(CARD);
		} else if (component instanceof JCheckBox || component instanceof JRadioButton) {
			((JComponent) component).setOpaque(false);
		}
		if (component instanceof final Container container) {
			for (final Component child : container.getComponents()) {
				blendIntoCard(child);
			}
		}
	}

	// ---------------------------------------------------------------- input fields

	/**
	 * Rounded border with inner padding for text fields and scroll panes. Shows an accent colored outline
	 * while an editable text component has the focus. The corners outside the rounded outline are filled
	 * with the surrounding background, so the component's rectangular background does not show there.
	 */
	public static class RoundedFieldBorder extends AbstractBorder {
		/** Serialization version */
		private static final long serialVersionUID = 1L;
		/** Corner diameter */
		private static final int ARC = 10;

		/** Inner padding at top and bottom */
		private final int vertical;
		/** Inner padding at left and right */
		private final int horizontal;
		/** Component whose focus decides the outline color, null for the bordered component itself */
		private final Component focusSource;

		/**
		 * Creates a border whose outline shows the focus of the bordered component itself.
		 *
		 * @param vertical   inner padding at top and bottom in pixels
		 * @param horizontal inner padding at left and right in pixels
		 */
		public RoundedFieldBorder(final int vertical, final int horizontal) {
			this(vertical, horizontal, null);
		}

		/**
		 * Creates a border whose outline shows the focus of another component.
		 *
		 * @param vertical    inner padding at top and bottom in pixels
		 * @param horizontal  inner padding at left and right in pixels
		 * @param focusSource component whose focus decides the outline color, e.g. the text area inside a
		 *                    scroll pane; null for the bordered component itself
		 */
		public RoundedFieldBorder(final int vertical, final int horizontal, final Component focusSource) {
			this.vertical = vertical;
			this.horizontal = horizontal;
			this.focusSource = focusSource;
		}

		@Override
		public Insets getBorderInsets(final Component component) {
			return new Insets(vertical, horizontal, vertical, horizontal);
		}

		@Override
		public Insets getBorderInsets(final Component component, final Insets insets) {
			insets.set(vertical, horizontal, vertical, horizontal);
			return insets;
		}

		@Override
		public void paintBorder(final Component component, final Graphics graphics, final int x, final int y, final int width, final int height) {
			final Graphics2D g2 = prepare(graphics);
			try {
				final RoundRectangle2D shape = new RoundRectangle2D.Double(x + 0.5, y + 0.5, width - 1.0, height - 1.0, ARC, ARC);
				final Area corners = new Area(new Rectangle(x, y, width, height));
				corners.subtract(new Area(shape));
				g2.setColor(getSurroundingBackground(component));
				g2.fill(corners);

				final Component focusComponent = focusSource != null ? focusSource : component;
				final boolean focused = focusComponent.isFocusOwner() && focusComponent.isEnabled()
					&& (!(focusComponent instanceof final JTextComponent textComponent) || textComponent.isEditable());
				g2.setColor(focused ? ACCENT : CARD_BORDER);
				g2.setStroke(new BasicStroke(focused ? 1.5f : 1f));
				g2.draw(shape);
			} finally {
				g2.dispose();
			}
		}

		/**
		 * Returns the background of the first opaque parent (a card counts as opaque white), used for the corners.
		 *
		 * @param component bordered component
		 * @return the surrounding background color
		 */
		private static Color getSurroundingBackground(final Component component) {
			Component parent = component.getParent();
			while (parent != null) {
				if (parent instanceof Card) {
					return CARD;
				} else if (parent.isOpaque()) {
					return parent.getBackground();
				}
				parent = parent.getParent();
			}
			return CARD;
		}
	}

	/**
	 * Gives a text field a rounded border with padding and an accent colored outline while focused.
	 * Read-only fields get a light grey background, editable fields get it while they are disabled.
	 *
	 * @param textField text field to style
	 * @param readOnly  true for the read-only background
	 */
	public static void styleTextField(final JTextField textField, final boolean readOnly) {
		textField.setBorder(new RoundedFieldBorder(5, 9));
		textField.setOpaque(true);
		textField.setForeground(TEXT);
		textField.setDisabledTextColor(DISABLED_TEXT);
		if (readOnly) {
			textField.setBackground(READ_ONLY_BACKGROUND);
		} else {
			textField.setBackground(textField.isEnabled() ? Color.WHITE : READ_ONLY_BACKGROUND);
			textField.addPropertyChangeListener("enabled", event -> textField.setBackground(textField.isEnabled() ? Color.WHITE : READ_ONLY_BACKGROUND));
		}
		addFocusRepaint(textField, textField);
	}

	/**
	 * Gives a text area a monospaced font and padding and its scroll pane a rounded border, whose outline
	 * shows the focus of the text area. Suitable e.g. for request and response bodies.
	 *
	 * @param textArea   text area to style
	 * @param scrollPane scroll pane containing the text area
	 * @param readOnly   true for the read-only background
	 */
	public static void styleTextArea(final JTextArea textArea, final JScrollPane scrollPane, final boolean readOnly) {
		final Color background = readOnly || !textArea.isEnabled() ? READ_ONLY_BACKGROUND : Color.WHITE;
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, baseFont().getSize()));
		textArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		textArea.setBackground(background);
		textArea.setForeground(TEXT);
		textArea.setDisabledTextColor(DISABLED_TEXT);
		styleScrollPane(scrollPane, background);
		if (!readOnly) {
			// Editable areas get the read-only background while they are disabled
			textArea.addPropertyChangeListener("enabled", event -> {
				final Color newBackground = textArea.isEnabled() ? Color.WHITE : READ_ONLY_BACKGROUND;
				textArea.setBackground(newBackground);
				scrollPane.getViewport().setBackground(newBackground);
				scrollPane.setBackground(newBackground);
				scrollPane.repaint();
			});
		}
		// The scroll pane's outline shows the focus of the text area
		scrollPane.setBorder(new RoundedFieldBorder(4, 4, textArea));
		addFocusRepaint(textArea, scrollPane);
	}

	/**
	 * Gives a text area the look of a single line text field (rounded border, padding, focus outline),
	 * e.g. for values which may contain line breaks. The font is not changed.
	 *
	 * @param textArea text area to style
	 */
	public static void styleTextAreaAsField(final JTextArea textArea) {
		textArea.setBorder(new RoundedFieldBorder(5, 9));
		textArea.setOpaque(true);
		textArea.setBackground(Color.WHITE);
		textArea.setForeground(TEXT);
		addFocusRepaint(textArea, textArea);
	}

	// ---------------------------------------------------------------- tables

	/**
	 * Gives a table more row height, light grid lines, a light accent selection and a flat header
	 * (see {@link TableHeaderRenderer}). Install own header renderers after calling this, they can wrap
	 * the flat renderer via {@code getDefaultRenderer()}.
	 *
	 * @param table table to style
	 */
	public static void styleTable(final JTable table) {
		table.setRowHeight(Math.max(table.getRowHeight(), table.getFontMetrics(table.getFont()).getHeight() + 8));
		table.setGridColor(INNER_BORDER);
		table.setBackground(CARD);
		table.setForeground(TEXT);
		table.setSelectionBackground(ACCENT_LIGHT);
		table.setSelectionForeground(TEXT);
		final JTableHeader header = table.getTableHeader();
		if (header != null) {
			header.setFont(baseFont().deriveFont(Font.BOLD));
			header.setBackground(TABLE_HEADER_BACKGROUND);
			header.setForeground(MUTED);
			header.setDefaultRenderer(new TableHeaderRenderer());
		}
	}

	/**
	 * Returns the background of a not selected table row: white and {@link #TABLE_ALTERNATE_ROW} alternating.
	 * Can be used in an overridden {@code JTable.prepareRenderer()} for striped rows.
	 *
	 * @param row view index of the row
	 * @return the row background
	 */
	public static Color getStripedRowBackground(final int row) {
		return row % 2 == 0 ? CARD : TABLE_ALTERNATE_ROW;
	}

	/**
	 * Flat table header cell: bold grey text on a light background with thin separator lines.
	 */
	public static class TableHeaderRenderer extends DefaultTableCellRenderer {
		/** Serialization version */
		private static final long serialVersionUID = 1L;

		/**
		 * Creates the renderer with centered texts.
		 */
		public TableHeaderRenderer() {
			setHorizontalAlignment(SwingConstants.CENTER);
			setOpaque(true);
		}

		/**
		 * Configures the renderer for a header cell. Selection and focus are not shown in the header.
		 *
		 * @param table      the table, may be null
		 * @param value      the column name
		 * @param isSelected ignored
		 * @param hasFocus   ignored
		 * @param row        the row index (-1 for the header)
		 * @param column     the view index of the column
		 * @return this renderer
		 */
		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
			super.getTableCellRendererComponent(table, value, false, false, row, column);
			setFont(table != null && table.getTableHeader() != null ? table.getTableHeader().getFont() : baseFont().deriveFont(Font.BOLD));
			setForeground(MUTED);
			setBackground(TABLE_HEADER_BACKGROUND);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 1, CARD_BORDER),
				BorderFactory.createEmptyBorder(4, 6, 4, 6)));
			return this;
		}
	}

	/**
	 * Repaints a component whenever another component gains or loses the focus, e.g. to update a focus outline.
	 *
	 * @param focusComponent   component whose focus changes are observed
	 * @param repaintComponent component to repaint
	 */
	private static void addFocusRepaint(final JComponent focusComponent, final JComponent repaintComponent) {
		focusComponent.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(final FocusEvent event) {
				repaintComponent.repaint();
			}

			@Override
			public void focusLost(final FocusEvent event) {
				repaintComponent.repaint();
			}
		});
	}
}
