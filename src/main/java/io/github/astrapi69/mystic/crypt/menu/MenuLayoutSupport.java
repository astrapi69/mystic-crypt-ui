/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.menu;

import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.MenuElement;

import io.github.astrapi69.swing.menu.build.ActionRegistry;
import io.github.astrapi69.swing.menu.build.MenuBuilder;
import io.github.astrapi69.swing.menu.build.MissingActionPolicy;
import io.github.astrapi69.swing.menu.model.MenuInfo;
import io.github.astrapi69.swing.menu.model.transform.MenuInfoExporter;
import io.github.astrapi69.swing.menu.xml.MenuXmlReader;
import io.github.astrapi69.swing.menu.xml.MenuXmlWriter;
import lombok.extern.java.Log;

/**
 * Reads and writes the application's menu layout as xml, and rebuilds a {@link JMenuBar} from such
 * a layout while keeping the behaviour of the menu that is currently in place.
 * <p>
 * The trick that makes a user-editable layout possible without the xml knowing anything about
 * action implementations: on export every menu component gets an explicit action id (see
 * {@link #ACTION_ID}), and on import the very same ids are looked up in an {@link ActionRegistry}
 * that was harvested from the live menu bar. A layout can therefore reorder, rename, regroup or
 * drop items, and each surviving item keeps exactly the action it had. Items whose action id is
 * unknown - for instance because the contributing plugin is currently disabled - are built but
 * disabled rather than failing the whole menu ({@link MissingActionPolicy#DISABLE}).
 */
@Log
public final class MenuLayoutSupport
{

	/** The name of the layout file inside the application's configuration directory */
	public static final String LAYOUT_FILE_NAME = "menubar.xml";

	/**
	 * The action id of a menu component: its component name when it has one - the host's menu items
	 * are named after their {@code MenuId} - otherwise a stable id derived from its text, which is
	 * what plugin contributed items fall back to
	 */
	public static final Function<AbstractButton, String> ACTION_ID = button -> button
		.getName() != null ? button.getName() : "text:" + button.getText();

	private MenuLayoutSupport()
	{
	}

	/**
	 * Exports the given menu bar as menu xml
	 *
	 * @param menuBar
	 *            the menu bar to export
	 * @return the menu bar as xml
	 */
	public static String exportXml(final JMenuBar menuBar)
	{
		return MenuXmlWriter.toXml(toMenuInfo(menuBar));
	}

	/**
	 * Exports the given menu bar to a {@link MenuInfo} tree with explicit action ids
	 *
	 * @param menuBar
	 *            the menu bar to export
	 * @return the exported tree
	 */
	public static MenuInfo toMenuInfo(final JMenuBar menuBar)
	{
		return MenuInfoExporter.withActionIds(ACTION_ID).export(menuBar);
	}

	/**
	 * Validates the given menu xml
	 *
	 * @param xml
	 *            the menu xml
	 * @return the list of validation errors, empty when the xml is valid
	 */
	public static List<String> validate(final String xml)
	{
		return MenuXmlReader.validate(xml);
	}

	/**
	 * Collects the action of every menu component of the given menu bar, keyed by the same action
	 * id that {@link #toMenuInfo(JMenuBar)} writes into the xml
	 *
	 * @param menuBar
	 *            the menu bar to harvest
	 * @return the actions by action id
	 */
	public static Map<String, ActionListener> harvestActions(final JMenuBar menuBar)
	{
		Map<String, ActionListener> actions = new LinkedHashMap<>();
		for (int index = 0; index < menuBar.getMenuCount(); index++)
		{
			JMenu menu = menuBar.getMenu(index);
			if (menu != null)
			{
				harvest(menu, actions);
			}
		}
		return actions;
	}

	private static void harvest(final MenuElement element,
		final Map<String, ActionListener> actions)
	{
		if (element instanceof AbstractButton)
		{
			AbstractButton button = (AbstractButton)element;
			ActionListener listener = actionOf(button);
			if (listener != null)
			{
				actions.putIfAbsent(ACTION_ID.apply(button), listener);
			}
		}
		for (MenuElement child : element.getSubElements())
		{
			harvest(child, actions);
		}
	}

	private static ActionListener actionOf(final AbstractButton button)
	{
		Action action = button.getAction();
		if (action != null)
		{
			return action;
		}
		ActionListener[] listeners = button.getActionListeners();
		if (listeners.length == 0)
		{
			return null;
		}
		ActionListener[] snapshot = listeners.clone();
		return event -> {
			for (ActionListener listener : snapshot)
			{
				listener.actionPerformed(event);
			}
		};
	}

	/**
	 * Builds a menu bar from the given menu xml, wiring every item to the action it has in the
	 * given currently used menu bar
	 *
	 * @param xml
	 *            the menu xml describing the wanted layout
	 * @param currentMenuBar
	 *            the menu bar that is in place, used as the source of the actions
	 * @return the newly built menu bar
	 */
	public static JMenuBar build(final String xml, final JMenuBar currentMenuBar)
	{
		MenuInfo menuInfo = MenuXmlReader.fromXml(xml);
		return new MenuBuilder(ActionRegistry.of(harvestActions(currentMenuBar)))
			.withMissingActionPolicy(MissingActionPolicy.DISABLE).buildMenuBar(menuInfo);
	}

	/**
	 * Gets the layout file inside the given configuration directory
	 *
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return the path of the layout file
	 */
	public static Path layoutFile(final File configurationDirectory)
	{
		return configurationDirectory.toPath().resolve(LAYOUT_FILE_NAME);
	}

	/**
	 * Saves the given menu xml as the persisted layout
	 *
	 * @param xml
	 *            the menu xml
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return the file the layout was written to
	 * @throws java.io.IOException
	 *             if writing fails
	 */
	public static Path save(final String xml, final File configurationDirectory)
		throws java.io.IOException
	{
		Path file = layoutFile(configurationDirectory);
		Files.createDirectories(file.getParent());
		Files.writeString(file, xml, StandardCharsets.UTF_8);
		return file;
	}

	/**
	 * Applies a persisted menu layout, when there is one, to the given menu bar and returns the
	 * menu bar to use. A missing layout file, or one that cannot be read or built, never breaks the
	 * application: the current menu bar is returned unchanged and the problem is logged
	 *
	 * @param currentMenuBar
	 *            the menu bar that was built programmatically
	 * @param configurationDirectory
	 *            the application's configuration directory
	 * @return the menu bar built from the layout, or the given one when there is no usable layout
	 */
	public static JMenuBar applyPersistedLayout(final JMenuBar currentMenuBar,
		final File configurationDirectory)
	{
		Path file = layoutFile(configurationDirectory);
		if (!Files.exists(file))
		{
			return currentMenuBar;
		}
		try
		{
			return build(Files.readString(file, StandardCharsets.UTF_8), currentMenuBar);
		}
		catch (Exception exception)
		{
			log.log(Level.WARNING,
				"The menu layout '" + file + "' could not be applied, keeping the standard menu",
				exception);
			return currentMenuBar;
		}
	}
}
