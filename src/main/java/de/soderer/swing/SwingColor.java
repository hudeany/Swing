package de.soderer.swing;

import java.awt.Color;

/**
 * Commonly used colors of the Swing dialogs, e.g. the background of error
 * messages.
 */
public class SwingColor {
	/**
	 * Light red, used as background of error messages.
	 */
	public static final Color LightRed = new Color(255, 127, 127);

	/**
	 * Yellow, used e.g. as background of warnings.
	 */
	public static final Color Yellow = Color.yellow;

	/**
	 * Green, used e.g. as background of success messages.
	 */
	public static final Color Green = Color.green;

	private SwingColor() {
		// Utility class
	}
}
