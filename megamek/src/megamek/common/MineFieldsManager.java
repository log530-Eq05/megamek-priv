package megamek.common;

import megamek.common.event.GameBoardChangeEvent;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class MineFieldsManager implements Serializable {

    private final Game game;
    private final Hashtable<Coords, Vector<Minefield>> minefields;
    private Vector<Minefield> vibrabombs = new Vector<>();
    public MineFieldsManager(Game game , Hashtable<Coords, Vector<Minefield>> minefields ,Vector<Minefield> vibrabombs ) {
        this.minefields=minefields;
        this.game = game;
        this.vibrabombs=vibrabombs;
    }

    public boolean containsMinefield(Coords coords) {
        return minefields.containsKey(coords);
    }

    public Vector<Minefield> getMinefields(Coords coords) {
        Vector<Minefield> mfs = minefields.get(coords);
        return (mfs == null) ? new Vector<Minefield>() : mfs;
    }

    public int getNbrMinefields(Coords coords) {
        Vector<Minefield> mfs = minefields.get(coords);
        return (mfs == null) ? 0 : mfs.size();
    }

    /**
     * Get the coordinates of all mined hexes in the game.
     *
     * @return an <code>Enumeration</code> of the <code>Coords</code> containing minefields. This will not be
     * <code>null</code>.
     */
    public Enumeration<Coords> getMinedCoords() {
        return minefields.keys();
    }

    public void addMinefield(Minefield mf) {
        addMinefieldHelper(mf);
        game.processGameEvent(new GameBoardChangeEvent(game));
    }

    public void addMinefields(Vector<Minefield> mines) {
        for (int i = 0; i < mines.size(); i++) {
            Minefield mf = mines.elementAt(i);
            addMinefieldHelper(mf);
        }
        game.processGameEvent(new GameBoardChangeEvent(game));
    }

    public void setMinefields(Vector<Minefield> minefields) {
        clearMinefieldsHelper();
        for (int i = 0; i < minefields.size(); i++) {
            Minefield mf = minefields.elementAt(i);
            addMinefieldHelper(mf);
        }
        game.processGameEvent(new GameBoardChangeEvent(game));
    }

    public void resetMinefieldDensity(Vector<Minefield> newMinefields) {
        if (newMinefields.isEmpty()) {
            return;
        }
        Vector<Minefield> mfs = minefields.get(newMinefields.firstElement().getCoords());
        if (mfs != null) {
            mfs.clear();
        }
        for (int i = 0; i < newMinefields.size(); i++) {
            Minefield mf = newMinefields.elementAt(i);
            addMinefieldHelper(mf);
        }
        game.processGameEvent(new GameBoardChangeEvent(game));
    }

    protected void addMinefieldHelper(Minefield mf) {
        Vector<Minefield> mfs = minefields.get(mf.getCoords());
        if (mfs == null) {
            mfs = new Vector<Minefield>();
            mfs.addElement(mf);
            minefields.put(mf.getCoords(), mfs);
            return;
        }
        mfs.addElement(mf);
    }

    public void removeMinefield(Minefield mf) {
        removeMinefieldHelper(mf);
        game.processGameEvent(new GameBoardChangeEvent(game));
    }

    public void removeMinefieldHelper(Minefield mf) {
        Vector<Minefield> mfs = minefields.get(mf.getCoords());
        if (mfs == null) {
            return;
        }

        Enumeration<Minefield> e = mfs.elements();
        while (e.hasMoreElements()) {
            Minefield mftemp = e.nextElement();
            if (mftemp.equals(mf)) {
                mfs.removeElement(mftemp);
                break;
            }
        }
        if (mfs.isEmpty()) {
           minefields.remove(mf.getCoords());
        }
    }

    public void clearMinefields() {
        clearMinefieldsHelper();
        game.processGameEvent(new GameBoardChangeEvent(game));
    }

    protected void clearMinefieldsHelper() {
        minefields.clear();
        game.getVibrabombs().removeAllElements();
        game.getPlayersList().forEach(Player::removeMinefields);
    }

    public Vector<Minefield> getVibrabombs() {
        return vibrabombs;
    }

    public void addVibrabomb(Minefield mf) {
        vibrabombs.addElement(mf);
    }

    public void removeVibrabomb(Minefield mf) {
        vibrabombs.removeElement(mf);
    }

    /**
     * Checks if the game contains the specified Vibrabomb
     *
     * @param mf the Vibrabomb to check
     * @return true if the minefield contains a vibrabomb.
     */
    public boolean containsVibrabomb(Minefield mf) {
        return vibrabombs.contains(mf);
    }
}