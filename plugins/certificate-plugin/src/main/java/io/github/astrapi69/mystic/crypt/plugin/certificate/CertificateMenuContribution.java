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
package io.github.astrapi69.mystic.crypt.plugin.certificate;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.List;

import javax.swing.*;

import org.pf4j.Extension;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.factory.KeyPairGeneratorFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.mystic.crypt.wizard.CertificateInfoModelToX509;
import io.github.astrapi69.mystic.crypt.wizard.CertificateWizardContentPanel;
import io.github.astrapi69.mystic.crypt.wizard.CertificateWizardPanel;
import io.github.astrapi69.mystic.crypt.wizard.ReviewPanel;
import io.github.astrapi69.mystic.crypt.wizard.ReviewPanelModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;

/**
 * Contributes the "Create Certificate" tool to the host's "Plugins" menu. The wizard panels and the
 * model-to-certificate generator live in the host (shared with the keygen plugin); this plugin only
 * opens the wizard in a modal dialog, keeps the wizard's Review step supplied with a preview of what
 * would be generated and, on Finish, generates the X.509 certificate and saves it.
 */
@Extension
public class CertificateMenuContribution implements PluginMenuContribution
{

	@Override
	public List<JMenuItem> getMenuItems()
	{
		JMenuItem createCertificate = new JMenuItem("Create Certificate...");
		createCertificate.addActionListener(event -> openWizard());
		return List.of(createCertificate);
	}

	@Override
	public String getMenuName()
	{
		return "Certificate";
	}

	private void openWizard()
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		final CertificateInfoModel model;
		try
		{
			model = newDefaultCertificateInfoModel();
		}
		catch (Exception exception)
		{
			JOptionPane.showMessageDialog(frame,
				"Could not initialize the certificate wizard: " + exception.getMessage(),
				"Certificate wizard failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final JDialog dialog = new JDialog(frame, "Create Certificate", true);
		dialog.setName("dlgCertificateWizard");
		CertificateWizardPanel wizardPanel = new CertificateWizardPanel(BaseModel.of(model))
		{
			@Override
			protected void onNext()
			{
				super.onNext();
				if (getStateMachine().getCurrentState() == CertificateWizardState.REVIEW)
				{
					refreshReview(this, model, frame);
				}
			}

			@Override
			protected void onFinish()
			{
				super.onFinish();
				saveReviewedCertificate(dialog, frame, reviewPanelOf(this), model);
			}

			@Override
			protected void onCancel()
			{
				super.onCancel();
				dialog.dispose();
			}
		};
		// the wizard scrolls rather than losing its lower half on a screen that cannot hold it
		JScrollPane scrollPane = new JScrollPane(wizardPanel);
		// named so a test can tell this one from the scroll panes inside the steps
		scrollPane.setName("scpCertificateWizard");
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
		// the size that holds every step, not the one the first step happens to need: the window is
		// opened once and must not resize itself while the user walks through the wizard.
		// pack() is used instead of a guessed inset constant so the title bar and dialog border are
		// whatever the window manager actually gives them, not a fixed number tuned against one
		// environment and short everywhere else (the scroll bar this dialog carries for exactly that
		// case turned out to still be needed on the tallest step - see #93 follow-up).
		// preferredSizeForEveryStep() is asked only after a first pack(): a step measured before the
		// dialog has a real display connection gets its text laid out with approximated font metrics,
		// which comes out a little short of what the same step asks for once realized
		dialog.pack();
		scrollPane.setPreferredSize(wizardPanel.preferredSizeForEveryStep());
		dialog.pack();
		dialog.setSize(
			WizardWindowSize.on(dialog.getSize(), Toolkit.getDefaultToolkit().getScreenSize()));
		dialog.setMinimumSize(new Dimension(WizardWindowSize.MINIMUM_WIDTH / 2,
			WizardWindowSize.MINIMUM_HEIGHT / 2));
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	private static ReviewPanel reviewPanelOf(CertificateWizardPanel wizardPanel)
	{
		return ((CertificateWizardContentPanel)wizardPanel.getWizardContentPanel())
			.getReviewPanel();
	}

	/**
	 * Regenerates the review step's preview and defaults once the user reaches it. Generating the
	 * certificate can fail - an earlier step left incomplete, a key that cannot use the configured
	 * signature algorithm - in which case the preview says why instead of leaving the wizard on a
	 * blank step with no feedback at all.
	 *
	 * @param wizardPanel
	 *            the wizard, to reach its review step through
	 * @param model
	 *            what the wizard has collected so far
	 * @param frame
	 *            the application frame, for the default save directory
	 */
	private static void refreshReview(CertificateWizardPanel wizardPanel, CertificateInfoModel model,
		MysticCryptApplicationFrame frame)
	{
		String preview;
		try
		{
			X509Certificate certificate = CertificateInfoModelToX509.toX509Certificate(model);
			preview = certificateSummary(certificate) + "\n\n" + toPemString(certificate);
		}
		catch (Exception exception)
		{
			preview = "Could not generate the certificate: " + exception.getMessage();
		}
		reviewPanelOf(wizardPanel).refresh(preview, defaultFileName(model),
			frame.getConfigurationDirectory());
	}

	/**
	 * Writes the certificate the review step describes and closes the wizard. The wizard's Finish
	 * button works from any step, not only Review, so the file name and directory the review step
	 * would have defaulted to are recomputed here rather than trusted to already be filled in.
	 *
	 * @param dialog
	 *            the wizard dialog, to close on success and to show an error dialog on top of
	 *            otherwise
	 * @param frame
	 *            the application frame, for the default save directory
	 * @param reviewPanel
	 *            the review step, for what the user entered there
	 * @param model
	 *            what the wizard has collected
	 */
	private static void saveReviewedCertificate(JDialog dialog, MysticCryptApplicationFrame frame,
		ReviewPanel reviewPanel, CertificateInfoModel model)
	{
		ReviewPanelModel reviewFormModel = reviewPanel.getReviewFormModel();
		File file = resolveSaveTarget(reviewFormModel.getFileName(),
			reviewFormModel.getSaveDirectory(), model, frame.getConfigurationDirectory());
		try
		{
			requireFreeFile(file);
			X509Certificate certificate = CertificateInfoModelToX509.toX509Certificate(model);
			CertificateWriter.writeInPemFormat(certificate, file);
			dialog.dispose();
		}
		catch (Exception exception)
		{
			JOptionPane.showMessageDialog(dialog,
				"Could not create the certificate: " + exception.getMessage(), "Certificate failed",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Refuses a path that already holds something, because writing a certificate there would
	 * silently truncate whatever was in it. Mirrors the same guard {@code KeyStorePanel} and
	 * {@code SecretSharingPanel} already use for their own save targets (#180).
	 *
	 * @param file
	 *            the resolved save target
	 * @throws IllegalStateException
	 *             if the file already exists
	 */
	static void requireFreeFile(File file)
	{
		if (file.exists())
		{
			throw new IllegalStateException(
				"'" + file.getName() + "' already exists - pick another name or remove it first");
		}
	}

	/**
	 * The file the certificate is saved to: what the review step holds, falling back to the same
	 * defaults it would have shown had the user visited it
	 *
	 * @param fileName
	 *            the file name from the review step, possibly blank
	 * @param directory
	 *            the directory from the review step, possibly {@code null}
	 * @param model
	 *            what the wizard collected, to derive a default file name from
	 * @param defaultDirectory
	 *            the directory to fall back to
	 * @return the file to save the certificate to
	 */
	static File resolveSaveTarget(String fileName, File directory, CertificateInfoModel model,
		File defaultDirectory)
	{
		String effectiveFileName = fileName == null || fileName.isBlank()
			? defaultFileName(model)
			: fileName;
		File effectiveDirectory = directory != null ? directory : defaultDirectory;
		return new File(effectiveDirectory, effectiveFileName);
	}

	/**
	 * The file name the review step starts with: the subject's common name, sanitized to what every
	 * filesystem this application runs on accepts
	 *
	 * @param model
	 *            what the wizard collected
	 * @return the default file name, always ending in {@code .crt}
	 */
	static String defaultFileName(CertificateInfoModel model)
	{
		String commonName = model.getSubject() == null ? null : model.getSubject().getCommonName();
		String base = commonName == null || commonName.isBlank()
			? "certificate"
			: commonName.trim().replaceAll("[^a-zA-Z0-9._-]+", "_");
		return base + ".crt";
	}

	private static String toPemString(X509Certificate certificate) throws Exception
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		CertificateWriter.writeInPemFormat(certificate, buffer);
		return buffer.toString(StandardCharsets.US_ASCII);
	}

	/**
	 * The certificate's important fields, one per line: subject, issuer, serial number (hex, the
	 * way a certificate viewer reports it), validity window, signature algorithm and public key
	 * algorithm - everything a reviewer needs to see what would actually be created, not just where
	 * it would go (#136)
	 *
	 * @param certificate
	 *            the certificate to summarize
	 * @return the summary, one field per line
	 */
	static String certificateSummary(X509Certificate certificate)
	{
		return String.join("\n", "Subject: " + certificate.getSubjectX500Principal().getName(),
			"Issuer: " + certificate.getIssuerX500Principal().getName(),
			"Serial: " + certificate.getSerialNumber().toString(16),
			"Valid from: " + certificate.getNotBefore(),
			"Valid until: " + certificate.getNotAfter(),
			"Signature algorithm: " + certificate.getSigAlgName(),
			"Public key algorithm: " + certificate.getPublicKey().getAlgorithm());
	}

	private static CertificateInfoModel newDefaultCertificateInfoModel() throws Exception
	{
		// the wizard follows the configured key algorithm; RSA is merely its default
		KeyPair keyPair = newCertificateKeyPair(CertificateSettingsContribution.keyAlgorithm());
		KeyInfoModel privateKeyInfo = KeyInfoModel
			.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPrivate()));
		KeyInfoModel publicKeyInfo = KeyInfoModel
			.toKeyInfoModel(KeyInfoExtensions.toKeyInfo(keyPair.getPublic()));
		ZonedDateTime now = ZonedDateTime.now();
		return CertificateInfoModel.builder().privateKeyInfo(privateKeyInfo)
			.publicKeyInfo(publicKeyInfo)
			// the wizard starts with what the user configured in the settings dialog
			.issuer(DistinguishedNameInfoModel.builder()
				.commonName(CertificateSettingsContribution.commonName()).build())
			.subject(DistinguishedNameInfoModel.builder()
				.commonName(CertificateSettingsContribution.commonName()).build())
			.validityModel(ValidityModel.builder().notBefore(now)
				.notAfter(now.plusYears(CertificateSettingsContribution.validityYears())).build())
			.serial(BigInteger.valueOf(now.toInstant().toEpochMilli()))
			.signatureAlgorithm(CertificateSettingsContribution.signatureAlgorithm()).build();
	}

	/**
	 * Generates a key pair for the given algorithm the way each actually needs to be generated -
	 * RSA and DSA take an explicit classical bit size, but EC, Ed25519 and every other
	 * fixed-parameter algorithm have no such size and crash if forced through
	 * {@code KeyPairFactory}'s size-defaulting convenience method (a default of 2048 makes no
	 * sense for a curve or a fixed-parameter algorithm -
	 * {@code java.security.InvalidParameterException: Unsupported size: 2048} for Ed25519, #141).
	 * Mirrors the same distinction {@code keygen-plugin}'s key generator already draws.
	 *
	 * @param keyAlgorithm
	 *            the key algorithm, as {@link CertificateSettingsContribution} documents it: RSA,
	 *            EC, DSA or Ed25519
	 * @return the generated key pair
	 * @throws Exception
	 *             if this machine cannot generate a key pair for the given algorithm
	 */
	static KeyPair newCertificateKeyPair(final String keyAlgorithm) throws Exception
	{
		if ("RSA".equalsIgnoreCase(keyAlgorithm) || "DSA".equalsIgnoreCase(keyAlgorithm))
		{
			return KeyPairFactory.newKeyPair(keyAlgorithm);
		}
		return KeyPairGeneratorFactory.newKeyPairGenerator(keyAlgorithm).generateKeyPair();
	}
}
