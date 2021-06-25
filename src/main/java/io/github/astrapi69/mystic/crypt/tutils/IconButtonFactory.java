package io.github.astrapi69.mystic.crypt.tutils;

import javax.swing.*;

public class IconButtonFactory
{

	public static JButton newIconButton(Icon icon)
	{
		JButton button = new JButton();
		button.setIcon(icon);
		return button;
	}

	public static JButton newIconButton(Icon icon, String toolTipText)
	{
		JButton button = newIconButton(icon);
		button.setToolTipText(toolTipText);
		return button;
	}

	public static JButton newIconButton(String relativeImagePath, String toolTipText)
	{
		JButton button = newIconButton(
			ImageIconFactory
				.newImageIcon(relativeImagePath), toolTipText);
		return button;
	}

	public static JButton newIconButton(Icon icon, String toolTipText, String text)
	{
		JButton button = newIconButton(icon, toolTipText);
		button.setText(text);
		return button;
	}

	public static JButton newIconButtonWithText(Icon icon, String text)
	{
		JButton button = newIconButton(icon);
		button.setText(text);
		return button;
	}

	public static JButton newJButton(Icon icon, String toolTipText, String text, String actionCommand)
	{
		JButton button = newIconButton(icon, toolTipText, text);
		button.setActionCommand(actionCommand);
		return button;
	}

}
