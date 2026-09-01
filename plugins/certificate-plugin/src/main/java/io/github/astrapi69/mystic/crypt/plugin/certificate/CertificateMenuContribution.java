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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.List;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.pf4j.Extension;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyInfoExtensions;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.mystic.crypt.wizard.CertificateInfoModelToX509;
import io.github.astrapi69.mystic.crypt.wizard.CertificateWizardPanel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.KeyInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.swing.filechooser.JFileChooserExtensions;

/**
 * Contributes the "Create Certificate" tool to the host's "Plugins" menu. The wizard panels and the
 * model-to-certificate generator live in the host (shared with the keygen plugin); this plugin only
 * opens the wizard in a modal dialog and, on Finish, generates the X.509 certificate and saves it.
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
			protected void onFinish()
			{
				super.onFinish();
				generateAndSave(dialog, frame, model);
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

	private static void generateAndSave(JDialog dialog, MysticCryptApplicationFrame frame,
		CertificateInfoModel model)
	{
		try
		{
			X509Certificate certificate = CertificateInfoModelToX509.toX509Certificate(model);
			JFileChooser fileChooser = new JFileChooser(frame.getConfigurationDirectory());
			fileChooser.setDialogTitle("Save the certificate as");
			fileChooser.setFileFilter(new FileNameExtensionFilter("Certificate (*.crt)", "crt"));
			if (fileChooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION)
			{
				File file = JFileChooserExtensions.getSelectedFileWithFirstExtension(fileChooser);
				CertificateWriter.writeInPemFormat(certificate, file);
				offerToOpen(dialog, file);
			}
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
	 * Tells the user the certificate was saved, with the choice to open it right away - there is no
	 * in-app viewer for it, so "open" always goes through whatever the operating system associates
	 * with the file
	 *
	 * @param dialog
	 *            the wizard dialog, as the parent for both this and any error dialog
	 * @param file
	 *            the file that was just written
	 */
	private static void offerToOpen(JDialog dialog, File file)
	{
		Object[] options = { "Open", "OK" };
		int chosen = JOptionPane.showOptionDialog(dialog, "Certificate saved to " + file.getName(),
			"Certificate created", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
			options, options[1]);
		if (chosen != 0)
		{
			return;
		}
		try
		{
			openWithSystemDefault(file);
		}
		catch (IOException exception)
		{
			JOptionPane.showMessageDialog(dialog,
				"Could not open " + file.getName() + ": " + exception.getMessage(), "Open failed",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Opens a file with whatever the operating system associates with it
	 *
	 * @param file
	 *            the file to open
	 * @throws IOException
	 *             if this system has no way to open files from an application, or launching the
	 *             association failed
	 */
	static void openWithSystemDefault(File file) throws IOException
	{
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
		{
			throw new IOException("this system offers no way to open files from an application");
		}
		Desktop.getDesktop().open(file);
	}

	private static CertificateInfoModel newDefaultCertificateInfoModel() throws Exception
	{
		// the wizard follows the configured key algorithm; RSA is merely its default
		KeyPair keyPair = KeyPairFactory.newKeyPair(CertificateSettingsContribution.keyAlgorithm());
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
}
