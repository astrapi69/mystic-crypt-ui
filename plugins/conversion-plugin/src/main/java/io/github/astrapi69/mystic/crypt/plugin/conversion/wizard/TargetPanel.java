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

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.api.IModel;
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
	private JLabel lblWhatItHoldsCaption;
	private JLabel lblWhatItHolds;

	/**
	 * Populated in {@link #onInitializeComponents()}, not by a field initializer: the base panel
	 * calls that method from its own constructor, before any field initializer of this class has run
	 * (the same reason {@code ReviewPanel} keeps its own form model out of a field initializer)
	 */
	private Map<ConversionOperation, JRadioButton> operationButtons;
	private ButtonGroup operationGroup;
	private JLabel lblTargetFile;
	private JMTextField txtTargetFile;
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
		lblWhatItHoldsCaption = new JLabel("It holds:");
		lblWhatItHolds = new JLabel(ConversionWizardModel.NOTHING_TO_SAY);
		lblWhatItHolds.setName("lblTargetWhatItHolds");
		lblWhatItHolds.setToolTipText(ConversionMessages.getString(
			"conversion.wizard.target.tooltip.what.it.holds",
			"what the source file was found to hold"));
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
	private static final class TargetFieldListener implements javax.swing.event.DocumentListener
	{
		private final JMTextField targetField;
		private final ConversionWizardModel domainModel;

		private TargetFieldListener(JMTextField targetField, ConversionWizardModel domainModel)
		{
			this.targetField = targetField;
			this.domainModel = domainModel;
		}

		@Override
		public void insertUpdate(javax.swing.event.DocumentEvent event)
		{
			domainModel.setTargetFilePath(targetField.getText());
		}

		@Override
		public void removeUpdate(javax.swing.event.DocumentEvent event)
		{
			domainModel.setTargetFilePath(targetField.getText());
		}

		@Override
		public void changedUpdate(javax.swing.event.DocumentEvent event)
		{
			domainModel.setTargetFilePath(targetField.getText());
		}
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		MigLayout migLayout = new MigLayout("wrap 3", "[][grow,fill][]", "[][][][][][][grow]");
		setLayout(migLayout);

		add(lblHeader, "span, align center, gapbottom 10");
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

	private void onOperationSelected(ConversionOperation operation)
	{
		ConversionWizardModel domainModel = getModelObject().getModelObject();
		domainModel.setOperation(operation);
		if (txtTargetFile.getText() == null || txtTargetFile.getText().isBlank())
		{
			String sourcePath = domainModel.getSourceFilePath();
			if (sourcePath != null && !sourcePath.isBlank())
			{
				File defaultTarget = operation.defaultTargetFile(new File(sourcePath.trim()));
				txtTargetFile.setText(defaultTarget.getAbsolutePath());
			}
		}
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
