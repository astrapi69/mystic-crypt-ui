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
package io.github.astrapi69.mystic.crypt.clipboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The lock empties the clipboard (#238), and the unlock prompt says so when the clipboard held text
 * (#1001): otherwise a password copied before the lock pastes nothing, which reads as "paste does
 * not work here". Each case uses a clipboard of the test's own, never the desktop's
 */
class ClipboardAtTheLockTest
{

	@AfterEach
	void forgetTheLock()
	{
		ClipboardAtTheLock.forget();
	}

	record Before(String name, String content, boolean emptiedText) {
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Before> clipboards()
	{
		return Stream.of(
			new Before("a clipboard holding a password", "copied before the lock", true),
			new Before("a clipboard holding one space", " ", true),
			new Before("a clipboard holding empty text", "", false),
			new Before("a clipboard nobody wrote to", null, false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("clipboards")
	@DisplayName("the lock empties the clipboard and remembers whether it held text")
	void theLockEmptiesTheClipboard_andRemembersWhetherItHeldText(final Before before)
		throws Exception
	{
		Clipboard clipboard = new Clipboard("the test's own");
		if (before.content() != null)
		{
			clipboard.setContents(new StringSelection(before.content()), null);
		}

		ClipboardAtTheLock.empty(clipboard);

		assertEquals("", clipboard.getData(DataFlavor.stringFlavor),
			"the lock left something to paste");
		assertEquals(before.emptiedText(), ClipboardAtTheLock.emptiedText());
	}

	@Test
	@DisplayName("unlocking forgets that the lock emptied text")
	void unlocking_forgetsIt()
	{
		Clipboard clipboard = new Clipboard("the test's own");
		clipboard.setContents(new StringSelection("copied before the lock"), null);
		ClipboardAtTheLock.empty(clipboard);
		assertTrue(ClipboardAtTheLock.emptiedText(), "precondition: the lock emptied text");

		ClipboardAtTheLock.forget();

		assertFalse(ClipboardAtTheLock.emptiedText());
	}

	@Test
	@DisplayName("a second lock over an empty clipboard does not repeat what the first one emptied")
	void aSecondLockOverAnEmptyClipboard_saysNothing()
	{
		Clipboard clipboard = new Clipboard("the test's own");
		clipboard.setContents(new StringSelection("copied before the lock"), null);
		ClipboardAtTheLock.empty(clipboard);

		ClipboardAtTheLock.empty(clipboard);

		assertFalse(ClipboardAtTheLock.emptiedText(),
			"the second lock found the clipboard already empty");
	}
}
