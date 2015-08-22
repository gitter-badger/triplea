package games.strategy.engine.data.properties;

import java.awt.FileDialog;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.ui.SwingLib;

/**
 * User editable property representing a file.
 * <p>
 * Presents a clickable label with the currently selected file name, through which a file dialog panel is accessible to change the file.
 */
public class FileProperty extends AEditableProperty {
  // compatible with 0.9.0.2 saved games
  private static final long serialVersionUID = 6826763550643504789L;
  /** The file associated with this property. */
  private File m_file;
  private final String[] m_acceptableSuffixes;

  /**
   * Construct a new file property.
   *
   * @param name
   *        The name of the property
   * @param fileName
   *        The name of the file to be associated with this property
   */
  public FileProperty(final String name, final String description, final String fileName) {
    super(name, description);
    m_file = new File(fileName);
    if (!m_file.exists()) {
      m_file = null;
    }
    m_acceptableSuffixes = new String[] {"png", "jpg", "jpeg", "gif"};
  }

  public FileProperty(final String name, final String description, final File file) {
    this(name, description, file, new String[] {"png", "jpg", "jpeg", "gif"});
  }

  public FileProperty(final String name, final String description, final File file, final String[] acceptableSuffixes) {
    super(name, description);
    if (file.exists()) {
      m_file = file;
    } else {
      m_file = null;
    }
    m_acceptableSuffixes = acceptableSuffixes;
  }

  /**
   * Gets the file associated with this property.
   *
   * @return The file associated with this property
   */
  @Override
  public Object getValue() {
    return m_file;
  }

  @Override
  public void setValue(final Object value) throws ClassCastException {
    m_file = (File) value;
  }

  /**
   * Gets a Swing component to display this property.
   *
   * @return a non-editable JTextField
   */
  @Override
  public JComponent getEditorComponent() {
    final JTextField label;
    if (m_file == null) {
      label = new JTextField();
    } else {
      label = new JTextField(m_file.getAbsolutePath());
    }
    label.setEditable(false);
    label.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        final File selection = getFileUsingDialog(m_acceptableSuffixes);
        if (selection != null) {
          m_file = selection;
          label.setText(m_file.getAbsolutePath());
          // Ask Swing to repaint this label when it's convenient
          SwingLib.invokeLater(new Runnable() {
            @Override
            public void run() {
              label.repaint();
            }
          });
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

  /**
   * Prompts the user to select a file.
   *
   * @param acceptableSuffixes
   */
  private File getFileUsingDialog(final String... acceptableSuffixes) {
    // For some strange reason,
    // the only way to get a Mac OS X native-style file dialog
    // is to use an AWT FileDialog instead of a Swing JDialog
    if (GameRunner.isMac()) {
      final FileDialog fileDialog = new FileDialog(MainFrame.getInstance());
      fileDialog.setMode(FileDialog.LOAD);
      fileDialog.setFilenameFilter(new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
          if (acceptableSuffixes == null || acceptableSuffixes.length == 0) {
            return true;
          }
          for (final String suffix : acceptableSuffixes) {
            if (name.toLowerCase().endsWith(suffix)) {
              return true;
            }
          }
          return false;
        }
      });
      fileDialog.setVisible(true);
      final String fileName = fileDialog.getFile();
      final String dirName = fileDialog.getDirectory();
      if (fileName == null) {
        return null;
      }
      return new File(dirName, fileName);
    }
    final JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(final File file) {
        if (file == null) {
          return false;
        } else if (file.isDirectory()) {
          return true;
        } else {
          final String name = file.getAbsolutePath().toLowerCase();
          for (final String suffix : acceptableSuffixes) {
            if (name.endsWith(suffix)) {
              return true;
            }
          }
          return false;
        }
      }

      @Override
      public String getDescription() {
        return Arrays.toString(acceptableSuffixes);
      }
    });
    final int rVal = fileChooser.showOpenDialog(MainFrame.getInstance());
    if (rVal == JFileChooser.APPROVE_OPTION) {
      return fileChooser.getSelectedFile();
    }
    return null;
  }

  @Override
  public boolean validate(final Object value) {
    if (value == null) {
      return true;
    }
    if (value instanceof File) {
      final File file = (File) value;
      for (final String suff : m_acceptableSuffixes) {
        if (file.getName() != null && file.getName().endsWith(suff)) {
          return true;
        }
      }
    }
    return false;
  }
}
