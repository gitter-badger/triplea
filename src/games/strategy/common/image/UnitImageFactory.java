package games.strategy.common.image;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.SwingLib;

/**
 * Utility class to get image for a Unit.
 * <p>
 * This class is a simplified version of Sean Bridges's games.strategy.triplea.image.UnitImageFactory.
 */
public class UnitImageFactory {
  private static final String FILE_NAME_BASE = "units/";
  // Image cache
  private final Map<String, Image> m_images = new HashMap<String, Image>();
  private ResourceLoader m_resourceLoader;

  /**
   * Creates new IconImageFactory
   */
  public UnitImageFactory() {
    m_resourceLoader = ResourceLoader.getMapResourceLoader(null, true);
  }

  public void setResourceLoader(final ResourceLoader loader) {
    m_resourceLoader = loader;
    clearImageCache();
  }

  private void clearImageCache() {
    m_images.clear();
  }

  /**
   * Return the appropriate unit image.
   */
  public Image getImage(final UnitType type, final PlayerID player, final GameData data) {
    final String baseName = getBaseImageName(type, player, data);
    final String fullName = baseName + player.getName();
    if (m_images.containsKey(fullName)) {
      return m_images.get(fullName);
    }
    final Image baseImage = getBaseImage(baseName, player);
    m_images.put(fullName, baseImage);
    return baseImage;
  }

  private Image getBaseImage(final String baseImageName, final PlayerID id) {
    // URL uses '/' not '\'
    final String fileName = FILE_NAME_BASE + id.getName() + "/" + baseImageName + ".png";
    final URL url = m_resourceLoader.getResource(fileName);
    if (url == null) {
      throw new IllegalStateException("Cant load: " + baseImageName + "  looking in: " + fileName);
    }
    final Image image = Toolkit.getDefaultToolkit().getImage(url);
    try {
      SwingLib.ensureImageLoaded(image);
    } catch (final InterruptedException ex) {
      ex.printStackTrace();
    }
    return image;
  }

  private String getBaseImageName(final UnitType type, final PlayerID id, final GameData data) {
    final StringBuilder name = new StringBuilder(32);
    name.append(type.getName());
    return name.toString();
  }
}
