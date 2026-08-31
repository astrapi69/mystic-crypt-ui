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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Headless tests of {@link MenuLayoutSupport}: exporting a menu bar to xml, rebuilding a menu bar
 * from that xml with the actions of the original one, and persisting a layout.
 */
class MenuLayoutSupportTest
{

	private static JMenuBar newMenuBar(AtomicInteger exitCalls)
	{
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		fileMenu.setName("global.menu.file");
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.setName("global.menu.file.exit");
		exitItem.addActionListener(event -> exitCalls.incrementAndGet());
		fileMenu.add(exitItem);

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setName("global.menu.help");
		// a contributed item without a name, like the ones the plugins add
		JMenuItem infoItem = new JMenuItem("Info");
		infoItem.addActionListener(event -> {
		});
		helpMenu.add(infoItem);

		menuBar.add(fileMenu);
		menuBar.add(helpMenu);
		return menuBar;
	}

	@Test
	void exportsTheMenuBarAsXml()
	{
		String xml = MenuLayoutSupport.exportXml(newMenuBar(new AtomicInteger()));

		assertTrue(xml.contains("global.menu.file"), "the xml must carry the menu ids: " + xml);
		assertTrue(xml.contains("Exit"), "the xml must carry the item texts: " + xml);
		assertTrue(MenuLayoutSupport.validate(xml).isEmpty(), "the exported xml must be valid");
	}

	@Test
	void harvestsTheActionsOfNamedAndUnnamedItems()
	{
		Map<String, java.awt.event.ActionListener> actions = MenuLayoutSupport
			.harvestActions(newMenuBar(new AtomicInteger()));

		assertNotNull(actions.get("global.menu.file.exit"),
			"a named item is harvested under its name");
		assertNotNull(actions.get("text:Info"),
			"an unnamed item is harvested under its text based id, was: " + actions.keySet());
	}

	@Test
	void rebuildsAMenuBarThatKeepsTheOriginalActions()
	{
		AtomicInteger exitCalls = new AtomicInteger();
		JMenuBar original = newMenuBar(exitCalls);

		JMenuBar rebuilt = MenuLayoutSupport.build(MenuLayoutSupport.exportXml(original), original);

		assertEquals(original.getMenuCount(), rebuilt.getMenuCount(),
			"the rebuilt menu bar must have the same menus");
		JMenuItem exitItem = (JMenuItem)rebuilt.getMenu(0).getMenuComponent(0);
		assertEquals("Exit", exitItem.getText());

		exitItem.doClick();
		assertEquals(1, exitCalls.get(),
			"the rebuilt item must still trigger the action of the original item");
	}

	@Test
	void unknownActionIdsProduceADisabledItemInsteadOfFailing()
	{
		JMenuBar original = newMenuBar(new AtomicInteger());
		String xml = """
			<menubar id="global.menu.bar">
			  <menu id="global.menu.file" text="File">
			    <item id="does.not.exist" text="Nothing" action="does.not.exist"/>
			  </menu>
			</menubar>
			""";

		JMenuBar rebuilt = MenuLayoutSupport.build(xml, original);

		JMenuItem item = (JMenuItem)rebuilt.getMenu(0).getMenuComponent(0);
		assertEquals("Nothing", item.getText());
		assertFalse(item.isEnabled(), "an item with an unknown action id must be disabled");
	}

	@Test
	void savesAndAppliesAPersistedLayout(@TempDir File configurationDirectory) throws Exception
	{
		JMenuBar original = newMenuBar(new AtomicInteger());

		// without a layout file the menu bar is returned unchanged
		assertEquals(original,
			MenuLayoutSupport.applyPersistedLayout(original, configurationDirectory));

		Path file = MenuLayoutSupport.save(MenuLayoutSupport.exportXml(original),
			configurationDirectory);
		assertTrue(Files.exists(file));
		assertEquals(MenuLayoutSupport.LAYOUT_FILE_NAME, file.getFileName().toString());

		JMenuBar applied = MenuLayoutSupport.applyPersistedLayout(original, configurationDirectory);
		assertTrue(applied != original, "with a layout file a new menu bar is built");
		assertEquals(original.getMenuCount(), applied.getMenuCount());
	}

	/**
	 * Menu xml that is not a usable layout, each broken in its own way: no xml at all, a truncated
	 * document, a root element the reader does not know, and an empty document.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "this is not menu xml", "<menubar id=\"x\">", "<nonsense/>", "" })
	void validateReportsProblemsForBrokenXml(String xml)
	{
		assertFalse(MenuLayoutSupport.validate(xml).isEmpty(),
			"broken menu xml must be reported as invalid, was accepted: '" + xml + "'");
	}

	@Test
	void validateAcceptsAnExportedLayout()
	{
		String xml = MenuLayoutSupport.exportXml(newMenuBar(new AtomicInteger()));

		assertTrue(MenuLayoutSupport.validate(xml).isEmpty(),
			"the xml this class exports must validate");
	}

	@Test
	void anEmptyMenuBarHarvestsNoActions()
	{
		assertTrue(MenuLayoutSupport.harvestActions(new JMenuBar()).isEmpty(),
			"a menu bar without menus has no actions to harvest");
	}

	@Test
	void anItemBackedByAnActionIsHarvestedThroughThatAction()
	{
		AtomicInteger calls = new AtomicInteger();
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		menu.setName("global.menu.file");
		// the host builds its menu items from Actions rather than plain listeners, so this is the
		// shape that actually occurs in the application
		javax.swing.Action action = new javax.swing.AbstractAction("Save")
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event)
			{
				calls.incrementAndGet();
			}
		};
		JMenuItem withAction = new JMenuItem(action);
		withAction.setName("global.menu.file.save");
		menu.add(withAction);
		menuBar.add(menu);

		java.awt.event.ActionListener harvested = MenuLayoutSupport.harvestActions(menuBar)
			.get("global.menu.file.save");

		assertNotNull(harvested, "an item backed by an Action must be harvested");
		harvested.actionPerformed(new java.awt.event.ActionEvent(withAction, 0, "test"));
		assertEquals(1, calls.get(), "the harvested listener must run the item's Action");
	}

	@Test
	void anItemWithoutAnActionIsNotHarvested()
	{
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		menu.setName("global.menu.file");
		// a plain label-like item: no Action, no ActionListener
		JMenuItem withoutAction = new JMenuItem("Nothing happens");
		withoutAction.setName("global.menu.file.nothing");
		menu.add(withoutAction);
		menuBar.add(menu);

		Map<String, java.awt.event.ActionListener> actions = MenuLayoutSupport
			.harvestActions(menuBar);

		assertFalse(actions.containsKey("global.menu.file.nothing"),
			"an item that carries no action must not end up in the registry, was: "
				+ actions.keySet());
	}

	@Test
	void abrokenLayoutFileKeepsTheStandardMenu(@TempDir File configurationDirectory)
		throws Exception
	{
		JMenuBar original = newMenuBar(new AtomicInteger());
		Files.writeString(MenuLayoutSupport.layoutFile(configurationDirectory),
			"this is not menu xml", StandardCharsets.UTF_8);

		assertEquals(original,
			MenuLayoutSupport.applyPersistedLayout(original, configurationDirectory),
			"a broken layout must never break the application menu");
	}

	@Test
	@DisplayName("a persisted layout survives the plugins menu being rebuilt")
	void aPersistedLayoutSurvivesThePluginsMenuBeingRebuilt(@TempDir File configurationDirectory)
		throws Exception
	{
		// the arrangement a user saved: Certificate before Checksum, the opposite of what a fresh,
		// alphabetically sorted plugins menu would show
		JMenuBar arranged = new JMenuBar();
		JMenu pluginsMenu = new JMenu("Plugins");
		pluginsMenu.setName("global.menu.plugins");
		pluginsMenu.add(pluginItem("Certificate"));
		pluginsMenu.add(pluginItem("Checksum"));
		arranged.add(pluginsMenu);
		MenuLayoutSupport.save(MenuLayoutSupport.exportXml(arranged), configurationDirectory);

		// a plugin is enabled: the host rebuilds the plugins menu from scratch, alphabetically -
		// "Checksum" now comes before "Certificate"
		JMenuBar rebuilt = new JMenuBar();
		JMenu freshPluginsMenu = new JMenu("Plugins");
		freshPluginsMenu.setName("global.menu.plugins");
		freshPluginsMenu.add(pluginItem("Checksum"));
		freshPluginsMenu.add(pluginItem("Certificate"));
		rebuilt.add(freshPluginsMenu);

		JMenuBar afterApplyingTheLayoutAgain = MenuLayoutSupport.applyPersistedLayout(rebuilt,
			configurationDirectory);

		JMenu resultingPluginsMenu = (JMenu)afterApplyingTheLayoutAgain.getMenu(0);
		assertEquals("Certificate", resultingPluginsMenu.getItem(0).getText(),
			"the saved arrangement was lost when the plugins menu was rebuilt");
		assertEquals("Checksum", resultingPluginsMenu.getItem(1).getText());
	}

	private static JMenuItem pluginItem(final String text)
	{
		JMenuItem item = new JMenuItem(text);
		item.setName("text:" + text);
		item.addActionListener(event -> {
		});
		return item;
	}

}
