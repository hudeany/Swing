package de.soderer.swing.modern;

import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Flat table header cell: bold grey text on a light background with thin separator lines.
 */
public class TableHeaderRenderer extends DefaultTableCellRenderer {
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
		setFont(table != null && table.getTableHeader() != null ? table.getTableHeader().getFont() : ModernUi.baseFont().deriveFont(Font.BOLD));
		setForeground(ModernUi.MUTED);
		setBackground(ModernUi.TABLE_HEADER_BACKGROUND);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 1, ModernUi.CARD_BORDER),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		return this;
	}
}
