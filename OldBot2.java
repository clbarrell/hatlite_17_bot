import hlt.*;

import java.util.*;

public class OldBot2 {

    final static Networking networking = new Networking();
    final static GameMap gameMap = networking.initialize("OldBot2");

    public static void main(final String[] args) {


        try {

            boolean madeTurn;
            // We now have 1 full minute to analyse the initial map.
            final String initialMapIntelligence =
                    "width: " + gameMap.getWidth() +
                    "; height: " + gameMap.getHeight() +
                    "; players: " + gameMap.getAllPlayers().size() +
                    "; planets: " + gameMap.getAllPlanets().size();
            Log.log(initialMapIntelligence);

            final ArrayList<Move> moveList = new ArrayList<>();
            for (;;) {

                moveList.clear();
                networking.updateMap(gameMap);

                for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                    if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                        continue;
                    }

                    madeTurn = false;
                    // get closest planets
                    ArrayList<Planet> planets = eligiblePlanets(ship);
                    for (Planet planet : planets) {

                        // MY PLANETS
                        if (ship.canDock(planet) && !planet.isFull() && (gameMap.isMyEntity(planet) || !planet.isOwned())) {
                            moveList.add(new DockMove(ship, planet));
                            ship.logMyAction("docking");
                            madeTurn = true;
                            break;
                        }

                        if ((gameMap.isMyEntity(planet) && !planet.isFull()) || !planet.isOwned() ) {
                            final ThrustMove newThrustMove1 = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                            if (newThrustMove1 != null) {
                                moveList.add(newThrustMove1);
                                ship.logMyAction("moving to dock");
                                madeTurn = true;
                            } else {
                                ArrayList<Ship> nearbyShips = gameMap.nearbyShipsByDistance(ship);
                                ThrustMove newThrustMove = Navigation.navigateShipToEnemyShip(gameMap, ship, nearbyShips.get(0), Constants.MAX_SPEED);
                                if (newThrustMove != null) {
                                    moveList.add(newThrustMove);
                                } else {
                                    Log.log("newThrustMove attack was null");
                                }
                                ship.logMyAction("COULN't GO TO DOCK!!!.  attacking ship ID: " + nearbyShips.get(0).getId());
                                madeTurn = true;
                            }
                            break;
                        }
                        // ENEMY PLANEST


                        // final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                        // if (newThrustMove != null) {
                        //     moveList.add(newThrustMove);
                        // }
                    }

                    if (!madeTurn) {
                        // go to enemy ship
                        ArrayList<Ship> nearbyShips = gameMap.nearbyShipsByDistance(ship);
                        ThrustMove newThrustMove = Navigation.navigateShipToEnemyShip(gameMap, ship, nearbyShips.get(0), Constants.MAX_SPEED);
                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                            ship.logMyAction("attacking ship ID: " + nearbyShips.get(0).getId());
                            madeTurn = true;
                        } else {
                            Log.log("newThrustMove attack was null");
                        }
                    }
                }
                // Log.log("sending moveList");
                // printMoveList(moveList);
                Networking.sendMoves(moveList);
            }
        } catch (NullPointerException e) {
            // Log.log(e);
            e.printStackTrace();
        }

    }

    public static ArrayList<Planet> eligiblePlanets(Entity ship) {
        // order planets by:
        //  - my planets with extra spots still
        //  - neutral planets
        //  - enemy planets?
        //  ... or go for ships then?
        ArrayList<Planet> planets = new ArrayList<Planet>();
        final Map<Double, Planet> planetsByDistance = gameMap.nearbyPlanetsByDistance(ship);

        for (Map.Entry<Double,Planet> entry : planetsByDistance.entrySet()) {
            Planet pnt = entry.getValue();

            if (pnt.isFull() && gameMap.isMyEntity(pnt)) {
                continue;
            }

            planets.add(pnt);
            // Entity value = entry.getValue();
            // Double key = entry.getKey();
        }

        if (planets.isEmpty()) {
            planets.addAll(gameMap.getAllPlanets().values());
        }
        return planets;
    }

    public static void printMoveList(ArrayList<Move> moveList) {
        Log.log("Movelist count: " + moveList.size());
        for (Move move : moveList) {
            Log.log("Move SID: " + move.getShip().getId() + " - moveType: " + move.getType());
        }
    }

}
