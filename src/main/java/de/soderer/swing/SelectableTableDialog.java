package de.soderer.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog showing a table with a checkbox in front of every row, so the
 * user can select rows.
 *
 * <p>
 * The header of the checkbox column shows a checkbox icon reflecting the
 * selection state of all rows (all, none or some selected). Clicking it selects
 * all rows, or deselects all rows if all rows are already selected.
 * </p>
 *
 * <p>
 * {@link #open()} returns true if the user confirmed the selection, false if
 * the dialog was canceled and null if it was closed via the window decoration.
 * The selected rows are available via {@link #getSelectedItems()} and
 * {@link #getSelectedRowIndexes()}.
 * </p>
 *
 * <p>
 * The initial status of the row checkboxes is given by the constructor for all
 * rows and can be set per row with {@link #setSelectedRowIndexes(Collection)}.
 * </p>
 */
public class SelectableTableDialog extends ModalDialog<Boolean> {
	private static final long serialVersionUID = 5520194410276683215L;

	/** Selection state of all rows, shown in the header of the checkbox column */
	private enum SelectionState {
		NONE,
		SOME,
		ALL
	}

	/** Table with a checkbox column in front of the value columns */
	private JTable table;

	/** Model of the table: column 0 is the checkbox, column i + 1 is header i */
	private final DefaultTableModel tableModel;

	/** Rows selected by the user with their values in the order of the headers, null if the dialog was not confirmed */
	private List<List<String>> selectedItems;

	/** Indexes of the rows selected by the user in the order of the items, null if the dialog was not confirmed */
	private List<Integer> selectedRowIndexes;

	/**
	 * Creates a selectable table dialog.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param text text shown above the table, may be blank
	 * @param headers column headers
	 * @param items rows of the table, each with one value per header
	 * @param defaultState initial status of the row checkboxes
	 * @param selectAllToolTipText tooltip of the header icon while not all rows are selected, may be blank
	 * @param selectNoneToolTipText tooltip of the header icon while all rows are selected, may be blank
	 * @param selectButtonText text of the button confirming the selection
	 * @param cancelButtonText text of the cancel button
	 */
	public SelectableTableDialog(final Window parent, final String title, final String text, final List<String> headers, final List<List<String>> items, final boolean defaultState, final String selectAllToolTipText, final String selectNoneToolTipText, final String selectButtonText, final String cancelButtonText) {
		super(parent, title);

		final SelectableTableDialog dialog = this;

		setResizable(false);

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		if (StringUtilities.isNotBlank(text)) {
			final JTextArea textField = new JTextArea();
			textField.setText(text);
			textField.setEditable(false);
			textField.setBackground(Color.LIGHT_GRAY);

			final JScrollPane textScrollpane = new JScrollPane(textField);
			textScrollpane.setPreferredSize(new Dimension(textField.getPreferredSize().width + 5, textField.getPreferredSize().height + 5));

			// Text Panel
			final JPanel textPanel = new JPanel();
			textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			textPanel.add(textScrollpane);
			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));

			panel.add(Box.createRigidArea(new Dimension(0, 5)));
			panel.add(textPanel);
		}

		// table area
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
		table = new JTable(tableModel);
		table.setBackground(Color.LIGHT_GRAY);

		// Header of the checkbox column: icon showing the selection state, click toggles all rows
		final JTableHeader tableHeader = table.getTableHeader();
		final SelectionStateIcon selectionStateIcon = new SelectionStateIcon();
		final DefaultTableCellRenderer checkboxHeaderRenderer = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 3402516683917846271L;

			@Override
			public Component getTableCellRendererComponent(final JTable rendererTable, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
				super.getTableCellRendererComponent(rendererTable, null, false, false, row, column);
				final SelectionState selectionState = getSelectionState(tableModel);
				selectionStateIcon.setSelectionState(selectionState);
				setIcon(selectionStateIcon);
				setText(null);
				setHorizontalAlignment(SwingConstants.CENTER);
				setBorder(UIManager.getBorder("TableHeader.cellBorder"));
				setFont(tableHeader.getFont());
				setForeground(tableHeader.getForeground());
				setBackground(tableHeader.getBackground());
				final String toolTipText = selectionState == SelectionState.ALL ? selectNoneToolTipText : selectAllToolTipText;
				setToolTipText(StringUtilities.isNotBlank(toolTipText) ? toolTipText : null);
				return this;
			}
		};
		table.getColumnModel().getColumn(0).setHeaderRenderer(checkboxHeaderRenderer);

		tableHeader.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent event) {
				if (event.getButton() != MouseEvent.BUTTON1 || tableHeader.getResizingColumn() != null) {
					return;
				}

				final int viewColumnIndex = tableHeader.columnAtPoint(event.getPoint());
				// Model index: the user may have reordered the view columns
				if (viewColumnIndex < 0 || table.convertColumnIndexToModel(viewColumnIndex) != 0) {
					return;
				}

				if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
					table.getCellEditor().cancelCellEditing();
				}

				final boolean newState = getSelectionState(tableModel) != SelectionState.ALL;
				for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
					tableModel.setValueAt(newState, rowIndex, 0);
				}
			}
		});

		// Keep the header icon in sync with the row checkboxes
		tableModel.addTableModelListener(event -> tableHeader.repaint());

		final JScrollPane tableScrollpane = new JScrollPane(table);
		tableScrollpane.setPreferredSize(new Dimension(table.getPreferredSize().width + 5, 100));

		final JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.LINE_AXIS));

		tablePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		tablePanel.add(tableScrollpane);
		tablePanel.add(Box.createRigidArea(new Dimension(5, 0)));

		// Button Panel
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		final JButton selectButton = new JButton(selectButtonText);
		selectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
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

				dialog.dispose();
			}
		});
		buttonPanel.add(selectButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		final JButton cancelButton = new JButton(cancelButtonText);
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				selectedItems = null;
				selectedRowIndexes = null;
				returnValue = false;
				dialog.dispose();
			}
		});
		buttonPanel.add(cancelButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(tablePanel);
		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(buttonPanel);

		add(panel);

		pack();

		setLocationRelativeTo(parent);

		getRootPane().setDefaultButton(selectButton);
	}

	/**
	 * Sets the initial status of the row checkboxes per row: the rows with the
	 * given indexes are checked, all other rows are unchecked. This overrules the
	 * default state given to the constructor.
	 *
	 * @param rowIndexes indexes of the rows to check in the order of the items;
	 *            indexes outside the table are ignored, null checks no row
	 */
	public void setSelectedRowIndexes(final Collection<Integer> rowIndexes) {
		for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
			tableModel.setValueAt(rowIndexes != null && rowIndexes.contains(rowIndex), rowIndex, 0);
		}
	}

	/**
	 * Sets the initial status of the row checkboxes per row and returns this
	 * dialog for chaining.
	 *
	 * @param rowIndexes indexes of the rows to check in the order of the items;
	 *            indexes outside the table are ignored, null checks no row
	 * @return this dialog for chaining
	 * @see #setSelectedRowIndexes(Collection)
	 */
	public SelectableTableDialog withSelectedRowIndexes(final Collection<Integer> rowIndexes) {
		setSelectedRowIndexes(rowIndexes);
		return this;
	}

	/**
	 * Returns the indexes of the rows selected by the user, e.g. to map the
	 * selection back to the objects the rows were created from.
	 *
	 * @return indexes of the selected rows in the order of the items, null if
	 *         the dialog was not confirmed
	 */
	public List<Integer> getSelectedRowIndexes() {
		return selectedRowIndexes;
	}

	/**
	 * Returns the rows selected by the user.
	 *
	 * @return selected rows with their values in the order of the headers, null
	 *         if the dialog was not confirmed
	 */
	public List<List<String>> getSelectedItems() {
		return selectedItems;
	}

	/**
	 * Determines whether all, none or some of the rows are selected. An empty
	 * table counts as none selected.
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
	 * Checkbox icon of the current look and feel showing a selection state.
	 * The mixed state is drawn as an unchecked box with a dash.
	 */
	private static class SelectionStateIcon implements Icon {
		/** Fallback size if the look and feel provides no checkbox icon */
		private static final int FALLBACK_SIZE = 13;

		/** Not displayed, only provides the state the look and feel icon paints */
		private final JCheckBox paintCheckBox = new JCheckBox();

		private SelectionState selectionState = SelectionState.NONE;

		public void setSelectionState(final SelectionState selectionState) {
			this.selectionState = selectionState;
		}

		private static Icon getLookAndFeelIcon() {
			return UIManager.getIcon("CheckBox.icon");
		}

		@Override
		public int getIconWidth() {
			final Icon icon = getLookAndFeelIcon();
			return icon != null ? icon.getIconWidth() : FALLBACK_SIZE;
		}

		@Override
		public int getIconHeight() {
			final Icon icon = getLookAndFeelIcon();
			return icon != null ? icon.getIconHeight() : FALLBACK_SIZE;
		}

		@Override
		public void paintIcon(final Component component, final Graphics graphics, final int x, final int y) {
			final int width = getIconWidth();
			final int height = getIconHeight();
			final Icon icon = getLookAndFeelIcon();

			if (icon != null) {
				paintCheckBox.setSelected(selectionState == SelectionState.ALL);
				icon.paintIcon(paintCheckBox, graphics, x, y);
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
