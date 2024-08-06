package io.github.astrapi69.mystic.crypt.wizard;

import java.awt.*;

import javax.swing.*;

import io.github.astrapi69.crypt.data.model.X509CertificateV3Info;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;
import io.github.astrapi69.swing.wizard.NavigationPanel;

public class CertificateWizardPanelTest
{

	public static void main(String[] args)
	{
		// Set up the frame for the demo
		JFrame frame = new JFrame("Certificate Wizard Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		final CertificateInfoModel certificateInfoModel = CertificateInfoModel.builder().build();
		// Create the wizard state machine model
		BaseWizardStateMachineModel<CertificateInfoModel> stateMachineModel = BaseWizardStateMachineModel
			.<CertificateInfoModel> builder().currentState(CertificateWizardState.ISSUER)
			.modelObject(certificateInfoModel).build();
		// Create the wizard panel
		CertificateWizardPanel wizardPanel = new CertificateWizardPanel(
			BaseModel.of(certificateInfoModel));
		wizardPanel.setStateMachine(stateMachineModel);

		// Set up the frame content
		frame.getContentPane().add(wizardPanel, BorderLayout.CENTER);

		// Set initial state
		CardLayout cardLayout = (CardLayout)wizardPanel.getWizardContentPanel().getLayout();
		cardLayout.show(wizardPanel.getWizardContentPanel(), CertificateWizardState.ISSUER.name());

		// Make the frame visible
		frame.setVisible(true);

		// Simulate button clicks for demo purposes
		// simulateButtonClicks(wizardPanel.getNavigationPanel());
	}

	private static void simulateButtonClicks(
		NavigationPanel<BaseWizardStateMachineModel<X509CertificateV3Info>> navigationPanel)
	{
		SwingUtilities.invokeLater(() -> {
			try
			{
				// Wait for a moment to let the UI render
				Thread.sleep(2000);

				// Simulate clicking "Next" button
				navigationPanel.getBtnNext().doClick();

				// Wait for a moment to see the transition
				Thread.sleep(2000);

				// Simulate clicking "Next" button again
				navigationPanel.getBtnNext().doClick();

				// Wait for a moment to see the transition
				Thread.sleep(2000);

				// Simulate clicking "Next" button again
				navigationPanel.getBtnNext().doClick();

				// Wait for a moment to see the transition
				Thread.sleep(2000);

				// Simulate clicking "Next" button again to final step
				navigationPanel.getBtnNext().doClick();

				// Wait for a moment to see the final state
				Thread.sleep(2000);

				// Simulate clicking "Previous" button to go back
				navigationPanel.getBtnPrevious().doClick();

			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		});
	}
}
