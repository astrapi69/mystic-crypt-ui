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
 * touches no vault: the extension point says the second plugin that opts in brings the locked-state
 * harness with it (#301), and building that harness is not this change. Until then the verifier is
 * reachable after a sign-in, like every other tool.
 */
@Extension
public class LethenonMenuContribution implements PluginMenuContribution
{

	@Override
	public List<JMenuItem> getMenuItems()
	{
		String title = LethenonMessages.getString("lethenon.menu.item.verify", "Verify a Chain");
		JMenuItem verifyAChain = new JMenuItem(title);
		verifyAChain.addActionListener(event -> openInternalFrame(title, new LethenonChainPanel()));
		String chainTitle = LethenonMessages.getString("lethenon.menu.item.show.chain",
			"Show a Chain");
		JMenuItem showAChain = new JMenuItem(chainTitle);
		showAChain.addActionListener(
			event -> openInternalFrame(chainTitle, new LethenonChainViewPanel()));
		String balanceTitle = LethenonMessages.getString("lethenon.menu.item.show.balance",
			"Show a Balance");
		JMenuItem showABalance = new JMenuItem(balanceTitle);
		showABalance.addActionListener(
			event -> openInternalFrame(balanceTitle, new LethenonBalancePanel()));
		String sendTitle = LethenonMessages.getString("lethenon.menu.item.send", "Send LETH");
		JMenuItem send = new JMenuItem(sendTitle);
		send.addActionListener(event -> openInternalFrame(sendTitle, new LethenonSendPanel()));
		String mineTitle = LethenonMessages.getString("lethenon.menu.item.mine", "Mine a Pun");
		JMenuItem mine = new JMenuItem(mineTitle);
		mine.addActionListener(event -> openInternalFrame(mineTitle, new LethenonMinePanel()));
		return List.of(verifyAChain, showAChain, showABalance, send, mine);
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
