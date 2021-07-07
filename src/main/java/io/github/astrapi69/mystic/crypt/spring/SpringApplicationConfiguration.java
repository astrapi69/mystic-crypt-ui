package io.github.astrapi69.mystic.crypt.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

@Configuration
public class SpringApplicationConfiguration
{
	@Bean
	public MysticCryptApplicationFrame mysticCryptApplicationFrame()
	{
		MysticCryptApplicationFrame frame = new MysticCryptApplicationFrame();
		return frame;
	}
}
