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
package io.github.astrapi69.mystic.crypt.wizard.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Every state's {@link WizardStateInfo} is built inline, one {@code boolean} flag at a time - and a
 * flag simply left off the builder chain is not "unset", it is {@code false}. That is exactly what
 * happened to {@link CertificateWizardState#DATES}: {@code previous} was never set, so the Previous
 * button was disabled on the one step in the middle of the wizard that most needs it. This pins
 * {@code hasPrevious}/{@code hasNext} for every state in the sequence, not only the one that was
 * wrong, since the same mistake could as easily be made on any of the others.
 */
class CertificateWizardStateTest
{

	@ParameterizedTest(name = "{0}: previous={1}, next={2}")
	@CsvSource({ "ISSUER, false, true", "SUBJECT, true, true", "DATES, true, true",
			"EXTENSIONS, true, true", "REVIEW, true, false" })
	void hasPreviousAndHasNextMatchThePositionInTheSequence(final CertificateWizardState state,
		final boolean expectedHasPrevious, final boolean expectedHasNext)
	{
		assertEquals(expectedHasPrevious, state.hasPrevious(),
			state + " disagrees with the wizard about whether it has a previous step");
		assertEquals(expectedHasNext, state.hasNext(),
			state + " disagrees with the wizard about whether it has a next step");
	}

}
