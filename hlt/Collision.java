package hlt;

// import Math;

public class Collision {
    /**
     * Test whether a given line segment intersects a circular area.
     *
     * @param start  The start of the segment.
     * @param end    The end of the segment.
     * @param circle The circle to test against.
     * @param fudge  An additional safety zone to leave when looking for collisions. Probably set it to ship radius.
     * @return true if the segment intersects, false otherwise
     */
    public static boolean segmentCircleIntersect(final Position start, final Position end, final Entity circle, final double fudge) {
        // Parameterize the segment as start + t * (end - start),
        // and substitute into the equation of a circle
        // Solve for t
        final double circleRadius = circle.getRadius();
        final double startX = start.getXPos();
        final double startY = start.getYPos();
        final double endX = end.getXPos();
        final double endY = end.getYPos();
        final double centerX = circle.getXPos();
        final double centerY = circle.getYPos();
        final double dx = endX - startX;
        final double dy = endY - startY;

        final double a = square(dx) + square(dy);

        final double b = -2 * (square(startX) - (startX * endX)
                            - (startX * centerX) + (endX * centerX)
                            + square(startY) - (startY * endY)
                            - (startY * centerY) + (endY * centerY));

        if (a == 0.0) {
            // Start and end are the same point
            return start.getDistanceTo(circle) <= circleRadius + fudge;
        }

        // Time along segment when closest to the circle (vertex of the quadratic)
        final double t = Math.min(-b / (2 * a), 1.0);
        if (t < 0) {
            return false;
        }

        final double closestX = startX + dx * t;
        final double closestY = startY + dy * t;
        final double closestDistance = new Position(closestX, closestY).getDistanceTo(circle);

        return closestDistance <= circleRadius + fudge;
    }

    public static double square(final double num) {
        return num * num;
    }



    public static boolean willColide(
        Position p1, Position p2,
        double[] t1, double[] t2) {
        // # p1 and p2 are the (x,y) starting positions for the circles
        // # t1 and t2 are the (x,y) changes each circle undergoes each time unit to the affect of the position of circle 1 is determined by p1 + t * t1 where t is the number of time units passed
        // # r1 and r2 are the radii of the circles
        double gx = p1.getXPos() - p2.getXPos();
        double gy = p1.getYPos() - p2.getYPos();
        double x = t1[0] - t2[0];
        double y = t1[1] - t2[1];
        double r1 = Constants.SHIP_RADIUS;
        double r2 = Constants.SHIP_RADIUS;
        double r = r1 + r2;
        double total = 4 * (Math.pow(x, 2) + Math.pow(y, 2)) * (Math.pow(gx, 2) + Math.pow(gy, 2) - Math.pow(r, 2));
        double first = Math.pow(gx * x, 2) + Math.pow(gy * y, 2);
        double second = -gx * x * gy * y;

        Log.log("wc -=-= p1: " + p1 + ", t1: " + t1 + ", p2: " + p2 + ", t2: " + t2);

        if (total > 0 && second < 0) {
            return false;
        }
        if ((gx * x + gy * y) > 0 && first >= total) {
            return true;
        }
        return first >= total && second > total;
    }
}
