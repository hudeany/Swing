package de.soderer.swing.modern;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

/**
 * Tooltip UI in ModernUi style: rounded corners, soft drop shadow, padding and
 * modern colors. Installed for all tooltips by {@link #install()}, which is called by
 * {@link ModernUi} automatically.
 *
 * <p>Rounded corners and shadow need a transparent popup. Lightweight popups
 * (tooltip fits inside the window) always support this. Heavyweight popups
 * (tooltip extends beyond the window) need per-pixel translucency support of
 * the platform; otherwise a plain rectangular tooltip is painted as fallback.</p>
 */
public class ModernToolTipUI extends BasicToolTipUI {
	/** Dark tooltip background, matching the ModernUi text color */
	private static final Color BACKGROUND = ModernUi.TEXT;
	/** Tooltip text color */
	private static final Color FOREGROUND = ModernUi.CARD;
	/** Subtle outline, slightly lighter than the background */
	private static final Color BORDER = ModernUi.mix(ModernUi.TEXT, Color.WHITE, 0.15);

	private static final int ARC = 10;
	private static final int SHADOW = 4;
	private static final int PAD_VERTICAL = 6;
	/** BasicToolTipUI already adds 3px on the left and right of the text */
	private static final int PAD_HORIZONTAL = 7;

	/** Client property: false if the tooltip lives in an opaque heavyweight window */
	private static final String ROUNDED_POSSIBLE = "ModernToolTipUI.roundedPossible";

	private final HierarchyListener hierarchyListener = this::hierarchyChanged;

	/**
	 * Called by the UIManager via reflection.
	 *
	 * @param component the tooltip component
	 * @return new UI instance
	 */
	public static ComponentUI createUI(final JComponent component) {
		return new ModernToolTipUI();
	}

	/**
	 * Registers this UI for all tooltips of the application and sets pleasant
	 * timing defaults. Survives look-and-feel changes, because it is stored in
	 * the developer defaults of the UIManager.
	 */
	public static void install() {
		UIManager.put("ToolTipUI", ModernToolTipUI.class.getName());
		// Store the class itself, so UIDefaults does not depend on its classloader lookup
		UIManager.put(ModernToolTipUI.class.getName(), ModernToolTipUI.class);

		final ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
		toolTipManager.setInitialDelay(450);
		toolTipManager.setReshowDelay(100);
		toolTipManager.setDismissDelay(15000);
	}

	@Override
	public void installUI(final JComponent component) {
		super.installUI(component);
		component.setOpaque(false);
		component.setBackground(BACKGROUND);
		component.setForeground(FOREGROUND);
		component.setFont(ModernUi.baseFont());
		// Space for shadow (slightly more at the bottom) plus inner padding
		component.setBorder(BorderFactory.createEmptyBorder(
				SHADOW - 1 + PAD_VERTICAL,
				SHADOW + PAD_HORIZONTAL,
				SHADOW + 1 + PAD_VERTICAL,
				SHADOW + PAD_HORIZONTAL));
		component.addHierarchyListener(hierarchyListener);
	}

	@Override
	public void uninstallUI(final JComponent component) {
		component.removeHierarchyListener(hierarchyListener);
		super.uninstallUI(component);
	}

	@Override
	public void paint(final Graphics graphics, final JComponent component) {
		final Graphics2D g2 = ModernUi.prepare(graphics);
		try {
			final int width = component.getWidth();
			final int height = component.getHeight();

			if (Boolean.FALSE.equals(component.getClientProperty(ROUNDED_POSSIBLE))) {
				// Fallback for opaque heavyweight windows: transparent corners are impossible
				g2.setColor(component.getBackground());
				g2.fillRect(0, 0, width, height);
				g2.setColor(BORDER);
				g2.drawRect(0, 0, width - 1, height - 1);
			} else {
				// Soft shadow: stacked translucent shapes, darker towards the bubble
				for (int i = 0; i < SHADOW; i++) {
					g2.setColor(new Color(0, 0, 0, 10 + i * 6));
					g2.fillRoundRect(i, i + 1, width - 2 * i, height - 2 * i - 1, ARC + SHADOW - i, ARC + SHADOW - i);
				}

				// Bubble
				final int bubbleX = SHADOW;
				final int bubbleY = SHADOW - 1;
				final int bubbleWidth = width - 2 * SHADOW;
				final int bubbleHeight = height - 2 * SHADOW;
				g2.setColor(component.getBackground());
				g2.fillRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, ARC, ARC);
				g2.setColor(BORDER);
				g2.setStroke(new BasicStroke(1f));
				g2.drawRoundRect(bubbleX, bubbleY, bubbleWidth - 1, bubbleHeight - 1, ARC, ARC);
			}

			// Text (plain or HTML) is painted by BasicToolTipUI inside the insets
			super.paint(g2, component);
		} finally {
			g2.dispose();
		}
	}

	/**
	 * Makes the popup container transparent as soon as the tooltip is added to it,
	 * so the rounded corners and the shadow are visible.
	 */
	private void hierarchyChanged(final HierarchyEvent event) {
		if ((event.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED)) == 0) {
			return;
		}
		final JComponent toolTip = (JComponent) event.getComponent();
		final Container parent = toolTip.getParent();
		if (parent == null) {
			return;
		}

		final Window window = SwingUtilities.getWindowAncestor(toolTip);
		if (window != null && window.getType() == Window.Type.POPUP && window instanceof final RootPaneContainer rootPaneContainer) {
			// Heavyweight popup: needs a per-pixel translucent window
			if (isTranslucencySupported(window)) {
				try {
					window.setBackground(new Color(0, 0, 0, 0));
					rootPaneContainer.getRootPane().setOpaque(false);
					if (rootPaneContainer.getContentPane() instanceof final JComponent contentPane) {
						contentPane.setOpaque(false);
					}
					toolTip.putClientProperty(ROUNDED_POSSIBLE, Boolean.TRUE);
				} catch (@SuppressWarnings("unused") final RuntimeException e) {
					toolTip.putClientProperty(ROUNDED_POSSIBLE, Boolean.FALSE);
				}
			} else {
				toolTip.putClientProperty(ROUNDED_POSSIBLE, Boolean.FALSE);
			}
		} else if (parent instanceof final JPanel panel && panel.getParent() instanceof JLayeredPane) {
			// Lightweight popup: its container panel is opaque by default
			panel.setOpaque(false);
			toolTip.putClientProperty(ROUNDED_POSSIBLE, Boolean.TRUE);
		} else if (!(parent instanceof JComponent)) {
			// Mediumweight popup (AWT Panel) cannot be transparent
			toolTip.putClientProperty(ROUNDED_POSSIBLE, Boolean.FALSE);
		}
	}

	private static boolean isTranslucencySupported(final Component component) {
		final GraphicsConfiguration graphicsConfiguration = component.getGraphicsConfiguration();
		return graphicsConfiguration != null
				&& graphicsConfiguration.getDevice().isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
	}
}
