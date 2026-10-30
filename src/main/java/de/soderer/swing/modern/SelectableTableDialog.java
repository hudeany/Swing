package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog in the {@link ModernUi} design showing a table with a checkbox in front of every row, so the user
 * can select rows. Same API as {@link de.soderer.swing.SelectableTableDialog}.
 *
 * <p>
 * {@link #open()} returns true if the user confirmed the selection, false if the dialog was canceled and null if
 * it was closed via the window decoration. The selected rows are available via {@link #getSelectedItems()}.
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

	/** Table with a checkbox column in front of the value columns */
	private final DataTable table;

	/** Rows selected by the user with their values in the order of the headers, null if the dialog was not confirmed */
	private List<List<String>> selectedItems;

	/**
	 * Creates a selectable table dialog.
	 *
	 * @param parent               owner window, may be null
	 * @param title                title of the dialog
	 * @param text                 text shown above the table, may be blank
	 * @param headers              column headers
	 * @param items                rows of the table, each with one value per header
	 * @param defaultState         initial status of the row checkboxes
	 * @param selectAllButtonText  text of the "select all" button, blank for no button
	 * @param selectNoneButtonText text of the "select none" button, blank for no button
	 * @param selectButtonText     text of the button confirming the selection
	 * @param cancelButtonText     text of the cancel button
	 */
	public SelectableTableDialog(final Window parent, final String title, final String text, final List<String> headers, final List<List<String>> items, final boolean defaultState, final String selectAllButtonText, final String selectNoneButtonText, final String selectButtonText, final String cancelButtonText) {
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

		final DefaultTableModel tableModel = new DefaultTableModel(itemsArray, headerArray) {
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

		final JScrollPane tableScrollPane = table.createScrollPane(360, 900, 15);
		getContentArea().add(tableScrollPane, BorderLayout.CENTER);

		// Left button group: change all checkboxes at once
		if (StringUtilities.isNotBlank(selectAllButtonText)) {
			addLeftButton(selectAllButtonText, IconKind.CHECK, ButtonStyle.SECONDARY, event -> setAllCheckboxes(tableModel, true));
		}
		if (StringUtilities.isNotBlank(selectNoneButtonText)) {
			addLeftButton(selectNoneButtonText, IconKind.CLOSE, ButtonStyle.SECONDARY, event -> setAllCheckboxes(tableModel, false));
		}

		final JButton selectButton = addButton(selectButtonText, null, ButtonStyle.PRIMARY, event -> {
			// Read the model by model indexes (column 0 is the checkbox, column i + 1 is header i):
			// view column indexes differ from the model indexes after the user reordered columns
			if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
				table.getCellEditor().cancelCellEditing();
			}

			selectedItems = new ArrayList<>();
			for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
				if (Boolean.TRUE.equals(tableModel.getValueAt(rowIndex, 0))) {
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
			returnValue = false;
			dispose();
		});

		pack();
		setMinimumSize(new Dimension(Math.min(getWidth(), 420), Math.min(getHeight(), 280)));

		setLocationRelativeTo(parent);

		getRootPane().setDefaultButton(selectButton);
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
	 * Returns the rows selected by the user.
	 *
	 * @return selected rows with their values in the order of the headers, null if the dialog was not confirmed
	 */
	public List<List<String>> getSelectedItems() {
		return selectedItems;
	}
}
