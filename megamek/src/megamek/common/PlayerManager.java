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

        if ((player.isBot()) && (!player.getSingleBlind())) {
            boolean sbb = game.getOptions().booleanOption(OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS);
            boolean db = game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
            player.setSingleBlind(sbb && db);
        }

       game.players.put(id, player);
        game.setupTeams();
        updatePlayer(player);
    }


    public void setPlayer(int id, Player player) {
        player.setGame(game);
        game.players.put(id, player);
        game.setupTeams();
        updatePlayer(player);
    }


    public void removePlayer(int id) {
        Player playerToRemove = game.getPlayer(id);
        game.players.remove(id);
        game.setupTeams();
        updatePlayer(playerToRemove);
    }

    private void updatePlayer(Player player) {
        game.processGameEvent(new GamePlayerChangeEvent(game, player));
    }
}