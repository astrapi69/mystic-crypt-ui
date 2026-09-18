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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.swing.JPanel;

import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.panel.certificate.wizard.CertificateWizardContentPanel;
import io.github.astrapi69.mystic.crypt.panel.certificate.wizard.FirstStepPanel;
import io.github.astrapi69.mystic.crypt.panel.certificate.wizard.SecondStepPanel;
import io.github.astrapi69.mystic.crypt.panel.certificate.wizard.ThirdStepPanel;

/**
 * Construction smoke tests for the placeholder wizard-skeleton panels (currently unused) - each
 * must still build without throwing
 */
class DeadWizardSkeletonPanelsConstructionSmokeTest
{

	private static void assertConstructs(java.util.function.Supplier<? extends JPanel> factory)
	{
		JPanel panel = GuiActionRunner.execute(factory::get);
		assertNotNull(panel, "the panel must be constructed");
	}

	@Test
	void firstStepPanelConstructs()
	{
		assertConstructs(FirstStepPanel::new);
	}

	@Test
	void secondStepPanelConstructs()
	{
		assertConstructs(SecondStepPanel::new);
	}

	@Test
	void thirdStepPanelConstructs()
	{
		assertConstructs(ThirdStepPanel::new);
	}

	@Test
	void skeletonContentPanelConstructs()
	{
		assertConstructs(CertificateWizardContentPanel::new);
	}
}
