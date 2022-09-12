package io.github.astrapi69.mystic.crypt;

import io.github.astrapi69.swing.layout.ScreenSizeExtensions;

/**
 * The class {@link StartMysticCryptApplication} without spring-boot
 */
public class StartMysticCryptApplication
{

	/**
	 * The main method that start this {@link MysticCryptApplicationFrame}
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args)
	{
		MysticCryptApplicationFrame frame = new MysticCryptApplicationFrame();
		while (!frame.isVisible())
		{
			ScreenSizeExtensions.showFrame(frame);
		}
	}
}
