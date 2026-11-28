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
package io.github.astrapi69.mystic.crypt.ui.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.swing.model.component.JMPasswordField;

/**
 * A password field is a key in the layout of its panel, and a key keeps its place only while its
 * hash code stays the same. {@link JMPasswordField} derives its hash code from its model, which
 * follows the text, so it must not be used as a layout key as it is.
 */
class LayoutSafePasswordFieldTest
{

	@Test
	void libraryField_hashCodeFollowsTheText_whichLosesItsPlaceInAMap()
	{
		JMPasswordField field = new JMPasswordField();
		Map<Object, String> places = new HashMap<>();
		places.put(field, "place");
		int before = field.hashCode();

		field.setText("typed");

		assertNotEquals(before, field.hashCode());
		assertNull(places.get(field));
	}

	@Test
	void layoutSafeField_keepsItsPlaceInAMap_whenTheTextChanges()
	{
		LayoutSafePasswordField field = new LayoutSafePasswordField();
		Map<Object, String> places = new HashMap<>();
		places.put(field, "place");

		field.setText("typed");

		assertEquals("place", places.get(field));
	}

	@Test
	void layoutSafeField_isEqualOnlyToItself_whenAnotherFieldHasTheSameText()
	{
		LayoutSafePasswordField field = new LayoutSafePasswordField();
		LayoutSafePasswordField other = new LayoutSafePasswordField();

		assertEquals(field, field);
		assertNotEquals(field, other);
	}
}
