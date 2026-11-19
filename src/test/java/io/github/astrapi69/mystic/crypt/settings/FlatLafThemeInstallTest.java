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
package io.github.astrapi69.mystic.crypt.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

/**
 * Installing the FlatLaf themes more than once installs each of them once
 */
class FlatLafThemeInstallTest
{

	@Test
	void installingTwice_listsEachThemeOnce()
	{
		FlatLafTheme.installAll();
		FlatLafTheme.installAll();

		for (FlatLafTheme theme : FlatLafTheme.values())
		{
			long listed = Arrays.stream(UIManager.getInstalledLookAndFeels())
				.filter(info -> info.getName().equals(theme.getLabel())).count();
			assertEquals(1, listed, theme.getLabel());
		}
	}
}
