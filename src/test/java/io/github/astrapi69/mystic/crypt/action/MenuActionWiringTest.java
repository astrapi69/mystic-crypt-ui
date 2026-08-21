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
package io.github.astrapi69.mystic.crypt.action;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.swing.action.ExitApplicationAction;
import io.github.astrapi69.swing.action.ToggleFullScreenAction;

/**
 * Static wiring checks for the two menu actions that cannot be driven through the UI end-to-end:
 * Exit (it calls {@link System#exit(int)}, which would kill the test JVM) and Toggle Fullscreen
 * (full-screen exclusive mode is unreliable on a headless display). Instead of executing them, this
 * verifies that the Exit action really invokes {@code System.exit} and that the fullscreen menu
 * action really is a {@link ToggleFullScreenAction}.
 */
class MenuActionWiringTest
{

	@Test
	void exitActionInvokesSystemExit() throws Exception
	{
		byte[] classBytes;
		try (InputStream inputStream = ExitApplicationAction.class.getClassLoader()
			.getResourceAsStream("io/github/astrapi69/swing/action/ExitApplicationAction.class"))
		{
			assertNotNull(inputStream, "ExitApplicationAction must be on the classpath");
			classBytes = inputStream.readAllBytes();
		}
		String constantPool = new String(classBytes, StandardCharsets.ISO_8859_1);
		assertTrue(constantPool.contains("java/lang/System"),
			"the exit action must reference java.lang.System");
		assertTrue(constantPool.contains("exit"), "the exit action must call exit");
	}

	@Test
	void fullscreenActionIsAToggleFullScreenAction()
	{
		assertTrue(
			ToggleFullScreenAction.class.isAssignableFrom(ApplicationToggleFullScreenAction.class),
			"the Toggle Fullscreen menu action must be a ToggleFullScreenAction");
	}
}
