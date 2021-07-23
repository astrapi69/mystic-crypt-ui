package io.github.astrapi69.mystic.crypt.panels.signin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationFileFactoryTest
{

	@Test void newApplicationFileWithPrivateKey()
	{
		MasterPwFileModelBean modelObject;
		// new scenario TODO set the appropriate data for the unit test
		modelObject = MasterPwFileModelBean.builder()

			.build();
		ApplicationFileFactory.newApplicationFileWithPrivateKey(modelObject);
	}

	@Test void newApplicationFileWithPasswordAndPrivateKey()
	{
		MasterPwFileModelBean modelObject;
		// new scenario TODO set the appropriate data for the unit test
		modelObject = MasterPwFileModelBean.builder().build();
		ApplicationFileFactory.newApplicationFileWithPasswordAndPrivateKey(modelObject);
	}

	@Test void newApplicationFileWithPassword()
	{
		MasterPwFileModelBean modelObject;
		// new scenario TODO set the appropriate data for the unit test
		modelObject = MasterPwFileModelBean.builder().build();
		ApplicationFileFactory.newApplicationFileWithPassword(modelObject);
	}
}
