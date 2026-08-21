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
package io.github.astrapi69.mystic.crypt.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link GeneralSettingsPanel#applyLookAndFeel(String)} - the look-and-feel switch
 * the settings dialog and the startup use. Driving the base-class Look and Feel <em>menu</em>
 * through AssertJ-Swing is not viable: a global look-and-feel change mutates the component tree
 * while the tool walks the hierarchy and throws a ConcurrentModificationException, so the switch
 * itself is verified here instead.
 */
class GeneralSettingsPanelTest
{

	@Test
	void applyLookAndFeelSwitchesTheUiManagerLookAndFeel()
	{
		GeneralSettingsPanel.applyLookAndFeel("Metal");
		assertEquals("Metal", UIManager.getLookAndFeel().getID(),
			"applying 'Metal' must switch the UIManager look and feel to Metal");

		GeneralSettingsPanel.applyLookAndFeel("Nimbus");
		assertEquals("Nimbus", UIManager.getLookAndFeel().getID(),
			"applying 'Nimbus' must switch the UIManager look and feel to Nimbus");
	}

	@Test
	void applyingAnUnknownLookAndFeelLeavesTheCurrentOne()
	{
		GeneralSettingsPanel.applyLookAndFeel("Metal");
		GeneralSettingsPanel.applyLookAndFeel("this-look-and-feel-does-not-exist");
		assertEquals("Metal", UIManager.getLookAndFeel().getID(),
			"an unknown look-and-feel name must leave the current look and feel unchanged");
	}
}
