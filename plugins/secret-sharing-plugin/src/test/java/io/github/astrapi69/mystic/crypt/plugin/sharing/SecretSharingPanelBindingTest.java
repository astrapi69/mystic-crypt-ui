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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

/**
 * "Shares needed" and "Shares produced" are Shamir's secret sharing threshold and total, and are
 * not self-explanatory from a two-word label alone - this panel had no tooltip on any field or
 * button at all
 */
class SecretSharingPanelBindingTest
{

	private static <T extends Component> T named(Container container, String name, Class<T> type)
	{
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component) && name.equals(component.getName()))
			{
				return type.cast(component);
			}
			if (component instanceof Container child)
			{
				T found = named(child, name, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		assertNotNull(component, fieldName + " must exist in the panel");
		String tooltip = component.getToolTipText();
		assertTrue(tooltip != null && !tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		SecretSharingPanel panel = new SecretSharingPanel();

		assertHasTooltip(named(panel, "pwdSecret", JComponent.class), "secret");
		assertHasTooltip(named(panel, "txtSecretFile", JComponent.class), "secret file");
		assertHasTooltip(named(panel, "chkUseFile", JComponent.class), "use file");
		assertHasTooltip(named(panel, "spnThreshold", JComponent.class), "threshold");
		assertHasTooltip(named(panel, "spnTotalShares", JComponent.class), "total shares");
		assertHasTooltip(named(panel, "txtShares", JComponent.class), "shares");
		assertHasTooltip(named(panel, "txtRebuilt", JComponent.class), "rebuilt secret");
		assertHasTooltip(named(panel, "txtRebuiltFile", JComponent.class), "rebuilt file");
		assertHasTooltip(named(panel, "btnBrowseSecretFile", JComponent.class),
			"browse secret file");
		assertHasTooltip(named(panel, "btnSplit", JComponent.class), "split");
		assertHasTooltip(named(panel, "btnSaveShares", JComponent.class), "save shares");
		assertHasTooltip(named(panel, "btnCombine", JComponent.class), "combine");
		assertHasTooltip(named(panel, "btnLoadShares", JComponent.class), "load shares");
		assertHasTooltip(named(panel, "btnBrowseRebuiltFile", JComponent.class),
			"browse rebuilt file");
		assertHasTooltip(named(panel, "btnSaveRebuilt", JComponent.class), "save rebuilt secret");
	}
}
