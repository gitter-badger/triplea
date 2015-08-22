package games.strategy.engine.data.properties;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import games.strategy.ui.SwingLib;

/**
 * User editable property representing a color.
 * <p>
 * Presents a clickable label with the currently selected color, through which a color swatch panel is accessable to change the color.
 */
public class ColorProperty extends AEditableProperty {
  // compatible with 0.9.0.2 saved games
  private static final long serialVersionUID = 6826763550643504789L;
  private final int m_max = 0xFFFFFF;
  private final int m_min = 0x000000;
  private Color m_color;

  public ColorProperty(final String name, final String description, final int def) {
    super(name, description);
    if (def > m_max || def < m_min) {
      throw new IllegalArgumentException("Default value out of range");
    }
    m_color = new Color(def);
  }

  public ColorProperty(final String name, final String description, final Color def) {
    super(name, description);
    if (def == null) {
      m_color = Color.black;
    } else {
      m_color = def;
    }
  }

  @Override
  public Object getValue() {
    return m_color;
  }

  @Override
  public void setValue(final Object value) throws ClassCastException {
    if (value == null) {
      m_color = Color.black;
    } else {
      m_color = (Color) value;
    }
  }

  @Override
  public JComponent getEditorComponent() {
    final JLabel label = new JLabel("        ") {
      private static final long serialVersionUID = 3833935337866905836L;

      @Override
      public void paintComponent(final Graphics g) {
        final Graphics2D g2 = (Graphics2D) g;
        g2.setColor(m_color);
        g2.fill(g2.getClip());
      }
    };
    label.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        System.out.println("Old color: " + m_color);
        try {
          final Color color =
              JColorChooser.showDialog(label, "Choose color", (m_color == null ? Color.black : m_color));
          if (color != null) {
            m_color = color;
            System.out.println("New color: " + m_color);
            // Ask Swing to repaint this label when it's convenient
            SwingLib.invokeLater(new Runnable() {
              @Override
              public void run() {
                label.repaint();
              }
            });
          }
        } catch (final Exception exception) {
          System.err.println(exception.getMessage());
        }
      }

      @Override
      public void mouseEntered(final MouseEvent e) {}

      @Override
      public void mouseExited(final MouseEvent e) {}

      @Override
      public void mousePressed(final MouseEvent e) {}

      @Override
      public void mouseReleased(final MouseEvent e) {}
    });
    return label;
  }

  @Override
  public boolean validate(final Object value) {
    if (value == null) {
      return true;
    }
    if (value instanceof Color) {
      return true;
    }
    return false;
  }
}
