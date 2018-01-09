package hlt;

import java.util.*;

public class GameMap {
    private final int width, height;
    private final int playerId;
    private final List<Player> players;
    private final List<Player> playersUnmodifiable;
    private final Map<Integer, Planet> planets;
    private final List<Ship> allShips;
    private final List<Ship> allShipsUnmodifiable;
    private final List<Ship> allEnemyShips;
    public List<ThrustMove> myMoves;


    // used only during parsing to reduce memory allocations
    private final List<Ship> currentShips = new ArrayList<>();

    public GameMap(final int width, final int height, final int playerId) {
        this.width = width;
        this.height = height;
        this.playerId = playerId;
        players = new ArrayList<>(Constants.MAX_PLAYERS);
        playersUnmodifiable = Collections.unmodifiableList(players);
        planets = new TreeMap<>();
        allShips = new ArrayList<>();
        allEnemyShips = new ArrayList<>();
        allShipsUnmodifiable = Collections.unmodifiableList(allShips);
        myMoves = new ArrayList<>();
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public boolean isMyEntity(Entity entity) {
        return this.playerId == entity.getOwner();
    }

    public int getMyPlayerId() {
        return playerId;
    }

    public List<Player> getAllPlayers() {
        return playersUnmodifiable;
    }

    public List<Ship> getAllEnemyShips() {
        return allEnemyShips;
    }

    public Player getMyPlayer() {
        return getAllPlayers().get(getMyPlayerId());
    }

    public Ship getShip(final int playerId, final int entityId) throws IndexOutOfBoundsException {
        return players.get(playerId).getShip(entityId);
    }

    public Planet getPlanet(final int entityId) {
        return planets.get(entityId);
    }

    public Map<Integer, Planet> getAllPlanets() {
        return planets;
    }

    public List<Ship> getAllShips() {
        return allShipsUnmodifiable;
    }

    public void addMove(ThrustMove newMove) {
        myMoves.add(newMove);
    }

    public List<ThrustMove> getMyMoves() {
        return myMoves;
    }

    public ArrayList<Entity> objectsBetween(Position start, Position target) {
        final ArrayList<Entity> entitiesFound = new ArrayList<>();

        addEntitiesBetween(entitiesFound, start, target, planets.values());
        addEntitiesBetween(entitiesFound, start, target, allShips);

        return entitiesFound;
    }

    private static void addEntitiesBetween(final List<Entity> entitiesFound,
                                           final Position start, final Position target,
                                           final Collection<? extends Entity> entitiesToCheck) {

        for (final Entity entity : entitiesToCheck) {
            if (entity.equals(start) || entity.equals(target)) {
                continue;
            }
            if (Collision.segmentCircleIntersect(start, target, entity, Constants.FORECAST_FUDGE_FACTOR)) {
                entitiesFound.add(entity);
            }
        }
    }

    public Map<Double, Entity> nearbyEntitiesByDistance(final Entity entity) {
        final Map<Double, Entity> entityByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            if (planet.equals(entity)) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(planet), planet);
        }

        for (final Ship ship : allShips) {
            if (ship.equals(entity)) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(ship), ship);
        }

        return entityByDistance;
    }

    public double nearbyShipDensity(final Ship entity) {
        // final Map<Double, Ship> nearbyShips = nearbyAllShipsByDistance(entity);

        double runningScore = 0;
        for (Ship otherShip : getAllShips()) {
            if (entity.getDistanceTo(otherShip) > (4 * entity.getRadius())) {
                break;
            }
            if (otherShip.getOwner() == getMyPlayerId()) {
                // my ship
                runningScore += 1;
            } else {
                // not my ship
                runningScore -= 1;
            }
        }
        entity.logMyAction("runningScore: " + runningScore);
        return runningScore;
    }

    public Map<Double, Ship> nearbyAllShipsByDistance(final Entity ship) {
        Map<Double, Ship> shipByDistance = new TreeMap<>();
        for (Ship otherShip : getAllShips()) {
            shipByDistance.put(otherShip.getDistanceTo(ship), otherShip);
        }
        return shipByDistance;
    }

    public ArrayList<Ship> nearbyMyShipsByDistance(final Entity ship) {
        Map<Double, Ship> shipByDistance = new TreeMap<>();
        for (Ship otherShip : getMyPlayer().getShips().values()) {
            shipByDistance.put(otherShip.getDistanceTo(ship), otherShip);
        }
        return new ArrayList<Ship>(shipByDistance.values());
    }

    public ArrayList<Ship> nearbyShipsByDistance(final Entity ship) {
        Map<Double, Ship> shipByDistance = new TreeMap<>();
        // Log.log("AllEnemyShips: " + getAllEnemyShips().size());
        for (Ship otherShip : getAllEnemyShips()) {
            shipByDistance.put(otherShip.getDistanceTo(ship), otherShip);
        }
        // Log.log("nearbyShipsByDistance: " + shipByDistance.size());
        return new ArrayList<Ship>(shipByDistance.values());
        //return shipByDistance;
    }

    public Map<Double, Planet> nearbyPlanetsByDistance(final Entity ship) {
        final Map<Double, Planet> planetByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            planetByDistance.put(ship.getDistanceTo(planet), planet);
        }

        return planetByDistance;
    }

    public Map<Double, Planet> nearbyPlanetsByScore(final Entity ship, double maxRadius) {
        final Map<Double, Planet> planetByDistance = new TreeMap<>();
        double score;
        for (final Planet planet : planets.values()) {
            double radius = planet.getRadius() / maxRadius;
            double curve = curve(radius, CurveType.Linear, -1, 1, 1.5, 0);
            score =  curve * ship.getDistanceTo(planet);
            // if ((ship.getId() % 5) == 0) {
            //     Log.log("Planet ID: " + planet.getId() + " dist / radius / curve / score " + ship.getDistanceTo(planet) + " / " + radius + " / " + curve + " / " + score);
            // }
            planetByDistance.put(score, planet);
        }

        return planetByDistance;
    }

    private enum CurveType {
        Parabola, Linear, Logistic
    }

    // plots x value onto curve, returning value that is between 0-1
    private double curve(double x, CurveType curve, double mSlope, double kExponent, double yShift, double xShift) {
        // m = slope, k = exponential, b = y shift, c = x shift
        double value = 1;
        switch(curve) {
        case Parabola:
            value = mSlope * Math.pow((x - xShift), kExponent) + yShift;
            break;
        case Linear:
            value = mSlope * (x - xShift) + yShift;
            // Log.log("Linear curved: " + value);
            break;
        case Logistic:
            double denom = 1 + Math.pow((Math.E * mSlope), (x*kExponent + xShift));
            value = (1 / denom) + yShift;
            break;
        }
        return clamp(value, 0, 1);
    }

    // clamp will ensure that no value is beyond max or below min
    // preffered use is  noramalise above
    public double clamp(double initialValue, double min, double max) {
        double fin = 1;
        if (initialValue >= min && initialValue <= max) {
            fin = initialValue;
        } else if (initialValue < min) {
            fin = min;
        } else if (initialValue > max) {
            fin = max;
        }
        // Log.log("Clamping: " + fin);
        return fin;
    }

    public Map<Double, Planet> nearbyKeyPlanetsByDistance(final Entity ship) {
        final Map<Double, Planet> planetByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            if (planet.getRadius() > 10) {
                planetByDistance.put(ship.getDistanceTo(planet), planet);
            }
        }

        return planetByDistance;
    }


    public GameMap updateMap(final Metadata mapMetadata) {
        final int numberOfPlayers = MetadataParser.parsePlayerNum(mapMetadata);
        Log.log("Total moves was: " + myMoves.size());
        players.clear();
        planets.clear();
        allShips.clear();
        allEnemyShips.clear();
        myMoves.clear();

        // update players info
        for (int i = 0; i < numberOfPlayers; ++i) {
            currentShips.clear();
            final Map<Integer, Ship> currentPlayerShips = new TreeMap<>();
            final int playerId = MetadataParser.parsePlayerId(mapMetadata);

            final Player currentPlayer = new Player(playerId, currentPlayerShips);
            MetadataParser.populateShipList(currentShips, playerId, mapMetadata);
            allShips.addAll(currentShips);

            if (getMyPlayerId() != currentPlayer.getId()) {
                allEnemyShips.addAll(currentShips);
            }

            for (final Ship ship : currentShips) {
                currentPlayerShips.put(ship.getId(), ship);
            }
            players.add(currentPlayer);
        }

        final int numberOfPlanets = Integer.parseInt(mapMetadata.pop());

        for (int i = 0; i < numberOfPlanets; ++i) {
            final List<Integer> dockedShips = new ArrayList<>();
            final Planet planet = MetadataParser.newPlanetFromMetadata(dockedShips, mapMetadata);
            planets.put(planet.getId(), planet);
        }

        if (!mapMetadata.isEmpty()) {
            throw new IllegalStateException("Failed to parse data from Halite game engine. Please contact maintainers.");
        }
        Log.log("Total ships: " + allShips.size() + "... Enemy ships: " + allEnemyShips.size());
        return this;
    }
}
