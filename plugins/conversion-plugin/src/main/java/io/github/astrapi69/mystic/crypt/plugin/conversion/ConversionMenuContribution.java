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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

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
 * Contributes the key/certificate file converter to the host's "Plugins" menu. Like the obfuscation
 * and checksum plugins, the converter used to be wired straight into the application menu and now
 * ships as an internal pf4j plugin instead. The tool converts a DER-encoded key or certificate file
 * into the PEM format
 */
@Extension
public class ConversionMenuContribution implements PluginMenuContribution
{

	private static final String FRAME_TITLE = "Convert *.der-file to *.pem-file";

	@Override
	public List<JMenuItem> getMenuItems()
	{
		JMenuItem convert = new JMenuItem("Convert DER to PEM");
		convert.addActionListener(event -> {
			MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
			if (!FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
			{
				instance.switchToDesktopPane();
			}
			JInternalFrame internalFrame = JComponentFactory.newInternalFrame(FRAME_TITLE, true,
				true, true, true);
			JInternalFrameExtensions.addInternalFrameToMainFrame(new FileConversionPanel(),
				internalFrame, instance);
		});
		return List.of(convert);
	}

}
