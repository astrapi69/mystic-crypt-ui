package de.alpharogroup.mystic.crypt.panels.obfuscate.character;

import java.io.IOException;

import javax.swing.JFrame;

import de.alpharogroup.layout.CloseWindow;

public class OperationRulePanelTest
{

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException
	{
		final JFrame frame = new JFrame();
		frame.addWindowListener(new CloseWindow());
		frame.setTitle("RulePanel");

		final OperationRulePanel panel = new OperationRulePanel();
		frame.add(panel);
		frame.setBounds(0, 0, 1280, 650);
		frame.setVisible(true);
	}
}
