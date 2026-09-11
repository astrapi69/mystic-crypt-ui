package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.thoughtworks.xstream.XStream;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.xstream.factory.XStreamFactory;

/**
 * An entry's custom properties cannot be turned into character arrays the way its own six text
 * fields were, and this is where that is written down so the next person finds it before the vault
 * format does (#335).
 * <p>
 * {@link VaultFormatIsUnchangedTest} pins the opposite claim about a different field, and both are
 * true for the same reason. The six fields #294 converted are declared on the bean, so XStream
 * writes the declared type and emits no class attribute: the text is identical whether the field is
 * a String or a character array. {@code KeyValuePair} declares {@code K key} and {@code V value},
 * which erase to {@code Object}, so XStream has to write the runtime type beside the value.
 * {@code <value class="string">} would become {@code <value class="char-array">}, in every vault
 * ever written and in every vault written afterwards.
 * <p>
 * <b>And it would not fail loudly.</b> XStream honours the class attribute rather than the field's
 * declared type, so a document of either shape parses without complaint and puts a String into a
 * field the compiler believes holds characters. The {@code ClassCastException} arrives later, at
 * the first {@link MysticCryptEntryModelBean#getProperty(String)} - a vault that opens, lists its
 * entries, and throws when something asks for a property. The last test here measures exactly that,
 * because a silent break is the part worth having in the repository rather than in a comment.
 * <p>
 * This is not hypothetical data: the KeePass import fills these for every custom field it finds, so
 * every imported database has a non-empty properties list.
 */
class EntryPropertiesTypeIsPartOfTheFormatTest
{

	/** Made up per run, for the same reason {@link VaultFormatIsUnchangedTest} does it */
	private static final String ENTRY_PASSWORD = TestPasswords.throwaway();

	/**
	 * An entry with a custom property, in the shape the application writes today. Captured from the
	 * codec's own output rather than written by hand
	 */
	private static final String XML_WITH_A_PROPERTY = """
		<io.github.astrapi69.mystic.crypt.ApplicationModelBean>
		  <dataOfNodes class="linked-hash-map">
		    <entry>
		      <long>1</long>
		      <list>
		        <io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean>
		          <title>the bank</title>
		          <password>%s</password>
		          <expirable>false</expirable>
		          <showPassword>false</showPassword>
		          <resources/>
		          <properties>
		            <io.github.astrapi69.collection.pair.KeyValuePair>
		              <key class="string">path</key>
		              <value class="string">Root/Banking</value>
		            </io.github.astrapi69.collection.pair.KeyValuePair>
		          </properties>
		          <dateTimesOfModification/>
		        </io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean>
		      </list>
		    </entry>
		  </dataOfNodes>
		  <showSplash>false</showSplash>
		  <signedIn>false</signedIn>
		  <dirty>false</dirty>
		</io.github.astrapi69.mystic.crypt.ApplicationModelBean>""".formatted(ENTRY_PASSWORD);

	/**
	 * Why this is a reflection test and not the obvious one.
	 * <p>
	 * The obvious guard - call {@code setProperty}, assert on the result - does not guard: those
	 * methods change signature with the field, so the test would fail to COMPILE after the change,
	 * and a compile error carries no message and reads as "update the test". A raw-typed test is
	 * worse: it compiles before and after AND still writes {@code class="string"}, so it would stay
	 * green straight through the break. The declared generic type is the one thing that is readable
	 * either way and can carry a sentence with it.
	 *
	 * @param fieldName
	 *            the field whose declared type is part of the vault format
	 * @param expectedType
	 *            the type name that is written into every vault file
	 */
	// the delimiter is a pipe because the values are generic type names and are full of commas
	@ParameterizedTest(name = "{0} is declared as {1}, and the vault file carries that")
	@CsvSource(delimiter = '|', value = {
			"properties | java.util.List<io.github.astrapi69.collection.pair.KeyValuePair<java.lang.String, java.lang.String>>",
			"dateTimesOfModification | io.github.astrapi69.collection.pair.KeySetPair<java.lang.String, java.time.OffsetDateTime>" })
	void aGenericFieldsTypeArguments_arePinned_becauseTheVaultFileCarriesThem(String fieldName,
		String expectedType) throws NoSuchFieldException
	{
		Field declared = MysticCryptEntryModelBean.class.getDeclaredField(fieldName);

		assertEquals(expectedType, declared.getGenericType().getTypeName(),
			"changing this field's type arguments is a VAULT FORMAT BREAK, not a refactor. "
				+ "KeyValuePair and KeySetPair declare their own type variables, which erase to "
				+ "Object, so XStream writes the RUNTIME type into the element: <value "
				+ "class=\"string\"> becomes <value class=\"char-array\">. Every vault in existence "
				+ "is then wrong, and a vault written here is wrong for every earlier release - and "
				+ "not loudly, see theBreakIsSilent below. This is why #294 was safe for title, "
				+ "userName, password, repeat, url and notes (declared char[], no class attribute) "
				+ "and is not safe here. If it has to change it needs a converter or a format "
				+ "version and a migration, decided in an issue first - architecture.md makes the "
				+ "vault format a protected decision (#335)");
	}

	/**
	 * A generic value beside a declared one, so the difference is visible in one document.
	 * <p>
	 * An ArrayList rather than {@code List.of}, because the immutable list serializes through
	 * {@code java.util.CollSer}, which the application's own XStream allows to be written and
	 * refuses to read back - and the point here is a document that can be read
	 */
	public static class WithStringProperties
	{
		char[] declaredCharacters = "beside it".toCharArray();
		List<KeyValuePair<String, String>> properties = new ArrayList<>(List
			.of(KeyValuePair.<String, String> builder().key("path").value("Root/Banking").build()));
	}

	public static class WithCharacterProperties
	{
		char[] declaredCharacters = "beside it".toCharArray();
		List<KeyValuePair<String, char[]>> properties = new ArrayList<>(List.of(KeyValuePair
			.<String, char[]> builder().key("path").value("Root/Banking".toCharArray()).build()));
	}

	@Test
	@DisplayName("a generic value carries its class into the xml, a declared one does not")
	void aGenericValue_carriesItsClass_whileTheFieldBesideItDoesNot()
	{
		String withStrings = xmlOf(WithStringProperties.class, new WithStringProperties());
		String withCharacters = xmlOf(WithCharacterProperties.class, new WithCharacterProperties());

		assertTrue(withStrings.contains("<value class=\"string\">Root/Banking</value>"),
			"what every vault file written so far contains, so: " + withStrings);
		assertTrue(withCharacters.contains("<value class=\"char-array\">Root/Banking</value>"),
			"and what the same data would become, so: " + withCharacters);
		assertNotEquals(withStrings, withCharacters,
			"the two documents differ, which is the whole finding - this conversion is not the "
				+ "no-op that #294's was");
		assertTrue(
			withStrings.contains("<declaredCharacters>beside it</declaredCharacters>")
				&& withCharacters.contains("<declaredCharacters>beside it</declaredCharacters>"),
			"and the contrast that explains it: a character array in a DECLARED field is written "
				+ "as plain text in both documents. The difference is the erasure, not the type");
	}

	@Test
	@DisplayName("a vault with a custom property round-trips through the application's own codec")
	void aVaultWithAProperty_roundTrips_inBothDirections()
	{
		ApplicationModelBean read = VaultXmlCodec.toModel(XML_WITH_A_PROPERTY.toCharArray());

		MysticCryptEntryModelBean entry = read.getDataOfNodes().get(1L).get(0);
		assertEquals("Root/Banking", entry.getProperty("path"),
			"a non-empty properties list was covered by nothing in this repository until now - the "
				+ "existing golden fixture carries <properties/>, which is the one shape that "
				+ "cannot show the class attribute");
		assertEquals("Root/Banking", entry.getPath(),
			"and the accessor the application actually calls reads it, which is where the "
				+ "ClassCastException would land");

		assertEquals(XML_WITH_A_PROPERTY, new String(VaultXmlCodec.toXml(modelWith(entry))),
			"character for character: what is written is what an earlier build reads");
	}

	@Test
	@DisplayName("the break would be silent - the document parses and the failure comes later")
	void theBreakIsSilent_whichIsWhatMakesItWorthPinning()
	{
		// both fixtures under ONE alias, so a document written by the future shape is handed to the
		// present one exactly as a future vault file would be handed to this build
		XStream writer = XStreamFactory.initializeXStream(null, null);
		writer.alias("holder", WithCharacterProperties.class);
		String writtenByAHypotheticalFutureBuild = writer.toXML(new WithCharacterProperties());
		XStream readingItAsTodaysBuildWould = XStreamFactory.initializeXStream(null, null);
		readingItAsTodaysBuildWould.alias("holder", WithStringProperties.class);

		WithStringProperties parsed = (WithStringProperties)readingItAsTodaysBuildWould
			.fromXML(writtenByAHypotheticalFutureBuild);

		assertFalse(parsed.properties.isEmpty(),
			"it parses, with no complaint at all. That is the dangerous half");
		assertThrows(ClassCastException.class, () -> {
			String value = parsed.properties.get(0).getValue();
			assertNotEquals("unreachable", value);
		}, "XStream honours the class attribute rather than the declared type, so the wrong object "
			+ "sits in a field the compiler believes is right and the failure arrives at the first "
			+ "read. In the application that read is getProperty, and what the user sees is a vault "
			+ "that opens, lists its entries, and throws");
	}

	private static String xmlOf(Class<?> type, Object holder)
	{
		XStream xstream = XStreamFactory.initializeXStream(null, null);
		xstream.alias(type.getSimpleName(), type);
		return xstream.toXML(holder);
	}

	private static ApplicationModelBean modelWith(MysticCryptEntryModelBean entry)
	{
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new LinkedHashMap<>();
		dataOfNodes.put(1L, new ArrayList<>(List.of(entry)));
		return ApplicationModelBean.builder().dataOfNodes(dataOfNodes).build();
	}
}
