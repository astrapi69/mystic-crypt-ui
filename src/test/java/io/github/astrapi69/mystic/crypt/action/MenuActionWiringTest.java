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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.swing.action.ToggleFullScreenAction;

/**
 * Static wiring checks for what cannot be driven through the UI end-to-end: the real exit (it calls
 * {@link System#exit(int)}, which would kill the test JVM) and Toggle Fullscreen (full-screen
 * exclusive mode is unreliable on a headless display).
 * <p>
 * The exit checks changed shape with #386 and #387. Before, the Exit menu item was wired to a
 * library action whose whole body was {@code System.exit(0)}, and this test proved that - which is
 * exactly what was wrong: an exit that never asked about unsaved changes and never overwrote the
 * vault. Now there is one ending path, {@code MysticCryptApplicationFrame.endTheApplication}, and
 * both the menu item and the window button go through it. The end-to-end tests drive that path with
 * an injected exit; what remains static is that the real exit still reaches {@code System.exit},
 * and that no caller reaches it any other way.
 */
class MenuActionWiringTest
{

	@Test
	@DisplayName("the menu's Exit is this application's own action, going through the one ending path")
	void theExitMenuActionGoesThroughTheEndingPath() throws IOException
	{
		String constantPool = constantPoolOf(EndApplicationAction.class);

		assertTrue(constantPool.contains("endTheApplication"),
			"the Exit menu action must end the application through the frame's one ending path, "
				+ "or it skips the question and the wipe again (#386, #387)");
		assertFalse(constantPool.contains("java/lang/System"),
			"the Exit menu action must not call System.exit itself - the exit belongs at the end "
				+ "of the ending path, after the question and the wipe, and nowhere else");
	}

	@Test
	@DisplayName("the ending path is what reaches System.exit, and it is the only place that does")
	void onlyTheEndingPathReachesSystemExit() throws IOException
	{
		String frame = constantPoolOf(MysticCryptApplicationFrame.class);

		assertTrue(frame.contains("java/lang/System") && frame.contains("exit"),
			"the frame's ending path must still end the JVM - injecting the exit for tests must not "
				+ "have removed the real one");
	}

	@Test
	void fullscreenActionIsAToggleFullScreenAction()
	{
		assertTrue(
			ToggleFullScreenAction.class.isAssignableFrom(ApplicationToggleFullScreenAction.class),
			"the Toggle Fullscreen menu action must be a ToggleFullScreenAction");
	}

	private static String constantPoolOf(final Class<?> type) throws IOException
	{
		String resource = type.getName().replace('.', '/') + ".class";
		try (InputStream inputStream = type.getClassLoader().getResourceAsStream(resource))
		{
			assertNotNull(inputStream, resource + " must be on the classpath");
			return new String(inputStream.readAllBytes(), StandardCharsets.ISO_8859_1);
		}
	}
}
