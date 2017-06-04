package de.alpharogroup.mystic.crypt.panels.conversion;

import java.io.File;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileConversionModelBean
{
	File derFile;
	File pemFile;

}
