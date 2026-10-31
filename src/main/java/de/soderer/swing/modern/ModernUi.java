package de.soderer.swing.modern;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
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
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.JTableHeader;

/**
 * Small, dependency-free toolkit for a modern look of application main windows:
 * gradient header, rounded "cards" with soft shadow, flat rounded buttons, pill shaped info chips,
 * a rounded progress bar, thin scroll bars and simple vector icons.
 *
 * <p>
 * All components paint themselves completely, so they look the same with every look and feel
 * (Windows, GTK, Metal). Tooltips, check boxes and radio buttons are restyled globally (see {@link #install()}).
 * Other native components like combo boxes and spinners are left untouched.
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
	static final int CARD_ARC = 16;
	/** Corner diameter of buttons */
	static final int BUTTON_ARC = 10;

	/** Whether the global UI delegates were already registered (no initializer, it would run after the static block) */
	private static boolean installed;

	static {
		// Must stay below the color constants, because the installed UIs use them during their own initialization
		install();
	}

	/** Utility class, no instances */
	private ModernUi() {
		// Utility class
	}

	// ---------------------------------------------------------------- global installation

	/**
	 * Registers the ModernUi delegates for all tooltips ({@link ToolTip}), check boxes and radio buttons
	 * ({@link CheckBox}) of the application. This is done automatically as soon as ModernUi is used for the
	 * first time, so calling it explicitly is only needed if such components are created before any other ModernUi
	 * usage. Calling it more than once has no further effect. The registration survives look and feel changes.
	 */
	public static synchronized void install() {
		if (!installed) {
			installed = true;
			ToolTip.install();
			CheckBox.install();
		}
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
	static String toHex(final Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	// ---------------------------------------------------------------- icons

	// ---------------------------------------------------------------- buttons

	// ---------------------------------------------------------------- header

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
				setUI(new ProgressBar());
			}
		};
		progressBar.setOpaque(false);
		progressBar.setBorderPainted(false);
		progressBar.setBorder(BorderFactory.createEmptyBorder());
		progressBar.setFont(baseFont().deriveFont(Font.BOLD, Math.max(10f, baseFont().getSize2D() - 1f)));
		return progressBar;
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
		scrollBar.setUI(new ScrollBar(trackColor));
		scrollBar.setOpaque(true);
		scrollBar.setBackground(trackColor);
		scrollBar.setUnitIncrement(16);
		if (scrollBar.getOrientation() == Adjustable.VERTICAL) {
			scrollBar.setPreferredSize(new Dimension(11, 0));
		} else {
			scrollBar.setPreferredSize(new Dimension(0, 11));
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
