package de.soderer.swing.modern;

import de.soderer.swing.modern.ModernUi.ModernButton;

/** Color variants of {@link ModernButton} */
public enum ButtonStyle {
	/** Filled accent button for the main action */
	PRIMARY,
	/** White button with thin border */
	SECONDARY,
	/** White button with red text and border, e.g. for cancel */
	DANGEROUS,
	/** Transparent button on the gradient header */
	HEADER,
	/** Transparent button for toolbars inside cards, light accent background on hover */
	TOOLBAR
}
