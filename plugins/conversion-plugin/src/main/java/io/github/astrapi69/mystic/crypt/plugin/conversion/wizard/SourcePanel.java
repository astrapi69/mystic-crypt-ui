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
package io.github.astrapi69.mystic.crypt.plugin.conversion.wizard;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionMessages;
import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionSupport;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMTextField;
import net.miginfocom.swing.MigLayout;

/**
 * The wizard's first step: pick the file to convert. Nothing is declared - the file says what it
 * holds, the same auto-detection {@code ConversionPanel} already used, now run on every edit of the
 * source field rather than behind a button. What was found is written into the wizard model so the
 * Target step can decide which conversions make sense.
 */
public class SourcePanel extends BasePanel<BaseWizardStateMachineModel<ConversionWizardModel>>
{

	private static final long serialVersionUID = 1L;

	private JLabel lblHeader;
	private javax.swing.text.JTextComponent lblIntro;
	private JLabel lblSourceFile;
	private JMTextField txtSourceFile;
	private JButton btnBrowseSource;
	/** What a failure is rendered in - the one thing on this panel that is not an answer */
	private static final java.awt.Color FAILURE_COLOUR = new java.awt.Color(0xB0, 0x00, 0x20);

	/** The look-and-feel's own text colour, kept so a later answer goes back to it */
	private java.awt.Color readableColour;

	private JLabel lblWhatItHoldsCaption;
	private JTextField lblWhatItHolds;

	public SourcePanel(IModel<BaseWizardStateMachineModel<ConversionWizardModel>> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblHeader = new JLabel("Source");
		lblIntro = ToolForm.intro(ConversionMessages.getString("conversion.source.intro",
			"Pick the key or certificate file you want to convert. The wizard reads it, says what "
				+ "it found, and offers only the conversions that make sense for that file."));
		lblIntro.setName("lblIntro");
		lblSourceFile = new JLabel("File:");
		txtSourceFile = new JMTextField(38);
		txtSourceFile.setName("txtSourceFile");
		btnBrowseSource = new JButton("...");
		btnBrowseSource.setName("btnBrowseSource");
		btnBrowseSource.addActionListener(event -> onBrowseSource());

		lblWhatItHoldsCaption = new JLabel("It holds:");
		// a text field that looks like a label: the answer here is the one line worth pasting into
		// a bug report, and a JLabel cannot be selected, so it could be read and not copied (#321)
		lblWhatItHolds = new JTextField(ConversionWizardModel.NOTHING_TO_SAY);
		lblWhatItHolds.setName("lblWhatItHolds");
		lblWhatItHolds.setEditable(false);
		lblWhatItHolds.setBorder(null);
		lblWhatItHolds.setOpaque(false);
		lblWhatItHolds.setFont(lblWhatItHoldsCaption.getFont());
		readableColour = lblWhatItHoldsCaption.getForeground();

		ConversionWizardModel domainModel = getModelObject().getModelObject();
		txtSourceFile.setPropertyModel(
			LambdaModel.of(domainModel::getSourceFilePath, domainModel::setSourceFilePath));

		txtSourceFile.setToolTipText(ConversionMessages.getString(
			"conversion.wizard.source.tooltip.source.file",
			"the key or certificate file to convert - its own content says what it holds, nothing has to be chosen"));
		btnBrowseSource.setToolTipText(ConversionMessages
			.getString("conversion.wizard.source.tooltip.browse.button", "choose the file to convert"));
		lblWhatItHolds.setToolTipText(ConversionMessages.getString(
			"conversion.wizard.source.tooltip.what.it.holds", "what the chosen file was found to hold"));

		// the file itself says what it is, so the wizard looks as soon as there is a path - typed,
		// pasted or picked. The document listener runs before the field's own binding is notified,
		// so the edited text is taken from the field here instead of waiting for it
		txtSourceFile.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				onSourceEdited();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				onSourceEdited();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				onSourceEdited();
			}
		});
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		MigLayout migLayout = new MigLayout("wrap 3", "[][grow,fill][]", "[][][][grow]");
		setLayout(migLayout);

		add(lblHeader, "span, align center, gapbottom 10");
		add(lblIntro, "span, growx, wmin 0, gapbottom 10");
		add(lblSourceFile);
		add(txtSourceFile, "growx");
		add(btnBrowseSource);
		add(lblWhatItHoldsCaption);
		add(lblWhatItHolds, "span 2, growx");
	}

	/**
	 * Carries an edit of the source field into the model and looks at the file it now names
	 */
	private void onSourceEdited()
	{
		ConversionWizardModel domainModel = getModelObject().getModelObject();
		domainModel.setSourceFilePath(txtSourceFile.getText());
		detectSource(domainModel);
	}

	/**
	 * Looks at the chosen file and records what it holds - or why it could not be read - so the
	 * Target step knows which conversions to offer
	 *
	 * @param domainModel
	 *            the wizard's domain model
	 */
	private void detectSource(ConversionWizardModel domainModel)
	{
		String path = domainModel.getSourceFilePath() == null
			? ""
			: domainModel.getSourceFilePath().trim();
		if (path.isEmpty())
		{
			showWhatItHolds(domainModel, ConversionWizardModel.NOTHING_TO_SAY, null, false);
			return;
		}
		try
		{
			ConversionSupport.FileKind kind = ConversionSupport.kindOf(new File(path));
			showWhatItHolds(domainModel, kind.description(), kind, false);
		}
		catch (Exception exception)
		{
			showWhatItHolds(domainModel, "not read: " + message(exception), null, true);
		}
	}

	/**
	 * Puts the answer on screen, in red when the file could not be read at all.
	 * <p>
	 * A failure used to be rendered in the same colour as "this holds an RSA private key", so the
	 * one line that says something went wrong looked exactly like the one that says everything is
	 * fine (#321). The colour is the signal; the text field above is what makes it copyable.
	 *
	 * @param domainModel
	 *            the wizard's domain model
	 * @param description
	 *            what to show
	 * @param kind
	 *            what the file was found to be, or null when it could not be read
	 * @param failed
	 *            whether this description is a failure rather than an answer
	 */
	private void showWhatItHolds(ConversionWizardModel domainModel, String description,
		ConversionSupport.FileKind kind, boolean failed)
	{
		domainModel.setFileKind(kind);
		domainModel.setWhatItHolds(description);
		lblWhatItHolds.setText(description);
		lblWhatItHolds.setForeground(failed ? FAILURE_COLOUR : readableColour);
		lblWhatItHolds.setCaretPosition(0);
	}

	private void onBrowseSource()
	{
		ConversionWizardModel domainModel = getModelObject().getModelObject();
		String currentPath = domainModel.getSourceFilePath() == null
			? ""
			: domainModel.getSourceFilePath().trim();
		JFileChooser fileChooser = new JFileChooser();
		if (!currentPath.isEmpty())
		{
			fileChooser.setSelectedFile(new File(currentPath));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			txtSourceFile.setText(fileChooser.getSelectedFile().getAbsolutePath());
		}
	}

	private static String message(Exception exception)
	{
		return exception.getMessage() != null
			? exception.getMessage()
			: exception.getClass().getSimpleName();
	}
}
