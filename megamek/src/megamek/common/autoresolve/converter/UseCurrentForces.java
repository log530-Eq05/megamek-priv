/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.autoresolve.converter;

import megamek.common.IGame;
import megamek.common.force.Force;
import megamek.common.force.Forces;

import java.util.*;

public class UseCurrentForces extends ForceConsolidation {
    @Override
    protected int getMaxEntitiesInSubForce() {
        return -1;
    }

    @Override
    protected int getMaxEntitiesInTopLevelForce() {
        return -1;
    }

    private record Node(int parent, Force force) {}

    @Override
    public void consolidateForces(IGame game) {

        var newTopLevelForces = new ArrayList<Container>();
        var forcesInternalRepresentation = game.getForces().getForcesInternalRepresentation();
        var parents = new HashMap<Integer, Integer>();
        Deque<Force> queue = new ArrayDeque<>(game.getForces().getTopLevelForces());
        int forceId = 0;
        var newForceMap = new HashMap<Integer, Container>();
        var entityDuplicationChecker = new HashSet<Integer>();
        while (!queue.isEmpty()) {
            var force = queue.poll();
            if (force == null) {
                continue;
            }
            var player = game.getPlayer(force.getOwnerId());
            var team = player.getTeam();
            var container = new Container(forceId++, force.getName(), team, force.getOwnerId(), new ArrayList<>(), new ArrayList<>());
            newForceMap.put(force.getId(), container);

            for (var entityId : force.getEntities()) {
                if (entityDuplicationChecker.contains(entityId)) {
                    throw new IllegalStateException("Entity " + entityId + " is duplicated");
                }
                container.entities().add(entityId);
                entityDuplicationChecker.add(entityId);
            }
            var parentId = parents.get(force.getId());
            if (parentId != null) {
                var parentNode = newForceMap.get(parentId);
                parentNode.subs().add(container);
            } else {
                newTopLevelForces.add(container);
            }

            for (var subForceId : force.getSubForces()) {
                var subForce = forcesInternalRepresentation.get(subForceId);
                parents.put(subForceId, force.getId());
                queue.add(subForce);
            }
        }

        game.setForces(new Forces(game));
        createForcesOnGame(game, newTopLevelForces, game.getForces());
    }
}
