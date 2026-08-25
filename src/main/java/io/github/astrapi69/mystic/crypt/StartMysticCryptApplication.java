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
package io.github.astrapi69.mystic.crypt;

import io.github.astrapi69.awt.screen.ScreenSizeExtensions;
import io.github.astrapi69.mystic.crypt.cli.MysticCryptUiCli;

/**
 * The class {@link StartMysticCryptApplication} starts the application
 */
public class StartMysticCryptApplication
{

	/**
	 * The main method that start this {@link MysticCryptApplicationFrame}, or, when the first
	 * argument is {@code --cli}, runs a command line command instead and exits with its exit code -
	 * without ever creating a window, so it also works on a machine without a display
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args)
	{
		if (MysticCryptUiCli.isCliInvocation(args))
		{
			System.exit(MysticCryptUiCli.execute(MysticCryptUiCli.stripCliArgument(args)));
		}
		MysticCryptApplicationFrame frame = new MysticCryptApplicationFrame();
		while (!frame.isVisible())
		{
			ScreenSizeExtensions.showFrame(frame);
		}
	}
}
