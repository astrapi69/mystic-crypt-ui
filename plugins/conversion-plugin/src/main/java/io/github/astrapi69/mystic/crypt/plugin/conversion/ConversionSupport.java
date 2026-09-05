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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.util.List;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import io.github.astrapi69.crypt.api.key.PemType;
import io.github.astrapi69.crypt.data.key.reader.PemObjectReader;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.mystic.crypt.crypto.KeyFiles;

/**
 * Converting a key or certificate file between the shapes it can have, without any user interface.
 * <p>
 * The old tool made the user declare what a file held - private key, public key, certificate - and
 * converted in one direction only. A file says what it is: a PEM file carries its type in its
 * header, and a DER file is decided by what can be read out of it. So nothing has to be declared,
 * and a wrong guess cannot silently produce a broken file.
 */
public final class ConversionSupport
{

	/** The header a PKCS#8 PEM carries, which is what a key without a traditional form falls back to. */
	private static final String PKCS8_PEM_HEADER = "-----BEGIN PRIVATE KEY-----";

	/** What was found in a file: its shape, its type, and what to call that in a sentence.
	 *
	 * @param pem
	 *            whether the file is PEM rather than DER
	 * @param pemType
	 *            the type from the PEM header, UNKNOWN for a DER file
	 * @param description
	 *            what the file holds, in words
	 */
	public record FileKind(boolean pem, PemType pemType, String description)
	{
	}

	private ConversionSupport()
	{
	}

	/**
	 * What a file holds, worked out rather than declared
	 *
	 * @param file
	 *            the file to look at
	 * @return what was found
	 * @throws Exception
	 *             if the file cannot be read at all
	 */
	/**
	 * The file a conversion writes to: the one that was picked, or - when none was picked - the
	 * source file with a {@code .pem} ending, next to it.
	 * <p>
	 * Picking an output file is the step everybody forgets, and forgetting it used to mean the
	 * conversion wrote to nothing at all.
	 *
	 * @param sourceFile
	 *            the file being converted
	 * @param pickedTarget
	 *            the file the user picked, may be null
	 * @return the file to write to
	 */
	public static File pemFileFor(final File sourceFile, final File pickedTarget)
	{
		if (pickedTarget != null)
		{
			return pickedTarget;
		}
		String name = sourceFile.getName();
		int lastDot = name.lastIndexOf('.');
		String withoutEnding = 0 < lastDot ? name.substring(0, lastDot) : name;
		return new File(sourceFile.getAbsoluteFile().getParentFile(), withoutEnding + ".pem");
	}

	public static FileKind kindOf(final File file) throws Exception
	{
		if (file == null || !file.isFile())
		{
			throw new IllegalArgumentException("'" + file + "' is not a file that could be read");
		}
		if (PemObjectReader.isPemObject(file))
		{
			PemType pemType = PemObjectReader.getPemType(file);
			return new FileKind(true, pemType, describe(pemType));
		}
		// a DER file carries no header, so what it is shows in what can be read out of it
		try
		{
			KeyFiles.readPrivateKey(file);
			return new FileKind(false, PemType.UNKNOWN, "a private key in DER form");
		}
		catch (Exception notAPrivateKey)
		{
			// try the next shape
		}
		try
		{
			KeyFiles.readCertificate(file);
			return new FileKind(false, PemType.UNKNOWN, "a certificate in DER form");
		}
		catch (Exception notACertificate)
		{
			// try the next shape
		}
		try
		{
			KeyFiles.readPublicKey(file);
			return new FileKind(false, PemType.UNKNOWN, "a public key in DER form");
		}
		catch (Exception notAPublicKeyEither)
		{
			return new FileKind(false, PemType.UNKNOWN, "nothing this tool recognises");
		}
	}

	/**
	 * What a pem type is called in a sentence
	 *
	 * @param pemType
	 *            the type
	 * @return the description
	 */
	public static String describe(final PemType pemType)
	{
		return switch (pemType)
		{
			case RSA_PRIVATE_KEY -> "an RSA private key, PKCS#1";
			case EC_PRIVATE_KEY -> "an EC private key, SEC1";
			case DSA_PRIVATE_KEY -> "a DSA private key";
			case PRIVATE_KEY -> "a private key, PKCS#8";
			case PUBLIC_KEY -> "a public key";
			case RSA_PUBLIC_KEY -> "an RSA public key";
			case CERTIFICATE -> "a certificate";
			case CERTIFICATE_REQUEST, NEW_CERTIFICATE_REQUEST -> "a certificate request";
			case X509_CRL -> "a certificate revocation list";
			case PKCS7_KEY -> "a PKCS#7 bundle";
			case PGP_PRIVATE_KEY -> "a PGP private key";
			case PGP_PUBLIC_KEY -> "a PGP public key";
			default -> "something this tool does not recognise";
		};
	}

	/**
	 * Writes the content of a PEM file as DER: the same bytes, without the header and the Base64
	 *
	 * @param source
	 *            the PEM file
	 * @param target
	 *            the file to write
	 * @throws Exception
	 *             if the source is not PEM or the target cannot be written
	 */
	public static void pemToDer(final File source, final File target) throws Exception
	{
		PemObject pemObject = PemObjectReader.getPemObject(source);
		if (pemObject == null)
		{
			throw new IllegalArgumentException("'" + source + "' is not a pem file");
		}
		writeNewFile(target, PemObjectReader.toDer(pemObject));
	}

	/**
	 * Writes the content of a DER file as PEM, under the header that fits what it holds
	 *
	 * @param source
	 *            the DER file
	 * @param target
	 *            the file to write
	 * @throws Exception
	 *             if what the file holds cannot be worked out, or the target cannot be written
	 */
	public static void derToPem(final File source, final File target) throws Exception
	{
		FileKind kind = kindOf(source);
		if (kind.pem())
		{
			throw new IllegalArgumentException("'" + source + "' is already pem");
		}
		String header = switch (kind.description())
		{
			case "a private key in DER form" -> "PRIVATE KEY";
			case "a certificate in DER form" -> "CERTIFICATE";
			case "a public key in DER form" -> "PUBLIC KEY";
			default -> throw new IllegalArgumentException(
				"'" + source + "' holds " + kind.description());
		};
		writePem(target, header, Files.readAllBytes(source.toPath()));
	}

	/**
	 * Writes a private key file as PKCS#8 - "BEGIN PRIVATE KEY" - whatever shape it was in.
	 * <p>
	 * That is what Java reads and writes, and what a key from openssl usually is not.
	 *
	 * @param source
	 *            the key file, PEM or DER, in any of the private key shapes
	 * @param target
	 *            the file to write
	 * @throws Exception
	 *             if the file holds no private key or the target cannot be written
	 */
	public static void toPkcs8(final File source, final File target) throws Exception
	{
		PrivateKey privateKey = KeyFiles.readPrivateKey(source);
		writePem(target, "PRIVATE KEY", privateKey.getEncoded());
	}

	/**
	 * Writes a private key file as PKCS#1 - "BEGIN RSA PRIVATE KEY" and its relatives - whatever
	 * shape it was in.
	 * <p>
	 * That is what openssl, nginx and much of the rest of the world expect.
	 *
	 * @param source
	 *            the key file, PEM or DER, in any of the private key shapes
	 * @param target
	 *            the file to write
	 * @throws Exception
	 *             if the file holds no private key or the target cannot be written
	 */
	public static void toPkcs1(final File source, final File target) throws Exception
	{
		PrivateKey privateKey = KeyFiles.readPrivateKey(source);
		String pem = io.github.astrapi69.crypt.data.key.PrivateKeyExtensions
			.toPemFormat(privateKey);
		requirePkcs1WasProduced(pem, privateKey);
		Files.writeString(requireNewFile(target).toPath(), pem, StandardCharsets.UTF_8);
	}

	/**
	 * Refuses a conversion the key cannot satisfy. RSA, DSA, EC and RSASSA-PSS have a traditional
	 * form of their own; the edwards and montgomery families, Diffie-Hellman and the post-quantum
	 * families have only PKCS#8, and crypt-data's writer falls through to it silently. The decision
	 * is read off the PEM that was produced rather than predicted from the algorithm name, so it
	 * cannot drift away from what the writer actually does - a second list of names is what went
	 * wrong everywhere else this question was answered.
	 *
	 * @param pem
	 *            the PEM the writer produced
	 * @param privateKey
	 *            the key it was produced from, named in the message
	 * @throws IllegalArgumentException
	 *             if PKCS#8 is what came out
	 */
	private static void requirePkcs1WasProduced(final String pem, final PrivateKey privateKey)
	{
		if (pem.startsWith(PKCS8_PEM_HEADER))
		{
			throw new IllegalArgumentException("PKCS#1 was asked for, but a '"
				+ privateKey.getAlgorithm()
				+ "' private key has no traditional form - PKCS#8 is the only encoding it has.");
		}
	}

	/**
	 * Whether a file holds a private key at all, which is what the two format conversions need
	 *
	 * @param file
	 *            the file to look at
	 * @return true if a private key can be read out of it
	 */
	public static boolean holdsAPrivateKey(final File file)
	{
		try
		{
			return KeyFiles.readPrivateKey(file) != null;
		}
		catch (Exception notAPrivateKey)
		{
			return false;
		}
	}

	/**
	 * The shape a private key file is in, as the library reports it
	 *
	 * @param file
	 *            the key file
	 * @return the format
	 * @throws Exception
	 *             if the file cannot be read
	 */
	public static io.github.astrapi69.crypt.api.key.KeyFileFormat formatOf(final File file)
		throws Exception
	{
		return PrivateKeyReader.getKeyFormat(file);
	}

	/** The types this tool can say something about */
	public static List<PemType> knownPemTypes()
	{
		return List.of(PemType.PRIVATE_KEY, PemType.RSA_PRIVATE_KEY, PemType.EC_PRIVATE_KEY,
			PemType.DSA_PRIVATE_KEY, PemType.PUBLIC_KEY, PemType.RSA_PUBLIC_KEY, PemType.CERTIFICATE,
			PemType.CERTIFICATE_REQUEST, PemType.X509_CRL, PemType.PKCS7_KEY);
	}

	private static void writePem(final File target, final String header, final byte[] content)
		throws Exception
	{
		try (java.io.Writer writer = Files.newBufferedWriter(requireNewFile(target).toPath(),
			StandardCharsets.US_ASCII); PemWriter pemWriter = new PemWriter(writer))
		{
			pemWriter.writeObject(new PemObject(header, content));
		}
	}

	private static void writeNewFile(final File target, final byte[] content) throws Exception
	{
		Files.write(requireNewFile(target).toPath(), content);
	}

	/** Overwriting a key file that is already there is not something anybody recovers from */
	private static File requireNewFile(final File target)
	{
		if (target == null)
		{
			throw new IllegalArgumentException("choose a file to write");
		}
		if (target.exists())
		{
			throw new IllegalArgumentException(
				"'" + target + "' already exists - pick another name or remove it first");
		}
		return target;
	}
}
