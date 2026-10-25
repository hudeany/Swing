package de.soderer.swing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Headless tests of {@link DropDown}. The popup is shown via a recording
 * {@link javax.swing.PopupFactory}, so no window is needed. All test code runs
 * on the event dispatch thread (see {@link SwingTestSupport#onEdt}).
 */
@SuppressWarnings("static-method")
public class DropDownTest {
	private static final String[] FRUITS = { "apple", "banana", "blueberry", "cherry" };

	private RecordingPopupFactory popupFactory;

	@BeforeEach
	public void installPopupFactory() throws Exception {
		SwingTestSupport.onEdt(() -> popupFactory = RecordingPopupFactory.install());
	}

	@AfterEach
	public void restorePopupFactory() throws Exception {
		SwingTestSupport.onEdt(() -> popupFactory.uninstall());
	}

	private static DropDown createDropDown() {
		final DropDown dropDown = new SwingTestSupport.ShowingDropDown().withItems(FRUITS);
		dropDown.setSize(200, 25);
		SwingTestSupport.layoutRecursively(dropDown);
		return dropDown;
	}

	private static void type(final DropDown dropDown, final String text) throws Exception {
		final JTextPane textPane = dropDown.getTextControl();
		textPane.getDocument().insertString(textPane.getCaretPosition(), text, null);
	}

	private static String displayed(final DropDown dropDown) {
		return dropDown.getTextControl().getText();
	}

	private static boolean pressKey(final DropDown dropDown, final int keyCode, final int modifiers) {
		final JTextPane textPane = dropDown.getTextControl();
		final KeyEvent event = new KeyEvent(textPane, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
		for (final KeyListener listener : textPane.getKeyListeners()) {
			if (listener.getClass().getEnclosingClass() == DropDown.class) {
				listener.keyPressed(event);
			}
		}
		return event.isConsumed();
	}

	private static boolean pressKey(final DropDown dropDown, final int keyCode) {
		return pressKey(dropDown, keyCode, 0);
	}

	private JList<?> popupList() {
		final JList<?> list = (JList<?>) ((JScrollPane) popupFactory.lastContents).getViewport().getView();
		list.setSize(200, 200);
		return list;
	}

	private static Point cellPoint(final JList<?> list, final int index, final boolean upperHalf) {
		final Rectangle cell = list.getCellBounds(index, index);
		return new Point(cell.x + 5, upperHalf ? cell.y + 1 : cell.y + cell.height - 1);
	}

	private static MouseEvent mouseEvent(final Component source, final int id, final Point point, final int button) {
		final int modifiers = button == MouseEvent.BUTTON1 && id != MouseEvent.MOUSE_RELEASED ? InputEvent.BUTTON1_DOWN_MASK : button == MouseEvent.BUTTON3 ? InputEvent.BUTTON3_DOWN_MASK : 0;
		// Explicit screen coordinates, the source may claim to be showing without a screen location
		return new MouseEvent(source, id, 0, modifiers, point.x, point.y, point.x, point.y, 1, false, button);
	}

	private static void popupMouse(final JList<?> list, final int id, final Point point, final int button) {
		final MouseEvent event = mouseEvent(list, id, point, button);
		for (final MouseListener listener : list.getMouseListeners()) {
			if (listener.getClass().getEnclosingClass() == DropDown.class) {
				if (id == MouseEvent.MOUSE_PRESSED) {
					listener.mousePressed(event);
				} else if (id == MouseEvent.MOUSE_RELEASED) {
					listener.mouseReleased(event);
				}
			}
		}
		for (final MouseMotionListener listener : list.getMouseMotionListeners()) {
			if (listener.getClass().getEnclosingClass() == DropDown.class) {
				if (id == MouseEvent.MOUSE_MOVED) {
					listener.mouseMoved(event);
				} else if (id == MouseEvent.MOUSE_DRAGGED) {
					listener.mouseDragged(event);
				}
			}
		}
	}

	private static BasicArrowButton dropdownButton(final DropDown dropDown) {
		for (final Component component : dropDown.getComponents()) {
			if (component instanceof BasicArrowButton) {
				return (BasicArrowButton) component;
			}
		}
		throw new AssertionError("No dropdown button");
	}

	@Test
	public void testPropertiesAndDefaults() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = new DropDown();
			assertEquals(0, dropDown.getItemCount());
			assertEquals("", dropDown.getText());
			assertTrue(dropDown.isValidValue());
			assertFalse(dropDown.isAllowCustomValues());
			assertFalse(dropDown.isCaseSensitive());
			assertEquals(DropDown.MatchMode.STARTS_WITH, dropDown.getMatchMode());
			assertFalse(dropDown.isReorderable());
			assertFalse(dropDown.isDeletable());
			assertNull(dropDown.getMessage());
			assertEquals(0, dropDown.getColumns());
			assertTrue(dropDown.isShowDropdownButton());
			assertEquals(new Color(170, 170, 170), dropDown.getSuggestionColor());
			assertEquals(new Color(220, 70, 70), dropDown.getInvalidColor());
			assertNull(dropDown.getMatchingItem());
			assertFalse(dropDown.isPopupVisible());
			assertEquals(2, DropDown.MatchMode.values().length);
			assertEquals(DropDown.MatchMode.CONTAINS, DropDown.MatchMode.valueOf("CONTAINS"));

			assertSame(dropDown, dropDown.withAllowCustomValues(true));
			assertTrue(dropDown.isAllowCustomValues());
			assertSame(dropDown, dropDown.withCaseSensitive(true));
			assertTrue(dropDown.isCaseSensitive());
			assertSame(dropDown, dropDown.withMatchMode(DropDown.MatchMode.CONTAINS));
			assertEquals(DropDown.MatchMode.CONTAINS, dropDown.getMatchMode());
			assertSame(dropDown, dropDown.withReorderable(true));
			assertTrue(dropDown.isReorderable());
			assertSame(dropDown, dropDown.withDeletable(true));
			assertTrue(dropDown.isDeletable());
			assertSame(dropDown, dropDown.withMessage("hint"));
			assertEquals("hint", dropDown.getMessage());
			assertSame(dropDown, dropDown.withColumns(12));
			assertEquals(12, dropDown.getColumns());
			assertSame(dropDown, dropDown.withShowDropdownButton(false));
			assertFalse(dropDown.isShowDropdownButton());
			assertSame(dropDown, dropDown.withSuggestionColor(Color.BLUE));
			assertEquals(Color.BLUE, dropDown.getSuggestionColor());
			assertSame(dropDown, dropDown.withInvalidColor(Color.MAGENTA));
			assertEquals(Color.MAGENTA, dropDown.getInvalidColor());
			assertSame(dropDown, dropDown.withEnabled(false));
			assertFalse(dropDown.isEnabled());
			assertFalse(dropDown.getTextControl().isEnabled());
			assertSame(dropDown, dropDown.withText("custom"));
			assertEquals("custom", dropDown.getText());

			final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 17);
			dropDown.setFont(font);
			assertEquals(font, dropDown.getTextControl().getFont());
			dropDown.setToolTipText("tip");
			assertEquals("tip", dropDown.getTextControl().getToolTipText());

			assertThrows(IllegalArgumentException.class, () -> dropDown.setColumns(-1));
			assertThrows(NullPointerException.class, () -> dropDown.setMatchMode(null));
			assertThrows(NullPointerException.class, () -> dropDown.setSuggestionColor(null));
			assertThrows(NullPointerException.class, () -> dropDown.setInvalidColor(null));
			assertThrows(NullPointerException.class, () -> dropDown.addItem(null));
			assertThrows(NullPointerException.class, () -> dropDown.setText(null));
			assertThrows(NullPointerException.class, () -> dropDown.addActionListener(null));
			assertThrows(NullPointerException.class, () -> dropDown.addChangeListener(null));
			assertThrows(NullPointerException.class, () -> dropDown.addValidationListener(null));
			assertThrows(NullPointerException.class, () -> dropDown.addItemDeletedListener(null));
			assertThrows(NullPointerException.class, () -> dropDown.addItemsReorderedListener(null));

			// Calls that only delegate to the text control
			dropDown.selectAll();
			assertEquals(6, dropDown.getSelection().y);
			dropDown.requestFocusInWindow();
			dropDown.requestFocus();
		});
	}

	@Test
	public void testItemManagement() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = new DropDown();
			dropDown.setItems((String[]) null);
			assertEquals(0, dropDown.getItemCount());
			dropDown.setItems((List<String>) null);
			assertEquals(0, dropDown.getItemCount());
			// Null entries are skipped
			assertSame(dropDown, dropDown.withItems(Arrays.asList("a", null, "b")));
			assertArrayEquals(new String[] { "a", "b" }, dropDown.getItems());
			dropDown.addItem("c");
			assertEquals(3, dropDown.getItemCount());
			dropDown.removeItemAt(0);
			assertArrayEquals(new String[] { "b", "c" }, dropDown.getItems());
			dropDown.removeItem("c");
			dropDown.removeItem("unknown");
			assertArrayEquals(new String[] { "b" }, dropDown.getItems());

			// Removing all items makes an accepted value invalid
			dropDown.setText("b");
			assertTrue(dropDown.isValidValue());
			dropDown.removeAllItems();
			assertEquals(0, dropDown.getItemCount());
			assertFalse(dropDown.isValidValue());
			assertNull(dropDown.getText());

			// Without items, the full list cannot be shown
			dropDown.showAllItems();
			assertFalse(dropDown.isPopupVisible());
		});
	}

	@Test
	public void testSetTextAndMatchingItem() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			dropDown.setText("Banana");
			assertEquals("Banana", dropDown.getText());
			// The matching item is shown as suggestion
			assertEquals("banana", displayed(dropDown));
			// Case insensitive match
			assertEquals("banana", dropDown.getMatchingItem());
			dropDown.setText("banana");
			assertEquals("banana", dropDown.getMatchingItem());
			dropDown.setText("ban");
			assertNull(dropDown.getMatchingItem());
			// Not shown programmatically
			assertFalse(dropDown.isPopupVisible());

			dropDown.setCaseSensitive(true);
			dropDown.setText("Banana");
			assertNull(dropDown.getMatchingItem());
			assertFalse(dropDown.isValidValue());
			assertNull(dropDown.getText());

			dropDown.setText("");
			assertTrue(dropDown.isValidValue());
			assertNull(dropDown.getMatchingItem());
		});
	}

	@Test
	public void testTypingShowsSuggestionAndAutoAcceptsSingleMatch() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			final List<String> actions = new ArrayList<>();
			final List<String> changes = new ArrayList<>();
			final ActionListener actionListener = e -> actions.add(e.getActionCommand());
			final ChangeListener changeListener = e -> changes.add(dropDown.getText());
			dropDown.addActionListener(actionListener);
			dropDown.addChangeListener(changeListener);

			type(dropDown, "b");
			assertEquals("b", dropDown.getText());
			// The first match is shown as suggestion
			assertEquals("banana", displayed(dropDown));
			assertEquals(1, dropDown.getTextControl().getCaretPosition());
			assertTrue(dropDown.isPopupVisible());
			assertEquals(2, popupList().getModel().getSize());
			assertTrue(actions.isEmpty());

			// Typing at the start of the suggestion tail: single match is accepted
			type(dropDown, "l");
			assertEquals("blueberry", dropDown.getText());
			assertEquals("blueberry", displayed(dropDown));
			assertFalse(dropDown.isPopupVisible());
			assertEquals(Arrays.asList(DropDown.ACTION_COMMAND_VALUE_ACCEPTED), actions);
			assertEquals(Arrays.asList("b", "blueberry"), changes);

			dropDown.removeActionListener(actionListener);
			dropDown.removeChangeListener(changeListener);
			dropDown.setText("cherry");
			assertEquals(1, actions.size());
			assertEquals(2, changes.size());
		});
	}

	@Test
	public void testDeletingInsideSuggestion() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			final AbstractDocument document = (AbstractDocument) dropDown.getTextControl().getDocument();

			type(dropDown, "b");
			assertEquals("banana", displayed(dropDown));
			// Removing the gray suggestion tail keeps the typed text
			document.remove(1, 5);
			assertEquals("b", dropDown.getText());
			assertEquals("banana", displayed(dropDown));

			// Backspace directly before the suggestion tail removes the last typed character
			document.remove(0, 1);
			assertEquals("", dropDown.getText());
			assertEquals("", displayed(dropDown));
			assertFalse(dropDown.isPopupVisible());

			// Other edits apply to the displayed text
			type(dropDown, "c");
			assertEquals("cherry", dropDown.getText());
			document.replace(0, 6, "bl", null);
			assertEquals("blueberry", dropDown.getText());
			// Line breaks are removed, an empty edit changes nothing
			document.replace(0, 0, "\r\n", null);
			assertEquals("blueberry", dropDown.getText());
			document.replace(0, 9, "ba\nn", null);
			assertEquals("banana", dropDown.getText());
		});
	}

	@Test
	public void testInvalidAndCustomValues() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			final List<Boolean> validations = new ArrayList<>();
			final Consumer<Boolean> validationListener = validations::add;
			dropDown.addValidationListener(validationListener);

			type(dropDown, "x");
			assertFalse(dropDown.isValidValue());
			assertNull(dropDown.getText());
			assertEquals("x", displayed(dropDown));
			assertEquals(Arrays.asList(Boolean.FALSE), validations);
			// ENTER and TAB are swallowed, the invalid value is not accepted
			assertTrue(pressKey(dropDown, KeyEvent.VK_ENTER));
			assertTrue(pressKey(dropDown, KeyEvent.VK_TAB));
			assertFalse(dropDown.isValidValue());

			dropDown.setAllowCustomValues(true);
			assertTrue(dropDown.isValidValue());
			assertEquals("x", dropDown.getText());
			assertEquals(Arrays.asList(Boolean.FALSE, Boolean.TRUE), validations);
			// Not consumed: may trigger a default button
			assertFalse(pressKey(dropDown, KeyEvent.VK_ENTER));

			type(dropDown, "y");
			assertEquals("xy", dropDown.getText());
			assertEquals("xy", displayed(dropDown));

			// With custom values a single match is not auto accepted
			dropDown.setText("");
			type(dropDown, "ch");
			assertEquals("ch", dropDown.getText());
			assertEquals("cherry", displayed(dropDown));
			assertFalse(dropDown.isPopupVisible());

			dropDown.removeValidationListener(validationListener);
			dropDown.setAllowCustomValues(false);
			dropDown.setText("zzz");
			dropDown.setText("");
			assertEquals(2, validations.size());

			// Emptying an accepted value without custom values is invalid
			dropDown.setText("apple");
			dropDown.getTextControl().getDocument().remove(0, 5);
			assertFalse(dropDown.isValidValue());
		});
	}

	@Test
	public void testMatchModesAndCaseSensitivity() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown().withMatchMode(DropDown.MatchMode.CONTAINS);
			type(dropDown, "rr");
			// "berry" and "cherry"
			assertEquals("blueberry", displayed(dropDown));
			assertEquals(2, popupList().getModel().getSize());

			final DropDown caseSensitive = createDropDown().withCaseSensitive(true).withItems("Apple", "apple", "apricot");
			type(caseSensitive, "a");
			assertEquals(2, popupList().getModel().getSize());
			// The exact match is preselected
			caseSensitive.setAllowCustomValues(true);
			caseSensitive.setText("");
			type(caseSensitive, "apple");
			assertEquals("apple", displayed(caseSensitive));
		});
	}

	@Test
	public void testKeyboardNavigation() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			// Keys without a match do nothing
			dropDown.setItems();
			assertFalse(pressKey(dropDown, KeyEvent.VK_DOWN));
			assertFalse(pressKey(dropDown, KeyEvent.VK_UP));
			assertFalse(pressKey(dropDown, KeyEvent.VK_PAGE_DOWN));
			assertFalse(pressKey(dropDown, KeyEvent.VK_PAGE_UP));
			assertFalse(pressKey(dropDown, KeyEvent.VK_ESCAPE));
			assertFalse(pressKey(dropDown, KeyEvent.VK_ENTER));
			assertFalse(pressKey(dropDown, KeyEvent.VK_DELETE));
			assertFalse(pressKey(dropDown, KeyEvent.VK_A));
			dropDown.setItems(FRUITS);

			// DOWN on an empty field opens the full list with the first entry
			assertTrue(pressKey(dropDown, KeyEvent.VK_DOWN));
			assertTrue(dropDown.isPopupVisible());
			assertEquals(0, popupList().getSelectedIndex());
			assertTrue(pressKey(dropDown, KeyEvent.VK_KP_DOWN));
			assertEquals(1, popupList().getSelectedIndex());
			assertTrue(pressKey(dropDown, KeyEvent.VK_UP));
			assertTrue(pressKey(dropDown, KeyEvent.VK_KP_UP));
			// Wraps around to the last entry
			assertEquals(3, popupList().getSelectedIndex());
			assertTrue(pressKey(dropDown, KeyEvent.VK_DOWN));
			assertEquals(0, popupList().getSelectedIndex());
			assertTrue(pressKey(dropDown, KeyEvent.VK_PAGE_DOWN));
			assertEquals(3, popupList().getSelectedIndex());
			assertTrue(pressKey(dropDown, KeyEvent.VK_PAGE_UP));
			assertEquals(0, popupList().getSelectedIndex());

			// ESCAPE closes the popup and is consumed only then
			assertTrue(pressKey(dropDown, KeyEvent.VK_ESCAPE));
			assertFalse(dropDown.isPopupVisible());
			assertFalse(pressKey(dropDown, KeyEvent.VK_ESCAPE));

			// UP on an empty field opens the full list with the last entry, ENTER accepts it
			dropDown.setText("");
			assertTrue(pressKey(dropDown, KeyEvent.VK_UP));
			assertEquals(3, popupList().getSelectedIndex());
			assertTrue(pressKey(dropDown, KeyEvent.VK_ENTER));
			assertEquals("cherry", dropDown.getText());
			assertFalse(dropDown.isPopupVisible());
			// Already accepted: ENTER is not consumed again
			assertFalse(pressKey(dropDown, KeyEvent.VK_ENTER));

			// Programmatically set text has matches, but no popup: page size is the default
			dropDown.setText("b");
			assertFalse(dropDown.isPopupVisible());
			assertTrue(pressKey(dropDown, KeyEvent.VK_PAGE_DOWN));
			assertEquals("blueberry", displayed(dropDown));
			assertTrue(pressKey(dropDown, KeyEvent.VK_PAGE_UP));
			assertEquals("banana", displayed(dropDown));
			assertTrue(pressKey(dropDown, KeyEvent.VK_ENTER));
			assertEquals("banana", dropDown.getText());

			// Disabled: keys are ignored
			dropDown.setText("");
			dropDown.setEnabled(false);
			assertFalse(pressKey(dropDown, KeyEvent.VK_DOWN));
		});
	}

	@Test
	public void testTabKey() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			// CTRL+TAB and ALT+TAB are left to others
			assertFalse(pressKey(dropDown, KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK));
			assertFalse(pressKey(dropDown, KeyEvent.VK_TAB, InputEvent.ALT_DOWN_MASK));

			type(dropDown, "b");
			// TAB accepts the suggestion and moves the focus
			assertTrue(pressKey(dropDown, KeyEvent.VK_TAB));
			assertEquals("banana", dropDown.getText());
			dropDown.setText("");
			type(dropDown, "b");
			assertTrue(pressKey(dropDown, KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
			assertEquals("banana", dropDown.getText());
		});
	}

	@Test
	public void testDeleteKey() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			final List<String> deleted = new ArrayList<>();
			final Consumer<String> deletedListener = deleted::add;
			dropDown.addItemDeletedListener(deletedListener);

			// Not deletable
			pressKey(dropDown, KeyEvent.VK_DOWN);
			assertFalse(pressKey(dropDown, KeyEvent.VK_DELETE));
			assertEquals(4, dropDown.getItemCount());

			dropDown.setDeletable(true);
			// Popup without a selection by the user
			dropDown.setText("");
			dropDown.showAllItems();
			assertFalse(pressKey(dropDown, KeyEvent.VK_DELETE));

			pressKey(dropDown, KeyEvent.VK_DOWN);
			pressKey(dropDown, KeyEvent.VK_DOWN);
			assertTrue(pressKey(dropDown, KeyEvent.VK_DELETE));
			assertArrayEquals(new String[] { "apple", "blueberry", "cherry" }, dropDown.getItems());
			assertEquals(Arrays.asList("banana"), deleted);
			assertEquals(3, popupList().getModel().getSize());
			assertEquals(1, popupList().getSelectedIndex());

			// Deleting the current value resets it
			dropDown.setText("cherry");
			dropDown.showAllItems();
			pressKey(dropDown, KeyEvent.VK_UP);
			assertTrue(pressKey(dropDown, KeyEvent.VK_DELETE));
			assertEquals("", dropDown.getText());
			assertTrue(dropDown.isValidValue());

			// Deleting the last remaining entry empties the field
			dropDown.setItems("only");
			dropDown.setText("");
			pressKey(dropDown, KeyEvent.VK_DOWN);
			assertTrue(pressKey(dropDown, KeyEvent.VK_DELETE));
			assertEquals(0, dropDown.getItemCount());
			assertEquals(Arrays.asList("banana", "cherry", "only"), deleted);

			dropDown.removeItemDeletedListener(deletedListener);
		});
	}

	@Test
	public void testPopupMouseSelection() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			dropDown.showAllItems();
			final JList<?> list = popupList();

			// Other buttons are ignored
			popupMouse(list, MouseEvent.MOUSE_PRESSED, cellPoint(list, 2, true), MouseEvent.BUTTON3);
			popupMouse(list, MouseEvent.MOUSE_RELEASED, cellPoint(list, 2, true), MouseEvent.BUTTON3);
			assertEquals("", displayed(dropDown));

			// Hovering previews the entry
			popupMouse(list, MouseEvent.MOUSE_MOVED, cellPoint(list, 1, true), MouseEvent.NOBUTTON);
			assertEquals(1, list.getSelectedIndex());
			popupMouse(list, MouseEvent.MOUSE_MOVED, cellPoint(list, 1, false), MouseEvent.NOBUTTON);
			assertEquals(1, list.getSelectedIndex());

			// Press previews, release accepts
			popupMouse(list, MouseEvent.MOUSE_PRESSED, cellPoint(list, 2, true), MouseEvent.BUTTON1);
			list.setSelectedIndex(2);
			assertTrue(dropDown.isPopupVisible());
			popupMouse(list, MouseEvent.MOUSE_RELEASED, cellPoint(list, 2, true), MouseEvent.BUTTON1);
			assertEquals("blueberry", dropDown.getText());
			assertFalse(dropDown.isPopupVisible());

			// Release without a selection accepts nothing
			dropDown.showAllItems();
			list.clearSelection();
			popupMouse(list, MouseEvent.MOUSE_PRESSED, new Point(5, 1000), MouseEvent.BUTTON1);
			list.clearSelection();
			popupMouse(list, MouseEvent.MOUSE_RELEASED, new Point(5, 1000), MouseEvent.BUTTON1);
			assertTrue(dropDown.isPopupVisible());

			// Dragging is ignored if not reorderable
			popupMouse(list, MouseEvent.MOUSE_PRESSED, cellPoint(list, 0, true), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 3, false), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_RELEASED, cellPoint(list, 3, false), MouseEvent.BUTTON1);
			assertArrayEquals(FRUITS, dropDown.getItems());
		});
	}

	@Test
	public void testReorderByDragging() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown().withReorderable(true);
			final List<List<String>> reorders = new ArrayList<>();
			final Consumer<List<String>> reorderListener = reorders::add;
			dropDown.addItemsReorderedListener(reorderListener);
			dropDown.showAllItems();
			final JList<?> list = popupList();

			// Drag with right button and without press is ignored
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 3, false), MouseEvent.BUTTON3);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 3, false), MouseEvent.BUTTON1);

			// Small movements are still a click
			final Point start = cellPoint(list, 0, true);
			popupMouse(list, MouseEvent.MOUSE_PRESSED, start, MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, new Point(start.x + 2, start.y + 2), MouseEvent.BUTTON1);

			// Drag "apple" behind "cherry"
			list.setSelectedIndex(2);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 3, false), MouseEvent.BUTTON1);
			assertEquals(0, list.getSelectedIndex());
			SwingTestSupport.paint(list);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 2, true), MouseEvent.BUTTON1);
			SwingTestSupport.paint(list);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 3, false), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_RELEASED, cellPoint(list, 3, false), MouseEvent.BUTTON1);
			assertArrayEquals(new String[] { "banana", "blueberry", "cherry", "apple" }, dropDown.getItems());
			assertEquals(Arrays.asList(Arrays.asList("banana", "blueberry", "cherry", "apple")), reorders);
			assertTrue(dropDown.isPopupVisible());

			// Drop on the same position changes nothing
			popupMouse(list, MouseEvent.MOUSE_PRESSED, cellPoint(list, 1, true), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 1, false), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 1, false), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_RELEASED, cellPoint(list, 1, false), MouseEvent.BUTTON1);
			assertEquals(1, reorders.size());

			// Drag "cherry" before "banana"
			popupMouse(list, MouseEvent.MOUSE_PRESSED, cellPoint(list, 2, true), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 0, true), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_RELEASED, cellPoint(list, 0, true), MouseEvent.BUTTON1);
			assertArrayEquals(new String[] { "cherry", "banana", "blueberry", "apple" }, dropDown.getItems());
			assertEquals(2, reorders.size());

			// Filtered matches cannot be reordered
			dropDown.setText("");
			type(dropDown, "b");
			popupMouse(list, MouseEvent.MOUSE_PRESSED, cellPoint(list, 0, true), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_DRAGGED, cellPoint(list, 1, false), MouseEvent.BUTTON1);
			popupMouse(list, MouseEvent.MOUSE_RELEASED, cellPoint(list, 1, false), MouseEvent.BUTTON1);
			assertEquals(2, reorders.size());

			dropDown.removeItemsReorderedListener(reorderListener);
		});
	}

	@Test
	public void testDropdownButtonAndTextMouseClick() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			final BasicArrowButton button = dropdownButton(dropDown);
			assertEquals(20, button.getPreferredSize().width);

			button.doClick(0);
			assertTrue(dropDown.isPopupVisible());
			assertEquals(4, popupList().getModel().getSize());
			// Second click collapses the full list
			button.doClick(0);
			assertFalse(dropDown.isPopupVisible());

			// The popup with the matches is expanded to the full list
			type(dropDown, "b");
			assertEquals(2, popupList().getModel().getSize());
			button.doClick(0);
			assertEquals(4, popupList().getModel().getSize());
			// Selecting an entry that does not contain the typed text shows it completely
			assertTrue(pressKey(dropDown, KeyEvent.VK_DOWN));
			assertEquals("apple", displayed(dropDown));
			pressKey(dropDown, KeyEvent.VK_ESCAPE);

			// A click into the text shows the popup if there are several matches
			final JTextPane textPane = dropDown.getTextControl();
			final MouseEvent click = mouseEvent(textPane, MouseEvent.MOUSE_PRESSED, new Point(1, 1), MouseEvent.BUTTON1);
			for (final MouseListener listener : textPane.getMouseListeners()) {
				if (listener.getClass().getEnclosingClass() == DropDown.class) {
					listener.mousePressed(click);
				}
			}
			assertTrue(dropDown.isPopupVisible());
		});
	}

	@Test
	public void testPopupIsHiddenByFocusLossAndAncestorChanges() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			final JTextPane textPane = dropDown.getTextControl();

			dropDown.showAllItems();
			for (final FocusListener listener : textPane.getFocusListeners()) {
				if (listener.getClass().getEnclosingClass() == DropDown.class) {
					listener.focusLost(new FocusEvent(textPane, FocusEvent.FOCUS_LOST));
				}
			}
			assertFalse(dropDown.isPopupVisible());

			for (final HierarchyBoundsListener listener : dropDown.getHierarchyBoundsListeners()) {
				dropDown.showAllItems();
				listener.ancestorMoved(new HierarchyEvent(dropDown, HierarchyEvent.ANCESTOR_MOVED, dropDown, null));
				assertFalse(dropDown.isPopupVisible());
				dropDown.showAllItems();
				listener.ancestorResized(new HierarchyEvent(dropDown, HierarchyEvent.ANCESTOR_RESIZED, dropDown, null));
				assertFalse(dropDown.isPopupVisible());
			}
		});
	}

	@Test
	public void testOutsideClickHidesPopup() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			final JButton otherComponent = new JButton("other");
			dropDown.addNotify();
			try {
				// A second registration is skipped
				dropDown.addNotify();

				dropDown.showAllItems();
				// Clicks into the drop down and into the popup keep the popup open
				dropDown.getTextControl().dispatchEvent(mouseEvent(dropDown.getTextControl(), MouseEvent.MOUSE_PRESSED, new Point(1, 1), MouseEvent.BUTTON1));
				assertTrue(dropDown.isPopupVisible());
				final Component popupContent = popupFactory.lastContents;
				popupContent.dispatchEvent(mouseEvent(popupContent, MouseEvent.MOUSE_PRESSED, new Point(1, 1), MouseEvent.BUTTON1));
				dropDown.showAllItems();
				assertTrue(dropDown.isPopupVisible());
				// Other mouse events are ignored
				otherComponent.dispatchEvent(mouseEvent(otherComponent, MouseEvent.MOUSE_RELEASED, new Point(1, 1), MouseEvent.BUTTON1));
				assertTrue(dropDown.isPopupVisible());

				otherComponent.dispatchEvent(mouseEvent(otherComponent, MouseEvent.MOUSE_PRESSED, new Point(1, 1), MouseEvent.BUTTON1));
				assertFalse(dropDown.isPopupVisible());
				// Without popup nothing happens
				otherComponent.dispatchEvent(mouseEvent(otherComponent, MouseEvent.MOUSE_PRESSED, new Point(1, 1), MouseEvent.BUTTON1));
			} finally {
				dropDown.removeNotify();
			}
			// Unregistered: the popup stays open
			dropDown.removeNotify();
			dropDown.showAllItems();
			otherComponent.dispatchEvent(mouseEvent(otherComponent, MouseEvent.MOUSE_PRESSED, new Point(1, 1), MouseEvent.BUTTON1));
			assertTrue(dropDown.isPopupVisible());
		});
	}

	@Test
	public void testPopupPositioning() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			dropDown.showAllItems();
			assertEquals(1, popupFactory.shownCount);
			assertEquals(200, popupFactory.lastContents.getPreferredSize().width);
			// Same bounds: the shown popup is reused
			dropDown.showAllItems();
			assertEquals(1, popupFactory.shownCount);
			// New bounds: a new popup replaces the old one
			dropDown.setSize(250, 25);
			dropDown.showAllItems();
			assertEquals(2, popupFactory.shownCount);
			assertEquals(1, popupFactory.hiddenCount);
			// The popup content follows the look and feel
			dropDown.updateUI();
			dropDown.setFont(new Font(Font.SERIF, Font.BOLD, 20));
			assertEquals(20, popupList().getFont().getSize());

			// Not showing: no popup
			final DropDown hiddenDropDown = new DropDown().withItems(FRUITS);
			hiddenDropDown.showAllItems();
			assertFalse(hiddenDropDown.isPopupVisible());
		});
		// Let the delayed repaint of the popup run
		Thread.sleep(250);
		SwingTestSupport.onEdt(() -> {
			// Nothing to do, only waits for the queued events
		});
	}

	@Test
	public void testEnableAndDisable() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown();
			type(dropDown, "b");
			assertTrue(dropDown.isPopupVisible());
			dropDown.setEnabled(false);
			assertFalse(dropDown.isPopupVisible());
			assertFalse(dropDown.getTextControl().isEnabled());
			// Disabled text control: clicks do not open the popup
			final MouseEvent click = mouseEvent(dropDown.getTextControl(), MouseEvent.MOUSE_PRESSED, new Point(1, 1), MouseEvent.BUTTON1);
			for (final MouseListener listener : dropDown.getTextControl().getMouseListeners()) {
				if (listener.getClass().getEnclosingClass() == DropDown.class) {
					listener.mousePressed(click);
				}
			}
			assertFalse(dropDown.isPopupVisible());

			// Look and feel without inactive colors
			SwingTestSupport.withoutUiDefaults(() -> dropDown.setEnabled(false), "TextField.inactiveBackground");
			SwingTestSupport.withoutUiDefaults(() -> {
				dropDown.setEnabled(false);
				assertEquals(UIManager.getColor("Panel.background"), dropDown.getTextControl().getBackground());
			}, "TextField.inactiveBackground", "TextField.disabledBackground");

			dropDown.setEnabled(true);
			assertTrue(dropDown.getTextControl().isEnabled());
			assertEquals("b", dropDown.getText());
			assertEquals("banana", displayed(dropDown));
		});
	}

	@Test
	public void testLookAndFeelFallbacks() throws Exception {
		SwingTestSupport.onEdt(() -> {
			SwingTestSupport.withoutUiDefaults(() -> {
				final DropDown dropDown = createDropDown();
				// The scroll pane border is used instead of the text field border
				assertNotNull(dropDown.getComponent(0) instanceof JScrollPane ? ((JScrollPane) dropDown.getComponent(0)).getBorder() : null);
				type(dropDown, "a");
				assertEquals("apple", dropDown.getText());
			}, "TextField.border", "TextField.font", "TextField.foreground");
		});
	}

	@Test
	public void testSizesAndPainting() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final DropDown dropDown = createDropDown().withMessage("A rather long hint message");
			final Dimension withHint = dropDown.getPreferredSize();
			dropDown.setMessage(null);
			final Dimension withoutHint = dropDown.getPreferredSize();
			assertTrue(withHint.width > withoutHint.width);
			dropDown.setColumns(40);
			assertTrue(dropDown.getPreferredSize().width > withHint.width);
			dropDown.setColumns(0);

			final Dimension minimumWithButton = dropDown.getMinimumSize();
			assertTrue(minimumWithButton.width <= dropDown.getPreferredSize().width);
			assertEquals(dropDown.getPreferredSize().height, minimumWithButton.height);
			dropDown.setShowDropdownButton(false);
			assertTrue(dropDown.getMinimumSize().width < minimumWithButton.width);
			dropDown.setMinimumSize(new Dimension(7, 8));
			assertEquals(new Dimension(7, 8), dropDown.getMinimumSize());

			// The hint is only painted for an empty field
			dropDown.setMessage("hint");
			SwingTestSupport.layoutRecursively(dropDown);
			SwingTestSupport.paint(dropDown);
			dropDown.setText("cherry");
			SwingTestSupport.paint(dropDown);
			assertNotNull(dropDown.getTextControl().getPreferredScrollableViewportSize());
			assertTrue(dropDown.getTextControl().getScrollableTracksViewportHeight());
			assertTrue(dropDown.getTextControl().getMinimumSize().width > 0);
			// Drawn as single line
			dropDown.getTextControl().setSize(10, 20);
			SwingTestSupport.paint(dropDown.getTextControl());

			// Popup list without drag marker
			dropDown.setText("");
			dropDown.showAllItems();
			SwingTestSupport.paint(popupList());
		});
	}

	@Test
	public void testSingleLineEditorKitViews() throws Exception {
		SwingTestSupport.onEdt(() -> {
			final ViewFactory viewFactory = ((StyledEditorKit) createDropDown().getTextControl().getEditorKit()).getViewFactory();
			final DefaultStyledDocument document = new DefaultStyledDocument();
			final SimpleAttributeSet componentAttributes = new SimpleAttributeSet();
			StyleConstants.setComponent(componentAttributes, new JLabel("component"));
			document.insertString(0, " ", componentAttributes);
			final SimpleAttributeSet iconAttributes = new SimpleAttributeSet();
			StyleConstants.setIcon(iconAttributes, UIManager.getIcon("OptionPane.informationIcon") == null ? new javax.swing.ImageIcon() : UIManager.getIcon("OptionPane.informationIcon"));
			document.insertString(1, " ", iconAttributes);
			document.insertString(2, "text", null);

			final Element paragraph = document.getDefaultRootElement().getElement(0);
			final View section = viewFactory.create(document.getDefaultRootElement());
			assertEquals(View.Y_AXIS, ((javax.swing.text.BoxView) section).getAxis());
			final View paragraphView = viewFactory.create(paragraph);
			assertEquals("NoWrapParagraphView", paragraphView.getClass().getSimpleName());
			assertTrue(viewFactory.create(paragraph.getElement(0)) instanceof javax.swing.text.ComponentView);
			assertTrue(viewFactory.create(paragraph.getElement(1)) instanceof javax.swing.text.IconView);
			assertTrue(viewFactory.create(paragraph.getElement(2)) instanceof javax.swing.text.LabelView);
		});
	}
}
