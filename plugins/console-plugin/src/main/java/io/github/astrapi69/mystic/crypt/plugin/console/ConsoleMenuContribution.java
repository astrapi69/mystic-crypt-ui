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
import java.io.PrintStream;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.pf4j.Extension;

import io.github.astrapi69.awt.screen.ScreenSizeExtensions;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.enumeration.FrameMode;
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
			JPanel component = newConsolePanel(internalFrame);
			// System.out/err now point into the console; java.util.logging keeps its own stale
			// reference to the original System.err otherwise, so nothing logged through it would
			// ever reach the panel (#133)
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

	/**
	 * The console's text area, capturing standard output and error through a buffer that holds a
	 * limited number of lines and is erased when the window closes (#375). The window closes with
	 * the workspace when it is locked, which is why the erasing hangs off the frame rather than off
	 * the menu item that opened it
	 *
	 * @param internalFrame
	 *            the window the console is shown in
	 * @return the panel to put into that window
	 */
	private static JPanel newConsolePanel(final JInternalFrame internalFrame)
	{
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		ConsoleBuffer buffer = new ConsoleBuffer(textArea,
			ConsoleSettingsContribution.maxLines());
		PrintStream systemOutBeforeTheConsole = System.out;
		PrintStream systemErrBeforeTheConsole = System.err;
		PrintStream toTheConsole = new PrintStream(buffer, true);
		System.setOut(toTheConsole);
		System.setErr(toTheConsole);
		ConsoleSession.opened(internalFrame, buffer, systemOutBeforeTheConsole,
			systemErrBeforeTheConsole);
		internalFrame.addInternalFrameListener(new InternalFrameAdapter()
		{
			@Override
			public void internalFrameClosed(InternalFrameEvent event)
			{
				ConsoleSession.ended();
			}
		});
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
		return panel;
	}

	@Override
	public String getMenuName()
	{
		return "Console";
	}

}
