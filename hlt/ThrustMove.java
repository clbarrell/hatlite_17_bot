package hlt;

// import Math.*;
public class ThrustMove extends Move {

    private final int angleDeg;
    private final int thrust;

    public ThrustMove(final Ship ship, final int angleDeg, final int thrust) {
        super(MoveType.Thrust, ship);
        this.thrust = thrust;
        this.angleDeg = angleDeg;
    }

    public int getAngle() {
        return angleDeg;
    }

    public int getThrust() {
        return thrust;
    }

    public double[] getTimeChanges() {
        double opposite = Math.sin(this.angleDeg) * 1;
        double adajcent = opposite / Math.tan(this.angleDeg);
        double[] arr = { adajcent, opposite };
        // final double x = getShip().getXPos() + adajcent;
        // final double y = getShip().getYPos() + opposite;

        // Position resultPosition = new Position(x, y);

        // return resultPosition;
        return arr;
    }
}
