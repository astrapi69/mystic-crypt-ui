package io.github.astrapi69.mystic.crypt.util;

import java.awt.Component;

import javax.swing.JInternalFrame;

import io.github.astrapi69.swing.base.ApplicationPanelFrame;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.panel.desktoppane.JDesktopPanePanel;
import io.github.astrapi69.swing.utils.JInternalFrameExtensions;

public class InternalFrameExtensions
{

	public static <T> void addInternalFrameToMainFrame(final Component component,
		JInternalFrame internalFrame, ApplicationPanelFrame<T> mainFrame)
	{
		JInternalFrameExtensions.addComponentToFrame(internalFrame, component);
		addInternalFrameToMainFrame(internalFrame, mainFrame);
	}

	public static <T> void addInternalFrameToMainFrame(JInternalFrame internalFrame,
		ApplicationPanelFrame<T> mainFrame)
	{
		BasePanel<T> mainComponent = mainFrame.getMainComponent();
		if (mainComponent instanceof JDesktopPanePanel)
		{
			JDesktopPanePanel<T> desktopPanePanel = (JDesktopPanePanel<T>)mainComponent;
			JInternalFrameExtensions.addJInternalFrame(desktopPanePanel.getDesktopPane(),
				internalFrame);
		}
	}
}
