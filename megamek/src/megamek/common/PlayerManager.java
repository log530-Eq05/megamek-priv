package megamek.common;

import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.options.OptionsConstants;
import java.io.Serializable;

public class PlayerManager implements Serializable {

    private final Game game;

    public PlayerManager(Game game) {
        this.game = game;
    }

    public void addPlayer(int id, Player player) {
        player.setGame(game);

        if ((player.isBot()) && (!player.getSingleBlind())) {
            boolean sbb = game.getOptions().booleanOption(OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS);
            boolean db = game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
            player.setSingleBlind(sbb && db);
        }

        game.getPlayers().put(id, player);
        game.setupTeams();
        updatePlayer(player);
    }


    public void setPlayer(int id, Player player) {
        player.setGame(game);
        game.getPlayers().put(id, player);
        game.setupTeams();
        updatePlayer(player);
    }


    public void removePlayer(int id) {
        Player playerToRemove = game.getPlayer(id);
        game.getPlayers().remove(id);
        game.setupTeams();
        updatePlayer(playerToRemove);
    }

    void updatePlayer(Player player) {
        game.processGameEvent(new GamePlayerChangeEvent(game, player));
    }

    /**
     * @return true if the specified player is either the victor, or is on the winning team. Best to call during
     * GamePhase.VICTORY.
     */
    public boolean isPlayerVictor(Player player) {
        if (player.getTeam() == Player.TEAM_NONE) {
            return player.getId() == game.getVictoryPlayerId();
        }
        return player.getTeam() == game.getVictoryTeam();
    }
}