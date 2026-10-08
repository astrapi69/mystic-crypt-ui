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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.util.JInternalFrameExtensions;

/**
 * Puts this plugin's tools under a "Lethenon" submenu of the host's "Plugins" menu.
 * <p>
 * {@link #isUsableWithoutAVault()} is deliberately NOT declared, although replaying a chain file
 * touches no vault. Declaring it opens EVERY item of a contribution while locked, and this one also
 * opens a wallet and spends from it. The read-only tools are to opt in through a contribution of
 * their own, together with the harness extension for approved file choosers they need (#301,
 * #450). Until then the verifier is reachable after a sign-in, like every other tool.
 */
@Extension
public class LethenonMenuContribution implements PluginMenuContribution
{

	@Override
	public List<JMenuItem> getMenuItems()
	{
		return List.of(item("lethenon.menu.item.verify", "Verify a Chain", LethenonChainPanel::new),
			item("lethenon.menu.item.show.chain", "Show a Chain", LethenonChainViewPanel::new),
			item("lethenon.menu.item.show.balance", "Show a Balance", LethenonBalancePanel::new),
			item("lethenon.menu.item.send", "Send LETH", LethenonSendPanel::new),
			item("lethenon.menu.item.mine", "Mine a Pun", LethenonMinePanel::new),
			item("lethenon.menu.item.sweep", "Sweep One-Time Payments", LethenonSweepPanel::new),
			item("lethenon.menu.item.sync", "Synchronise with a Node", LethenonSyncPanel::new),
			item("lethenon.menu.item.node", "Run a Node", LethenonNodePanel::new));
	}

	/**
	 * A menu item that opens a new tool window, titled like the item, on every click
	 *
	 * @param key
	 *            the key of the item's text in the plugin's messages
	 * @param defaultTitle
	 *            the text when the key is missing
	 * @param panel
	 *            makes the window's content, anew for every window
	 * @return the menu item
	 */
	private JMenuItem item(final String key, final String defaultTitle,
		final Supplier<Component> panel)
	{
		String title = LethenonMessages.getString(key, defaultTitle);
		JMenuItem item = new JMenuItem(title);
		item.addActionListener(event -> openInternalFrame(title, panel.get()));
		return item;
	}

	@Override
	public String getMenuName()
	{
		return LethenonMessages.getString("lethenon.menu.name", "Lethenon");
	}

	private void openInternalFrame(String title, Component panel)
	{
		MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
		if (!FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
		{
			instance.switchToDesktopPane();
		}
		JInternalFrame internalFrame = JComponentFactory.newInternalFrame(title, true, true, true,
			true);
		JInternalFrameExtensions.addInternalFrameToMainFrame(panel, internalFrame, instance);
	}
}
