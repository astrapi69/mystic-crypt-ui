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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.security.KeyStore;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.pf4j.Extension;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.mystic.crypt.plugin.keystore.wizard.CreateKeyStoreReviewPanel;
import io.github.astrapi69.mystic.crypt.plugin.keystore.wizard.CreateKeyStoreWizardContentPanel;
import io.github.astrapi69.mystic.crypt.plugin.keystore.wizard.CreateKeyStoreWizardModel;
import io.github.astrapi69.mystic.crypt.plugin.keystore.wizard.CreateKeyStoreWizardPanel;
import io.github.astrapi69.mystic.crypt.plugin.keystore.wizard.CreateKeyStoreWizardState;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.util.JInternalFrameExtensions;

/**
 * Contributes the key store tools to the host's "Plugins" menu: the existing dense "Manage Key
 * Store" panel (unchanged) for working an existing store, and the guided "Create Key Store..."
 * wizard (issue #191) for the specific job of creating a brand-new one - optionally with one key
 * pair in it - split the same way the conversion plugin's wizard was split from its old two-button
 * menu (issue #182).
 */
@Extension
public class KeyStoreMenuContribution implements PluginMenuContribution
{

	@Override
	public List<JMenuItem> getMenuItems()
	{
		JMenuItem manageKeyStore = new JMenuItem("Manage Key Store");
		manageKeyStore.addActionListener(
			event -> openInternalFrame("Manage Key Store", new KeyStorePanel()));
		JMenuItem createKeyStore = new JMenuItem("Create Key Store...");
		createKeyStore.addActionListener(event -> openCreateWizard());
		return List.of(manageKeyStore, createKeyStore);
	}

	@Override
	public String getMenuName()
	{
		return "Key Stores";
	}

	private void openInternalFrame(String title, Component panel)
	{
		MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
		if (!FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
		{
			instance.switchToDesktopPane();
		}
		JInternalFrame internalFrame = JComponentFactory.newInternalFrame(title, true, true, true,
			true);
		JInternalFrameExtensions.addInternalFrameToMainFrame(panel, internalFrame, instance);
	}

	private void openCreateWizard()
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();

		final JDialog dialog = new JDialog(frame, "Create Key Store", true);
		dialog.setName("dlgCreateKeyStoreWizard");
		CreateKeyStoreWizardPanel wizardPanel = newWizardPanel(dialog, model);
		sizeAndShow(dialog, wizardPanel, frame);
	}

	/**
	 * Builds the wizard panel with the application-specific behavior a plain
	 * {@link CreateKeyStoreWizardPanel} does not have on its own: refreshing the Review step as it is
	 * reached, creating the store on Finish and closing the dialog on Cancel. Defined as an instance
	 * method (not static, unlike the conversion wizard's otherwise identical helper) since a
	 * successful Finish hands off to {@link #openInternalFrame(String, Component)}.
	 *
	 * @param dialog
	 *            the wizard dialog, closed by Cancel and by a successful Finish
	 * @param model
	 *            the wizard's domain model
	 * @return the wizard panel
	 */
	private CreateKeyStoreWizardPanel newWizardPanel(JDialog dialog, CreateKeyStoreWizardModel model)
	{
		return new CreateKeyStoreWizardPanel(BaseModel.of(model))
		{
			@Override
			protected void onNext()
			{
				super.onNext();
				CreateKeyStoreWizardState currentState = (CreateKeyStoreWizardState)getStateMachine()
					.getCurrentState();
				if (currentState == CreateKeyStoreWizardState.REVIEW)
				{
					refreshReview(this, model);
				}
			}

			@Override
			protected void onFinish()
			{
				super.onFinish();
				finishCreateKeyStore(dialog, model);
			}

			@Override
			protected void onCancel()
			{
				super.onCancel();
				dialog.dispose();
			}
		};
	}

	/**
	 * Sizes the dialog to what the wizard actually needs and shows it. The window is opened once and
	 * must not resize itself while the user walks through the wizard - see
	 * {@code CertificateMenuContribution.openWizard} for why {@code pack()} runs twice
	 *
	 * @param dialog
	 *            the wizard dialog
	 * @param wizardPanel
	 *            the wizard panel the dialog shows
	 * @param frame
	 *            the application frame, to center the dialog over
	 */
	private static void sizeAndShow(JDialog dialog, CreateKeyStoreWizardPanel wizardPanel,
		MysticCryptApplicationFrame frame)
	{
		// the wizard scrolls rather than losing its lower half on a screen that cannot hold it
		JScrollPane scrollPane = new JScrollPane(wizardPanel);
		// named so a test can tell this one from the scroll panes inside the steps
		scrollPane.setName("scpCreateKeyStoreWizard");
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
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

	private static CreateKeyStoreReviewPanel reviewPanelOf(CreateKeyStoreWizardPanel wizardPanel)
	{
		return ((CreateKeyStoreWizardContentPanel)wizardPanel.getWizardContentPanel())
			.getReviewPanel();
	}

	/**
	 * Regenerates the Review step's summary once the user reaches it
	 *
	 * @param wizardPanel
	 *            the wizard, to reach its Review step through
	 * @param model
	 *            what the wizard has collected so far
	 */
	private static void refreshReview(CreateKeyStoreWizardPanel wizardPanel,
		CreateKeyStoreWizardModel model)
	{
		reviewPanelOf(wizardPanel).refresh(buildSummary(model));
	}

	/**
	 * The Review step's summary: the file, its type and - when one was configured - the first key
	 * pair's alias, subject and algorithm, or a note that the store will be empty otherwise
	 *
	 * @param model
	 *            what the wizard has collected
	 * @return the summary, one field per line
	 */
	static String buildSummary(CreateKeyStoreWizardModel model)
	{
		String filePath = blankToPlaceholder(model.getKeyStoreFilePath());
		String type = model.getKeystoreType() == null ? "-" : model.getKeystoreType().toString();
		String entry = model.isAddKeyPairNow() ? keyPairSummary(model) : "no key pair - an empty store"
			+ " will be created";
		return String.join("\n", "File: " + filePath, "Type: " + type, entry);
	}

	private static String keyPairSummary(CreateKeyStoreWizardModel model)
	{
		String algorithm = model.getKeyAlgorithm() == null ? "-" : model.getKeyAlgorithm().toString();
		return String.join("\n", "Alias: " + blankToPlaceholder(model.getAlias()),
			"Distinguished name: " + blankToPlaceholder(model.getDistinguishedName()),
			"Key algorithm: " + algorithm);
	}

	/**
	 * Creates the store the model describes and closes the wizard. The wizard's Finish button works
	 * from any step, not only Review, so an existing file at the target is refused the same way
	 * {@code KeyStorePanel.onCreate()} refuses it - through the shared
	 * {@link KeyStoreSupport#requireFreeFile(File)} guard
	 *
	 * @param dialog
	 *            the wizard dialog, to close on success and to show an error dialog on top of
	 *            otherwise
	 * @param model
	 *            what the wizard has collected
	 */
	private void finishCreateKeyStore(JDialog dialog, CreateKeyStoreWizardModel model)
	{
		try
		{
			File file = new File(model.getKeyStoreFilePath().trim());
			KeyStoreSupport.requireFreeFile(file);
			String password = new String(model.getStorePassword());
			KeyStore keyStore = KeyStoreSupport.create(file, model.getKeystoreType(), password);
			if (model.isAddKeyPairNow())
			{
				KeyStoreSupport.addKeyPair(keyStore, file, password, model.getAlias(),
					model.getDistinguishedName(), model.getKeyAlgorithm());
			}
			dialog.dispose();
			openInternalFrame("Manage Key Store", new KeyStorePanel());
		}
		catch (Exception exception)
		{
			JOptionPane.showMessageDialog(dialog,
				"Could not create the key store: " + exception.getMessage(),
				"Key store creation failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	private static String blankToPlaceholder(String value)
	{
		return value == null || value.isBlank() ? "-" : value.trim();
	}

}
