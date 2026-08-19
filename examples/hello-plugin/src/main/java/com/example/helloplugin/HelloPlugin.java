package com.example.helloplugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * Example pf4j plugin main class. The legacy {@code (PluginWrapper wrapper)} constructor is
 * required: it is what {@code DefaultPluginFactory} instantiates plugin classes with in pf4j
 * 3.15.0, despite that constructor being marked deprecated in favor of a not-yet-standard
 * {@code PluginContext} alternative.
 */
public class HelloPlugin extends Plugin
{

	public HelloPlugin(PluginWrapper wrapper)
	{
		super(wrapper);
	}

	@Override
	public void start()
	{
		System.out.println("HelloPlugin started");
	}

	@Override
	public void stop()
	{
		System.out.println("HelloPlugin stopped");
	}

}
