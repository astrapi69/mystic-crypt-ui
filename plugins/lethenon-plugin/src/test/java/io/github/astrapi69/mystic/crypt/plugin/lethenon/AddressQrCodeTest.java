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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.lethenon.Wallet;
import io.nayuki.qrcodegen.QrCode;

/**
 * The overview's address as a QR code (#874): the image shows exactly the modules of the code of
 * the address text, inside a white quiet zone
 */
class AddressQrCodeTest
{

	private static final int BLACK = Color.BLACK.getRGB();

	private static final int WHITE = Color.WHITE.getRGB();

	private final String address = Wallet.create().address().toText();

	@Test
	@DisplayName("every module of the address's code is drawn where it belongs, dark where the code is dark")
	void theImage_showsTheModulesOfTheAddressesCode()
	{
		QrCode code = AddressQrCode.codeOf(address).orElseThrow();
		BufferedImage image = AddressQrCode.imageOf(address).orElseThrow();

		int side = (code.size + 2 * AddressQrCode.BORDER) * AddressQrCode.SCALE;
		assertEquals(side, image.getWidth());
		assertEquals(side, image.getHeight());
		for (int y = 0; y < code.size; y++)
		{
			for (int x = 0; x < code.size; x++)
			{
				int pixel = image.getRGB((x + AddressQrCode.BORDER) * AddressQrCode.SCALE + 1,
					(y + AddressQrCode.BORDER) * AddressQrCode.SCALE + 1);
				assertEquals(code.getModule(x, y) ? BLACK : WHITE, pixel, "module " + x + "," + y);
			}
		}
	}

	@Test
	@DisplayName("the quiet zone around the code is white on every side")
	void theQuietZone_isWhite()
	{
		BufferedImage image = AddressQrCode.imageOf(address).orElseThrow();
		int zone = AddressQrCode.BORDER * AddressQrCode.SCALE;
		int last = image.getWidth() - 1;
		for (int along = 0; along <= last; along++)
		{
			for (int depth = 0; depth < zone; depth++)
			{
				assertEquals(WHITE, image.getRGB(along, depth));
				assertEquals(WHITE, image.getRGB(along, last - depth));
				assertEquals(WHITE, image.getRGB(depth, along));
				assertEquals(WHITE, image.getRGB(last - depth, along));
			}
		}
	}

	@Test
	@DisplayName("the code encodes the address text as the overview shows it, so two addresses give two codes")
	void theCode_encodesTheAddressText()
	{
		QrCode code = AddressQrCode.codeOf(address).orElseThrow();
		assertEquals(QrCode.encodeText(address, QrCode.Ecc.MEDIUM).size, code.size);
		assertTrue(sameModules(code, QrCode.encodeText(address, QrCode.Ecc.MEDIUM)));

		String other = Wallet.create().address().toText();
		assertNotEquals(address, other);
		assertTrue(!sameModules(code, AddressQrCode.codeOf(other).orElseThrow()),
			"another address gives another code");
	}

	@ParameterizedTest(name = "\"{0}\"")
	@NullSource
	@ValueSource(strings = { "", "   " })
	@DisplayName("no address gives no code")
	void noAddress_givesNoCode(final String none)
	{
		assertTrue(AddressQrCode.codeOf(none).isEmpty());
		assertTrue(AddressQrCode.imageOf(none).isEmpty());
	}

	private static boolean sameModules(final QrCode one, final QrCode other)
	{
		if (one.size != other.size)
		{
			return false;
		}
		for (int y = 0; y < one.size; y++)
		{
			for (int x = 0; x < one.size; x++)
			{
				if (one.getModule(x, y) != other.getModule(x, y))
				{
					return false;
				}
			}
		}
		return true;
	}
}
