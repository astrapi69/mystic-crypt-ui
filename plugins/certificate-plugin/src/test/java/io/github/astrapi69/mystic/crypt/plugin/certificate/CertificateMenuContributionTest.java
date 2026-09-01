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
package io.github.astrapi69.mystic.crypt.plugin.certificate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * There is no in-app viewer for a certificate file, so "open" after creating one has to go through
 * whatever the operating system associates with it - this pins the one part of that path with real
 * behavior to check: a system that offers no way to open files fails with a reason a user can act
 * on, rather than silently doing nothing or throwing something unexplained.
 */
class CertificateMenuContributionTest
{

	@Test
	void aSystemWithNoWayToOpenFilesSaysSo(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "test.crt");
		file.createNewFile();

		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
		{
			// the CI/Xvfb environment this normally runs in has no desktop session to open
			// anything with, which is exactly the case this test pins - a real desktop is free to
			// skip it
			return;
		}

		IOException thrown = assertThrows(IOException.class,
			() -> CertificateMenuContribution.openWithSystemDefault(file));
		assertTrue(thrown.getMessage() != null && !thrown.getMessage().isBlank(),
			"the reason has to be shown, not swallowed");
	}

}
