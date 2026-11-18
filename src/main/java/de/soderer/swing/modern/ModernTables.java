package de.soderer.swing.modern;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * Tables in the ModernUi design for the dialogs of the application: striped rows, cell padding and no vertical grid
 * lines.
 */
final class ModernTables {
	/** Horizontal padding inside the cells */
	private static final int CELL_PADDING = 8;

	/** Utility class, no instances */
	private ModernTables() {
	}

	/**
	 * Creates a read-only table with striped rows.
	 *
	 * @param tableModel model of the table
	 * @return the table
	 */
	static JTable createTable(final TableModel tableModel) {
		final JTable table = new JTable(tableModel) {
			private static final long serialVersionUID = -1867012655337310485L;

			@Override
			public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
				final Component component = super.prepareRenderer(renderer, row, column);
				if (!isRowSelected(row)) {
					component.setBackground(ModernUi.getStripedRowBackground(row));
				}
				if (component instanceof JComponent) {
					final JComponent jComponent = (JComponent) component;
					jComponent.setBorder(BorderFactory.createEmptyBorder(0, CELL_PADDING, 0, CELL_PADDING));
				}
				return component;
			}
		};
		ModernUi.styleTable(table);
		table.setShowVerticalLines(false);
		table.setFillsViewportHeight(true);
		table.setDefaultEditor(Object.class, null);
		return table;
	}

	/**
	 * Creates a styled scroll pane for a table.
	 *
	 * @param table  table to show
	 * @param width  preferred width
	 * @param height preferred height
	 * @return the scroll pane
	 */
	static JScrollPane createScrollPane(final JTable table, final int width, final int height) {
		final JScrollPane scrollPane = new JScrollPane(table);
		ModernUi.styleScrollPane(scrollPane, ModernUi.CARD);
		scrollPane.setPreferredSize(new Dimension(width, height));
		return scrollPane;
	}
}
