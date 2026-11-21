package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog in the {@link ModernUi} design showing a table with a checkbox in front of every row, so the user
 * can select rows. Same API as {@link de.soderer.swing.SelectableTableDialog}.
 *
 * <p>
 * The header of the checkbox column shows a checkbox icon reflecting the selection state of all rows (all, none or
 * some selected). Clicking it selects all rows, or deselects all rows if all rows are already selected.
 * </p>
 *
 * <p>
 * {@link #open()} returns true if the user confirmed the selection, false if the dialog was canceled and null if
 * it was closed via the window decoration. The selected rows are available via {@link #getSelectedItems()} and
 * {@link #getSelectedRowIndexes()}.
 * </p>
 *
 * <p>
 * The initial status of the row checkboxes is given by the constructor for all rows and can be set per row with
 * {@link #setSelectedRowIndexes(Collection)}.
 * </p>
 *
 * <p>
 * Unlike the classic dialog it can be resized, the table grows with the window.
 * </p>
 */
public class SelectableTableDialog extends ModalDialog<Boolean> {
	/** Serialization version */
	private static final long serialVersionUID = -1823365640257904876L;

	/** Width of the checkbox column */
	private static final int CHECKBOX_COLUMN_WIDTH = 40;

	/** Selection state of all rows, shown in the header of the checkbox column */
	private enum SelectionState {
		/** No row selected (or no rows at all) */
		NONE,
		/** Some but not all rows selected */
		SOME,
		/** All rows selected */
		ALL
	}

	/** Table with a checkbox column in front of the value columns */
	private final DataTable table;

	/** Model of the table: column 0 is the checkbox, column i + 1 is header i */
	private final DefaultTableModel tableModel;

	/** Rows selected by the user with their values in the order of the headers, null if the dialog was not confirmed */
	private List<List<String>> selectedItems;

	/** Indexes of the rows selected by the user in the order of the items, null if the dialog was not confirmed */
	private List<Integer> selectedRowIndexes;

	/**
	 * Creates a selectable table dialog.
	 *
	 * @param parent                owner window, may be null
	 * @param title                 title of the dialog
	 * @param text                  text shown above the table, may be blank
	 * @param headers               column headers
	 * @param items                 rows of the table, each with one value per header
	 * @param defaultState          initial status of the row checkboxes
	 * @param selectAllToolTipText  tooltip of the header icon while not all rows are selected, may be blank
	 * @param selectNoneToolTipText tooltip of the header icon while all rows are selected, may be blank
	 * @param selectButtonText      text of the button confirming the selection
	 * @param cancelButtonText      text of the cancel button
	 */
	public SelectableTableDialog(final Window parent, final String title, final String text, final List<String> headers, final List<List<String>> items, final boolean defaultState, final String selectAllToolTipText, final String selectNoneToolTipText, final String selectButtonText, final String cancelButtonText) {
		super(parent, title, IconKind.CHECK);

		if (StringUtilities.isNotBlank(text)) {
			getContentArea().add(TextSupport.createTextScrollPane(text, 800, 200), BorderLayout.NORTH);
		}

		// Column 0 is the checkbox, column i + 1 is header i
		final Object[] headerArray = new Object[headers.size() + 1];
		headerArray[0] = "";
		for (int i = 0; i < headers.size(); i++) {
			headerArray[i + 1] = headers.get(i);
		}

		final Object[][] itemsArray = new Object[items.size()][];
		for (int i = 0; i < items.size(); i++) {
			final Object[] itemArray = new Object[items.get(i).size() + 1];
			itemArray[0] = defaultState;
			for (int j = 0; j < items.get(i).size(); j++) {
				itemArray[j + 1] = items.get(i).get(j);
			}
			itemsArray[i] = itemArray;
		}

		tableModel = new DefaultTableModel(itemsArray, headerArray) {
			private static final long serialVersionUID = -372185535503984868L;

			@Override
			public Class<?> getColumnClass(final int columnIndex) {
				if (columnIndex == 0) {
					return Boolean.class;
				} else {
					return String.class;
				}
			}

			@Override
			public boolean isCellEditable(final int rowIndex, final int columnIndex) {
				// Only the checkbox column is editable, the values must be returned unchanged
				return columnIndex == 0;
			}
		};
		table = new DataTable(tableModel);
		table.setFixedColumnWidth(0, CHECKBOX_COLUMN_WIDTH);

		installSelectAllHeader(tableModel, selectAllToolTipText, selectNoneToolTipText);

		final JScrollPane tableScrollPane = table.createScrollPane(360, 900, 15);
		getContentArea().add(tableScrollPane, BorderLayout.CENTER);

		final JButton selectButton = addButton(selectButtonText, null, ButtonStyle.PRIMARY, event -> {
			// Read the model by model indexes (column 0 is the checkbox, column i + 1 is header i):
			// view column indexes differ from the model indexes after the user reordered columns
			if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
				table.getCellEditor().cancelCellEditing();
			}

			selectedItems = new ArrayList<>();
			selectedRowIndexes = new ArrayList<>();
			for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
				if (Boolean.TRUE.equals(tableModel.getValueAt(rowIndex, 0))) {
					selectedRowIndexes.add(rowIndex);
					final List<String> item = new ArrayList<>();
					for (int columnIndex = 1; columnIndex <= headers.size(); columnIndex++) {
						item.add((String) tableModel.getValueAt(rowIndex, columnIndex));
					}
					selectedItems.add(item);
				}
			}

			returnValue = true;
			dispose();
		});

		addButton(cancelButtonText, null, ButtonStyle.SECONDARY, event -> {
			selectedItems = null;
			selectedRowIndexes = null;
			returnValue = false;
			dispose();
		});

		pack();
		setMinimumSize(new Dimension(Math.min(getWidth(), 420), Math.min(getHeight(), 280)));

		setLocationRelativeTo(parent);

		getRootPane().setDefaultButton(selectButton);
	}

	/**
	 * Shows the selection state icon in the header of the checkbox column and lets a click on it select all or no
	 * rows.
	 *
	 * @param newTableModel            model of the table
	 * @param selectAllToolTipText  tooltip while not all rows are selected, may be blank
	 * @param selectNoneToolTipText tooltip while all rows are selected, may be blank
	 */
	private void installSelectAllHeader(final DefaultTableModel newTableModel, final String selectAllToolTipText, final String selectNoneToolTipText) {
		final JTableHeader tableHeader = table.getTableHeader();
		final SelectionStateIcon selectionStateIcon = new SelectionStateIcon();

		// Wrap the styled default header renderer, so the header keeps the ModernUi look.
		// The default renderer component is shared by all columns, so the other columns must remove the icon again.
		final TableCellRenderer styledHeaderRenderer = tableHeader.getDefaultRenderer();
		tableHeader.setDefaultRenderer((rendererTable, value, isSelected, hasFocus, row, column) -> {
			final Component component = styledHeaderRenderer.getTableCellRendererComponent(rendererTable, value, isSelected, hasFocus, row, column);
			if (component instanceof final JLabel label) {
				if (column >= 0 && rendererTable != null && rendererTable.convertColumnIndexToModel(column) == 0) {
					final SelectionState selectionState = getSelectionState(newTableModel);
					selectionStateIcon.setSelectionState(selectionState);
					label.setIcon(selectionStateIcon);
					label.setText(null);
					label.setHorizontalAlignment(SwingConstants.CENTER);
					final String toolTipText = selectionState == SelectionState.ALL ? selectNoneToolTipText : selectAllToolTipText;
					label.setToolTipText(StringUtilities.isNotBlank(toolTipText) ? toolTipText : null);
				} else if (label.getIcon() == selectionStateIcon) {
					label.setIcon(null);
					label.setToolTipText(null);
				}
			}
			return component;
		});

		tableHeader.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent event) {
				if (event.getButton() != MouseEvent.BUTTON1 || tableHeader.getResizingColumn() != null) {
					return;
				}

				final int viewColumnIndex = tableHeader.columnAtPoint(event.getPoint());
				// Model index: the user may have reordered the view columns
				if (viewColumnIndex >= 0 && table.convertColumnIndexToModel(viewColumnIndex) == 0) {
					setAllCheckboxes(newTableModel, getSelectionState(newTableModel) != SelectionState.ALL);
				}
			}
		});

		// Keep the header icon in sync with the row checkboxes
		newTableModel.addTableModelListener(event -> tableHeader.repaint());
	}

	/**
	 * Sets the checkboxes of all rows.
	 *
	 * @param tableModel model of the table
	 * @param checked    new status of the checkboxes
	 */
	private void setAllCheckboxes(final DefaultTableModel tableModel, final boolean checked) {
		// A running edit would otherwise write its old value back afterwards
		if (table.isEditing()) {
			table.getCellEditor().cancelCellEditing();
		}
		// Model indexes: the user may have reordered the view columns
		for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
			tableModel.setValueAt(checked, rowIndex, 0);
		}
	}

	/**
	 * Determines whether all, none or some of the rows are selected. An empty table counts as none selected.
	 *
	 * @param tableModel model of the table
	 * @return selection state of all rows
	 */
	private static SelectionState getSelectionState(final DefaultTableModel tableModel) {
		int selectedCount = 0;
		for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
			if (Boolean.TRUE.equals(tableModel.getValueAt(rowIndex, 0))) {
				selectedCount++;
			}
		}
		if (selectedCount == 0) {
			return SelectionState.NONE;
		} else if (selectedCount == tableModel.getRowCount()) {
			return SelectionState.ALL;
		} else {
			return SelectionState.SOME;
		}
	}

	/**
	 * Sets the initial status of the row checkboxes per row: the rows with the given indexes are checked, all other
	 * rows are unchecked. This overrules the default state given to the constructor.
	 *
	 * @param rowIndexes indexes of the rows to check in the order of the items; indexes outside the table are ignored,
	 *                   null checks no row
	 */
	public void setSelectedRowIndexes(final Collection<Integer> rowIndexes) {
		for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
			tableModel.setValueAt(rowIndexes != null && rowIndexes.contains(rowIndex), rowIndex, 0);
		}
	}

	/**
	 * Sets the initial status of the row checkboxes per row and returns this dialog for chaining.
	 *
	 * @param rowIndexes indexes of the rows to check in the order of the items; indexes outside the table are ignored,
	 *                   null checks no row
	 * @return this dialog for chaining
	 * @see #setSelectedRowIndexes(Collection)
	 */
	public SelectableTableDialog withSelectedRowIndexes(final Collection<Integer> rowIndexes) {
		setSelectedRowIndexes(rowIndexes);
		return this;
	}

	/**
	 * Returns the indexes of the rows selected by the user, e.g. to map the selection back to the objects the rows
	 * were created from.
	 *
	 * @return indexes of the selected rows in the order of the items, null if the dialog was not confirmed
	 */
	public List<Integer> getSelectedRowIndexes() {
		return selectedRowIndexes;
	}

	/**
	 * Returns the rows selected by the user.
	 *
	 * @return selected rows with their values in the order of the headers, null if the dialog was not confirmed
	 */
	public List<List<String>> getSelectedItems() {
		return selectedItems;
	}

	/**
	 * Checkbox icon showing a selection state. It uses the same checkbox icon as the checkbox cells of the table, so
	 * header and rows look alike. The mixed state is drawn as an unchecked box with a dash.
	 */
	private class SelectionStateIcon implements Icon {
		/** Fallback size if no checkbox icon is available */
		private static final int FALLBACK_SIZE = 13;

		/** Selection state to show */
		private SelectionState selectionState = SelectionState.NONE;

		/**
		 * Sets the selection state to show.
		 *
		 * @param selectionState selection state
		 */
		void setSelectionState(final SelectionState selectionState) {
			this.selectionState = selectionState;
		}

		/**
		 * Returns the checkbox renderer of the table's checkbox cells prepared for the current state.
		 *
		 * @return checkbox renderer button, null if the cells are not rendered by a button
		 */
		private AbstractButton getCheckboxRenderer() {
			// Not prepareRenderer: that would apply the row striping and cell padding
			final Component component = table.getDefaultRenderer(Boolean.class).getTableCellRendererComponent(table, selectionState == SelectionState.ALL, false, false, 0, 0);
			return component instanceof final AbstractButton button ? button : null;
		}

		/**
		 * Returns the checkbox icon used by the checkbox cells.
		 *
		 * @param button checkbox renderer button, may be null
		 * @return checkbox icon, null if none is available
		 */
		private Icon getCheckboxIcon(final AbstractButton button) {
			if (button != null && button.getIcon() != null) {
				return button.getIcon();
			} else {
				return UIManager.getIcon("CheckBox.icon");
			}
		}

		@Override
		public int getIconWidth() {
			final Icon icon = getCheckboxIcon(getCheckboxRenderer());
			return icon != null ? icon.getIconWidth() : FALLBACK_SIZE;
		}

		@Override
		public int getIconHeight() {
			final Icon icon = getCheckboxIcon(getCheckboxRenderer());
			return icon != null ? icon.getIconHeight() : FALLBACK_SIZE;
		}

		@Override
		public void paintIcon(final Component component, final Graphics graphics, final int x, final int y) {
			final AbstractButton button = getCheckboxRenderer();
			final Icon icon = getCheckboxIcon(button);
			final int width = getIconWidth();
			final int height = getIconHeight();

			if (icon != null && button != null) {
				// The look and feel icon reads the checked state from the button it is painted for
				icon.paintIcon(button, graphics, x, y);
			} else {
				// Simple self drawn checkbox
				graphics.setColor(Color.WHITE);
				graphics.fillRect(x, y, width - 1, height - 1);
				graphics.setColor(Color.DARK_GRAY);
				graphics.drawRect(x, y, width - 1, height - 1);
				if (selectionState == SelectionState.ALL) {
					graphics.drawLine(x + 3, y + height / 2, x + width / 2 - 1, y + height - 4);
					graphics.drawLine(x + 3, y + height / 2 + 1, x + width / 2 - 1, y + height - 3);
					graphics.drawLine(x + width / 2 - 1, y + height - 4, x + width - 4, y + 3);
					graphics.drawLine(x + width / 2 - 1, y + height - 3, x + width - 4, y + 4);
				}
			}

			if (selectionState == SelectionState.SOME) {
				// Dash for the mixed state
				graphics.setColor(component != null ? component.getForeground() : Color.DARK_GRAY);
				graphics.fillRect(x + width / 4, y + height / 2 - 1, width - 2 * (width / 4), 2);
			}
		}
	}
}
