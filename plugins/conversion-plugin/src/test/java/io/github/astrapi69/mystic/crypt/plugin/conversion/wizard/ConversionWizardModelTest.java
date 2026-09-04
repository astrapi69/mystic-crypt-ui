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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.key.PemType;
import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionSupport;

/**
 * Tests that {@link ConversionWizardModel} starts with sensible defaults and carries every value
 * written into it, since every wizard step reads and writes this model rather than the widgets.
 */
class ConversionWizardModelTest
{

	@Test
	void startsEmptyWithNothingToSayAndNoDetectedKindOrOperation()
	{
		ConversionWizardModel model = new ConversionWizardModel();

		assertEquals("", model.getSourceFilePath());
		assertEquals("", model.getTargetFilePath());
		assertEquals(ConversionWizardModel.NOTHING_TO_SAY, model.getWhatItHolds());
		assertEquals(ConversionWizardModel.NOTHING_TO_SAY, model.getResultMessage());
		assertNull(model.getFileKind());
		assertNull(model.getOperation());
	}

	@Test
	void carriesEveryValueWrittenIntoIt()
	{
		ConversionWizardModel model = new ConversionWizardModel();
		ConversionSupport.FileKind fileKind = new ConversionSupport.FileKind(true,
			PemType.PRIVATE_KEY, "a private key, PKCS#8");

		model.setSourceFilePath("/tmp/source.pem");
		model.setTargetFilePath("/tmp/target.der");
		model.setFileKind(fileKind);
		model.setOperation(ConversionOperation.PEM_TO_DER);
		model.setWhatItHolds("a private key, PKCS#8");
		model.setResultMessage("written as DER to target.der");

		assertEquals("/tmp/source.pem", model.getSourceFilePath());
		assertEquals("/tmp/target.der", model.getTargetFilePath());
		assertEquals(fileKind, model.getFileKind());
		assertEquals(ConversionOperation.PEM_TO_DER, model.getOperation());
		assertEquals("a private key, PKCS#8", model.getWhatItHolds());
		assertEquals("written as DER to target.der", model.getResultMessage());
	}
}
