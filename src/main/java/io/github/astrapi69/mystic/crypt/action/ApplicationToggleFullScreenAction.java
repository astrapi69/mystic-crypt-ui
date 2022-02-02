package io.github.astrapi69.mystic.crypt.action;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.swing.action.ToggleFullScreenAction;
import io.github.astrapi69.swing.layout.ScreenSizeExtensions;

public class ApplicationToggleFullScreenAction extends ToggleFullScreenAction
{
	/**
	 * Instantiates a new {@link ToggleFullScreenAction} object.
	 *
	 * @param name
	 *            the name
	 * @param frame
	 *            the frame which will toggle to full screen
	 */
	public ApplicationToggleFullScreenAction(String name, JFrame frame)
	{
		super(name, frame);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		super.actionPerformed(e);
	}
}
