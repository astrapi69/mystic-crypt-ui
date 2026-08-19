/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.wizard;

import java.awt.*;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.ZonedDateTime;

import javax.swing.*;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.crypt.data.model.X509CertificateV3Info;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;
import io.github.astrapi69.swing.wizard.NavigationPanel;

public class CertificateWizardPanelTest
{

	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();
		KeyInfoModel privateKeyInfoModel = KeyInfoModel
			.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(privateKey));
		KeyInfoModel publicKeyInfoModel = KeyInfoModel
			.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(publicKey));
		ZonedDateTime now = ZonedDateTime.now();
		ValidityModel validityModel = ValidityModel.builder().notBefore(now)
			.notAfter(now.plusYears(1)).build();
		// Set up the frame for the demo
		JFrame frame = new JFrame("Certificate Wizard Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		final CertificateInfoModel certificateInfoModel = CertificateInfoModel.builder()
			.publicKeyInfo(publicKeyInfoModel).privateKeyInfo(privateKeyInfoModel)
			.issuer(DistinguishedNameInfoModel.builder().build())
			.subject(DistinguishedNameInfoModel.builder().build()).signatureAlgorithm("SHA1withRSA")
			.validityModel(validityModel).build();
		// Create the wizard state machine model
		BaseWizardStateMachineModel<CertificateInfoModel> stateMachineModel = BaseWizardStateMachineModel
			.<CertificateInfoModel> builder().currentState(CertificateWizardState.ISSUER)
			.modelObject(certificateInfoModel).build();
		// Create the wizard panel
		CertificateWizardPanel wizardPanel = new CertificateWizardPanel(
			BaseModel.of(certificateInfoModel));

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
