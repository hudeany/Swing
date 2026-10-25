package de.soderer.swing;

import java.awt.Component;

import javax.swing.Popup;
import javax.swing.PopupFactory;

/**
 * Popup factory for headless tests: records the popup content and returns
 * popups that show nothing, because real popups need a window.
 */
public class RecordingPopupFactory extends PopupFactory {
	private final PopupFactory previousFactory;

	public Component lastContents;
	public int shownCount = 0;
	public int hiddenCount = 0;

	private RecordingPopupFactory(final PopupFactory previousFactory) {
		this.previousFactory = previousFactory;
	}

	public static RecordingPopupFactory install() {
		final RecordingPopupFactory factory = new RecordingPopupFactory(PopupFactory.getSharedInstance());
		PopupFactory.setSharedInstance(factory);
		return factory;
	}

	public void uninstall() {
		PopupFactory.setSharedInstance(previousFactory);
	}

	@Override
	public Popup getPopup(final Component owner, final Component contents, final int x, final int y) {
		lastContents = contents;
		return new Popup() {
			@Override
			public void show() {
				shownCount++;
			}

			@Override
			public void hide() {
				hiddenCount++;
			}
		};
	}
}
