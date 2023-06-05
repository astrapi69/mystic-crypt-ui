package io.github.astrapi69.mystic.crypt;

import io.github.astrapi69.swing.layout.ScreenSizeExtensions;

public class StartApplication
{
	public static void main(String[] args)
	{
		MysticCryptApplicationFrame frame = new MysticCryptApplicationFrame();
		while (!frame.isVisible())
		{
			ScreenSizeExtensions.showFrame(frame);
		}
	}
}
