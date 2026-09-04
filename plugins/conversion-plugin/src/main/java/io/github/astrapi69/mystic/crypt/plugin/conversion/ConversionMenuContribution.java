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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.pf4j.Extension;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.mystic.crypt.plugin.conversion.wizard.ConversionOperation;
import io.github.astrapi69.mystic.crypt.plugin.conversion.wizard.ConversionReviewPanel;
import io.github.astrapi69.mystic.crypt.plugin.conversion.wizard.ConversionWizardContentPanel;
import io.github.astrapi69.mystic.crypt.plugin.conversion.wizard.ConversionWizardModel;
import io.github.astrapi69.mystic.crypt.plugin.conversion.wizard.ConversionWizardPanel;
import io.github.astrapi69.mystic.crypt.plugin.conversion.wizard.ConversionWizardState;
import io.github.astrapi69.mystic.crypt.plugin.conversion.wizard.TargetPanel;

/**
 * Contributes the conversion wizard to the host's "Plugins" menu, replacing the two former menu
 * items ("Convert DER to PEM" and "Convert key file...") with one guided flow: Source, Target,
 * Review. The wizard panels and its model stay in this plugin (unlike the certificate wizard, this
 * one is not shared with any other plugin); this class only opens it in a modal dialog, keeps the
 * Target and Review steps supplied with what the model currently holds and, on Finish, converts the
 * file.
 */
@Extension
public class ConversionMenuContribution implements PluginMenuContribution
{

	@Override
	public List<JMenuItem> getMenuItems()
	{
		JMenuItem convert = new JMenuItem("Convert Key/Certificate...");
		convert.addActionListener(event -> openWizard());
		return List.of(convert);
	}

	@Override
	public String getMenuName()
	{
		return "Conversion";
	}

	private void openWizard()
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		ConversionWizardModel model = new ConversionWizardModel();

		final JDialog dialog = new JDialog(frame, "Convert Key or Certificate File", true);
		dialog.setName("dlgConversionWizard");
		ConversionWizardPanel wizardPanel = newWizardPanel(dialog, model);
		sizeAndShow(dialog, wizardPanel, frame);
	}

	/**
	 * Builds the wizard panel with the application-specific behavior a plain
	 * {@link ConversionWizardPanel} does not have on its own: refreshing the Target and Review steps
	 * as they are reached, converting the file on Finish and closing the dialog on Cancel
	 *
	 * @param dialog
	 *            the wizard dialog, closed by Cancel and by a successful Finish
	 * @param model
	 *            the wizard's domain model
	 * @return the wizard panel
	 */
	private static ConversionWizardPanel newWizardPanel(JDialog dialog, ConversionWizardModel model)
	{
		return new ConversionWizardPanel(BaseModel.of(model))
		{
			@Override
			protected void onNext()
			{
				super.onNext();
				ConversionWizardState currentState = (ConversionWizardState)getStateMachine()
					.getCurrentState();
				if (currentState == ConversionWizardState.TARGET)
				{
					refreshTarget(this, model);
				}
				else if (currentState == ConversionWizardState.REVIEW)
				{
					refreshReview(this, model);
				}
			}

			@Override
			protected void onFinish()
			{
				super.onFinish();
				finishConversion(dialog, model);
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
	private static void sizeAndShow(JDialog dialog, ConversionWizardPanel wizardPanel,
		MysticCryptApplicationFrame frame)
	{
		// the wizard scrolls rather than losing its lower half on a screen that cannot hold it
		JScrollPane scrollPane = new JScrollPane(wizardPanel);
		// named so a test can tell this one from the scroll panes inside the steps
		scrollPane.setName("scpConversionWizard");
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

	private static TargetPanel targetPanelOf(ConversionWizardPanel wizardPanel)
	{
		return ((ConversionWizardContentPanel)wizardPanel.getWizardContentPanel()).getTargetPanel();
	}

	private static ConversionReviewPanel reviewPanelOf(ConversionWizardPanel wizardPanel)
	{
		return ((ConversionWizardContentPanel)wizardPanel.getWizardContentPanel()).getReviewPanel();
	}

	/**
	 * Re-evaluates which conversions the Target step offers, since the source file the Source step
	 * detected can have changed since the last visit
	 *
	 * @param wizardPanel
	 *            the wizard, to reach its Target step through
	 * @param model
	 *            what the wizard has collected so far
	 */
	private static void refreshTarget(ConversionWizardPanel wizardPanel, ConversionWizardModel model)
	{
		File sourceFile = blankToNull(model.getSourceFilePath()) == null
			? null
			: new File(model.getSourceFilePath().trim());
		targetPanelOf(wizardPanel).refresh(model.getFileKind(), sourceFile);
	}

	/**
	 * Regenerates the Review step's summary once the user reaches it
	 *
	 * @param wizardPanel
	 *            the wizard, to reach its Review step through
	 * @param model
	 *            what the wizard has collected so far
	 */
	private static void refreshReview(ConversionWizardPanel wizardPanel, ConversionWizardModel model)
	{
		reviewPanelOf(wizardPanel).refresh(buildSummary(model));
	}

	/**
	 * The Review step's summary: source path, what it holds, the chosen conversion and the resolved
	 * destination - everything Finish would act on, spelled out before it does
	 *
	 * @param model
	 *            what the wizard has collected
	 * @return the summary, one field per line
	 */
	static String buildSummary(ConversionWizardModel model)
	{
		String sourcePath = blankToPlaceholder(model.getSourceFilePath());
		String kindDescription = model.getWhatItHolds() == null || model.getWhatItHolds().isBlank()
			? ConversionWizardModel.NOTHING_TO_SAY
			: model.getWhatItHolds();
		String operationLabel = model.getOperation() == null
			? ConversionWizardModel.NOTHING_TO_SAY
			: model.getOperation().getLabel();
		String targetPath;
		try
		{
			File source = requireSourceFile(model.getSourceFilePath());
			ConversionOperation operation = requireOperation(model.getOperation());
			targetPath = resolveTargetFile(source, model.getTargetFilePath(), operation)
				.getAbsolutePath();
		}
		catch (RuntimeException incomplete)
		{
			// requireSourceFile/requireOperation throw when a step has not been completed yet - the
			// summary says so rather than the review step blowing up before Finish was ever pressed
			targetPath = ConversionWizardModel.NOTHING_TO_SAY;
		}
		return String.join("\n", "Source: " + sourcePath, "It holds: " + kindDescription,
			"Conversion: " + operationLabel, "Write to: " + targetPath);
	}

	/**
	 * Converts the file the model describes and closes the wizard. The wizard's Finish button works
	 * from any step, not only Review, so what is missing is reported rather than assumed - and
	 * {@code ConversionSupport} itself refuses to overwrite a file that already exists at the target,
	 * surfaced here the same way
	 *
	 * @param dialog
	 *            the wizard dialog, to close on success and to show an error dialog on top of
	 *            otherwise
	 * @param model
	 *            what the wizard has collected
	 */
	private static void finishConversion(JDialog dialog, ConversionWizardModel model)
	{
		try
		{
			File source = requireSourceFile(model.getSourceFilePath());
			ConversionOperation operation = requireOperation(model.getOperation());
			File target = resolveTargetFile(source, model.getTargetFilePath(), operation);
			operation.execute(source, target);
			dialog.dispose();
		}
		catch (Exception exception)
		{
			JOptionPane.showMessageDialog(dialog,
				"Could not convert the file: " + exception.getMessage(), "Conversion failed",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * The file to convert, as far as the model names one
	 *
	 * @param sourceFilePath
	 *            the source path the model holds
	 * @return the source file
	 */
	static File requireSourceFile(String sourceFilePath)
	{
		if (sourceFilePath == null || sourceFilePath.isBlank())
		{
			throw new IllegalArgumentException("choose a file to convert");
		}
		return new File(sourceFilePath.trim());
	}

	/**
	 * The conversion to apply, as far as the model names one
	 *
	 * @param operation
	 *            the operation the model holds
	 * @return the operation
	 */
	static ConversionOperation requireOperation(ConversionOperation operation)
	{
		if (operation == null)
		{
			throw new IllegalStateException("choose a conversion");
		}
		return operation;
	}

	/**
	 * The file to write the conversion to: what the Target step holds, falling back to the same
	 * default it would have shown had the user left the field blank
	 *
	 * @param source
	 *            the file being converted
	 * @param targetFilePath
	 *            the target path the model holds, possibly blank
	 * @param operation
	 *            the chosen conversion, for its default target file
	 * @return the file to write to
	 */
	static File resolveTargetFile(File source, String targetFilePath, ConversionOperation operation)
	{
		if (targetFilePath != null && !targetFilePath.isBlank())
		{
			return new File(targetFilePath.trim());
		}
		return operation.defaultTargetFile(source);
	}

	private static String blankToPlaceholder(String value)
	{
		return value == null || value.isBlank() ? ConversionWizardModel.NOTHING_TO_SAY : value.trim();
	}

	private static String blankToNull(String value)
	{
		return value == null || value.isBlank() ? null : value;
	}

}
