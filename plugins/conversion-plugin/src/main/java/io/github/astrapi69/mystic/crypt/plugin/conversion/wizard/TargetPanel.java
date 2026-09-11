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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionMessages;
import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionSupport;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMTextField;
import net.miginfocom.swing.MigLayout;

/**
 * The wizard's second step: choose which of the four conversions to apply and where to write the
 * result. Only the conversions valid for what the Source step detected are enabled, mirroring
 * {@code ConversionPanel.setConversionsFor(...)}. Since the source file can change if the user goes
 * back to Source and picks a different one, this step is refreshed explicitly by whoever opens the
 * wizard, through {@link #refresh(ConversionSupport.FileKind, File)}, every time it is reached.
 */
public class TargetPanel extends BasePanel<BaseWizardStateMachineModel<ConversionWizardModel>>
{

	private static final long serialVersionUID = 1L;

	private JLabel lblHeader;
	private javax.swing.text.JTextComponent lblIntro;
	private JLabel lblWhatItHoldsCaption;
	private JLabel lblWhatItHolds;

	/**
	 * Populated in {@link #onInitializeComponents()}, not by a field initializer: the base panel
	 * calls that method from its own constructor, before any field initializer of this class has run
	 * (the same reason {@code ReviewPanel} keeps its own form model out of a field initializer)
	 */
	private Map<ConversionOperation, JRadioButton> operationButtons;
	private ButtonGroup operationGroup;
	private JLabel lblSourceFileCaption;
	private JTextField txtSourceFileOnTarget;
	private JLabel lblTargetFile;
	private JMTextField txtTargetFile;

	/**
	 * Whether the path in the target field is one this panel derived rather than one the user typed
	 * or chose. Only a derived path is replaced when the operation changes (#297)
	 */
	private boolean targetWasDerived;
	private JButton btnBrowseTarget;

	public TargetPanel(IModel<BaseWizardStateMachineModel<ConversionWizardModel>> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		initializeWhatItHoldsLabel();
		initializeOperationRadios();
		initializeTargetField();
	}

	private void initializeWhatItHoldsLabel()
	{
		lblHeader = new JLabel("Target");
		lblIntro = ToolForm.intro(ConversionMessages.getString("conversion.target.intro",
			"Choose what to convert the file into and where to write the result. A format the "
				+ "source cannot produce is refused rather than written in another shape, and an "
				+ "existing file is never overwritten."));
		lblIntro.setName("lblIntro");
		lblWhatItHoldsCaption = new JLabel("It holds:");
		lblWhatItHolds = new JLabel(ConversionWizardModel.NOTHING_TO_SAY);
		lblWhatItHolds.setName("lblTargetWhatItHolds");
		lblWhatItHolds.setToolTipText(ConversionMessages.getString(
			"conversion.wizard.target.tooltip.what.it.holds",
			"what the source file was found to hold"));
		// which file is being converted. The step showed what the source holds and where to write
		// to, and never the source itself - and a target path can look exactly like a source path,
		// so the one question a reader had was the one the screen did not answer (#297). Read-only:
		// the source is chosen in the previous step, and offering to change it here would be a
		// second place to do the same thing
		lblSourceFileCaption = new JLabel("From file:");
		txtSourceFileOnTarget = new JTextField();
		txtSourceFileOnTarget.setName("txtSourceFileOnTarget");
		txtSourceFileOnTarget.setEditable(false);
		txtSourceFileOnTarget.setBorder(null);
		txtSourceFileOnTarget.setOpaque(false);
		txtSourceFileOnTarget.setFont(lblWhatItHolds.getFont());
		txtSourceFileOnTarget.setToolTipText(ConversionMessages.getString(
			"conversion.wizard.target.tooltip.source.file",
			"the file being converted, chosen in the previous step"));
	}

	private void initializeOperationRadios()
	{
		operationButtons = new LinkedHashMap<>();
		operationGroup = new ButtonGroup();
		for (ConversionOperation operation : ConversionOperation.values())
		{
			JRadioButton radioButton = new JRadioButton(operation.getLabel());
			radioButton.setName("rdo" + capitalize(operation));
			radioButton.setEnabled(false);
			radioButton.setToolTipText(ConversionMessages.getString(
				"conversion.wizard.target.tooltip.operation." + operation.name().toLowerCase(),
				operation.getLabel()));
			radioButton.addActionListener(event -> onOperationSelected(operation));
			operationGroup.add(radioButton);
			operationButtons.put(operation, radioButton);
		}
	}

	private void initializeTargetField()
	{
		lblTargetFile = new JLabel("Write to:");
		txtTargetFile = new JMTextField(38);
		txtTargetFile.setName("txtTargetFile");
		txtTargetFile.setToolTipText(ConversionMessages.getString(
			"conversion.wizard.target.tooltip.target.file",
			"the file to write to - left blank, a default next to the source file is used"));
		btnBrowseTarget = new JButton("...");
		btnBrowseTarget.setName("btnBrowseTarget");
		btnBrowseTarget.addActionListener(event -> onBrowseTarget());
		btnBrowseTarget.setToolTipText(ConversionMessages.getString(
			"conversion.wizard.target.tooltip.browse.button", "choose where to write the result"));

		ConversionWizardModel domainModel = getModelObject().getModelObject();
		txtTargetFile.getDocument()
			.addDocumentListener(new TargetFieldListener(txtTargetFile, domainModel));
	}

	/**
	 * Carries every edit of the target field into the model, the same way {@code SourcePanel} carries
	 * edits of the source field - a plain {@link javax.swing.event.DocumentListener} rather than the
	 * field's own binding, since the model needs the value the moment it changes, not on focus loss
	 */
	private final class TargetFieldListener implements javax.swing.event.DocumentListener
	{
		private final JMTextField targetField;
		private final ConversionWizardModel domainModel;

		private TargetFieldListener(JMTextField targetField, ConversionWizardModel domainModel)
		{
			this.targetField = targetField;
			this.domainModel = domainModel;
		}

		/**
		 * Every edit carries into the model, and every edit also means the path is no longer one
		 * this panel derived. {@code setDerivedTarget} sets that flag back AFTER its own setText,
		 * so the panel's own writes end up marked as derived and a user's keystrokes do not (#297)
		 */
		private void carry()
		{
			domainModel.setTargetFilePath(targetField.getText());
			targetWasDerived = false;
		}

		@Override
		public void insertUpdate(javax.swing.event.DocumentEvent event)
		{
			carry();
		}

		@Override
		public void removeUpdate(javax.swing.event.DocumentEvent event)
		{
			carry();
		}

		@Override
		public void changedUpdate(javax.swing.event.DocumentEvent event)
		{
			carry();
		}
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		MigLayout migLayout = new MigLayout("wrap 3", "[][grow,fill][]", "[][][][][][][grow]");
		setLayout(migLayout);

		add(lblHeader, "span, align center, gapbottom 10");
		add(lblIntro, "span, growx, wmin 0, gapbottom 10");
		add(lblSourceFileCaption);
		add(txtSourceFileOnTarget, "span 2, growx, wmin 0");
		add(lblWhatItHoldsCaption);
		add(lblWhatItHolds, "span 2, growx");
		for (JRadioButton radioButton : operationButtons.values())
		{
			add(radioButton, "span 3");
		}
		add(lblTargetFile);
		add(txtTargetFile, "growx");
		add(btnBrowseTarget);
	}

	/**
	 * Re-evaluates which conversions make sense for the source file now, and re-shows what it holds.
	 * Called by whoever opens the wizard whenever this step is reached, since the source file can
	 * have changed since the last visit (the user went back to Source and picked a different file)
	 *
	 * @param fileKind
	 *            what the source file currently holds, as the Source step detected it
	 * @param sourceFile
	 *            the source file itself
	 */
	public void refresh(ConversionSupport.FileKind fileKind, File sourceFile)
	{
		ConversionWizardModel domainModel = getModelObject().getModelObject();
		lblWhatItHolds.setText(domainModel.getWhatItHolds());
		String sourcePath = domainModel.getSourceFilePath();
		txtSourceFileOnTarget.setText(sourcePath == null ? "" : sourcePath.trim());
		txtSourceFileOnTarget.setCaretPosition(0);

		boolean selectionBecameInvalid = false;
		for (Map.Entry<ConversionOperation, JRadioButton> entry : operationButtons.entrySet())
		{
			ConversionOperation operation = entry.getKey();
			JRadioButton radioButton = entry.getValue();
			boolean valid = operation.isValidFor(fileKind, sourceFile);
			radioButton.setEnabled(valid);
			if (!valid && radioButton.isSelected())
			{
				selectionBecameInvalid = true;
			}
		}
		if (selectionBecameInvalid)
		{
			// a JRadioButton inside a ButtonGroup ignores setSelected(false) on itself - only the
			// group can clear a selection once one of its buttons was chosen
			operationGroup.clearSelection();
			domainModel.setOperation(null);
		}
	}

	/**
	 * Records the chosen operation and re-derives the default target for it.
	 * <p>
	 * The default is derived per operation - {@code <name>-pkcs1.pem}, {@code <name>.der} - and it
	 * used to be written only into a BLANK field. So choosing PKCS#1 after DER left a path ending
	 * in {@code .der} standing while the wizard converted to PKCS#1, and that stale path is exactly
	 * what made the screen unreadable in the report behind #297.
	 * <p>
	 * It now replaces a path this panel derived, and never one the user typed or picked with the
	 * browse button. Deriving over somebody's own choice would be worse than leaving a stale
	 * default: the stale one is visible and wrong, the overwritten one is invisible and wrong.
	 *
	 * @param operation
	 *            the conversion the user has just chosen
	 */
	private void onOperationSelected(ConversionOperation operation)
	{
		ConversionWizardModel domainModel = getModelObject().getModelObject();
		domainModel.setOperation(operation);
		String current = txtTargetFile.getText();
		boolean blank = current == null || current.isBlank();
		if (!blank && !targetWasDerived)
		{
			return;
		}
		String sourcePath = domainModel.getSourceFilePath();
		if (sourcePath != null && !sourcePath.isBlank())
		{
			File defaultTarget = operation.defaultTargetFile(new File(sourcePath.trim()));
			setDerivedTarget(defaultTarget.getAbsolutePath());
		}
	}

	/**
	 * Writes a path the panel worked out itself, and remembers that it did
	 *
	 * @param path
	 *            the derived path
	 */
	private void setDerivedTarget(final String path)
	{
		txtTargetFile.setText(path);
		targetWasDerived = true;
	}

	private void onBrowseTarget()
	{
		ConversionWizardModel domainModel = getModelObject().getModelObject();
		String currentPath = domainModel.getTargetFilePath() == null
			? ""
			: domainModel.getTargetFilePath().trim();
		JFileChooser fileChooser = new JFileChooser();
		if (!currentPath.isEmpty())
		{
			fileChooser.setSelectedFile(new File(currentPath));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			txtTargetFile.setText(fileChooser.getSelectedFile().getAbsolutePath());
			targetWasDerived = false;
		}
	}

	private static String capitalize(ConversionOperation operation)
	{
		String name = operation.name().toLowerCase().replace("_", " ");
		StringBuilder capitalized = new StringBuilder();
		for (String word : name.split(" "))
		{
			capitalized.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return capitalized.toString();
	}
}
