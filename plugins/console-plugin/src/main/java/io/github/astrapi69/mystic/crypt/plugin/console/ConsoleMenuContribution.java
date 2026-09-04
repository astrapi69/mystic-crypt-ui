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

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JDesktopPane;
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
			// the panel just redirected System.out/err to itself; java.util.logging keeps its own
			// stale reference to the original System.err otherwise, so nothing logged through it
			// would ever reach the panel (#133)
			ConsoleLogRedirectSupport.redirectRootLoggingToCurrentSystemErr();
			// the console lives inside the application's desktop, so that is what it is measured
			// against; the screen is the wrong yardstick and left it a stamp in the corner of a
			// window that did not fill the screen
			JDesktopPane desktopPane = instance.getDesktopPanePanel().getDesktopPane();
			int divisor = ConsoleSettingsContribution.heightDivisor();
			boolean resizable = ConsoleSettingsContribution.resizable();
			internalFrame.setResizable(resizable);
			if (!resizable)
			{
				// docked rather than floating: follow the desktop when the window is resized,
				// instead of sitting at a size that fitted the window it was opened in
				desktopPane.addComponentListener(new ComponentAdapter()
				{
					@Override
					public void componentResized(ComponentEvent event)
					{
						ConsoleDock.dock(internalFrame, desktopPane, divisor);
					}
				});
			}
			internalFrame.putClientProperty("dragMode", resizable ? "default" : "fixed");
			// add the component directly, without JInternalFrameExtensions.addComponentToFrame's
			// pack() - pack() resizes the frame to the content's tiny preferred size, silently
			// overwriting whatever bounds are set before it runs. dock() below has to be the very
			// last thing that touches this frame's bounds, so nothing after it can clobber them
			// again (#133)
			internalFrame.add(component, BorderLayout.CENTER);
			JInternalFrameExtensions.addInternalFrameToMainFrame(internalFrame, instance);
			ConsoleDock.dock(internalFrame, desktopPane, divisor);
		});
		return List.of(console);
	}

	@Override
	public String getMenuName()
	{
		return "Console";
	}

}
