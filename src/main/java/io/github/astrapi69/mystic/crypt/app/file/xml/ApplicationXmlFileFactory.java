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

import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.SignInType;

public final class ApplicationXmlFileFactory
{
	public static File newApplicationFileWithPrivateKey(MasterPwFileModelBean modelObject)
	{
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder().build();
		applicationModelBean.setMasterPwFileModelBean(modelObject);
		return ApplicationXmlFileStoreWorker.saveToFileWithPrivateKey(applicationModelBean);
	}

	public static File newApplicationFileWithPasswordAndPrivateKey(
		MasterPwFileModelBean modelObject)
	{
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder().build();
		applicationModelBean.setMasterPwFileModelBean(modelObject);
		return ApplicationXmlFileStoreWorker
			.saveToFileWithPasswordAndPrivateKey(applicationModelBean);
	}

	public static File newApplicationFileWithPassword(final MasterPwFileModelBean modelObject)
	{
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder().build();
		applicationModelBean.setMasterPwFileModelBean(modelObject);
		return ApplicationXmlFileStoreWorker.saveToFileWithPassword(applicationModelBean);
	}

	/**
	 * Writes a new database with the master key these settings choose: password, key file, or both
	 *
	 * @param modelObject
	 *            the new database's file and master key
	 * @return the written file, or null when the settings choose no master key, so nothing was
	 *         written
	 */
	public static File newApplicationFile(final MasterPwFileModelBean modelObject)
	{
		SignInType signInType = SignInType.toSignInType(modelObject);
		if (SignInType.PASSWORD_AND_PRIVATE_KEY.equals(signInType))
		{
			return newApplicationFileWithPasswordAndPrivateKey(modelObject);
		}
		if (SignInType.PRIVATE_KEY.equals(signInType))
		{
			return newApplicationFileWithPrivateKey(modelObject);
		}
		if (SignInType.PASSWORD.equals(signInType))
		{
			return newApplicationFileWithPassword(modelObject);
		}
		return null;
	}

}
