package io.github.astrapi69.mystic.crypt.panels.dbtree;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MysticCryptEntryModelBean
{
	String title;
	String userName;
	String password;
	String url;

	/** The map with optional properties */
	@Builder.Default
	final Map<String, Object> properties = new LinkedHashMap<>();
}
