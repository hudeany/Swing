package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog in the {@link ModernUi} design showing a read-only table of string values with a close button.
 * Same API as {@link de.soderer.swing.TableDialog}. {@link #open()} returns null.
 *
 * <p>
 * Unlike the classic dialog it can be resized, the table grows with the window.
 * </p>
 */
public class TableDialog extends ModalDialog<Boolean> {
	/** Serialization version */
	private static final long serialVersionUID = 2214739125704860131L;

	/** Read-only table showing the values */
	private final DataTable table;

	/**
	 * Creates a table dialog.
	 *
	 * @param parent          owner window, may be null
	 * @param title           title of the dialog
	 * @param text            text shown above the table, may be blank
	 * @param headers         column headers
	 * @param items           rows of the table, each with one value per header
	 * @param closeButtonText text of the close button
	 */
	public TableDialog(final Window parent, final String title, final String text, final List<String> headers, final List<List<String>> items, final String closeButtonText) {
		super(parent, title, IconKind.LOG);

		if (StringUtilities.isNotBlank(text)) {
			getContentArea().add(TextSupport.createTextScrollPane(text, 800, 200), BorderLayout.NORTH);
		}

		final Object[] headerArray = headers.toArray();

		final Object[][] itemsArray = new Object[items.size()][];
		for (int i = 0; i < items.size(); i++) {
			itemsArray[i] = items.get(i).toArray();
		}

		final DefaultTableModel tableModel = new DefaultTableModel(itemsArray, headerArray) {
			private static final long serialVersionUID = -372185535503984868L;

			@Override
			public Class<?> getColumnClass(final int columnIndex) {
				return String.class;
			}

			@Override
			public boolean isCellEditable(final int rowIndex, final int columnIndex) {
				// Read-only table
				return false;
			}
		};
		table = new DataTable(tableModel);

		final JScrollPane tableScrollPane = table.createScrollPane(360, 900, 15);
		getContentArea().add(tableScrollPane, BorderLayout.CENTER);

		final JButton closeButton = addButton(closeButtonText, null, ButtonStyle.PRIMARY, event -> dispose());

		pack();
		setMinimumSize(new Dimension(Math.min(getWidth(), 360), Math.min(getHeight(), 260)));

		setLocationRelativeTo(parent);

		getRootPane().setDefaultButton(closeButton);
	}
}
