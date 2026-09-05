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
package io.github.astrapi69.mystic.crypt.keepass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import javax.swing.Icon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.astrapi69.lang.ClassExtensions;

/**
 * An imported KeePass entry or group carries an icon index from 0 to 68, and until #206 nothing
 * turned that number back into the icon the user picked in KeePass - this proves the lookup that
 * does
 */
class KeePassIconsTest
{

	@Test
	@DisplayName("every one of the 69 icon indices resolves to a resource that is really shipped")
	void everyIndexResolvesToAShippedResource() throws Exception
	{
		for (int index = 0; index <= KeePassIcons.HIGHEST_INDEX; index++)
		{
			String path = KeePassIcons.pathOf(index);
			assertNotNull(path, "index " + index + " has no icon path");
			try (InputStream resource = ClassExtensions.getResourceAsStream(path))
			{
				assertNotNull(resource,
					"index " + index + " points at a missing resource: " + path);
			}
		}
	}

	@Test
	@DisplayName("the index is the KeePass index: 0 is the key, 48 is the folder")
	void theIndexIsTheKeePassIndex()
	{
		assertEquals("img/keepass/C00_Password.png", KeePassIcons.pathOf(0));
		assertEquals("img/keepass/C48_Folder.png", KeePassIcons.pathOf(48));
		assertEquals("img/keepass/C68_BlackBerry.png",
			KeePassIcons.pathOf(KeePassIcons.HIGHEST_INDEX));
	}

	@ParameterizedTest(name = "an icon index of {0} has no icon rather than an exception")
	@ValueSource(ints = { -1, 69, 1000, Integer.MIN_VALUE, Integer.MAX_VALUE })
	void anIndexOutsideTheSetHasNoIcon(final int index)
	{
		assertNull(KeePassIcons.pathOf(index), "index " + index + " is not part of the set");
		assertNull(KeePassIcons.of(index));
	}

	@Test
	@DisplayName("an entry or group without an icon index simply has no icon")
	void aMissingIndexHasNoIcon()
	{
		assertNull(KeePassIcons.pathOf(null));
		assertNull(KeePassIcons.of(null));
	}

	@Test
	@DisplayName("the loaded icon is the 16x16 image and the same instance on the next lookup")
	void theIconIsLoadedOnceAndReused()
	{
		Icon first = KeePassIcons.of(0);

		assertNotNull(first);
		assertEquals(16, first.getIconWidth());
		assertEquals(16, first.getIconHeight());
		assertTrue(first == KeePassIcons.of(0), "the icon must be cached, not re-read per cell");
	}
}
