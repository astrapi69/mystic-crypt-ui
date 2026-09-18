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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.key.KeySize;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The window is built to be tried out: algorithms picked one after another, key sizes changed, keys
 * generated. Nothing in it may move while that happens. This walks the states a user walks and
 * reports the first one that moves something, with the whole table, so the cause is visible rather
 * than guessed at.
 */
class KeygenLayoutStaysPutUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private String geometryOf(final FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> {
			StringBuilder geometry = new StringBuilder();
			for (String name : new String[] { "txtPrivateKey", "txtPublicKey", "txtToEncrypt",
					"txtEncrypted" })
			{
				java.awt.Component component = frame.textBox(name).target();
				geometry.append(name).append("@x=").append(component.getLocationOnScreen().x)
					.append(' ');
			}
			// the scroll pane rather than the text area inside it: the area legitimately loses the
			// width of a scroll bar as soon as there is a key long enough to scroll, and that is
			// not the window moving
			java.awt.Component scrollPane = frame.textBox("txtPrivateKey").target().getParent()
				.getParent();
			geometry.append("keyArea=w").append(scrollPane.getWidth()).append(' ');
			for (java.awt.Component parent = scrollPane.getParent(); parent != null; parent = parent
				.getParent())
			{
				geometry.append(parent.getClass().getSimpleName()).append("=w")
					.append(parent.getWidth()).append(' ');
				if (parent.getClass().getSimpleName().contains("InternalFrame"))
				{
					break;
				}
			}
			return geometry.toString();
		});
	}

	@Test
	@DisplayName("nothing in the window moves while it is being tried out")
	void nothingInTheWindowMovesWhileItIsBeingTriedOut() throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);
		File databaseFile = new File(tempHome, "keygen-layout.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");

		Map<String, String> seen = new LinkedHashMap<>();
		seen.put("freshly opened", geometryOf(frame));

		for (KeyPairGeneratorAlgorithm algorithm : KeyPairGeneratorAlgorithm.values())
		{
			if (!isOffered(frame, algorithm))
			{
				continue;
			}
			GuiActionRunner
				.execute(() -> frame.comboBox("cmbAlgorithm").target().setSelectedItem(algorithm));
			robot.waitForIdle();
			seen.put("picked " + algorithm, geometryOf(frame));
		}

		GuiActionRunner.execute(() -> frame.comboBox("cmbAlgorithm").target()
			.setSelectedItem(KeyPairGeneratorAlgorithm.RSA));
		robot.waitForIdle();
		for (KeySize keySize : KeySize.values())
		{
			GuiActionRunner
				.execute(() -> frame.comboBox("cmbKeySize").target().setSelectedItem(keySize));
			robot.waitForIdle();
			seen.put("picked size " + keySize, geometryOf(frame));
			if (keySize == KeySize.KEYSIZE_1024 || keySize == KeySize.KEYSIZE_2048)
			{
				GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
				robot.waitForIdle();
				seen.put("generated " + keySize, geometryOf(frame));
			}
		}
		GuiActionRunner.execute(
			() -> frame.button(org.assertj.swing.core.matcher.JButtonMatcher.withText("Clear keys"))
				.target().doClick());
		robot.waitForIdle();
		seen.put("cleared", geometryOf(frame));

		String first = seen.values().iterator().next();
		String table = seen.entrySet().stream()
			.map(entry -> "    " + entry.getKey() + ": " + entry.getValue())
			.reduce("", (left, right) -> left + System.lineSeparator() + right);
		for (Map.Entry<String, String> state : seen.entrySet())
		{
			assertEquals(first, state.getValue(),
				"the form moved at '" + state.getKey() + "'." + table);
		}
	}

	private boolean isOffered(final FrameFixture frame, final KeyPairGeneratorAlgorithm algorithm)
	{
		return GuiActionRunner.execute(() -> {
			javax.swing.JComboBox<?> box = frame.comboBox("cmbAlgorithm").target();
			for (int index = 0; index < box.getItemCount(); index++)
			{
				if (algorithm.equals(box.getItemAt(index)))
				{
					return true;
				}
			}
			return false;
		});
	}

}
