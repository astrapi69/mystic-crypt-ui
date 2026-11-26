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

import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * The application's icon, the key monogram: an M whose right stem carries a key bit, teal on a dark
 * tile (#815, chosen by the maintainer on 2026-11-26). It is drawn in four sizes, each square and
 * with an alpha channel; the 16 px one is drawn for its size, the others are rendered from one SVG.
 * The sources and how the images were made are in {@code design/app-icon/}.
 * <p>
 * Every window of the application shows it. The main window carries all four sizes, so the system
 * picks the one it needs instead of scaling a single image, and a dialog it owns shows the same.
 * {@link #install()} covers the windows that do not get it from the main window.
 */
public final class ApplicationIcon
{

	/** The sizes the icon is drawn in, in pixels, smallest first */
	public static final List<Integer> SIZES = List.of(16, 32, 48, 256);

	/**
	 * The resource of the largest size, for the places that show one image at its own size: the
	 * splash screen, Help > Info, the installer's language selection and the desktop entry
	 */
	public static final String LARGEST = resource(256);

	/** The look and feel default every internal frame takes its frame icon from */
	private static final String INTERNAL_FRAME_ICON = "InternalFrame.icon";

	/**
	 * The sizes an internal frame's icon carries: 16 px and the variants a scaled screen asks for
	 */
	private static final int FRAME_ICON_SIZES = 3;

	private static List<Image> images;

	private static Icon frameIcon;

	private ApplicationIcon()
	{
	}

	/**
	 * The class path resource of the icon in the given size
	 *
	 * @param size
	 *            one of {@link #SIZES}
	 * @return for example {@code img/app-icon/mystic-crypt-ui-16.png}
	 */
	public static String resource(final int size)
	{
		return "img/app-icon/mystic-crypt-ui-" + size + ".png";
	}

	/**
	 * The icon in every size, smallest first, for {@link java.awt.Window#setIconImages(List)}
	 *
	 * @return the four images, read once
	 * @throws IllegalStateException
	 *             if one of the images is not on the class path or not readable as an image
	 */
	public static synchronized List<Image> images()
	{
		if (images == null)
		{
			images = SIZES.stream().map(ApplicationIcon::read).toList();
		}
		return images;
	}

	/**
	 * The icon of an internal frame: 16 px, with the 32 and 48 px images as the variants a scaled
	 * screen draws instead
	 *
	 * @return the one frame icon every internal frame shares
	 */
	public static synchronized Icon frameIcon()
	{
		if (frameIcon == null)
		{
			frameIcon = new ImageIcon(new BaseMultiResolutionImage(
				images().subList(0, FRAME_ICON_SIZES).toArray(Image[]::new)));
		}
		return frameIcon;
	}

	/**
	 * Gives the icon to the windows that do not take it from the main window. Measured on
	 * 2026-11-17 (#815): a dialog without a parent belongs to Swing's shared owner frame, which had
	 * no icon, and every internal frame carried the look and feel's default frame icon.
	 * <ul>
	 * <li>the shared owner frame, which {@link JOptionPane#getRootFrame()} returns, gets every
	 * size, so a dialog without a parent shows the icon as its owner's;</li>
	 * <li>the frame icon goes into the developer defaults of {@link UIManager}, which every look
	 * and feel reads when it installs an internal frame and which outlive a change of the look and
	 * feel, so a plugin's window has it without doing anything.</li>
	 * </ul>
	 * Called before the first window of the application opens; on the event thread or before the
	 * user interface exists.
	 */
	public static void install()
	{
		JOptionPane.getRootFrame().setIconImages(images());
		UIManager.put(INTERNAL_FRAME_ICON, frameIcon());
	}

	private static Image read(final int size)
	{
		String resource = resource(size);
		try (InputStream stream = ApplicationIcon.class.getClassLoader()
			.getResourceAsStream(resource))
		{
			if (stream == null)
			{
				throw new IllegalStateException(
					"the application icon " + resource + " is not on the class path");
			}
			BufferedImage image = ImageIO.read(stream);
			if (image == null)
			{
				throw new IllegalStateException(
					"the application icon " + resource + " is not an image ImageIO can read");
			}
			return image;
		}
		catch (IOException exception)
		{
			throw new UncheckedIOException(
				"the application icon " + resource + " could not be read", exception);
		}
	}
}
