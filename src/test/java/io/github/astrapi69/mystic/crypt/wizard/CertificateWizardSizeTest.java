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
package io.github.astrapi69.mystic.crypt.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JScrollPane;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;

/**
 * The steps of the wizard sit in a card layout, which asks for the size of the largest of them
 * whatever is on screen. In a scroll pane that puts a scroll bar next to a short step, and
 * scrolling it reveals nothing but the empty space another step would have needed - which is what
 * the certificate window looked like.
 */
class CertificateWizardSizeTest
{

	private static CertificateWizardPanel aWizard()
	{
		java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
		CertificateInfoModel describing = CertificateInfoModel.builder()
			.issuer(io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel
				.builder().commonName("issuer").build())
			.subject(io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel
				.builder().commonName("subject").build())
			.validityModel(io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel.builder()
				.notBefore(now).notAfter(now.plusYears(1)).build())
			.build();
		return new CertificateWizardPanel(BaseModel.of(describing));
	}

	private static Component visibleStepOf(final CertificateWizardPanel wizard)
	{
		for (Component step : wizard.getWizardContentPanel().getComponents())
		{
			if (step.isVisible())
			{
				return step;
			}
		}
		throw new IllegalStateException("the wizard shows no step at all");
	}

	@Test
	@DisplayName("the content asks for the height of the step that is on screen")
	void theContentAsksForTheHeightOfTheStepThatIsOnScreen()
	{
		CertificateWizardPanel wizard = aWizard();

		java.awt.Insets insets = wizard.getWizardContentPanel().getInsets();
		int asked = wizard.getWizardContentPanel().getPreferredSize().height;

		assertEquals(visibleStepOf(wizard).getPreferredSize().height + insets.top + insets.bottom,
			asked,
			"the content asks for a height no step on screen needs, which is what puts a scroll "
				+ "bar next to a short step");
		assertTrue(asked < wizard.preferredSizeForEveryStep().height,
			"the step on screen is not shorter than the tallest one, so this test proves nothing");
	}

	@Test
	@DisplayName("the size for every step holds each of them")
	void theSizeForEveryStepHoldsEachOfThem()
	{
		CertificateWizardPanel wizard = aWizard();

		Dimension forAll = wizard.preferredSizeForEveryStep();

		for (Component step : wizard.getWizardContentPanel().getComponents())
		{
			assertTrue(step.getPreferredSize().height <= forAll.height,
				"a step needs more height than the window would be opened with");
			assertTrue(step.getPreferredSize().width <= forAll.width,
				"a step needs more width than the window would be opened with");
		}
	}

	@Test
	@DisplayName("a window opened for every step does not scroll on the first one")
	void aWindowOpenedForEveryStepDoesNotScrollOnTheFirstOne()
	{
		CertificateWizardPanel wizard = aWizard();
		Dimension forAll = wizard.preferredSizeForEveryStep();
		JScrollPane scrollPane = new JScrollPane(wizard);
		scrollPane.setSize(forAll.width + 40, forAll.height + 40);
		scrollPane.doLayout();
		scrollPane.getViewport().doLayout();

		assertTrue(wizard.getPreferredSize().height <= scrollPane.getViewport().getHeight(),
			"the wizard asks for more height than the window it is opened in, so the user is made "
				+ "to scroll: asks for " + wizard.getPreferredSize().height + ", has "
				+ scrollPane.getViewport().getHeight());
	}

}
