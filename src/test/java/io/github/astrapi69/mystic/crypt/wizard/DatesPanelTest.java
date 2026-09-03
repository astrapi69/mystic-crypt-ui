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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;

/**
 * The Dates step used to show a permanently embedded calendar grid whose picked date never reached
 * the model - the certificate got built with whatever validity dates the wizard started with, no
 * matter what was clicked here (#105)
 */
class DatesPanelTest
{

	private static DatesPanel newPanel(ZonedDateTime notBefore, ZonedDateTime notAfter)
	{
		CertificateInfoModel certificateInfoModel = CertificateInfoModel.builder()
			.issuer(DistinguishedNameInfoModel.builder().commonName("issuer").build())
			.subject(DistinguishedNameInfoModel.builder().commonName("subject").build())
			.validityModel(ValidityModel.builder().notBefore(notBefore).notAfter(notAfter).build())
			.build();
		IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model = BaseModel
			.of(BaseWizardStateMachineModel.<CertificateInfoModel> builder()
				.currentState(CertificateWizardState.DATES).modelObject(certificateInfoModel)
				.build());
		return new DatesPanel(model);
	}

	@Test
	void pickingANotBeforeDateReachesTheValidityModel()
	{
		ZonedDateTime now = ZonedDateTime.now();
		DatesPanel panel = newPanel(now, now.plusYears(1));
		LocalDate picked = now.toLocalDate().minusMonths(1);

		panel.getTxtNotBefore().setDate(picked);

		assertEquals(picked,
			panel.getModelObject().getModelObject().getValidityModel().getNotBefore().toLocalDate(),
			"the date picked in the Not Before field must reach the certificate "
				+ "that gets built, not only the text field");
	}

	@Test
	void pickingANotAfterDateReachesTheValidityModel()
	{
		ZonedDateTime now = ZonedDateTime.now();
		DatesPanel panel = newPanel(now, now.plusYears(1));
		LocalDate picked = now.toLocalDate().plusYears(2);

		panel.getTxtNotAfter().setDate(picked);

		assertEquals(picked,
			panel.getModelObject().getModelObject().getValidityModel().getNotAfter().toLocalDate(),
			"the date picked in the Not After field must reach the certificate that "
				+ "gets built, not only the text field");
	}

	@Test
	void pickingADateDoesNotDropTheTimeOfDayOrZone()
	{
		ZonedDateTime now = ZonedDateTime.now();
		DatesPanel panel = newPanel(now, now.plusYears(1));

		panel.getTxtNotBefore().setDate(now.toLocalDate().plusDays(5));

		ZonedDateTime updated = panel.getModelObject().getModelObject().getValidityModel()
			.getNotBefore();
		assertEquals(now.toLocalTime().withNano(0), updated.toLocalTime().withNano(0));
		assertEquals(now.getZone(), updated.getZone());
	}

	@Test
	void theStepDoesNotEmbedAPermanentlyVisibleCalendarGrid()
	{
		ZonedDateTime now = ZonedDateTime.now();
		DatesPanel panel = newPanel(now, now.plusYears(1));

		// a com.github.lgooddatepicker.components.CalendarPanel is the always-open 42-cell month
		// grid a DatePicker only opens transiently in its own popup - it must not sit permanently
		// in this panel's own component tree
		assertTrue(
			java.util.Arrays.stream(panel.getComponents()).noneMatch(
				component -> component.getClass().getSimpleName().equals("CalendarPanel")),
			"a CalendarPanel must not be embedded directly in the step");
	}

	private static void assertHasTooltip(JComponent component, String fieldName)
	{
		String tooltip = component.getToolTipText();
		assertNotNull(tooltip, fieldName + " must have a tooltip");
		assertFalse(tooltip.isBlank(), fieldName + " must have a tooltip");
	}

	@Test
	void everyFieldExplainsItselfWithATooltip()
	{
		ZonedDateTime now = ZonedDateTime.now();
		DatesPanel panel = newPanel(now, now.plusYears(1));

		assertHasTooltip(panel.getCmbVersion(), "version");
		assertHasTooltip(panel.getTxtSerialNumber(), "serial number");
		assertHasTooltip(panel.getBtnGenerateSerialNumber(), "generate serial number");
		assertHasTooltip(panel.getTxtNotBefore(), "not before");
		assertHasTooltip(panel.getTxtNotAfter(), "not after");
		assertHasTooltip(panel.getCmbSignatureAlgorithm(), "signature algorithm");
	}

}
