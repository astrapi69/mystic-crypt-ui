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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JComponent;
import javax.swing.JTextArea;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;

/**
 * Tests that the wizard's Review step shows exactly the summary it was given
 */
class ConversionReviewPanelTest
{

	private static ConversionReviewPanel newPanel()
	{
		BaseWizardStateMachineModel<ConversionWizardModel> stateMachine = BaseWizardStateMachineModel
			.<ConversionWizardModel> builder().currentState(ConversionWizardState.REVIEW)
			.modelObject(new ConversionWizardModel()).build();
		return new ConversionReviewPanel(BaseModel.of(stateMachine));
	}

	@Test
	void refreshShowsExactlyTheGivenSummary()
	{
		ConversionReviewPanel panel = newPanel();

		panel.refresh("Source: /tmp/key.pem\nConversion: to DER");

		assertEquals("Source: /tmp/key.pem\nConversion: to DER", textArea(panel, "txtSummary").getText());
	}

	@Test
	void theSummaryFieldExplainsItselfWithATooltip()
	{
		ConversionReviewPanel panel = newPanel();

		String tooltip = ((JComponent)componentNamed(panel, "txtSummary")).getToolTipText();

		assertTrue(tooltip != null && !tooltip.isBlank(), "the summary field must have a tooltip");
	}

	private JTextArea textArea(Container container, String name)
	{
		return (JTextArea)componentNamed(container, name);
	}

	private static Component componentNamed(Container container, String name)
	{
		for (Component child : container.getComponents())
		{
			if (name.equals(child.getName()))
			{
				return child;
			}
			if (child instanceof Container nested)
			{
				Component found = componentNamed(nested, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
