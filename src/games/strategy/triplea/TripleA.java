package games.strategy.triplea;

import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.AbstractGameLoader;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServerUI;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.sound.DefaultSoundChannel;
import games.strategy.sound.DummySound;
import games.strategy.sound.ISound;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.strongAI.StrongAI;
import games.strategy.triplea.ai.weakAI.DoesNothingAI;
import games.strategy.triplea.ai.weakAI.WeakAI;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.HeadlessUIContext;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.display.DummyTripleaDisplay;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.triplea.ui.display.TripleaDisplay;
import games.strategy.ui.SwingLib;

public class TripleA extends AbstractGameLoader implements IGameLoader {
  // compatible with 0.9.0.2 saved games
  private static final long serialVersionUID = -8374315848374732436L;
  public static final String HUMAN_PLAYER_TYPE = "Human";
  public static final String WEAK_COMPUTER_PLAYER_TYPE = "Easy (AI)";
  public static final String STRONG_COMPUTER_PLAYER_TYPE = "Medium (AI)";
  public static final String PRO_COMPUTER_PLAYER_TYPE = "Hard (AI)";
  public static final String DOESNOTHINGAI_COMPUTER_PLAYER_TYPE = "Does Nothing (AI)";
  // public static final String NONE = "None (AI)";
  protected transient ITripleaDisplay m_display;

  @Override
  public Set<IGamePlayer> createPlayers(final Map<String, String> playerNames) {
    final Set<IGamePlayer> players = new HashSet<IGamePlayer>();
    for (final String name : playerNames.keySet()) {
      final String type = playerNames.get(name);
      if (type.equals(WEAK_COMPUTER_PLAYER_TYPE)) {
        players.add(new WeakAI(name, type));
      } else if (type.equals(STRONG_COMPUTER_PLAYER_TYPE)) {
        players.add(new StrongAI(name, type));
      } else if (type.equals(PRO_COMPUTER_PLAYER_TYPE)) {
        players.add(new ProAI(name, type));
      } else if (type.equals(DOESNOTHINGAI_COMPUTER_PLAYER_TYPE)) {
        players.add(new DoesNothingAI(name, type));
      } else if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE)) {
        final TripleAPlayer player = new TripleAPlayer(name, type);
        players.add(player);
      } else {
        throw new IllegalStateException("Player type not recognized:" + type);
      }
    }
    return players;
  }

  @Override
  public void shutDown() {
    super.shutDown();
    if (m_display != null) {
      m_game.removeDisplay(m_display);
      m_display.shutDown();
      m_display = null;
    }
  }

  protected void initializeGame() {}

  @Override
  public void startGame(final IGame game, final Set<IGamePlayer> players, final boolean headless) throws Exception {
    /*
     * Retreive the map name from xml file
     * This is the key for triplea to find the maps
     */
    m_game = game;
    // final String mapDir = game.getData().getProperties().get(Constants.MAP_NAME).toString();
    if (game.getData().getDelegateList().getDelegate("edit") == null) {
      // an evil awful hack
      // we don't want to change the game xml
      // and invalidate mods so hack it
      // and force the addition here
      final EditDelegate delegate = new EditDelegate();
      delegate.initialize("edit", "edit");
      m_game.getData().getDelegateList().addDelegate(delegate);
      if (game instanceof ServerGame) {
        ((ServerGame) game).addDelegateMessenger(delegate);
      }
    }
    final LocalPlayers localPlayers = new LocalPlayers(players);
    if (headless) {
      final IUIContext uiContext = new HeadlessUIContext();
      uiContext.setDefaultMapDir(game.getData());
      // uiContext.getMapData().verify(game.getData());
      uiContext.setLocalPlayers(localPlayers);
      final boolean useServerUI = HeadlessGameServer.getUseGameServerUI();
      final HeadlessGameServerUI headlessFrameUI;
      if (useServerUI) {
        headlessFrameUI = new HeadlessGameServerUI(game, localPlayers, uiContext);
      } else {
        headlessFrameUI = null;
      }
      m_display = new DummyTripleaDisplay(headlessFrameUI);
      m_soundChannel = new DummySound();
      game.addDisplay(m_display);
      game.addSoundChannel(m_soundChannel);
      initializeGame();
      connectPlayers(players, null);// technically not needed because we won't have any "local human players" in a headless game.
      if (headlessFrameUI != null) {
        headlessFrameUI.setLocationRelativeTo(null);
        headlessFrameUI.setSize(700, 400);
        headlessFrameUI.setVisible(true);
        headlessFrameUI.toFront();
      }
    } else {
      SwingLib.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          final TripleAFrame frame;
          try {
            frame = new TripleAFrame(game, localPlayers);
          } catch (final IOException e) {
            e.printStackTrace();
            System.exit(-1);
            return;
          }
          m_display = new TripleaDisplay(frame);
          game.addDisplay(m_display);
          m_soundChannel = new DefaultSoundChannel(localPlayers);
          game.addSoundChannel(m_soundChannel);
          frame.setSize(700, 400);
          frame.setVisible(true);
          DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_GAME_START, null);
          connectPlayers(players, frame);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              frame.setExtendedState(Frame.MAXIMIZED_BOTH);
              frame.toFront();
            }
          });
        }
      });
    }
  }

  private void connectPlayers(final Set<IGamePlayer> players, final TripleAFrame frame) {
    for (final IGamePlayer player : players) {
      if (player instanceof TripleAPlayer) {
        ((TripleAPlayer) player).setFrame(frame);
      }
    }
  }

  /**
   * Return an array of player types that can play on the server.
   */
  @Override
  public String[] getServerPlayerTypes() {
    return new String[] {HUMAN_PLAYER_TYPE, WEAK_COMPUTER_PLAYER_TYPE, STRONG_COMPUTER_PLAYER_TYPE,
        PRO_COMPUTER_PLAYER_TYPE, DOESNOTHINGAI_COMPUTER_PLAYER_TYPE};
  }

  @Override
  public Class<? extends IChannelSubscribor> getDisplayType() {
    return ITripleaDisplay.class;
  }

  @Override
  public Class<? extends IChannelSubscribor> getSoundType() {
    return ISound.class;
  }

  @Override
  public Class<? extends IRemote> getRemotePlayerType() {
    return ITripleaPlayer.class;
  }

  @Override
  public IUnitFactory getUnitFactory() {
    return new IUnitFactory() {
      private static final long serialVersionUID = 5684926837825766505L;

      @Override
      public Unit createUnit(final UnitType type, final PlayerID owner, final GameData data) {
        return new TripleAUnit(type, owner, data);
      }
    };
  }
}
