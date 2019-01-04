package de.alpharogroup.mystic.crypt.panels.obfuscate;

import com.thoughtworks.xstream.XStream;
import de.alpharogroup.collections.pairs.KeyValuePair;
import de.alpharogroup.crypto.hex.HexExtensions;
import de.alpharogroup.crypto.obfuscation.rule.ObfuscationRule;
import de.alpharogroup.file.read.ReadFileExtensions;
import de.alpharogroup.file.write.WriteFileExtensions;
import de.alpharogroup.xml.ObjectToXmlExtensions;
import de.alpharogroup.xml.XmlToObjectExtensions;
import lombok.experimental.UtilityClass;
import org.apache.commons.codec.DecoderException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;


/**
 * The class {@link XmlEnDecryptionExtensions} provides methods for write and encrypt and read
 * and decrypt xml data.
 */
@UtilityClass
public class XmlEnDecryptionExtensions
{

	public static <T> void write(XStream xstream, final Map<String, Class<?>> aliases, T data,
		File file) throws IOException
	{
		String xmlString = ObjectToXmlExtensions.toXmlWithXStream(xstream, data, aliases);
		final String hexXmlString = HexExtensions.encodeHex(xmlString, Charset.forName("UTF-8"),
			true);
		WriteFileExtensions.writeStringToFile(file, hexXmlString, "UTF-8");
	}


	public static <T> T read(XStream xstream, final Map<String, Class<?>> aliases,
		File selectedFile) throws IOException, DecoderException
	{
		final String hexXmlString = ReadFileExtensions.readFromFile(selectedFile);
		String xmlString = HexExtensions.decodeHex(hexXmlString);

		return XmlToObjectExtensions
			.toObjectWithXStream(xstream, xmlString, aliases);
	}
}
