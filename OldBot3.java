import hlt.*;

import java.util.*;

public class OldBot3 {

    final static Networking networking = new Networking();
    final static GameMap gameMap = networking.initialize("OldBot3");

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
            double largestPlanetRadius = getLargestRadius(gameMap.getAllPlanets());
            Log.log("Largest Radius: " + largestPlanetRadius);
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
                    ArrayList<Planet> planets = eligibleScoredPlanets(ship, largestPlanetRadius);
                    Planet keyPlanet = closestKeyPlanet(ship);
                    for (Planet planet : planets) {

                        // MY PLANETS
                        if (ship.canDock(planet) && !planet.isFull() && (gameMap.isMyEntity(planet) || !planet.isOwned())) {
                            moveList.add(new DockMove(ship, planet));
                            ship.logMyAction("docking");
                            madeTurn = true;
                            break;
                        }

                        // // keep a list of KEY_PLANETS where radius is > 10
                        // // if distance to KEY_PLANET is < 2x closest OPEN planet go go it!
                        // if (ship.getDistanceTo(keyPlanet) < (1.8 * ship.getDistanceTo(planet))) {
                        //     final ThrustMove newThrustMove1 = Navigation.navigateShipToDock(gameMap, ship, keyPlanet, Constants.MAX_SPEED);
                        //     if (newThrustMove1 != null) {
                        //         moveList.add(newThrustMove1);
                        //         ship.logMyAction("moving to closestkeyplanet");
                        //         madeTurn = true;
                        //     } else {
                        //         ArrayList<Ship> nearbyShips = gameMap.nearbyShipsByDistance(ship);
                        //         ThrustMove newThrustMove = Navigation.navigateShipToEnemyShip(gameMap, ship, nearbyShips.get(0), Constants.MAX_SPEED);
                        //         if (newThrustMove != null) {
                        //             moveList.add(newThrustMove);
                        //         } else {
                        //             Log.log("newThrustMove attack was null");
                        //         }
                        //         ship.logMyAction("Couldn't go to key planet.  attacking ship ID: " + nearbyShips.get(0).getId());
                        //         madeTurn = true;
                        //     }
                        //     break;
                        // }

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

                        // if planet distance is more than 1/3 the X length of map... break
                        if (ship.getDistanceTo(planet) > (.3 * gameMap.getWidth())) {
                            // how far away is key planet?
                            // if (ship.getDistanceTo(keyPlanet) < (.35 * gameMap.getWidth())) {
                            //     final ThrustMove newThrustMove1 = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                            //     if (newThrustMove1 != null) {
                            //         moveList.add(newThrustMove1);
                            //         ship.logMyAction("moving to large planet");
                            //         madeTurn = true;
                            //     } else {
                            //         ArrayList<Ship> nearbyShips = gameMap.nearbyShipsByDistance(ship);
                            //         ThrustMove newThrustMove = Navigation.navigateShipToEnemyShip(gameMap, ship, nearbyShips.get(0), Constants.MAX_SPEED);
                            //         if (newThrustMove != null) {
                            //             moveList.add(newThrustMove);
                            //         } else {
                            //             Log.log("couldn't go to nearship`");
                            //         }
                            //         ship.logMyAction("Couldn't goto key planet.  attacking ship ID: " + nearbyShips.get(0).getId());
                            //         madeTurn = true;
                            //     }
                            // }
                            break;
                        }
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

    public static ArrayList<Planet> eligibleScoredPlanets(Entity ship, double maxRadius) {
        // order planets by:
        //  - my planets with extra spots still
        //  - neutral planets
        //  - enemy planets?
        //  ... or go for ships then?
        ArrayList<Planet> planets = new ArrayList<Planet>();
        final Map<Double, Planet> planetsByDistance = gameMap.nearbyPlanetsByScore(ship, maxRadius);
        for (Map.Entry<Double,Planet> entry : planetsByDistance.entrySet()) {
            Planet pnt = entry.getValue();

            if (pnt.isFull() && gameMap.isMyEntity(pnt)) {
                continue;
            } else {
                planets.add(pnt);
            }
            // Entity value = entry.getValue();
            // Double key = entry.getKey();
        }

        if (planets.isEmpty()) {
            planets.addAll(gameMap.getAllPlanets().values());
        }
        return planets;
    }

    public static ArrayList<Planet> eligiblePlanets(Entity ship) {
        // order planets by:
        //  - my planets with extra spots still
        //  - neutral planets
        //  - enemy planets?
        //  ... or go for ships then?
        ArrayList<Planet> planets = new ArrayList<Planet>();
        final Map<Double, Planet> planetsByDistance = gameMap.nearbyPlanetsByDistance(ship);
        double maxRadius = 1;
        for (Map.Entry<Double,Planet> entry : planetsByDistance.entrySet()) {
            Planet pnt = entry.getValue();

            if (pnt.isFull() && gameMap.isMyEntity(pnt)) {
                continue;
            } else if (planets.size() < 4 && pnt.getRadius() > 1.25 * maxRadius) {
                // less than 5 planets, add key planets
                planets.add(0, pnt);
                Log.log("Put Key Planet in front! Plant id: " + pnt.getId());
            } else {
                planets.add(pnt);
            }

            maxRadius = (pnt.getRadius() > maxRadius) ? pnt.getRadius() : maxRadius;

            // Entity value = entry.getValue();
            // Double key = entry.getKey();
        }

        if (planets.isEmpty()) {
            planets.addAll(gameMap.getAllPlanets().values());
        }
        return planets;
    }

    public static Planet closestKeyPlanet(Entity ship) {
        // order planets by:
        //  - my planets with extra spots still
        //  - neutral planets
        //  - enemy planets?
        //  ... or go for ships then?
        ArrayList<Planet> planets = new ArrayList<Planet>();
        final Map<Double, Planet> planetsByDistance = gameMap.nearbyKeyPlanetsByDistance(ship);

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
        return planets.get(0);
    }

    public static void printMoveList(ArrayList<Move> moveList) {
        Log.log("Movelist count: " + moveList.size());
        for (Move move : moveList) {
            Log.log("Move SID: " + move.getShip().getId() + " - moveType: " + move.getType());
        }
    }

    public static double getLargestRadius(Map<Integer, Planet> planets) {
        double max = 1;
        for (Planet planet : planets.values()) {
            if (planet.getRadius() > max) {
                max = planet.getRadius();
            }
        }
        return max;
    }

}
