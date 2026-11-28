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
package io.github.astrapi69.mystic.crypt.plugin.wifiqr;

import java.awt.Component;
import java.awt.Window;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.plugin.api.EntryActionContribution;
import net.miginfocom.swing.MigLayout;

/**
 * "Show Wi-Fi QR code..." in the context menu of the entry table (#1120): the network of the picked
 * entry as a QR code in a window of its own, for as long as the window is open. The password of the
 * copy the host handed over, and the payload built from it, are overwritten as soon as the code is
 * drawn; nothing is written to disk.
 */
@Extension
public class WifiQrEntryAction implements EntryActionContribution
{

	@Override
	public String getActionName()
	{
		return WifiQrMessages.getString("wifiqr.action.show", "Show Wi-Fi QR code...");
	}

	@Override
	public void act(final MysticCryptEntryModelBean entry, final Component parent)
	{
		WifiNetwork network;
		try
		{
			network = WifiNetwork.of(entry);
		}
		catch (final IllegalArgumentException refused)
		{
			JOptionPane.showMessageDialog(parent, refused.getMessage(), title(),
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		finally
		{
			wipe(entry);
		}
		window(parent, network.ssid(), imageOf(network)).setVisible(true);
	}

	/**
	 * The image of a network's code, with the network's password and the payload overwritten
	 * afterwards
	 *
	 * @param network
	 *            the network
	 * @return the image
	 */
	static ImageIcon imageOf(final WifiNetwork network)
	{
		char[] payload = WifiPayload.of(network);
		try
		{
			return new ImageIcon(QrImage.of(QrImage.codeOf(payload)));
		}
		finally
		{
			Arrays.fill(payload, '\0');
			network.wipe();
		}
	}

	/**
	 * The window that shows the code
	 *
	 * @param parent
	 *            where the action was chosen
	 * @param ssid
	 *            the network's name, shown above the code
	 * @param code
	 *            the code
	 * @return the window, not yet shown
	 */
	static JDialog window(final Component parent, final String ssid, final ImageIcon code)
	{
		Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
		JDialog dialog = new JDialog(owner, title(), JDialog.ModalityType.APPLICATION_MODAL);
		dialog.setName("dlgWifiQr");
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		JPanel content = new JPanel(new MigLayout("wrap 1, insets 12", "[center]"));
		JLabel network = new JLabel(
			WifiQrMessages.format("wifiqr.dialog.network", "Network: {0}", ssid));
		network.setName("lblWifiNetwork");
		JLabel image = new JLabel(code);
		image.setName("lblWifiQr");
		image.setToolTipText(WifiQrMessages.getString("wifiqr.tooltip.code",
			"the network and its password as a QR code, for a phone to join it"));
		image.getAccessibleContext().setAccessibleName(title());
		JLabel hint = new JLabel(
			"<html><body style='width:280px'>" + WifiQrMessages.getString("wifiqr.dialog.hint",
				"Scan it with the camera of a phone to join the network. Nothing of it is kept "
					+ "once this window is closed.")
				+ "</body></html>");
		content.add(network);
		content.add(image);
		content.add(hint);
		dialog.setContentPane(content);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		return dialog;
	}

	private static String title()
	{
		return WifiQrMessages.getString("wifiqr.dialog.title", "Wi-Fi QR code");
	}

	private static void wipe(final MysticCryptEntryModelBean entry)
	{
		if (entry.getPassword() != null)
		{
			Arrays.fill(entry.getPassword(), '\0');
		}
		if (entry.getRepeat() != null)
		{
			Arrays.fill(entry.getRepeat(), '\0');
		}
	}
}
