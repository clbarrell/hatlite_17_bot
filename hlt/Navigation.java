package hlt;
import java.util.*;

public class Navigation {

    public static ThrustMove navigateShipToDock(
            final GameMap gameMap,
            final Ship ship,
            final Entity dockTarget,
            final int maxThrust)
    {
        final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/180.0;
        final Position targetPos = ship.getClosestPoint(dockTarget);

        return navigateShipTowardsTarget(gameMap, ship, targetPos, maxThrust, avoidObstacles, maxCorrections, angularStepRad);
    }

    public static ThrustMove navigateShipToEnemyShip(
            final GameMap gameMap,
            final Ship ship,
            final Entity enemyShip,
            final int maxThrust)
    {
        final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final boolean avoidObstacles = true;
        final double angularStepRad = Math.PI/150.0;
        final Position targetPos = ship.getClosestPoint(enemyShip);
        int thrust = (int)(Math.random() * 2) + 5;

        return navigateShipTowardsTarget(gameMap, ship, targetPos, thrust, avoidObstacles, maxCorrections, angularStepRad);
    }

    public static ThrustMove navigateShipTowardsTarget(
            final GameMap gameMap,
            final Ship ship,
            final Position targetPos,
            final int maxThrust,
            final boolean avoidObstacles,
            final int maxCorrections,
            final double angularStepRad)
    {
        if (maxCorrections <= 0) {
            return null;
        }

        final double distance = ship.getDistanceTo(targetPos);
        final double angleRad = ship.orientTowardsInRad(targetPos);



        if (avoidObstacles && !gameMap.objectsBetween(ship, targetPos).isEmpty()) {
            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrust, true, (maxCorrections-1), angularStepRad);
        }

        final int thrust;
        if (distance < maxThrust) {
            // Do not round up, since overshooting might cause collision.
            thrust = (int) distance;
        }
        else {
            thrust = maxThrust;
        }

        final int angleDeg = Util.angleRadToDegClipped(angleRad);
        ThrustMove newMove = new ThrustMove(ship, angleDeg, thrust);

        final boolean willColideWithTeamMates = willICollide(ship, gameMap.getMyMoves(), newMove);

        if (willColideWithTeamMates) {
            final double newTargetDx = Math.cos(angleRad + angularStepRad) * distance;
            final double newTargetDy = Math.sin(angleRad + angularStepRad) * distance;
            final Position newTarget = new Position(ship.getXPos() + newTargetDx, ship.getYPos() + newTargetDy);

            return navigateShipTowardsTarget(gameMap, ship, newTarget, maxThrust, true, (maxCorrections-1), angularStepRad);

        }

        gameMap.addMove(newMove);
        return newMove;
    }

    private static boolean willICollide(Ship ship, List<ThrustMove> myMoves, ThrustMove newMove) {
        boolean anyCollision = false;
        for (ThrustMove move : myMoves) {
            boolean coll = Collision.willColide(ship, move.getShip(), newMove.getTimeChanges(), move.getTimeChanges());
            if (coll) {
                anyCollision = true;
                ship.logMyAction("I'm going to collide! ah no");
                break;
            }
            ship.logMyAction("Not colliding!");
        }
        return anyCollision;
    }

}




