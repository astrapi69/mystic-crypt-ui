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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * Whether a window shows its answer once there is one (#835): the window is packed when it opens,
 * while only the sentence stands in the place of the answer
 */
class AnswerFitsTheWindowTest
{

	@TempDir
	File configuration;

	@TempDir
	Path directory;

	@BeforeEach
	void aConfigurationOfItsOwn()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.getAbsolutePath());
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	private static JInternalFrame packedAround(final JPanel content)
	{
		JInternalFrame frame = new JInternalFrame("a window", true, true, true, true);
		frame.add(content);
		frame.pack();
		return frame;
	}

	@Test
	@DisplayName("an answer area that starts showing its answer leaves the window at the size it was packed to (pinned, #835)")
	void showingTheAnswer_leavesTheWindowAtItsPackedSize()
	{
		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		form.add(AnswerArea.toCome("what will show here", "where the answer appears"));
		form.add(new JTextArea(20, 40));
		AnswerArea area = AnswerArea.of(form, 0, new JPanel());
		area.show(false, false);
		JInternalFrame frame = packedAround(form);

		area.show(true, false);

		assertTrue(frame.getHeight() < frame.getPreferredSize().height,
			"the window grew to its content (pinned, #835): " + frame.getHeight() + " of "
				+ frame.getPreferredSize().height);
	}

	@Test
	@DisplayName("after a check the chain window is shorter than its answer (pinned, #835)")
	void afterACheck_theChainWindowIsShorterThanItsAnswer() throws Exception
	{
		Path chainFile = directory.resolve("chain.lethenon");
		new ChainFile(chainFile).write(
			LethenonFixtures.aChainOf(Chain.TEST_IDENTIFIER, 2, Bytes.of(new byte[] { 9 })));
		LethenonChainPanel panel = new LethenonChainPanel();
		panel.getModelObject().setChainFile(chainFile.toString());
		JInternalFrame frame = packedAround(panel);

		panel.onVerify();

		assertTrue(frame.getHeight() < frame.getPreferredSize().height,
			"the window grew to its answer (pinned, #835): " + frame.getHeight() + " of "
				+ frame.getPreferredSize().height);
	}
}
