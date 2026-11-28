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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import io.nayuki.qrcodegen.QrCode;
import io.nayuki.qrcodegen.QrSegment;

/**
 * A QR code of a text that holds a secret (#1120). The text goes into the code as its UTF-8 bytes
 * in byte mode, built here from the character array, because qrcodegen's {@code encodeText} turns
 * such a text into a String it cannot overwrite; the bytes are overwritten once the code is built.
 * Dark modules on a light ground with the quiet zone of four modules ISO/IEC 18004 asks for, in
 * black and white whatever the theme: a scanner needs the contrast.
 */
final class QrImage
{

	/** The quiet zone around the code, in modules */
	static final int BORDER = 4;

	/** The pixels one module is drawn with */
	static final int SCALE = 6;

	private QrImage()
	{
	}

	/**
	 * The QR code of a text, error correction level M
	 *
	 * @param text
	 *            the text; left as it is, the caller overwrites it
	 * @return the code
	 */
	static QrCode codeOf(final char[] text)
	{
		ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(text));
		byte[] bytes = new byte[encoded.remaining()];
		encoded.get(bytes);
		try
		{
			return QrCode.encodeSegments(List.of(QrSegment.makeBytes(bytes)), QrCode.Ecc.MEDIUM);
		}
		finally
		{
			Arrays.fill(bytes, (byte)0);
			if (encoded.hasArray())
			{
				Arrays.fill(encoded.array(), (byte)0);
			}
		}
	}

	/**
	 * The image of a code
	 *
	 * @param code
	 *            the code
	 * @return {@value #SCALE} pixels per module, with a quiet zone of {@value #BORDER} modules on
	 *         every side
	 */
	static BufferedImage of(final QrCode code)
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
