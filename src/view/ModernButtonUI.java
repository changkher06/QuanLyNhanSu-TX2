package view;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

public class ModernButtonUI extends BasicButtonUI {
    @Override
    public void paint(Graphics graphics, JComponent component) {
        AbstractButton button = (AbstractButton) component;
        Color background = button.getBackground();
        graphics.setColor(background);
        graphics.fillRect(0, 0, component.getWidth(), component.getHeight());
        super.paint(graphics, component);
    }
}
