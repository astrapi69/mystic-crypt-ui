package io.github.astrapi69.mystic.crypt.tutils;

import de.alpharogroup.lang.ClassExtensions;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

public class ImageIconFactory
{

	/**
	 * Factory method for create a new {@link ImageIcon}
	 *
	 * @param image
	 *            the file that contains the image
	 * @return the new {@link ImageIcon}
	 */
	public static ImageIcon newImageIcon(File image)
	{
		ImageIcon img = newImageIcon(image.getAbsolutePath());
		return img;
	}

	/**
	 * Factory method for create a new {@link ImageIcon}
	 *
	 * @param relativeImagePath
	 *            the relative image path
	 * @return the new {@link ImageIcon}
	 */
	public static ImageIcon newImageIcon(String relativeImagePath)
	{
		return newImageIcon(relativeImagePath, true);
	}

	/**
	 * Factory method for create a new {@link ImageIcon}
	 *
	 * @param imagePath
	 *            the image path
	 * @param relativePath
	 *            the flag that indicates if the given path is relative
	 * @return the new {@link ImageIcon}
	 */
	public static ImageIcon newImageIcon(String imagePath, boolean relativePath)
	{
		if(relativePath) {
			InputStream resourceAsStream = ClassExtensions.getResourceAsStream(imagePath);
			final BufferedImage bufferedImage = RuntimeExceptionDecorator
				.decorate(() -> ImageIO.read(resourceAsStream));
			return new ImageIcon(bufferedImage);
		}
		return new ImageIcon(imagePath);
	}
}
