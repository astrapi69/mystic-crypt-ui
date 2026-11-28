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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.ui.icon.PluginIcon;
import io.nayuki.qrcodegen.QrCode;
import io.nayuki.qrcodegen.QrSegment;

/**
 * The code the action draws (#1120): exactly the code of the payload, with the network's password
 * overwritten once it is drawn
 */
class WifiQrEntryActionTest
{

	@Test
	@DisplayName("the drawn code is the code of the payload, and the password is overwritten afterwards")
	void theDrawnCode_isThePayloads_andThePasswordIsOverwritten()
	{
		String made = UUID.randomUUID().toString();
		char[] password = made.toCharArray();
		WifiNetwork network = new WifiNetwork("home", password, "WPA", false);
		QrCode expected = QrCode.encodeSegments(
			List.of(QrSegment.makeBytes(
				("WIFI:T:WPA;S:home;P:" + made + ";;").getBytes(StandardCharsets.UTF_8))),
			QrCode.Ecc.MEDIUM);

		ImageIcon drawn = WifiQrEntryAction.imageOf(network);

		assertArrayEquals(new char[password.length], password, "the password is overwritten");
		BufferedImage image = (BufferedImage)drawn.getImage();
		assertEquals((expected.size + 2 * QrImage.BORDER) * QrImage.SCALE, image.getWidth());
		for (int y = 0; y < expected.size; y++)
		{
			for (int x = 0; x < expected.size; x++)
			{
				int pixel = image.getRGB((x + QrImage.BORDER) * QrImage.SCALE + 1,
					(y + QrImage.BORDER) * QrImage.SCALE + 1);
				assertEquals(expected.getModule(x, y) ? 0xFF000000 : 0xFFFFFFFF, pixel,
					"module " + x + "," + y);
			}
		}
	}

	@Test
	@DisplayName("the action's name comes from the plugin's bundle, in English or German")
	void theActionsName()
	{
		String name = new WifiQrEntryAction().getActionName();
		assertTrue(name.equals("Show Wi-Fi QR code...") || name.equals("WLAN-QR-Code zeigen..."),
			name);
	}

	@Test
	@DisplayName("the plugin's icon loads under its plugin id, the way the host sets it on the action (#1192)")
	void theIcon_loadsUnderThePluginId()
	{
		Icon icon = PluginIcon.of(WifiQrEntryAction.class, "wifi-qr-plugin");

		assertNotNull(icon);
		assertEquals(16, icon.getIconWidth());
	}
}
