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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

import java.awt.Font;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.*;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMSpinner;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Tool panel for splitting a secret into shares and putting it back together.
 * <p>
 * This answers what happens to a master password if its owner cannot use it any more: the secret is
 * split so that a few of the shares are enough to rebuild it, and fewer than that reveal nothing.
 * Each share is a line of text meant to leave this machine - onto paper, into someone else's safe -
 * so each one carries a check value that catches a character mistyped while copying it back.
 * <p>
 * Every component is bound to {@link SecretSharingPanelModel}, so a button reads what the user
 * entered from the model and never out of the widgets.
 * <p>
 * The window is laid out with {@link ToolForm}, the one shape every tool window in this
 * application uses: labels in a narrow right aligned column, fields taking the width, the share
 * list and the rebuilt secret taking the height that is left, buttons under what they act on.
 */
public class SecretSharingPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that belongs to the field above it rather than to a label of its own */
	private static final String UNDER_THE_FIELD = "skip, growx";

	/**
	 * A component that is not stretched across the window: a spinner over the range 2 to 255 is
	 * read at the width it asks for
	 */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	/**
	 * The rebuilt secret takes height when the window has it, but less of it than the share list
	 * above, which is the longer text of the two
	 */
	private static final String REBUILT_AREA = "grow, pushy 40";

	private final SecretSharingPanelModel modelObject = new SecretSharingPanelModel();

	private final JMPasswordField pwdSecret = new JMPasswordField(28);
	private final JMTextField txtSecretFile = new JMTextField(34);
	private final JMCheckBox chkUseFile = new JMCheckBox(
		"split the file instead of the secret above");
	private final JMSpinner<Integer> spnThreshold = new JMSpinner<>(
		new SpinnerNumberModel(3, 2, 255, 1));
	private final JMSpinner<Integer> spnTotalShares = new JMSpinner<>(
		new SpinnerNumberModel(5, 2, 255, 1));
	private final JMTextArea txtShares = new JMTextArea(8, 62);
	private final JMTextArea txtRebuilt = new JMTextArea(3, 62);
	private final JMTextField txtRebuiltFile = new JMTextField(34);
	private final JLabel lblResult = new JLabel(" ");

	public SecretSharingPanel()
	{
		super(ToolForm.newLayout());

		pwdSecret.setName("pwdSecret");
		txtSecretFile.setName("txtSecretFile");
		chkUseFile.setName("chkUseFile");
		spnThreshold.setName("spnThreshold");
		spnTotalShares.setName("spnTotalShares");
		txtShares.setName("txtShares");
		txtShares.setFont(new Font("monospaced", Font.PLAIN, 12));
		txtRebuilt.setName("txtRebuilt");
		txtRebuilt.setLineWrap(true);
		txtRebuiltFile.setName("txtRebuiltFile");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		// the tool starts with what the user configured in the settings dialog
		modelObject.setThreshold(SecretSharingSettingsContribution.threshold());
		modelObject.setTotalShares(SecretSharingSettingsContribution.totalShares());

		bindToModel();

		ToolForm.sized(pwdSecret);
		ToolForm.sized(txtSecretFile);
		ToolForm.sized(txtRebuiltFile);

		add(new JLabel("Secret:"));
		add(pwdSecret, ToolForm.FIELD);

		add(new JLabel("or file:"));
		add(txtSecretFile, WITH_BUTTON);
		add(button("btnBrowseSecretFile", "...", event -> onBrowseSecretFile()));
		add(chkUseFile, UNDER_THE_FIELD);

		add(new JLabel("Shares needed:"));
		add(spnThreshold, OWN_WIDTH);
		add(new JLabel("Shares produced:"));
		add(spnTotalShares, OWN_WIDTH);
		add(ToolForm.buttons(button("btnSplit", "Split", event -> onSplit()),
			button("btnSaveShares", "Save shares", event -> onSaveShares())), ToolForm.BUTTON_ROW);

		add(new JLabel("Shares:"), "aligny top");
		add(ToolForm.scrolled(txtShares), "grow, push");
		add(ToolForm.buttons(button("btnCombine", "Combine", event -> onCombine()),
			button("btnLoadShares", "Load shares", event -> onLoadShares())), ToolForm.BUTTON_ROW);

		add(new JLabel("Rebuilt secret:"), "aligny top");
		add(ToolForm.scrolled(txtRebuilt), REBUILT_AREA);
		add(new JLabel("Write it to:"));
		add(txtRebuiltFile, WITH_BUTTON);
		add(button("btnBrowseRebuiltFile", "...", event -> onBrowseRebuiltFile()));
		add(ToolForm.buttons(
			button("btnSaveRebuilt", "Save rebuilt secret", event -> onSaveRebuilt())),
			ToolForm.BUTTON_ROW);

		add(lblResult, ToolForm.RESULT_LINE);
	}

	/**
	 * Binds every component to its field in {@link SecretSharingPanelModel}: from here on an edit
	 * in the panel lands in the model, and a value set on a component is passed into the model by
	 * the component itself.
	 */
	private void bindToModel()
	{
		pwdSecret.setPropertyModel(LambdaModel.of(modelObject::getSecret, modelObject::setSecret));
		txtSecretFile.setPropertyModel(
			LambdaModel.of(modelObject::getSecretFile, modelObject::setSecretFile));
		chkUseFile
			.setPropertyModel(LambdaModel.of(modelObject::isUseFile, modelObject::setUseFile));
		spnThreshold
			.setPropertyModel(LambdaModel.of(modelObject::getThreshold, modelObject::setThreshold));
		spnTotalShares.setPropertyModel(
			LambdaModel.of(modelObject::getTotalShares, modelObject::setTotalShares));
		txtShares.setPropertyModel(LambdaModel.of(modelObject::getShares, modelObject::setShares));
		txtRebuilt.setPropertyModel(
			LambdaModel.of(modelObject::getRebuiltSecret, modelObject::setRebuiltSecret));
		txtRebuiltFile.setPropertyModel(
			LambdaModel.of(modelObject::getRebuiltFile, modelObject::setRebuiltFile));
	}

	private void onSplit()
	{
		run("split", () -> {
			int threshold = modelObject.getThreshold();
			int totalShares = modelObject.getTotalShares();
			byte[] secret = secret();
			try
			{
				List<String> shares = SecretSharingSupport.split(secret, threshold, totalShares);
				showShares(String.join(System.lineSeparator(), shares));
				return totalShares + " shares, " + threshold + " of them are enough - keep them apart";
			}
			finally
			{
				Arrays.fill(secret, (byte)0);
			}
		});
	}

	private void onCombine()
	{
		run("combined", () -> {
			List<String> shares = List.of(modelObject.getShares().split("\\R"));
			byte[] secret = SecretSharingSupport.combine(shares);
			showRebuiltSecret(new String(secret, StandardCharsets.UTF_8));
			return "rebuilt " + secret.length + " bytes";
		});
	}

	private void onSaveShares()
	{
		run("saved", () -> {
			if (modelObject.getShares().isBlank())
			{
				throw new IllegalArgumentException("there is nothing to save - split a secret first");
			}
			File target = requireNewFile(modelObject.getSecretFile(), ".shares.txt");
			Files.writeString(target.toPath(), modelObject.getShares(), StandardCharsets.UTF_8);
			return "wrote the shares to " + target.getName()
				+ " - they belong apart, not in one file";
		});
	}

	private void onLoadShares()
	{
		run("loaded", () -> {
			File source = new File(modelObject.getSecretFile().trim());
			if (!source.isFile())
			{
				throw new IllegalArgumentException("choose the file the shares are in");
			}
			showShares(Files.readString(source.toPath(), StandardCharsets.UTF_8));
			return "read the shares from " + source.getName();
		});
	}

	private void onSaveRebuilt()
	{
		run("saved", () -> {
			if (modelObject.getRebuiltSecret().isEmpty())
			{
				throw new IllegalArgumentException("there is nothing to save - combine the shares first");
			}
			String path = modelObject.getRebuiltFile().trim();
			if (path.isEmpty())
			{
				throw new IllegalArgumentException("choose a file to write the secret to");
			}
			File target = new File(path);
			if (target.exists())
			{
				throw new IllegalArgumentException("'" + target
					+ "' already exists - pick another name or remove it first");
			}
			Files.write(target.toPath(),
				modelObject.getRebuiltSecret().getBytes(StandardCharsets.UTF_8));
			return "wrote the secret to " + target.getName();
		});
	}

	private byte[] secret() throws Exception
	{
		if (modelObject.isUseFile())
		{
			File file = new File(modelObject.getSecretFile().trim());
			if (!file.isFile())
			{
				throw new IllegalArgumentException("choose a file to split");
			}
			return Files.readAllBytes(file.toPath());
		}
		char[] typed = modelObject.getSecret();
		if (typed == null || typed.length == 0)
		{
			throw new IllegalArgumentException("there is no secret to split");
		}
		return new String(typed).getBytes(StandardCharsets.UTF_8);
	}

	private File requireNewFile(String path, String suffix)
	{
		String chosen = path.trim();
		File target = chosen.isEmpty() ? new File("shares" + suffix) : new File(chosen + suffix);
		if (target.exists())
		{
			throw new IllegalArgumentException(
				"'" + target + "' already exists - pick another name or remove it first");
		}
		return target;
	}

	/** The message shown at the bottom of the panel */
	public String getResultText()
	{
		return modelObject.getResultText();
	}

	private void run(String what, SharingOperation operation)
	{
		try
		{
			showResult(operation.execute());
		}
		catch (Exception exception)
		{
			showResult("not " + what + ": " + message(exception));
		}
	}

	/**
	 * Shows the given share lines: written into the model backed text area, which passes them on
	 * into the model
	 *
	 * @param shareLines
	 *            the share lines, one share per line
	 */
	private void showShares(String shareLines)
	{
		txtShares.setText(shareLines);
		txtShares.setCaretPosition(0);
	}

	/**
	 * Shows the secret that was rebuilt from the shares: written into the model backed text area,
	 * which passes it on into the model
	 *
	 * @param rebuiltSecret
	 *            the rebuilt secret
	 */
	private void showRebuiltSecret(String rebuiltSecret)
	{
		txtRebuilt.setText(rebuiltSecret);
		txtRebuilt.setCaretPosition(0);
	}

	/**
	 * Shows the given message at the bottom of the panel and keeps it in the model
	 *
	 * @param resultText
	 *            the message
	 */
	private void showResult(String resultText)
	{
		modelObject.setResultText(resultText);
		lblResult.setText(resultText);
	}

	private void onBrowseSecretFile()
	{
		chooseFile(modelObject.getSecretFile()).ifPresent(txtSecretFile::setText);
	}

	private void onBrowseRebuiltFile()
	{
		chooseFile(modelObject.getRebuiltFile()).ifPresent(txtRebuiltFile::setText);
	}

	/**
	 * Opens the file chooser, starting at the path the model already holds
	 *
	 * @param currentPath
	 *            the path the model holds, may be empty
	 * @return the absolute path of the chosen file, or empty if nothing was chosen
	 */
	private Optional<String> chooseFile(String currentPath)
	{
		JFileChooser fileChooser = new JFileChooser();
		if (currentPath != null && !currentPath.isBlank())
		{
			fileChooser.setSelectedFile(new File(currentPath));
		}
		if (fileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
		{
			return Optional.of(fileChooser.getSelectedFile().getAbsolutePath());
		}
		return Optional.empty();
	}

	private static String message(Exception exception)
	{
		return exception.getMessage() != null
			? exception.getMessage()
			: exception.getClass().getSimpleName();
	}

	@FunctionalInterface
	private interface SharingOperation
	{
		String execute() throws Exception;
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		return button;
	}
}
