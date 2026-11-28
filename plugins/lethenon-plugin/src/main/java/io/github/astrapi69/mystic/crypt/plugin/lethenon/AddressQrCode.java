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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

import io.nayuki.qrcodegen.QrCode;

/**
 * The address of a wallet as a QR code, for a phone to read it off the screen (#874). It encodes
 * the address text exactly as the overview shows it, so another address format (#924) changes
 * nothing here. Dark modules on a light ground with the quiet zone of four modules ISO/IEC 18004
 * asks for, in black and white whatever the look and feel: a scanner needs the contrast, not the
 * theme.
 */
final class AddressQrCode
{

	/** The quiet zone around the code, in modules */
	static final int BORDER = 4;

	/** The pixels one module is drawn with */
	static final int SCALE = 4;

	private AddressQrCode()
	{
	}

	/**
	 * The QR code of an address
	 *
	 * @param address
	 *            the address as the overview shows it
	 * @return the code, error correction level M; empty for no address
	 */
	static Optional<QrCode> codeOf(final String address)
	{
		if (address == null || address.isBlank())
		{
			return Optional.empty();
		}
		return Optional.of(QrCode.encodeText(address.strip(), QrCode.Ecc.MEDIUM));
	}

	/**
	 * The image of an address's QR code
	 *
	 * @param address
	 *            the address as the overview shows it
	 * @return the image, {@value #SCALE} pixels per module with a quiet zone of {@value #BORDER}
	 *         modules on every side; empty for no address
	 */
	static Optional<BufferedImage> imageOf(final String address)
	{
		return codeOf(address).map(AddressQrCode::draw);
	}

	private static BufferedImage draw(final QrCode code)
	{
		int side = (code.size + 2 * BORDER) * SCALE;
		BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, side, side);
			graphics.setColor(Color.BLACK);
			for (int y = 0; y < code.size; y++)
			{
				for (int x = 0; x < code.size; x++)
				{
					if (code.getModule(x, y))
					{
						graphics.fillRect((x + BORDER) * SCALE, (y + BORDER) * SCALE, SCALE, SCALE);
					}
				}
			}
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}
}
