package de.soderer.swing;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;

import javax.swing.UIManager;

import de.soderer.utilities.SystemUtilities;

/**
 * Helper methods for Swing applications (class preloading, Linux window
 * manager class, look and feel).
 */
public class SwingUtilities {
	private SwingUtilities() {
		// Utility class
	}

	/**
	 * Loads and initializes a class and its constructors by calling its first
	 * public constructor with dummy values (false, 0, 'x' or null).
	 *
	 * <p>
	 * Prevents exceptions of type
	 * {@code Exception in thread "AWT-EventQueue-0" java.lang.NoClassDefFoundError}
	 * when classes are first used within AWT threads while the jar-in-jar-loader
	 * is used. Exceptions thrown by the constructor itself because of the dummy
	 * values are ignored, other problems are printed to the console.
	 * </p>
	 *
	 * @param clazz class to initialize
	 */
	public static void forceConstructorInit(final Class<?> clazz) {
		if (GraphicsEnvironment.isHeadless() && Component.class.isAssignableFrom(clazz)) {
			// AWT components cannot be created without a display, loading the class is all that is possible
			try {
				Class.forName(clazz.getName(), true, clazz.getClassLoader());
			} catch (final Exception e) {
				System.out.println("forceConstructorInit failed for class: " + clazz.getName());
				e.printStackTrace();
			}
			return;
		}

		// Swing components must be created on the event dispatch thread
		if (Component.class.isAssignableFrom(clazz) && !javax.swing.SwingUtilities.isEventDispatchThread()) {
			try {
				javax.swing.SwingUtilities.invokeAndWait(() -> forceConstructorInitInternal(clazz));
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				System.out.println("forceConstructorInit interrupted for class: " + clazz.getName());
			} catch (final InvocationTargetException e) {
				System.out.println("forceConstructorInit failed for class: " + clazz.getName());
				e.printStackTrace();
			}
		} else {
			forceConstructorInitInternal(clazz);
		}
	}

	private static void forceConstructorInitInternal(final Class<?> clazz) {
		try {
			final Class<?> initClass = Class.forName(clazz.getName(), true, clazz.getClassLoader());
			final Constructor<?>[] constructors = initClass.getConstructors();
			if (constructors.length > 0) {
				final Constructor<?> constructor = constructors[0];
				final Object[] parameters = new Object[constructor.getParameters().length];
				int i = 0;
				for (final Parameter parameter : constructor.getParameters()) {
					if (parameter.getType() == boolean.class) {
						parameters[i] = false;
					} else if (parameter.getType() == byte.class) {
						parameters[i] = (byte) 0;
					} else if (parameter.getType() == short.class) {
						parameters[i] = (short) 0;
					} else if (parameter.getType() == int.class) {
						parameters[i] = 0;
					} else if (parameter.getType() == long.class) {
						parameters[i] = 0L;
					} else if (parameter.getType() == float.class) {
						parameters[i] = 0F;
					} else if (parameter.getType() == double.class) {
						parameters[i] = 0D;
					} else if (parameter.getType() == char.class) {
						parameters[i] = 'x';
					} else {
						parameters[i] = null;
					}
					i++;
				}
				final Object instance = constructor.newInstance(parameters);
				if (instance instanceof Window) {
					// The dummy window (e.g. a dialog that called pack()) must not keep native resources
					// and must not prevent the AWT auto shutdown
					((Window) instance).dispose();
				}
			}
		} catch (@SuppressWarnings("unused") final InvocationTargetException e) {
			// Something went wrong inside the constructor because of the dummy values.
			// This doesn't matter, the classloader loaded the class and constructors anyway
			// do nothing
		} catch (final Exception e) {
			System.out.println("forceConstructorInit failed for class: " + clazz.getName());
			e.printStackTrace();
		}
	}

	/**
	 * Sets the AWT WM_CLASS on Linux, so the Gnome window manager can pin the
	 * application to its applications dock. Does nothing on other systems.
	 *
	 * <p>
	 * Needs the java runtime option
	 * {@code --add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED}, otherwise an error
	 * message is printed to System.err.
	 * </p>
	 *
	 * @param applicationName window manager class name of the application
	 */
	public static void setAwtWmClass(final String applicationName) {
		if (SystemUtilities.isLinuxSystem()) {
			try {
				final Toolkit toolkit = Toolkit.getDefaultToolkit();
				final Field awtAppClassNameField = toolkit.getClass().getDeclaredField("awtAppClassName");
				awtAppClassNameField.setAccessible(true);
				awtAppClassNameField.set(toolkit, applicationName);
			} catch (final Exception e) {
				// A module can only be opened by itself (or via the command line), so there is no programmatic fallback
				System.err.println("Cannot set AWT WM_CLASS: " + e.getMessage());
				System.err.println("Start the application with the java runtime option \"--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED\"");
			}
		}
	}

	/**
	 * Sets the look and feel of the operating system. Errors are printed to the
	 * console and leave the current look and feel unchanged.
	 */
	public static void setSystemLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
