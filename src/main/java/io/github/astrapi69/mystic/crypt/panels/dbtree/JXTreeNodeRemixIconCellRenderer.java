package io.github.astrapi69.mystic.crypt.panels.dbtree;

import io.github.astrapi69.icon.ImageIconFactory;
import io.github.astrapi69.swing.tree.renderer.GenericTreeNodeCellRenderer;
import io.github.astrapi69.swing.tree.renderer.JXTreeNodeCellRenderer;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

import javax.swing.*;

public class JXTreeNodeRemixIconCellRenderer<T> extends GenericTreeNodeCellRenderer<T>
{
	ImageIcon openIcon;
	ImageIcon closedIcon;
	ImageIcon leafIcon;
	public Icon getOpenIcon() {
		if(openIcon == null){
			String imagePath = "io/github/astrapi69/remixicon/Document/folder-5-fill.svg";
			openIcon = RuntimeExceptionDecorator.decorate(()-> ImageIconFactory.newImageIconFromSVG(imagePath, 16.f,
				16.f));
		}
		return openIcon;
	}

	public Icon getLeafIcon() {
		if(leafIcon == null){
			String imagePath = "io/github/astrapi69/remixicon/Document/file-text-line.svg";
			leafIcon = RuntimeExceptionDecorator.decorate(()->ImageIconFactory.newImageIconFromSVG(imagePath, 16.f,
				16.f));
		}
		return leafIcon;
	}

	public Icon getClosedIcon() {
		if(closedIcon == null){
			String imagePath = "io/github/astrapi69/remixicon/Document/folder-3-fill.svg";
			closedIcon = RuntimeExceptionDecorator.decorate(()->ImageIconFactory.newImageIconFromSVG(imagePath, 16.f,
				16.f));
		}
		return closedIcon;
	}
}
