package com.example.helloplugin;

import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.util.JInternalFrameExtensions;

/**
 * Demonstrates two ways a plugin can contribute to the "Plugins" menu: a trivial action, and the
 * "open a real internal frame panel" pattern every built-in feature of the host app uses (see
 * {@code io.github.astrapi69.mystic.crypt.action.NewChecksumFrameAction} in the host app for the
 * pattern this is modeled on).
 */
@Extension
public class HelloMenuContribution implements PluginMenuContribution
{

	@Override
	public List<JMenuItem> getMenuItems()
	{
		JMenuItem hello = new JMenuItem("Hello from Plugin");
		hello.addActionListener(
			event -> JOptionPane.showMessageDialog(null, "Hello from a pf4j plugin!"));

		JMenuItem openPanel = new JMenuItem("Open Hello Panel");
		openPanel.addActionListener(event -> {
			MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
			if (!FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
			{
				instance.switchToDesktopPane();
			}
			JInternalFrame internalFrame = JComponentFactory.newInternalFrame("Hello Plugin Panel",
				true, true, true, true);
			JInternalFrameExtensions.addInternalFrameToMainFrame(
				new JLabel("Hello from a real plugin panel!"), internalFrame, instance);
		});

		return List.of(hello, openPanel);
	}

}
