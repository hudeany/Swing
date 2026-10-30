package de.soderer.swing.modern;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextArea;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.StringUtilities;

/**
 * Modal dialog in the {@link ModernUi} design to select one value out of a list of values.
 * Same API as {@link de.soderer.swing.ComboSelectionDialog}.
 *
 * <p>
 * Uses {@link DropDown} with autocomplete. The entries can be reordered via drag&amp;drop and removed via the
 * Delete key in the popup list. Callers can take over those changes via {@link #getItems()} after the dialog was
 * closed, regardless of whether a value was selected or the dialog was canceled.
 * </p>
 *
 * <p>
 * {@link #open()} returns the selected value (always one of the items), or {@code null} if the dialog was
 * canceled.
 * </p>
 */
public class ComboSelectionDialog extends ModalDialog<String> {
	/** Serialization version */
	private static final long serialVersionUID = 5184092736511829073L;

	/** Minimum width of the dialog */
	private static final int MIN_WIDTH = 360;

	/** Selection with autocomplete, its items may be reordered and deleted by the user */
	private final DropDown dropDown;

	/** OK button, only enabled while a valid, non-empty value is selected */
	private final JButton okButton;

	/**
	 * Creates a selection dialog without preselected value (a single item is preselected anyway).
	 *
	 * @param parent owner window, may be null
	 * @param title  title of the dialog
	 * @param text   text shown above the selection, may be blank
	 * @param items  items to select from, may be null for none
	 */
	public ComboSelectionDialog(final Window parent, final String title, final String text, final Collection<String> items) {
		this(parent, title, text, items, -1);
	}

	/**
	 * Creates a selection dialog without preselected value (a single item is preselected anyway).
	 *
	 * @param parent owner window, may be null
	 * @param title  title of the dialog
	 * @param text   text shown above the selection, may be blank
	 * @param items  items to select from, may be null for none
	 */
	public ComboSelectionDialog(final Window parent, final String title, final String text, final String... items) {
		this(parent, title, text, items == null ? null : Arrays.asList(items), -1);
	}

	/**
	 * Creates a selection dialog.
	 *
	 * @param parent           owner window, may be null
	 * @param title            title of the dialog
	 * @param text             text shown above the selection, may be blank
	 * @param items            items to select from, may be null for none
	 * @param preselectedIndex index of the item shown initially, or -1 for an empty selection (a single item is
	 *                         always preselected)
	 */
	public ComboSelectionDialog(final Window parent, final String title, final String text, final Collection<String> items, final int preselectedIndex) {
		super(parent, title, IconKind.SEARCH);

		returnValue = null;

		if (StringUtilities.isNotBlank(text)) {
			final JTextArea textField = TextSupport.install(new JTextArea(text));
			textField.setFocusable(false);
			textField.setOpaque(false);
			textField.setBorder(BorderFactory.createEmptyBorder());
			getContentArea().add(textField, BorderLayout.NORTH);
		}

		final List<String> itemList = items == null ? new ArrayList<>() : new ArrayList<>(items);

		dropDown = new DropDown()
				.withItems(itemList)
				.withAllowCustomValues(false)
				.withMatchMode(DropDown.MatchMode.CONTAINS)
				.withReorderable(true)
				.withDeletable(true);
		if (preselectedIndex >= 0 && preselectedIndex < itemList.size()) {
			dropDown.setText(itemList.get(preselectedIndex));
		} else if (itemList.size() == 1) {
			// Only one possible choice: preselect it, so it can be confirmed directly
			dropDown.setText(itemList.get(0));
		}
		dropDown.addChangeListener(event -> checkButtonStatus());
		dropDown.addValidationListener(valid -> checkButtonStatus());
		dropDown.addItemDeletedListener(deletedItem -> checkButtonStatus());
		getContentArea().add(dropDown, BorderLayout.CENTER);
		ModernUi.blendIntoCard(dropDown);

		okButton = addButton(LangResources.get("ok"), null, ButtonStyle.PRIMARY, event -> {
			// Only a complete item, never a partial text like "ap" that merely matches some items
			returnValue = dropDown.getMatchingItem();
			if (returnValue != null) {
				dispose();
			}
		});
		addButton(LangResources.get("cancel"), null, ButtonStyle.SECONDARY, event -> {
			returnValue = null;
			dispose();
		});

		// Enter is only passed through by the DropDown if there is no suggestion to accept, so it confirms a valid value
		getRootPane().setDefaultButton(okButton);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(final WindowEvent event) {
				dropDown.requestFocusInWindow();
			}
		});

		setResizable(false);

		pack();

		setMinimumSize(new Dimension(Math.max(getWidth(), MIN_WIDTH), getHeight()));
		setSize(getMinimumSize());

		setLocationRelativeTo(parent);

		checkButtonStatus();
	}

	/**
	 * Sets an explicit dialog size and centers the dialog on its owner again.
	 *
	 * @param width  new width in pixels, a value &lt;= 0 keeps the current width
	 * @param height new height in pixels, a value &lt;= 0 keeps the current height
	 * @return this dialog for chaining
	 */
	public ComboSelectionDialog withSize(final int width, final int height) {
		setSize(width > 0 ? width : getWidth(), height > 0 ? height : getHeight());
		setLocationRelativeTo(getOwner());
		return this;
	}

	/**
	 * Returns the current items in their current order, including reordering and deletions done by the user in
	 * the dialog.
	 *
	 * @return new list of the items
	 */
	public List<String> getItems() {
		return new ArrayList<>(Arrays.asList(dropDown.getItems()));
	}

	/**
	 * Enables the OK button only while the text is a complete item.
	 */
	private void checkButtonStatus() {
		// isValidValue() is already true for a partial text, so the text must be a complete item
		okButton.setEnabled(dropDown.isValidValue() && dropDown.getMatchingItem() != null);
	}
}
