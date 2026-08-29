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
package io.github.astrapi69.mystic.crypt.ui.form;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.panel.certificate.NewCertificateInfoPanel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.AttachmentPanel;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryPanel;
import io.github.astrapi69.mystic.crypt.panel.keepass.ExportKeePassPanel;
import io.github.astrapi69.mystic.crypt.panel.keepass.ImportKeePassPanel;
import io.github.astrapi69.mystic.crypt.panel.keygen.EnDecryptPanel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesNewEntryPanel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesPanel;
import io.github.astrapi69.mystic.crypt.panel.pw.GeneratePasswordPanel;
import io.github.astrapi69.mystic.crypt.panel.search.SearchToolbarPanel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.DistinguishedNameInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.model.ValidityModel;
import io.github.astrapi69.swing.base.BasePanel;

/**
 * Every panel of this application holds its state in a model, and a field that was placed but never
 * bound writes what the user types into nothing. One assertion per panel catches that, including
 * for a field added later by someone who did not think of it.
 * <p>
 * Panels that need a running application to be built are not here; they are reached through the
 * end-to-end suite instead. The two KeePass panels are not here either, and for a different reason:
 * they hold no model at all (see #77).
 */
class PanelsAreBoundTest
{

	private static MysticCryptEntryModelBean anEntry()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		entry.setProperties(
			List.of(KeyValuePair.<String, String> builder().key("k").value("v").build()));
		entry.setResources(
			List.of(FileContentInfo.builder().name("a.txt").content(new byte[] { 1 }).build()));
		return entry;
	}

	/**
	 * A certificate description with a validity period in it: the two date fields write into that
	 * period, and a description without one has nowhere for them to write - which says nothing
	 * about the binding
	 *
	 * @return the description
	 */
	private static CertificateInfoModel aCertificateToDescribe()
	{
		ZonedDateTime now = ZonedDateTime.now();
		return CertificateInfoModel.builder()
			.issuer(DistinguishedNameInfoModel.builder().commonName("issuer").build())
			.subject(DistinguishedNameInfoModel.builder().commonName("subject").build())
			.validityModel(
				ValidityModel.builder().notBefore(now).notAfter(now.plusYears(1)).build())
			.build();
	}

	static Stream<Arguments> everyPanelThatStandsOnItsOwn()
	{
		return Stream.of(
			Arguments.of("EnDecryptPanel", (Supplier<BasePanel<?>>)EnDecryptPanel::new),
			Arguments.of("GeneratePasswordPanel",
				(Supplier<BasePanel<?>>)GeneratePasswordPanel::new),
			Arguments.of("PropertiesNewEntryPanel",
				(Supplier<BasePanel<?>>)PropertiesNewEntryPanel::new),
			Arguments.of("NewCertificateInfoPanel",
				(Supplier<BasePanel<?>>)() -> new NewCertificateInfoPanel(
					BaseModel.of(aCertificateToDescribe()))),
			Arguments.of("MysticCryptEntryPanel",
				(Supplier<BasePanel<?>>)() -> new MysticCryptEntryPanel(BaseModel.of(anEntry()))),
			Arguments.of("AttachmentPanel",
				(Supplier<BasePanel<?>>)() -> new AttachmentPanel(BaseModel.of(anEntry()))),
			Arguments.of("SearchToolbarPanel", (Supplier<BasePanel<?>>)SearchToolbarPanel::new),
			Arguments.of("ImportKeePassPanel", (Supplier<BasePanel<?>>)ImportKeePassPanel::new),
			Arguments.of("ExportKeePassPanel", (Supplier<BasePanel<?>>)ExportKeePassPanel::new),
			Arguments.of("PropertiesPanel",
				(Supplier<BasePanel<?>>)() -> new PropertiesPanel(BaseModel.of(anEntry()))));
	}

	@ParameterizedTest(name = "every model component of {0} is bound")
	@MethodSource("everyPanelThatStandsOnItsOwn")
	void everyModelComponentOfThePanelIsBound(final String name, final Supplier<BasePanel<?>> panel)
	{
		List<String> unbound = ModelBinding.unboundComponentsOf(panel.get());

		assertTrue(unbound.isEmpty(),
			name + " has components that write what the user types into nothing: " + unbound);
	}

}
