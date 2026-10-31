package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * White rounded panel with soft shadow and an optional title row.
 * The content is added with {@link #setContent(Component)}, additional components for the right side of the
 * title row with {@link #addHeaderComponent(Component)}.
 */
public class Card extends JPanel {
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
			header.add(ModernUi.createTitleLabel(title, iconKind), BorderLayout.WEST);
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
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			final int width = getWidth();
			final int height = getHeight();
			// Soft shadow, slightly shifted downwards
			for (int layer = 0; layer < SHADOW; layer++) {
				g2.setColor(new Color(20, 30, 60, 4 + layer * 2));
				final double inset = layer;
				g2.fill(new RoundRectangle2D.Double(inset, inset + 2, width - inset * 2, height - inset * 2 - 1, ModernUi.CARD_ARC + SHADOW - layer, ModernUi.CARD_ARC + SHADOW - layer));
			}
			final RoundRectangle2D shape = new RoundRectangle2D.Double(SHADOW, SHADOW - 1, width - SHADOW * 2.0, height - SHADOW * 2.0, ModernUi.CARD_ARC, ModernUi.CARD_ARC);
			g2.setColor(ModernUi.CARD);
			g2.fill(shape);
			g2.setColor(ModernUi.CARD_BORDER);
			g2.draw(shape);
		} finally {
			g2.dispose();
		}
	}
}
