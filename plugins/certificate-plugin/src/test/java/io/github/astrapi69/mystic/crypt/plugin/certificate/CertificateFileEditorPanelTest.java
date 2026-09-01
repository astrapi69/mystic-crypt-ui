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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.model.BaseModel;

/**
 * There is no in-app viewer for a certificate file otherwise (#110), so this panel is the one
 * place that both shows what was written and can change it - what it saves has to be what is
 * currently in the editor, not what the file held when it was opened.
 */
class CertificateFileEditorPanelTest
{

	private static CertificateFileEditorPanel panelFor(File file, String content)
	{
		CertificateFileEditorModel model = CertificateFileEditorModel.builder().file(file)
			.content(content).build();
		return new CertificateFileEditorPanel(BaseModel.of(model));
	}

	@Test
	void theFileContentIsShownInTheEditorWhenOpened(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "test.crt");
		Files.writeString(file.toPath(), "-----BEGIN CERTIFICATE-----\nabc\n-----END CERTIFICATE-----");

		CertificateFileEditorPanel panel = panelFor(file, Files.readString(file.toPath()));

		assertTrue(panel.getTxtContent().getText().contains("BEGIN CERTIFICATE"),
			"the editor must show what the file holds");
	}

	@Test
	void editingTheTextAreaUpdatesTheModel(@TempDir File directory) throws Exception
	{
		File file = new File(directory, "test.crt");
		CertificateFileEditorPanel panel = panelFor(file, "original");

		panel.getTxtContent().setText("changed");

		assertEquals("changed", panel.getModelObject().getContent(),
			"what is typed has to reach the model the save button reads");
	}

	@Test
	void saveWritesWhatIsCurrentlyInTheEditorNotTheOriginalFileContent(@TempDir File directory)
		throws Exception
	{
		File file = new File(directory, "test.crt");
		Files.writeString(file.toPath(), "original");
		CertificateFileEditorPanel panel = panelFor(file, "original");
		panel.getTxtContent().setText("edited by the user");

		panel.onSave();

		assertEquals("edited by the user", Files.readString(file.toPath()),
			"the file must hold the edit, not what it had before the editor opened");
	}

	@Test
	void aFileThatCannotBeWrittenReportsWhy(@TempDir File directory) throws Exception
	{
		File unwritableTarget = new File(directory, "does-not-exist/test.crt");
		CertificateFileEditorPanel panel = panelFor(unwritableTarget, "content");
		java.util.concurrent.atomic.AtomicReference<String> shown = new java.util.concurrent.atomic.AtomicReference<>();
		CertificateFileEditorPanel panelWithVisibleError = new CertificateFileEditorPanel(
			BaseModel.of(panel.getModelObject()))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void showErrorDialog(String title, String message)
			{
				shown.set(message);
			}
		};

		panelWithVisibleError.onSave();

		assertTrue(shown.get() != null && !shown.get().isBlank(),
			"a save that fails has to say why, not fail silently");
	}

	@Test
	void closeIsANoOpUntilAnOpenerOverridesIt(@TempDir File directory)
	{
		File file = new File(directory, "test.crt");
		CertificateFileEditorPanel panel = panelFor(file, "content");

		assertDoesNotThrow(() -> panel.getBtnClose().doClick(),
			"the default Close action must be a no-op until a dialog overrides onClose()");
	}

}
