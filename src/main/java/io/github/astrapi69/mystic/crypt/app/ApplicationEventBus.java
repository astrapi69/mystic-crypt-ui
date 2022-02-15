package io.github.astrapi69.mystic.crypt.app;

import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.swing.visibility.RenderMode;
import lombok.Getter;

import com.google.common.eventbus.EventBus;

import io.github.astrapi69.design.pattern.eventbus.GenericEventBus;
import io.github.astrapi69.design.pattern.observer.event.EventSource;

public class ApplicationEventBus
{

	/** The instance. */
	private static final ApplicationEventBus instance = new ApplicationEventBus();
	@Getter
	private final EventBus applicationEventBus = new EventBus();

	private ApplicationEventBus()
	{
	}

	public static EventSource<EventObject<RenderMode>> getSaveState()
	{
		return GenericEventBus.getEventSource(RenderMode.class);
	}

	public static EventSource<?> get(final String key)
	{
		return GenericEventBus.get(key);
	}

	public static ApplicationEventBus getInstance()
	{
		return instance;
	}

}
