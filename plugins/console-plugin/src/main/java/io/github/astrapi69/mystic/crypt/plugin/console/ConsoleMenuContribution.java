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
package io.github.astrapi69.mystic.crypt.plugin.console;

import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;

import org.pf4j.Extension;

import io.github.astrapi69.awt.screen.ScreenSizeExtensions;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.panel.output.ConsolePanel;
import io.github.astrapi69.swing.util.JInternalFrameExtensions;

/**
 * Contributes the output console to the host's "Plugins" menu. Like the other internal plugins, the
 * console used to be wired straight into the application menu and now ships as a pf4j plugin. The
 * console captures the application's standard output/error into a docked, fixed frame at the bottom
 * of the desktop pane
 */
@Extension
public class ConsoleMenuContribution implements PluginMenuContribution
{

	@Override
	public List<JMenuItem> getMenuItems()
	{
		JMenuItem console = new JMenuItem("Console");
		console.addActionListener(event -> {
			MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
			if (!FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
			{
				instance.switchToDesktopPane();
			}
			JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Console", true, true,
				true, true);
			ConsolePanel component = new ConsolePanel();
			int screenHeight = ScreenSizeExtensions.getScreenHeight(instance);
			int screenWidth = ScreenSizeExtensions.getScreenWidth(instance);
			// dock it into the bottom quarter of the desktop, fixed like the old built-in item
			internalFrame.setSize(screenWidth, screenHeight / 4);
			internalFrame.setLocation(0, (screenHeight / 4) * 3);
			internalFrame.setResizable(false);
			internalFrame.putClientProperty("dragMode", "fixed");
			JInternalFrameExtensions.addInternalFrameToMainFrame(component, internalFrame, instance);
		});
		return List.of(console);
	}

	@Override
	public String getMenuName()
	{
		return "Console";
	}

}
