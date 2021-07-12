/**
 * The MIT License
 * <p>
 * Copyright (C) 2015 Asterios Raptis
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panels.signin;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import io.github.astrapi69.collections.list.ListFactory;

/**
 * The bean class {@link MasterPwFileModelBean} is for holding the sign in data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MasterPwFileModelBean implements Serializable
{
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The encrypted data file for the application. */
	File applicationFile;

	/** The currently selected key file path */
	String selectedApplicationFilePath;

	/** The key file paths for the combo box */
	@Builder.Default
	List<String> applicationFilePaths = ListFactory.newArrayList("");

	/** The key file. */
	File keyFile;

	/** The currently selected key file path */
	String selectedKeyFilePath;

	/** The key file paths for the combo box */
	@Builder.Default
	List<String> keyFilePaths = ListFactory.newArrayList("");

	/** The master password char array. */
	char[] masterPw;

	/** The flag if the master password is displayed in plain text. */
	boolean showMasterPw;

	/** The flag if the key file will be used in the authentication. */
	boolean withKeyFile;

	/** The flag if the master password will be used in the authentication. */
	@Builder.Default
	boolean withMasterPw = true;
}
