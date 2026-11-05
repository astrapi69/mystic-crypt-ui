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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Base64;
import java.util.logging.Level;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.github.astrapi69.crypt.api.algorithm.AesAlgorithm;
import io.github.astrapi69.crypt.api.algorithm.Algorithm;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.crypt.data.key.reader.PemObjectReader;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.model.CryptModel;
import io.github.astrapi69.file.create.FileFactory;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.algorithm.MysticSymmetricAlgorithm;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyDecryptor;
import io.github.astrapi69.mystic.crypt.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.PasswordType;
import io.github.astrapi69.mystic.crypt.pw.PasswordStringDecryptor;
import io.github.astrapi69.mystic.crypt.secret.SecretBuffers;
import lombok.NonNull;
import lombok.extern.java.Log;

@Log
public class ApplicationXmlFileReader
{

	public static ApplicationModelBean read(@NonNull MasterPwFileModelBean modelObject)
	{
		PasswordType passwordType = PasswordType.resolve(modelObject.isWithMasterPw(),
			modelObject.isWithKeyFile());
		if (passwordType.equals(PasswordType.PASSWORD_WITH_PRIVATE_KEY))
		{
			return readApplicationFileWithPasswordAndPrivateKey(modelObject);
		}
		else if (passwordType.equals(PasswordType.PASSWORD))
		{
			return readApplicationFileWithPassword(modelObject);
		}
		return readApplicationFileWithPrivateKey(modelObject);
	}

	/**
	 * Stamps the file that was actually opened onto the model that came out of it.
	 * <p>
	 * A database carries its own path inside its encrypted xml, which is the path it had when it
	 * was last saved. Without this, opening a file that was moved, renamed or copied leaves the
	 * application pointing at where the file used to be, and every later save goes there instead: a
	 * moved file is recreated at the old location holding the new entries, and a copy overwrites
	 * the database it was copied from. Neither says anything.
	 *
	 * @param applicationModelBean
	 *            the model read out of the file
	 * @param applicationFile
	 *            the file the user actually opened
	 * @return the same model, pointing at the file it was read from
	 */
	private static ApplicationModelBean openedFrom(final ApplicationModelBean applicationModelBean,
		final File applicationFile, final MasterPwFileModelBean credentials)
	{
		if (applicationModelBean == null || applicationModelBean.getMasterPwFileModelBean() == null
			|| applicationFile == null || credentials == null)
		{
			return applicationModelBean;
		}
		MasterPwFileModelBean signInModelBean = applicationModelBean.getMasterPwFileModelBean();
		signInModelBean.setApplicationFileInfo(FileInfo.toFileInfo(applicationFile));
		signInModelBean.setSelectedApplicationFilePath(applicationFile.getAbsolutePath());
		// the password is not in the file and must not be: it comes from whoever just signed in,
		// and the model needs it for the next save and for the unlock dialog
		signInModelBean.setMasterPw(credentials.getMasterPw());
		return applicationModelBean;
	}

	public static ApplicationModelBean readApplicationFileWithPasswordAndPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		char[] password = modelObject.getMasterPw();
		File keyFile = FileFactory.newFileQuietly(modelObject.getKeyFileInfo());
		try
		{
			return openedFrom(getApplicationModelBean(applicationFile, password, keyFile),
				applicationFile, modelObject);
		}
		catch (Exception exception)
		{
			log.log(Level.SEVERE, exception.getLocalizedMessage(), exception);
			throw notOpened("Authentication with Password or key file",
				"Password or key file or both are not valid", exception);
		}
	}

	public static ApplicationModelBean readApplicationFileWithPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		ApplicationModelBean applicationModelBean;
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		File keyFile = FileFactory.newFileQuietly(modelObject.getKeyFileInfo());
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security
				.addProvider(MysticCryptApplicationFrame.getInstance().getBouncyCastleProvider());
		}
		try
		{
			PrivateKey privateKey;
			if (modelObject.getPrivateKeyInfo() != null)
			{
				privateKey = KeyModelExtensions.toPrivateKey(modelObject.getPrivateKeyInfo());
			}
			else
			{
				if (PemObjectReader.isPemObject(keyFile))
				{
					privateKey = PrivateKeyReader.readPemPrivateKey(keyFile);
				}
				else
				{
					privateKey = PrivateKeyReader.readPrivateKey(keyFile);
				}
			}
			applicationModelBean = openedFrom(getApplicationModelBean(applicationFile, privateKey),
				applicationFile, modelObject);
		}
		catch (Exception exception)
		{
			throw notOpened("Authentication with key file", "Key file is not valid", exception);
		}
		return applicationModelBean;
	}

	public static ApplicationModelBean readApplicationFileWithPassword(
		@NonNull final MasterPwFileModelBean modelObject)
	{
		File applicationFile = FileFactory.newFileQuietly(modelObject.getApplicationFileInfo());
		char[] password = modelObject.getMasterPw();
		try
		{
			return openedFrom(getApplicationModelBean(applicationFile, password), applicationFile,
				modelObject);
		}
		catch (Exception exception)
		{
			throw notOpened("Authentication with Password", "Password is not valid", exception);
		}
	}

	/**
	 * What the sign-in shows when a database could not be opened, as {@code title::html}. A file
	 * that names a type no database holds says so and names the type, a file that decrypted but
	 * whose content cannot be read says so and names the reason (#624), and a file whose key the
	 * key file opened but whose content is in no known format says that (#650), rather than any of
	 * them being taken for a wrong password or key file; every other failure keeps the given title
	 * and reason.
	 *
	 * @param title
	 *            the title of the failure as the sign-in path names it
	 * @param reason
	 *            what the sign-in path takes to be wrong
	 * @param exception
	 *            what opening threw
	 * @return the exception to throw
	 */
	private static RuntimeException notOpened(final String title, final String reason,
		final Exception exception)
	{
		VaultHoldsAForeignTypeException foreign = causeOf(exception,
			VaultHoldsAForeignTypeException.class);
		if (foreign != null)
		{
			return databaseNotOpened("dialog.vault.foreign.type",
				"This file names the type %1$s, which no database of this application holds. It "
					+ "was not opened.",
				foreign.getTypeName(), foreign, exception);
		}
		VaultContentUnreadableException unreadable = causeOf(exception,
			VaultContentUnreadableException.class);
		if (unreadable != null)
		{
			return databaseNotOpened("dialog.vault.unreadable",
				"This database was decrypted, but its content could not be read: %1$s. It was not "
					+ "opened, and the file is unchanged.",
				unreadable.getReason(), unreadable, exception);
		}
		VaultFormatUnknownException unknownFormat = causeOf(exception,
			VaultFormatUnknownException.class);
		if (unknownFormat != null)
		{
			return databaseNotOpened("dialog.vault.format.unknown",
				"The key file opened the key of this database, but its content is in no format "
					+ "this version reads (%1$s). It was not opened, and the file is unchanged.",
				unknownFormat.getReason(), unknownFormat, exception);
		}
		String htmlMessage = "<html><body width='350'>" + "<h2>" + title + "</h2>" + "<p> " + reason
			+ "<p>" + exception.getMessage();
		return new RuntimeException(title + "::" + htmlMessage, exception);
	}

	/**
	 * A "Database not opened" failure from the bundle keys {@code <keyPrefix>.title} and
	 * {@code <keyPrefix>.message}, whose message names the given detail
	 *
	 * @param keyPrefix
	 *            the bundle key without {@code .title} or {@code .message}
	 * @param defaultMessage
	 *            the message when the bundle has none, with {@code %1$s} for the detail
	 * @param detail
	 *            what the message names, never any of the content
	 * @param cause
	 *            the failure that decides the message, logged
	 * @param exception
	 *            what opening threw
	 * @return the exception to throw
	 */
	private static RuntimeException databaseNotOpened(final String keyPrefix,
		final String defaultMessage, final String detail, final Exception cause,
		final Exception exception)
	{
		log.log(Level.SEVERE, cause.getMessage(), cause);
		String notOpenedTitle = Messages.getString(keyPrefix + ".title", "Database not opened");
		// %1$s rather than {0}: the bundle lookup formats braces itself and fills them with null
		String notOpenedReason = String
			.format(Messages.getString(keyPrefix + ".message", defaultMessage), detail);
		return new RuntimeException(notOpenedTitle + "::" + "<html><body width='350'>" + "<h2>"
			+ notOpenedTitle + "</h2>" + "<p>" + notOpenedReason, exception);
	}

	private static <T extends Throwable> T causeOf(final Throwable failure, final Class<T> type)
	{
		for (Throwable cause = failure; cause != null; cause = cause.getCause())
		{
			if (type.isInstance(cause))
			{
				return type.cast(cause);
			}
		}
		return null;
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile,
		char[] password) throws Exception
	{
		// PasswordVaultFormat decides by the marker in the file which of the two formats this is,
		// and reads a database written before the marker existed just as well
		char[] xml = PasswordVaultFormat.decrypt(applicationFile, password);
		try
		{
			return VaultXmlCodec.toModel(xml);
		}
		finally
		{
			SecretBuffers.wipe(xml);
		}
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile,
		char[] password, File keyFile) throws Exception
	{
		ApplicationModelBean applicationModelBean;
		PrivateKey privateKey;
		privateKey = PrivateKeyReader.readPemPrivateKey(keyFile);

		byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
		String encryptedXml = decryptedWithKeyFile(privateKey, encryptedBytes);
		String xml = decryptedWithPassword(encryptedXml, password);
		applicationModelBean = throughTheCodec(xml);
		// the key is not in the file and must not be (#350): it comes from the key file whoever
		// signed in just picked, and the model needs it for the next save and for the plugins that
		// read it. The key-only path below does the same thing for the same reason
		applicationModelBean.getMasterPwFileModelBean()
			.setPrivateKeyInfo(KeyModelExtensions.toKeyModel(privateKey));
		return applicationModelBean;
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile, File keyFile)
		throws Exception
	{
		return getApplicationModelBean(applicationFile,
			PrivateKeyReader.readPemPrivateKey(keyFile));
	}

	public static ApplicationModelBean getApplicationModelBean(File applicationFile,
		PrivateKey privateKey) throws Exception
	{
		ApplicationModelBean applicationModelBean;
		byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(applicationFile);
		String xml = decryptedWithKeyFile(privateKey, encryptedBytes);
		applicationModelBean = throughTheCodec(xml);
		applicationModelBean.getMasterPwFileModelBean()
			.setPrivateKeyInfo(KeyModelExtensions.toKeyModel(privateKey));
		return applicationModelBean;
	}

	/**
	 * The password layer of a database with a password and a key file, in the format this version
	 * writes or in the one 8 wrote: {@code PBEWithMD5AndDES} with the fixed salt (mystic-crypt 9),
	 * which the next save replaces (#650). Neither layer authenticates, so when both fail the
	 * failure stays the current format's, and the password is called not valid as before
	 *
	 * @param encryptedXml
	 *            the password layer, in Base64
	 * @param password
	 *            the master password
	 * @return the xml
	 * @throws Exception
	 *             when the password does not decrypt it in either format
	 */
	private static String decryptedWithPassword(final String encryptedXml, final char[] password)
		throws Exception
	{
		// one unwipeable copy of the master password, and one of the xml: this path decrypts
		// through PasswordStringDecryptor, which takes a String and is a library class that is not
		// changed from this repository (#294, architecture.md)
		String passwordText = String.valueOf(password);
		try
		{
			return new PasswordStringDecryptor(passwordText).decrypt(encryptedXml);
		}
		catch (GeneralSecurityException notTheCurrentFormat)
		{
			try
			{
				return PasswordVaultFormat.decryptLegacy(Base64.getDecoder().decode(encryptedXml),
					passwordText);
			}
			catch (GeneralSecurityException notTheFormatOfEight)
			{
				notTheCurrentFormat.addSuppressed(notTheFormatOfEight);
				throw notTheCurrentFormat;
			}
		}
	}

	/**
	 * The content behind the key that the key file opens, in the format this version writes or in
	 * the one 8 wrote. 8 encrypted the content with plain AES (mystic-crypt 9), and everything
	 * since 8.1.1 with AES-GCM, and both wrap the key the same way. A wrong key file fails at that
	 * key and stays a wrong key file; a failed GCM tag means the key was right and the content
	 * older, so the content is read the way 8 wrote it, and the next save writes the current format
	 * (#650). Content that neither reads is reported as an unknown format, never as a wrong key
	 * file.
	 *
	 * @param privateKey
	 *            the key from the key file
	 * @param encryptedBytes
	 *            the file's bytes
	 * @return the content
	 * @throws Exception
	 *             when the key file does not open the key, or the content cannot be read
	 */
	private static String decryptedWithKeyFile(final PrivateKey privateKey,
		final byte[] encryptedBytes) throws Exception
	{
		try
		{
			return decryptedWith(privateKey, MysticSymmetricAlgorithm.AES_GCM_NO_PADDING,
				encryptedBytes);
		}
		catch (AEADBadTagException notTheCurrentFormat)
		{
			try
			{
				return decryptedWith(privateKey, AesAlgorithm.AES, encryptedBytes);
			}
			catch (GeneralSecurityException | IOException notTheFormatOfEight)
			{
				notTheFormatOfEight.addSuppressed(notTheCurrentFormat);
				throw new VaultFormatUnknownException(notTheFormatOfEight);
			}
		}
	}

	private static String decryptedWith(final PrivateKey privateKey,
		final Algorithm symmetricAlgorithm, final byte[] encryptedBytes) throws Exception
	{
		CryptModel<Cipher, PrivateKey, byte[]> decryptModel = CryptModel
			.<Cipher, PrivateKey, byte[]> builder().key(privateKey).build();
		PrivateKeyGenericDecryptor<String> genericDecryptor = new PrivateKeyGenericDecryptor<>(
			new PrivateKeyDecryptor(decryptModel, symmetricAlgorithm));
		return genericDecryptor.decrypt(encryptedBytes);
	}

	/**
	 * Reads the model the way the password path does: through the codec, which skips an element it
	 * does not know instead of failing into "Password is not valid" (#402). The characters handed
	 * to it are overwritten; the String is the one copy these paths already kept (#294)
	 *
	 * @param xml
	 *            the decrypted xml
	 * @return the model
	 */
	private static ApplicationModelBean throughTheCodec(final String xml)
	{
		char[] characters = xml.toCharArray();
		try
		{
			return VaultXmlCodec.toModel(characters);
		}
		finally
		{
			SecretBuffers.wipe(characters);
		}
	}
}
