package io.github.astrapi69.mystic.crypt.panel.certificate;

import javax.swing.*;

public class NewCertificateInfoPanelTest extends JFrame
{

	public NewCertificateInfoPanelTest()
	{
		// Set the title of the JFrame
		setTitle("New Certificate Info");

		// Set the default close operation
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create an instance of your panel
		NewCertificateInfoPanel panel = new NewCertificateInfoPanel();

		// Add the panel to the frame
		add(panel);

		// Pack the frame to fit the preferred size and layouts of its subcomponents
		pack();

		// Set the location of the frame to the center of the screen
		setLocationRelativeTo(null);
	}

	public static void main(String[] args)
	{
		// Use the event dispatch thread for Swing components
		SwingUtilities.invokeLater(() -> {
			// Create and show the frame
			NewCertificateInfoPanelTest frame = new NewCertificateInfoPanelTest();
			frame.setVisible(true);
		});
	}
}
