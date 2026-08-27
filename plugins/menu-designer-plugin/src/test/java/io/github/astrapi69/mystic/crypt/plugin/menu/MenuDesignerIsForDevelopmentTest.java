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
package io.github.astrapi69.mystic.crypt.plugin.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * This tool edits the application's own menu bar, which is work on the application rather than
 * something a user of a password manager has any reason to do. An installation does not receive the
 * plugin, and where an earlier release already put it, it stays out of the menu unless someone asks
 * for it on purpose.
 */
class MenuDesignerIsForDevelopmentTest
{

	private final String original = System
		.getProperty(MenuDesignerMenuContribution.DEVELOPER_PROPERTY);

	@AfterEach
	void restoreTheProperty()
	{
		if (original == null)
		{
			System.clearProperty(MenuDesignerMenuContribution.DEVELOPER_PROPERTY);
		}
		else
		{
			System.setProperty(MenuDesignerMenuContribution.DEVELOPER_PROPERTY, original);
		}
	}

	@Test
	@DisplayName("without the property the tool offers nothing to the menu")
	void withoutThePropertyNothingIsOffered()
	{
		System.clearProperty(MenuDesignerMenuContribution.DEVELOPER_PROPERTY);

		assertTrue(new MenuDesignerMenuContribution().getMenuItems().isEmpty(),
			"an installation must not offer the menu designer");
	}

	@Test
	@DisplayName("a value that is not true is not an invitation either")
	void onlyTrueCounts()
	{
		System.setProperty(MenuDesignerMenuContribution.DEVELOPER_PROPERTY, "yes please");

		assertTrue(new MenuDesignerMenuContribution().getMenuItems().isEmpty());
	}

	@Test
	@DisplayName("with the property the tool is there, under the name the test suite looks for")
	void withThePropertyTheToolIsOffered()
	{
		System.setProperty(MenuDesignerMenuContribution.DEVELOPER_PROPERTY, "true");

		assertEquals(1, new MenuDesignerMenuContribution().getMenuItems().size());
		assertEquals("Menu Designer",
			new MenuDesignerMenuContribution().getMenuItems().get(0).getText());
	}
}
