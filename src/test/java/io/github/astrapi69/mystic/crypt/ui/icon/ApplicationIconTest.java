/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.ui.icon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Frame;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * The application's icon, the key monogram (#815): its sizes, and the two places it is given to the
 * windows that do not get it from the main window - dialogs without a parent and internal frames
 */
class ApplicationIconTest
{

	private LookAndFeel originalLookAndFeel;

	@BeforeEach
	void rememberTheLookAndFeel()
	{
		originalLookAndFeel = UIManager.getLookAndFeel();
	}

	@AfterEach
	void putBackWhatTheTestChanged() throws Exception
	{
		onTheEventThread(() -> {
			UIManager.put("InternalFrame.icon", null);
			JOptionPane.getRootFrame().setIconImages(List.of());
			try
			{
				UIManager.setLookAndFeel(originalLookAndFeel);
			}
			catch (javax.swing.UnsupportedLookAndFeelException exception)
			{
				throw new IllegalStateException(exception);
			}
		});
	}

	@Test
	@DisplayName("the icon comes in 16, 32, 48 and 256 px, smallest first")
	void theSizes()
	{
		assertEquals(List.of(16, 32, 48, 256), ApplicationIcon.SIZES);
		assertEquals(List.of(16, 32, 48, 256),
			ApplicationIcon.images().stream().map(image -> image.getWidth(null)).toList());
	}

	@ParameterizedTest(name = "the {0} px image is square and has an alpha channel, its corner outside the tile transparent")
	@ValueSource(ints = { 16, 32, 48, 256 })
	void everySizeIsSquareWithAlpha(final int size)
	{
		BufferedImage image = (BufferedImage)ApplicationIcon.images()
			.get(ApplicationIcon.SIZES.indexOf(size));

		assertEquals(size, image.getWidth());
		assertEquals(size, image.getHeight());
		assertTrue(image.getColorModel().hasAlpha(), "an alpha channel");
		assertEquals(0, image.getRGB(0, 0) >>> 24, "the corner outside the rounded tile");
		assertEquals(255, image.getRGB(size / 2, size / 2) >>> 24, "the middle of the tile");
	}

	@Test
	@DisplayName("the largest size is the resource of the places that show one image at its own size")
	void theLargestIsAResource()
	{
		assertEquals("img/app-icon/mystic-crypt-ui-256.png", ApplicationIcon.LARGEST);
		assertNotNull(ApplicationIcon.class.getClassLoader().getResource(ApplicationIcon.LARGEST),
			"on the class path, where the splash screen, Help > Info and the installer read it");
	}

	@Test
	@DisplayName("the icon of an internal frame is 16 px and carries 32 and 48 px for a scaled screen")
	void theFrameIconCarriesThreeResolutions()
	{
		Icon frameIcon = ApplicationIcon.frameIcon();

		assertEquals(16, frameIcon.getIconWidth());
		assertEquals(16, frameIcon.getIconHeight());
		MultiResolutionImage image = (MultiResolutionImage)((ImageIcon)frameIcon).getImage();
		assertEquals(List.of(16, 32, 48),
			image.getResolutionVariants().stream().map(variant -> variant.getWidth(null)).toList());
	}

	@ParameterizedTest(name = "under {0} a new internal frame carries the application icon")
	@ValueSource(strings = { "Metal", "Nimbus", "FlatLaf Light", "FlatLaf Dark" })
	void everyInternalFrameCarriesTheIcon(final String lookAndFeel) throws Exception
	{
		ApplicationIcon.install();

		AtomicReference<Icon> frameIcon = new AtomicReference<>();
		onTheEventThread(() -> {
			switchTo(lookAndFeel);
			frameIcon
				.set(new JInternalFrame("a tool window", true, true, true, true).getFrameIcon());
		});

		assertSame(ApplicationIcon.frameIcon(), frameIcon.get());
	}

	@Test
	@DisplayName("an internal frame opened before a change of the look and feel keeps the application icon")
	void anInternalFrameKeepsTheIconThroughALookAndFeelChange() throws Exception
	{
		ApplicationIcon.install();

		AtomicReference<Icon> frameIcon = new AtomicReference<>();
		onTheEventThread(() -> {
			JInternalFrame frame = new JInternalFrame("a tool window");
			switchTo("Nimbus");
			SwingUtilities.updateComponentTreeUI(frame);
			frameIcon.set(frame.getFrameIcon());
		});

		assertSame(ApplicationIcon.frameIcon(), frameIcon.get());
	}

	@Test
	@DisplayName("a dialog without a parent shows the application icon on the X server")
	void aDialogWithoutAParentShowsTheIcon() throws Exception
	{
		ApplicationIcon.install();

		int[] shown = sixteenPixelsOfAParentlessDialog(
			"ApplicationIconTest, a dialog without a parent");

		assertArrayEquals(sixteenPixels(), shown,
			"the dialog takes the icon of Swing's shared owner frame");
	}

	@Test
	@DisplayName("without the install a dialog without a parent shows another icon on the X server")
	void withoutTheInstallADialogWithoutAParentShowsAnotherIcon() throws Exception
	{
		int[] shown = sixteenPixelsOfAParentlessDialog(
			"ApplicationIconTest, a dialog without a parent, no install");

		assertFalse(Arrays.equals(sixteenPixels(), shown),
			"the default icon, which is what such a dialog showed before #815");
	}

	private static int[] sixteenPixelsOfAParentlessDialog(final String title) throws Exception
	{
		AtomicReference<JDialog> dialog = new AtomicReference<>();
		onTheEventThread(() -> {
			JDialog parentless = new JDialog((Frame)null, title);
			parentless.setSize(240, 120);
			parentless.setVisible(true);
			dialog.set(parentless);
		});
		try
		{
			return XWindowIcons.sixteenBySixteen(title);
		}
		finally
		{
			onTheEventThread(() -> dialog.get().dispose());
		}
	}

	private static int[] sixteenPixels()
	{
		Image sixteen = ApplicationIcon.images().get(0);
		return ((BufferedImage)sixteen).getRGB(0, 0, 16, 16, null, 0, 16);
	}

	private static void switchTo(final String lookAndFeel)
	{
		try
		{
			UIManager.setLookAndFeel(switch (lookAndFeel)
			{
				case "Metal" -> new MetalLookAndFeel();
				case "Nimbus" -> new NimbusLookAndFeel();
				case "FlatLaf Light" -> new FlatLightLaf();
				case "FlatLaf Dark" -> new FlatDarkLaf();
				default -> throw new IllegalArgumentException("no case for " + lookAndFeel);
			});
		}
		catch (javax.swing.UnsupportedLookAndFeelException exception)
		{
			throw new IllegalStateException(exception);
		}
	}

	private static void onTheEventThread(final Runnable runnable)
		throws InterruptedException, InvocationTargetException
	{
		SwingUtilities.invokeAndWait(runnable);
	}
}
