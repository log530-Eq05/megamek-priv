/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.server.totalwarfare.TWGameManager;

import java.util.Vector;

public class PulseLaserWeaponHandler extends EnergyWeaponHandler {
    private static final long serialVersionUID = -5701939682138221449L;

    public PulseLaserWeaponHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m) {
        super(toHit, waa, g, m);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (super.doChecks(vPhaseReport)) {
            return true;
        }

        WeaponMounted laser = waa.getEntity(game).getWeapon(waa.getWeaponId());

        if ((roll.getIntValue() == 2) && laser.curMode().getName().startsWith("Pulse")) {
            vPhaseReport.addAll(gameManager.explodeEquipment(laser.getEntity(), laser.getLocation(), laser.getLinkedBy()));
        }
        return false;
    }

    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ENERGY_WEAPONS)
            && weapon.hasModes()) {
            toReturn = Compute.dialDownDamage(weapon, wtype, nRange);
        }

        // during a swarm, all damage gets applied as one block to one location
        if ((ae instanceof BattleArmor)
            && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
            && !(weapon.isSquadSupportWeapon())
            && (ae.getSwarmTargetId() == target.getId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }
        // Check for Altered Damage from Energy Weapons (TacOp, pg.83)
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_ALTDMG)) {
            if (nRange <= 1) {
                toReturn++;
            } else if (nRange <= wtype.getMediumRange()) {
                // Do Nothing for Short and Medium Range
            } else if (nRange <= wtype.getLongRange()) {
                toReturn--;
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
            && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn / 3.0);
        }

        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                    bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());
        return (int) Math.ceil(toReturn);
    }
}
