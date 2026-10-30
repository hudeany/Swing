package de.soderer.swing.modern;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Table in the {@link ModernUi} design for the table dialogs: striped rows, cell padding, no vertical grid lines
 * and column widths fitted to the content.
 */
final class DataTable extends JTable {
	/** Serialization version */
	private static final long serialVersionUID = 4630951752851216148L;

	/** Horizontal padding inside the cells */
	private static final int CELL_PADDING = 9;

	/** Maximum width of a single column when fitting the widths to the content */
	private static final int MAX_COLUMN_WIDTH = 360;

	/** Number of rows inspected when fitting the column widths, limits the effort for large tables */
	private static final int MAX_MEASURED_ROWS = 500;

	/**
	 * Creates a table for a model and fits the column widths to the content.
	 *
	 * @param tableModel model of the table
	 */
	DataTable(final TableModel tableModel) {
		super(tableModel);
		ModernUi.styleTable(this);
		setShowVerticalLines(false);
		setIntercellSpacing(new Dimension(0, 1));
		setFillsViewportHeight(true);
		fitColumnWidths();
	}

	/**
	 * Gives every cell padding and every not selected row the striped background.
	 *
	 * @param renderer cell renderer
	 * @param row      view row index
	 * @param column   view column index
	 * @return the prepared renderer component
	 */
	@Override
	public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
		final Component component = super.prepareRenderer(renderer, row, column);
		if (!isRowSelected(row)) {
			component.setBackground(ModernUi.getStripedRowBackground(row));
		}
		if (component instanceof final JComponent jComponent) {
			jComponent.setBorder(BorderFactory.createEmptyBorder(0, CELL_PADDING, 0, CELL_PADDING));
		}
		return component;
	}

	/**
	 * Sets the preferred width of every column to the width of its widest header or cell (limited to
	 * {@link #MAX_COLUMN_WIDTH}).
	 */
	private void fitColumnWidths() {
		for (int columnIndex = 0; columnIndex < getColumnCount(); columnIndex++) {
			final TableColumn column = getColumnModel().getColumn(columnIndex);
			TableCellRenderer headerRenderer = column.getHeaderRenderer();
			if (headerRenderer == null) {
				headerRenderer = getTableHeader().getDefaultRenderer();
			}
			int width = headerRenderer.getTableCellRendererComponent(this, column.getHeaderValue(), false, false, -1, columnIndex).getPreferredSize().width;
			for (int rowIndex = 0; rowIndex < Math.min(getRowCount(), MAX_MEASURED_ROWS); rowIndex++) {
				width = Math.max(width, prepareRenderer(getCellRenderer(rowIndex, columnIndex), rowIndex, columnIndex).getPreferredSize().width);
			}
			column.setPreferredWidth(Math.min(width + 2 * CELL_PADDING, MAX_COLUMN_WIDTH));
		}
	}

	/**
	 * Fixes the width of a column, e.g. for a checkbox column.
	 *
	 * @param columnIndex view index of the column
	 * @param width       fixed width
	 */
	void setFixedColumnWidth(final int columnIndex, final int width) {
		final TableColumn column = getColumnModel().getColumn(columnIndex);
		column.setMinWidth(width);
		column.setMaxWidth(width);
		column.setPreferredWidth(width);
	}

	/**
	 * Creates a styled scroll pane for this table, sized to show all columns and up to a number of rows.
	 *
	 * @param minWidth   minimum preferred width
	 * @param maxWidth   maximum preferred width
	 * @param maxRows    maximum number of rows visible without scrolling
	 * @return the scroll pane containing this table
	 */
	JScrollPane createScrollPane(final int minWidth, final int maxWidth, final int maxRows) {
		final JScrollPane scrollPane = new JScrollPane(this);
		ModernUi.styleScrollPane(scrollPane, ModernUi.CARD);
		final int headerHeight = getTableHeader().getPreferredSize().height;
		final int visibleRows = Math.max(3, Math.min(getRowCount(), maxRows));
		final int width = Math.max(minWidth, Math.min(getPreferredSize().width + 4, maxWidth));
		scrollPane.setPreferredSize(new Dimension(width, headerHeight + visibleRows * (getRowHeight() + getRowMargin()) + 4));
		return scrollPane;
	}
}
