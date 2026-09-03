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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

import io.github.astrapi69.awt.extension.ClipboardExtensions;
import io.github.astrapi69.checksum.ChecksumExtensions;
import io.github.astrapi69.checksum.FileChecksumExtensions;
import io.github.astrapi69.crypt.api.algorithm.ChecksumAlgorithm;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.file.system.SystemFileExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.listener.document.EnableButtonBehavior;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * The tool that computes the checksum of a file and compares it with the checksum its owner
 * published.
 * <p>
 * Every input component is bound to the {@link ChecksumBean} this panel holds as its model, so what
 * was chosen, typed or computed is readable from the model at any moment and the buttons read their
 * values there instead of out of the widgets.
 */
@Getter
@Log
public class ChecksumPanel extends BasePanel<ChecksumBean>
{

	/** The message shown in the result field before anything was compared */
	private static final String NOTHING_COMPARED_YET = "Checksum Match Result";

	/** The colour the result field takes when the two checksums are the same */
	private static final Color MATCH_COLOR = new Color(0, 255, 0);

	/** The colour the result field takes when they are not, or when the file cannot be used */
	private static final Color NO_MATCH_COLOR = new Color(255, 0, 0);

	/** The size a checksum file may have at most, in bytes */
	private static final long MAXIMUM_CHECKSUM_FILE_LENGTH = 128;

	/**
	 * The file extension conventionally produced for each algorithm this tool offers (the same
	 * ones {@code md5sum}, {@code sha256sum} and their siblings write), tried in this order when
	 * looking for a checksum file next to the one being checked
	 */
	// package-private (not private): the sibling-detection tests parameterize over this same
	// mapping instead of duplicating it, so a changed extension cannot silently drift out of sync
	// with what is actually tested
	static final Map<ChecksumAlgorithm, String> CHECKSUM_FILE_EXTENSIONS = new LinkedHashMap<>();
	static
	{
		CHECKSUM_FILE_EXTENSIONS.put(ChecksumAlgorithm.SHA_256, "sha256");
		CHECKSUM_FILE_EXTENSIONS.put(ChecksumAlgorithm.SHA_512, "sha512");
		CHECKSUM_FILE_EXTENSIONS.put(ChecksumAlgorithm.SHA_384, "sha384");
		CHECKSUM_FILE_EXTENSIONS.put(ChecksumAlgorithm.SHA_1, "sha1");
		CHECKSUM_FILE_EXTENSIONS.put(ChecksumAlgorithm.MD5, "md5");
		CHECKSUM_FILE_EXTENSIONS.put(ChecksumAlgorithm.MD2, "md2");
	}

	private JButton btnClearChecksumFile;
	private JButton btnClearOpenFile;
	private JButton btnCompare;
	private JButton btnCopyGeneratedChecksum;
	private JButton btnCopyOwnersChecksum;
	private JButton btnOpenChecksumFile;
	private JButton btnOpenFile;
	private JLabel lblChecksumAlgorithm;
	private JLabel lblGeneratedChecksum;
	private JLabel lblOwnersChecksum;
	private JScrollPane srcGeneratedChecksum;
	private JScrollPane srcOwnersChecksum;
	private JMTextField txtChecksumFile;
	private JMTextField txtChecksumMatchResult;
	private JMTextArea txtGeneratedChecksum;
	private JMTextField txtOpenFile;
	private JMTextArea txtOwnersChecksum;
	// manually changed
	private JMComboBox<ChecksumAlgorithm, ComboBoxModel<ChecksumAlgorithm>> cbxChecksumAlgorithm;
	private JFileChooser fileChooser;

	/**
	 * Creates new {@link ChecksumPanel}
	 */
	public ChecksumPanel()
	{
		this(BaseModel.of(ChecksumBean.builder().build()));
	}

	/**
	 * Creates new form ChecksumPanel
	 */
	public ChecksumPanel(final IModel<ChecksumBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		txtOpenFile = new JMTextField();
		btnOpenFile = new JButton();
		btnClearOpenFile = new JButton();
		lblGeneratedChecksum = new JLabel();
		srcGeneratedChecksum = new JScrollPane();
		txtGeneratedChecksum = new JMTextArea();
		btnCopyGeneratedChecksum = new JButton();
		lblOwnersChecksum = new JLabel();
		txtChecksumFile = new JMTextField();
		btnOpenChecksumFile = new JButton();
		btnClearChecksumFile = new JButton();
		srcOwnersChecksum = new JScrollPane();
		txtOwnersChecksum = new JMTextArea();
		btnCopyOwnersChecksum = new JButton();
		lblChecksumAlgorithm = new JLabel();
		cbxChecksumAlgorithm = new JMComboBox<>();
		btnCompare = new JButton();
		txtChecksumMatchResult = new JMTextField();

		// manually changed
		txtOpenFile.setEnabled(false);
		txtGeneratedChecksum.setEnabled(false);
		txtChecksumFile.setEnabled(false);

		// stable component names for UI tests (AssertJ-Swing lookups by name)
		cbxChecksumAlgorithm.setName("cbxChecksumAlgorithm");
		btnOpenFile.setName("btnOpenFile");
		txtGeneratedChecksum.setName("txtGeneratedChecksum");

		btnOpenFile.addActionListener(this::onOpenFile);
		btnClearOpenFile.addActionListener(this::onClearOpenFile);
		EnableButtonBehavior.builder().buttonModel(btnClearOpenFile.getModel())
			.document(txtGeneratedChecksum.getDocument()).build();

		btnOpenFile.setText("Open File to check");
		btnClearOpenFile.setText("Clear");


		lblGeneratedChecksum.setText("Generated checksum");

		txtGeneratedChecksum.setColumns(20);
		txtGeneratedChecksum.setRows(3);
		srcGeneratedChecksum.setViewportView(txtGeneratedChecksum);

		btnCopyGeneratedChecksum.setName("btnCopyGeneratedChecksum");
		btnCopyGeneratedChecksum.setText("Copy");
		btnCopyGeneratedChecksum.addActionListener(this::onCopyGeneratedChecksum);
		// nothing to copy before a checksum was generated
		EnableButtonBehavior.builder().buttonModel(btnCopyGeneratedChecksum.getModel())
			.document(txtGeneratedChecksum.getDocument()).build();

		lblOwnersChecksum.setText("Checksum from owner");

		txtOwnersChecksum.setColumns(20);
		txtOwnersChecksum.setRows(3);

		btnCopyOwnersChecksum.setName("btnCopyOwnersChecksum");
		btnCopyOwnersChecksum.setText("Copy");
		btnCopyOwnersChecksum.addActionListener(this::onCopyOwnersChecksum);
		// nothing to copy before the owner's checksum was typed or loaded
		EnableButtonBehavior.builder().buttonModel(btnCopyOwnersChecksum.getModel())
			.document(txtOwnersChecksum.getDocument()).build();

		btnOpenChecksumFile.addActionListener(this::onOpenChecksumFile);
		btnClearChecksumFile.addActionListener(this::onClearChecksumFile);
		EnableButtonBehavior.builder().buttonModel(btnClearChecksumFile.getModel())
			.document(txtOwnersChecksum.getDocument()).build();

		btnOpenChecksumFile.setText("Open Checksum File");
		btnClearChecksumFile.setText("Clear");

		srcOwnersChecksum.setViewportView(txtOwnersChecksum);

		lblChecksumAlgorithm.setText("Checksum algorithm");

		// the tool starts with what the user configured in the settings dialog; the model carries
		// it into the combo box when the components are bound
		ChecksumAlgorithm configuredAlgorithm = ChecksumSettingsContribution.algorithm();
		getModelObject().setSelectedAlgorithm(configuredAlgorithm);
		// UNKNOWN is crypt-api's own sentinel for "not one of the real algorithms" - not a real
		// choice for generating a checksum, so it never belongs in this box
		cbxChecksumAlgorithm.setModel(new EnumComboBoxModel<>(ChecksumAlgorithm.class,
			configuredAlgorithm, Set.of(ChecksumAlgorithm.UNKNOWN)));
		cbxChecksumAlgorithm.addActionListener(this::onChangeChecksumAlgorithm);

		btnCompare.setText("Compare");
		btnCompare.addActionListener(this::onCompare);

		getModelObject().setChecksumMatchResult(NOTHING_COMPARED_YET);

		bindComponents();

		txtOpenFile.setToolTipText("the file whose checksum is computed");
		btnOpenFile.setToolTipText("choose the file to check");
		btnClearOpenFile.setToolTipText("clears the file to check and the checksum computed for it");
		txtGeneratedChecksum.setToolTipText(
			"the checksum computed for the file above, using the selected algorithm");
		btnCopyGeneratedChecksum.setToolTipText("copies the generated checksum to the clipboard");
		txtChecksumFile.setToolTipText("the file the checksum below was loaded from, if any");
		btnOpenChecksumFile
			.setToolTipText("load a checksum published by the file's owner from a file");
		btnClearChecksumFile.setToolTipText(
			"clears the loaded checksum file and what was typed or loaded below");
		txtOwnersChecksum.setToolTipText(
			"the checksum published by the file's owner, typed or loaded from a file");
		btnCopyOwnersChecksum.setToolTipText("copies the owner's checksum to the clipboard");
		cbxChecksumAlgorithm.setToolTipText(
			"the algorithm used to compute the checksum above - it has to match what the owner used");
		btnCompare.setToolTipText("compares the generated checksum with the owner's checksum");
		txtChecksumMatchResult.setToolTipText(
			"Match or No Match, depending on whether the two checksums above are the same");

		fileChooser = new JFileChooser(SystemFileExtensions.getUserDownloadsDir());
	}

	/**
	 * Binds every input component to the model of this panel, so that each edit lands in the model
	 * and the model is what the buttons read - the components take the values the model already
	 * holds
	 */
	private void bindComponents()
	{
		ChecksumBean modelObject = getModelObject();
		txtOpenFile.setPropertyModel(
			LambdaModel.of(modelObject::getSelectedFilename, modelObject::setSelectedFilename));
		txtGeneratedChecksum.setPropertyModel(
			LambdaModel.of(modelObject::getGeneratedChecksum, modelObject::setGeneratedChecksum));
		txtChecksumFile.setPropertyModel(LambdaModel.of(modelObject::getSelectedChecksumFilename,
			modelObject::setSelectedChecksumFilename));
		txtOwnersChecksum.setPropertyModel(
			LambdaModel.of(modelObject::getOwnersChecksum, modelObject::setOwnersChecksum));
		cbxChecksumAlgorithm.setPropertyModel(
			LambdaModel.of(modelObject::getSelectedAlgorithm, modelObject::setSelectedAlgorithm));
		txtChecksumMatchResult.setPropertyModel(LambdaModel.of(modelObject::getChecksumMatchResult,
			modelObject::setChecksumMatchResult));
	}

	private void onOpenChecksumFile(ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(ChecksumPanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			applyChecksumFile(fileChooser.getSelectedFile());
		}
	}

	/**
	 * Loads the given file into "Checksum from owner" - what "Open Checksum File" does once a file
	 * is chosen, and what a sibling file found next to the file being checked is loaded with too
	 *
	 * @param checksumFile
	 *            the checksum file to load
	 */
	private void applyChecksumFile(final File checksumFile)
	{
		long length = checksumFile.length();
		if (length <= MAXIMUM_CHECKSUM_FILE_LENGTH)
		{
			getModelObject().setSelectedChecksumFile(checksumFile);
			txtChecksumFile.setText(checksumFile.getName());
			try
			{
				String checksum = ReadFileExtensions.fromFile(checksumFile).trim();
				txtOwnersChecksum.setText(checksum);
				txtOwnersChecksum.setEnabled(false);
				ChecksumAlgorithm checksumAlgorithmOfFile = algorithmForFileExtension(checksumFile)
					.orElseGet(() -> ChecksumExtensions.resolveChecksumAlgorithm(checksum));
				cbxChecksumAlgorithm.setSelectedItem(checksumAlgorithmOfFile);
				this.revalidate();
			}
			catch (IOException e)
			{
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
		else
		{
			showChecksumMatchResult("Given checksum file is invalid", NO_MATCH_COLOR);
		}
	}

	/**
	 * Looks for a same-named checksum file next to the one being checked, trying both naming
	 * conventions tools actually use: appending the algorithm extension ({@code backup.alb} +
	 * {@code backup.alb.sha256}) and replacing the original extension with it ({@code backup.alb}
	 * + {@code backup.sha256}) - and loads it into "Checksum from owner" the way choosing one by
	 * hand would - only when that field is still empty, so a checksum the user already typed or
	 * loaded is never silently replaced
	 *
	 * @param file
	 *            the file that was chosen to be checked
	 */
	private void probeForSiblingChecksumFile(final File file)
	{
		if (!txtOwnersChecksum.getText().isBlank())
		{
			return;
		}
		File parent = file.getParentFile();
		if (parent == null)
		{
			return;
		}
		String baseName = baseNameWithoutExtension(file.getName());
		for (String extension : CHECKSUM_FILE_EXTENSIONS.values())
		{
			File appended = new File(parent, file.getName() + "." + extension);
			if (appended.isFile())
			{
				applyChecksumFile(appended);
				return;
			}
			File replaced = new File(parent, baseName + "." + extension);
			if (replaced.isFile())
			{
				applyChecksumFile(replaced);
				return;
			}
		}
	}

	/**
	 * The given file name with its own extension removed, or the name unchanged if it has none -
	 * {@code "backup.alb"} becomes {@code "backup"}
	 *
	 * @param name
	 *            the file name
	 * @return the name without its extension
	 */
	private static String baseNameWithoutExtension(final String name)
	{
		int dot = name.lastIndexOf('.');
		return dot < 0 ? name : name.substring(0, dot);
	}

	/**
	 * Looks up which algorithm a checksum file's own extension implies, from the same
	 * {@link #CHECKSUM_FILE_EXTENSIONS} table {@link #probeForSiblingChecksumFile(File)} searches
	 * with - a reliable answer where {@link ChecksumExtensions#resolveChecksumAlgorithm(String)}
	 * is not: MD2 and MD5 both produce a 32 hex character digest, so guessing from the checksum
	 * text alone can never tell them apart and always lands on MD5 (#128)
	 *
	 * @param checksumFile
	 *            the checksum file whose extension is checked
	 * @return the algorithm the extension names, empty if the extension names none of them
	 */
	private static Optional<ChecksumAlgorithm> algorithmForFileExtension(final File checksumFile)
	{
		String name = checksumFile.getName();
		int dot = name.lastIndexOf('.');
		if (dot < 0)
		{
			return Optional.empty();
		}
		String extension = name.substring(dot + 1);
		return CHECKSUM_FILE_EXTENSIONS.entrySet().stream()
			.filter(entry -> entry.getValue().equalsIgnoreCase(extension)).map(Map.Entry::getKey)
			.findFirst();
	}

	protected void onClearChecksumFile(ActionEvent actionEvent)
	{
		getModelObject().setSelectedChecksumFile(null);
		txtChecksumFile.setText("");
		txtOwnersChecksum.setText("");
		txtOwnersChecksum.setEnabled(true);
	}

	/**
	 * Copies the generated checksum to the system clipboard, so it can be pasted wherever it needs
	 * to be published or compared, without selecting the text by hand
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onCopyGeneratedChecksum(final ActionEvent actionEvent)
	{
		ClipboardExtensions.copyToClipboard(getModelObject().getGeneratedChecksum());
	}

	/**
	 * Copies the checksum published by the file's owner to the system clipboard
	 *
	 * @param actionEvent
	 *            the action event
	 */
	protected void onCopyOwnersChecksum(final ActionEvent actionEvent)
	{
		ClipboardExtensions.copyToClipboard(getModelObject().getOwnersChecksum());
	}

	protected void onCompare(final ActionEvent actionEvent)
	{
		String ownersChecksumText = getModelObject().getOwnersChecksum();
		String generatedChecksumText = getModelObject().getGeneratedChecksum();
		if (Objects.equals(ownersChecksumText, generatedChecksumText))
		{
			showChecksumMatchResult("Match", MATCH_COLOR);
		}
		else
		{
			showChecksumMatchResult("No Match", NO_MATCH_COLOR);
		}
	}

	/**
	 * Shows what the last comparison found, which the result field carries into the model
	 *
	 * @param matchResult
	 *            what the panel has to say about the two checksums
	 * @param background
	 *            the colour that says the same thing without being read
	 */
	private void showChecksumMatchResult(final String matchResult, final Color background)
	{
		txtChecksumMatchResult.setText(matchResult);
		txtChecksumMatchResult.setBackground(new ColorUIResource(background));
		txtChecksumMatchResult.revalidate();
	}

	protected void onChangeChecksumAlgorithm(final ActionEvent actionEvent)
	{
		// the combo box has already written the chosen algorithm into the model
		calculateChecksum();
	}

	protected void onClearOpenFile(ActionEvent actionEvent)
	{
		getModelObject().setSelectedFile(null);
		txtOpenFile.setText("");
		txtGeneratedChecksum.setText("");
	}

	protected void onOpenFile(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showSaveDialog(ChecksumPanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			applySelectedFile(fileChooser.getSelectedFile());
		}
	}

	/**
	 * Applies the file chosen to be checked - what {@link #onOpenFile(ActionEvent)} does once a
	 * file is chosen, factored out so it is reachable without the interactive file chooser it is
	 * otherwise reached through
	 *
	 * @param selectedFile
	 *            the file to check
	 */
	void applySelectedFile(final File selectedFile)
	{
		getModelObject().setSelectedFile(selectedFile);
		txtOpenFile.setText(selectedFile.getName());
		calculateChecksum();
		probeForSiblingChecksumFile(selectedFile);
	}

	private void calculateChecksum()
	{
		if (getModelObject().getSelectedFile() != null
			&& getModelObject().getSelectedFile().exists())
		{
			ChecksumAlgorithm selectedAlgorithm = getModelObject().getSelectedAlgorithm();
			try
			{
				String checksum = FileChecksumExtensions
					.getChecksum(getModelObject().getSelectedFile(), selectedAlgorithm);
				txtGeneratedChecksum.setText(checksum);
			}
			catch (NoSuchAlgorithmException | IOException e)
			{
				log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}

	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	protected void onInitializeGroupLayout()
	{
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(30, 30, 30)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(lblGeneratedChecksum, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addGroup(layout.createSequentialGroup()
								.addComponent(srcGeneratedChecksum, GroupLayout.DEFAULT_SIZE, 1000,
									Short.MAX_VALUE)
								.addGap(18, 18, 18).addComponent(btnCopyGeneratedChecksum))
							.addComponent(lblOwnersChecksum, GroupLayout.DEFAULT_SIZE,
								GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addGroup(layout.createSequentialGroup()
								.addComponent(txtOpenFile, GroupLayout.PREFERRED_SIZE, 780,
									GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
									GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(btnOpenFile, GroupLayout.PREFERRED_SIZE, 240,
									GroupLayout.PREFERRED_SIZE)
								.addGap(18, 18, 18).addComponent(btnClearOpenFile)))
						.addGap(30, 30, 30))
					.addGroup(layout.createSequentialGroup()
						.addComponent(lblChecksumAlgorithm, GroupLayout.PREFERRED_SIZE, 199,
							GroupLayout.PREFERRED_SIZE)
						.addGap(0, 0, Short.MAX_VALUE))
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
							.addGroup(layout.createSequentialGroup()
								.addComponent(cbxChecksumAlgorithm, GroupLayout.PREFERRED_SIZE, 240,
									GroupLayout.PREFERRED_SIZE)
								.addGap(31, 31, 31)
								.addComponent(btnCompare, GroupLayout.PREFERRED_SIZE, 180,
									GroupLayout.PREFERRED_SIZE)
								.addGap(57, 57, 57).addComponent(txtChecksumMatchResult))
							.addGroup(layout.createSequentialGroup()
								.addComponent(srcOwnersChecksum).addGap(18, 18, 18)
								.addComponent(btnCopyOwnersChecksum))
							.addGroup(layout.createSequentialGroup()
								.addComponent(txtChecksumFile, GroupLayout.PREFERRED_SIZE, 780,
									GroupLayout.PREFERRED_SIZE)
								.addGap(57, 57, 57)
								.addComponent(btnOpenChecksumFile, GroupLayout.PREFERRED_SIZE, 240,
									GroupLayout.PREFERRED_SIZE)
								.addGap(18, 18, 18).addComponent(btnClearChecksumFile)))
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(30, 30, 30)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(btnOpenFile)
					.addComponent(txtOpenFile, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(btnClearOpenFile))
				.addGap(18, 18, 18).addComponent(lblGeneratedChecksum).addGap(26, 26, 26)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addComponent(srcGeneratedChecksum, GroupLayout.PREFERRED_SIZE, 58,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(btnCopyGeneratedChecksum))
				.addGap(18, 18, 18).addComponent(lblOwnersChecksum).addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(btnOpenChecksumFile).addComponent(btnClearChecksumFile))
					.addComponent(txtChecksumFile, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addComponent(srcOwnersChecksum, GroupLayout.PREFERRED_SIZE, 60,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(btnCopyOwnersChecksum))
				.addGap(18, 18, 18).addComponent(lblChecksumAlgorithm).addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(cbxChecksumAlgorithm, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(btnCompare).addComponent(txtChecksumMatchResult,
						GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addContainerGap(40, Short.MAX_VALUE)));
	}

}
