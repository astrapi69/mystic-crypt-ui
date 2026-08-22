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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;

/**
 * End-to-end good case: adding several nodes to the tree root leaves all of them in the tree
 */
class AddMultipleNodesUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "add-multiple-pw-123";

	@Test
	void allAddedNodesAppearInTheTree() throws IOException
	{
		File databaseFile = new File(tempHome, "add-multiple-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();

		application.addNodeToTreeRoot(frame, "NodeAlpha");
		application.addNodeToTreeRoot(frame, "NodeBeta");
		application.addNodeToTreeRoot(frame, "NodeGamma");

		assertTrue(application.treeContainsNodeStartingWith("NodeAlpha"),
			"NodeAlpha must be present");
		assertTrue(application.treeContainsNodeStartingWith("NodeBeta"),
			"NodeBeta must be present");
		assertTrue(application.treeContainsNodeStartingWith("NodeGamma"),
			"NodeGamma must be present");
	}
}
