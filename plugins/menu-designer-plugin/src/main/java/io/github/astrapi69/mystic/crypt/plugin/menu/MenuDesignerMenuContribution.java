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
package io.github.astrapi69.mystic.crypt.plugin.menu;

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
 * Contributes the "Menu Designer" tool to the host's "Plugins" menu, for development only.
 * <p>
 * This tool edits the application's own menu bar and writes the layout the application reads on
 * start. That is work on the application, not work a user of a password manager has any reason to
 * do, and a menu rearranged by accident is a support case rather than a feature. The installer
 * therefore does not ship this plugin at all.
 * <p>
 * A machine that received it from an earlier release still has the zip, so the entry is offered
 * only when {@value #DEVELOPER_PROPERTY} says so. Without it the plugin loads, contributes nothing
 * and stays out of the way.
 */
@Extension
public class MenuDesignerMenuContribution implements PluginMenuContribution
{

	/**
	 * The system property that makes this development tool appear: {@code -Dmystic.crypt.ui.menu.designer=true}
	 */
	public static final String DEVELOPER_PROPERTY = "mystic.crypt.ui.menu.designer";

	/**
	 * Whether this tool is offered at all
	 *
	 * @return true when the developer property asks for it
	 */
	public static boolean isOffered()
	{
		return Boolean.parseBoolean(System.getProperty(DEVELOPER_PROPERTY));
	}

	@Override
	public List<JMenuItem> getMenuItems()
	{
		if (!isOffered())
		{
			return List.of();
		}
		JMenuItem menuDesigner = new JMenuItem("Menu Designer");
		menuDesigner
			.addActionListener(event -> openInternalFrame("Menu Designer", new MenuDesignerPanel()));
		return List.of(menuDesigner);
	}

	@Override
	public String getMenuName()
	{
		return "Menu Designer";
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
