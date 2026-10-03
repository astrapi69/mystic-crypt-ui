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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.ChecksumAlgorithm;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;

/**
 * Every enum-backed combo box in this application shows its values in the order the enum declares
 * them, and starts on the first of them.
 * <p>
 * It is a library behaviour, pinned here because this application depends on it in several panels -
 * the key size of a new private key, the view mode in the settings, the checksum algorithm, the
 * recipient kind of a Lethenon transfer - and because it was NOT true until swing-base-components
 * 5.2. Up to 5.1 the model built its items from a {@code HashSet}, and {@code Enum} does not
 * override {@code hashCode()}, so the order came from identity hashes: measured over a 26-value
 * enum, 5.1 answered {@code MBENYTQRSFDGXIAZ...} and 5.2 answers {@code ABC...XYZ}
 * (swing-base-components#1, fixed in its #2).
 * <p>
 * What it cost before the fix: an end-to-end test of the Lethenon send window selected the
 * recipient kind by INDEX, passed once and failed on the next run with the two values swapped. That
 * test now selects by value, which is right either way - and this test is what keeps the library
 * side from regressing under a later bump.
 */
class EnumComboBoxOrderTest
{

	@Test
	@DisplayName("a combo box over an enum lists its values in declaration order")
	void theItems_areInDeclarationOrder()
	{
		assertEquals(valuesOf(KeySize.class), itemsOf(new EnumComboBoxModel<>(KeySize.class)),
			"the key size box of the private key panel");
		assertEquals(valuesOf(FrameMode.class), itemsOf(new EnumComboBoxModel<>(FrameMode.class)),
			"the view mode box of the general settings");
		assertEquals(valuesOf(ChecksumAlgorithm.class),
			itemsOf(new EnumComboBoxModel<>(ChecksumAlgorithm.class)),
			"the algorithm box the checksum tools show - enough values that a hash-ordered set "
				+ "stops looking accidental");
	}

	@Test
	@DisplayName("and it starts on the first value the enum declares")
	void theSelection_isTheFirstDeclaredValue()
	{
		assertEquals(KeySize.values()[0], new EnumComboBoxModel<>(KeySize.class).getSelectedItem());
		assertEquals(FrameMode.values()[0],
			new EnumComboBoxModel<>(FrameMode.class).getSelectedItem());
		assertEquals(ChecksumAlgorithm.values()[0],
			new EnumComboBoxModel<>(ChecksumAlgorithm.class).getSelectedItem(),
			"up to 5.1 the selection came from an EnumSet and the items from a HashSet, so these "
				+ "two could disagree - the surprise underneath the flaky test");
	}

	private static <E extends Enum<E>> List<E> valuesOf(final Class<E> enumClass)
	{
		return Arrays.asList(enumClass.getEnumConstants());
	}

	private static <E extends Enum<E>> List<E> itemsOf(final EnumComboBoxModel<E> model)
	{
		List<E> items = new ArrayList<>();
		for (int index = 0; index < model.getSize(); index++)
		{
			items.add(model.getElementAt(index));
		}
		return items;
	}
}
