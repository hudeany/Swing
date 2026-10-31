package de.soderer.swing.modern;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import javax.swing.border.AbstractBorder;
import javax.swing.text.JTextComponent;

/**
 * Rounded border with inner padding for text fields and scroll panes. Shows an accent colored outline
 * while an editable text component has the focus. The corners outside the rounded outline are filled
 * with the surrounding background, so the component's rectangular background does not show there.
 */
public class RoundedFieldBorder extends AbstractBorder {
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
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			final RoundRectangle2D shape = new RoundRectangle2D.Double(x + 0.5, y + 0.5, width - 1.0, height - 1.0, ARC, ARC);
			final Area corners = new Area(new Rectangle(x, y, width, height));
			corners.subtract(new Area(shape));
			g2.setColor(getSurroundingBackground(component));
			g2.fill(corners);

			final Component focusComponent = focusSource != null ? focusSource : component;
			final boolean focused = focusComponent.isFocusOwner() && focusComponent.isEnabled()
				&& (!(focusComponent instanceof final JTextComponent textComponent) || textComponent.isEditable());
			g2.setColor(focused ? ModernUi.ACCENT : ModernUi.CARD_BORDER);
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
				return ModernUi.CARD;
			} else if (parent.isOpaque()) {
				return parent.getBackground();
			}
			parent = parent.getParent();
		}
		return ModernUi.CARD;
	}
}
