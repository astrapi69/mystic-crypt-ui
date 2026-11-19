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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import io.github.astrapi69.mystic.crypt.settings.FlatLafTheme;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;

/**
 * The Look and Feel menu offered only the JDK-bundled look and feels (GTK, Metal, Ocean, Motif,
 * Nimbus, System) - FlatLaf's four themes are added alongside them, not in place of them (#116)
 */
class DesktopMenuLookAndFeelTest
{

	private final LookAndFeel originalLookAndFeel = UIManager.getLookAndFeel();

	@AfterEach
	void restoreTheOriginalLookAndFeel() throws Exception
	{
		UIManager.setLookAndFeel(originalLookAndFeel);
	}

	private static List<String> menuItemNames(final JMenu menu)
	{
		return Arrays.stream(menu.getMenuComponents()).filter(JMenuItem.class::isInstance)
			.map(component -> ((JMenuItem)component).getText()).collect(Collectors.toList());
	}

	@Test
	void offersAllFourFlatLafThemesAlongsideTheExistingOnes()
	{
		JMenu menu = new DesktopMenu(new JFrame()).newLookAndFeelMenu();

		List<String> names = menuItemNames(menu);
		assertTrue(names.contains("GTK"), "the existing items must still be there");
		assertTrue(names.contains("FlatLaf Light"));
		assertTrue(names.contains("FlatLaf Dark"));
		assertTrue(names.contains("FlatLaf IntelliJ"));
		assertTrue(names.contains("FlatLaf Darcula"));
	}

	@Test
	void pickingAFlatLafThemeActuallySwitchesToIt() throws Exception
	{
		DesktopMenu menu = new DesktopMenu(new JFrame());
		JMenuItem flatLightItem = Arrays.stream(menu.newLookAndFeelMenu().getMenuComponents())
			.filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
			.filter(item -> "FlatLaf Light".equals(item.getText())).findFirst().orElseThrow();

		flatLightItem.doClick();

		assertEquals(FlatLightLaf.class, UIManager.getLookAndFeel().getClass(),
			"picking the theme must actually switch to it, not only show it as a choice");
	}

	@Test
	void aThemeThatIsAlreadyActiveDisablesItsOwnMenuItem() throws Exception
	{
		UIManager.setLookAndFeel(new FlatDarkLaf());
		JMenu menu = new DesktopMenu(new JFrame()).newLookAndFeelMenu();

		JMenuItem flatDarkItem = Arrays.stream(menu.getMenuComponents())
			.filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
			.filter(item -> "FlatLaf Dark".equals(item.getText())).findFirst().orElseThrow();

		assertFalse(flatDarkItem.isEnabled(),
			"switching to a theme that is already active would be a no-op, so the item greys "
				+ "itself out - the same pattern the JDK-bundled items already use");
	}

	@Test
	void pickingAFlatLafThemeKeepsTheChoiceInTheSettings(@TempDir final File directory)
		throws Exception
	{
		FlatLafTheme.installAll();
		DesktopMenu menu = new DesktopMenu(new JFrame())
		{
			@Override
			protected File configurationDirectory()
			{
				return directory;
			}
		};
		JMenuItem flatDarkItem = Arrays.stream(menu.newLookAndFeelMenu().getMenuComponents())
			.filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
			.filter(item -> "FlatLaf Dark".equals(item.getText())).findFirst().orElseThrow();

		// on the event dispatch thread, as a click is: the menu saves the look in a later event,
		// and
		// from another thread that event would run while the item is still switching
		java.awt.EventQueue.invokeAndWait(flatDarkItem::doClick);
		java.awt.EventQueue.invokeAndWait(() -> {
		});

		assertEquals("FlatLaf Dark", MysticCryptSettings.load(directory).getLookAndFeel(),
			"the choice of the menu was not written to the settings");
	}

}
