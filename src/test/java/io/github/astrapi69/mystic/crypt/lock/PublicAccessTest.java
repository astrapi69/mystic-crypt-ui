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
package io.github.astrapi69.mystic.crypt.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.mystic.crypt.MenuId;
import io.github.astrapi69.mystic.crypt.settings.FlatLafTheme;
import io.github.astrapi69.swing.menu.enumeration.BaseMenuId;

/**
 * The decision that used to be a blacklist, now where it can be read and mutated: what may be
 * offered while no vault is open (#232)
 */
class PublicAccessTest
{

	@ParameterizedTest(name = "signedIn={0}, declaredPublic={1} -> offered={2}")
	@CsvSource({ "true,  true,  true", "true,  false, true", "false, true,  true",
			"false, false, false" })
	@DisplayName("without a vault, only what declares itself public is offered")
	void theTruthTable(final boolean signedIn, final boolean declaredPublic, final boolean expected)
	{
		assertEquals(expected, PublicAccess.isOffered(signedIn, declaredPublic));
	}

	@Test
	@DisplayName("'Open Database' is private, because clicking it without a vault throws")
	void openingADatabaseIsNotPublic()
	{
		assertFalse(PublicAccess.isPublicMenuId(MenuId.OPEN_DATABASE.propertiesKey()),
			"the action behind that label re-shows a vault that is already open; without one it "
				+ "throws a NullPointerException. A way into a vault has to be built first (#266)");
	}

	@Test
	@DisplayName("what needs a vault is not public, and that is the whole list of what was wrong")
	void theEntriesThatUsedToSlipThroughAreNotPublic()
	{
		assertFalse(PublicAccess.isPublicMenuId(MenuId.IMPORT_KEEPASS.propertiesKey()),
			"importing into a vault that is not open answers a click with 'Import failed'");
		assertFalse(PublicAccess.isPublicMenuId(MenuId.EXPORT_KEEPASS.propertiesKey()),
			"exporting a vault that is not open answers a click with 'Export failed'");
		assertFalse(PublicAccess.isPublicMenuId(MenuId.LOCK_WORKSPACE.propertiesKey()),
			"locking nothing does nothing, silently");
		assertFalse(PublicAccess.isPublicMenuId(MenuId.SAVE_APPLICATION_FILE.propertiesKey()));
		assertFalse(PublicAccess.isPublicMenuId(MenuId.SEARCH.propertiesKey()));
		assertFalse(PublicAccess.isPublicMenuId(MenuId.SECRET_KEY.propertiesKey()));
	}

	@Test
	@DisplayName("an entry nobody listed is private, which is the point of a whitelist")
	void anUnknownEntryIsPrivate()
	{
		assertFalse(PublicAccess.isPublicMenuId("global.menu.something.added.tomorrow"));
		assertFalse(PublicAccess.isPublicMenuId(null), "and so is one without a name at all");
	}

	@ParameterizedTest(name = "{0} is public")
	@EnumSource(FlatLafTheme.class)
	@DisplayName("every FlatLaf theme is public, by the name its own item carries")
	void everyFlatLafThemeIsPublic(final FlatLafTheme theme)
	{
		assertTrue(PublicAccess.isPublicMenuId(theme.menuItemName()),
			"the list derives these names from FlatLafTheme, so a new theme is public without "
				+ "anyone having to remember this list");
	}

	@Test
	@DisplayName("the containers are public, or nothing below them can be reached")
	void theContainersArePublic()
	{
		Arrays
			.asList(BaseMenuId.FILE.propertiesKey(), BaseMenuId.HELP.propertiesKey(),
				MenuId.VIEW.propertiesKey(), MenuId.PLUGINS.propertiesKey())
			.forEach(container -> assertTrue(PublicAccess.isPublicMenuId(container), container));
	}

	@Test
	@DisplayName("the View menu id is the name the menu actually carries")
	void theViewMenuIdMatchesTheMenuItIsBuiltWith()
	{
		assertEquals("global.menu.view", MenuId.VIEW.propertiesKey(),
			"a different string would build a View menu the whitelist does not know, and the "
				+ "menu would go private without anyone noticing");
	}
}
