package de.soderer.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.junit.jupiter.api.function.Executable;

/**
 * Helpers for headless Swing tests.
 */
public final class SwingTestSupport {
	private SwingTestSupport() {
		// Utility class
	}

	/**
	 * Drop down that claims to be showing, so it opens its popup without a window.
	 */
	public static class ShowingDropDown extends DropDown {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isShowing() {
			return true;
		}
	}

	/**
	 * Runs test code on the event dispatch thread and rethrows its errors.
	 */
	public static void onEdt(final Executable executable) throws Exception {
		final Throwable[] error = new Throwable[1];
		final Runnable runnable = () -> {
			try {
				executable.execute();
			} catch (final Throwable t) {
				error[0] = t;
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			javax.swing.SwingUtilities.invokeAndWait(runnable);
		}
		if (error[0] instanceof Error) {
			throw (Error) error[0];
		} else if (error[0] instanceof Exception) {
			throw (Exception) error[0];
		} else if (error[0] != null) {
			throw new IllegalStateException(error[0]);
		}
	}

	/**
	 * Runs code with look and feel defaults removed, to test the fallbacks for missing defaults.
	 */
	public static void withoutUiDefaults(final Executable executable, final String... keys) throws Exception {
		final UIDefaults defaults = UIManager.getLookAndFeelDefaults();
		final Map<String, Object> removedValues = new HashMap<>();
		for (final String key : keys) {
			removedValues.put(key, defaults.get(key));
			defaults.remove(key);
		}
		try {
			onEdt(executable);
		} finally {
			for (final Map.Entry<String, Object> entry : removedValues.entrySet()) {
				if (entry.getValue() != null) {
					defaults.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	/**
	 * Lays out a component tree without a window.
	 */
	public static void layoutRecursively(final Component component) {
		if (component instanceof Container) {
			final Container container = (Container) component;
			container.doLayout();
			for (final Component child : container.getComponents()) {
				layoutRecursively(child);
			}
		}
	}

	/**
	 * Paints a component into an image. Printing avoids the double buffer, which needs a display.
	 */
	public static void paint(final Component component) {
		final BufferedImage image = new BufferedImage(Math.max(1, component.getWidth()), Math.max(1, component.getHeight()), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try {
			component.printAll(graphics);
		} finally {
			graphics.dispose();
		}
	}

	/**
	 * Calls a private method, for helpers that are only reachable via a dialog.
	 */
	public static Object invokePrivate(final Object target, final Class<?> clazz, final String methodName, final Class<?>[] parameterTypes, final Object... parameters) throws Exception {
		final Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
		method.setAccessible(true);
		try {
			return method.invoke(target, parameters);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			} else if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			} else {
				throw e;
			}
		}
	}

	/**
	 * Reads a private field.
	 */
	public static Object getPrivateField(final Object target, final Class<?> clazz, final String fieldName) throws Exception {
		final Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}
}
