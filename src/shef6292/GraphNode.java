package shef6292;

import spacesettlers.utilities.Position;

public class GraphNode {
    private Position myPosition;
    private double distToGoal;
    
    GraphNode(Position p) {
        myPosition = p.deepCopy();
        distToGoal = -500;
    }
    
    public void setDistToGoal(double d) {
        distToGoal = d;
    }
    
    public double getDistToGoal() {
        return distToGoal;
    }
    
    public Position getPosition() {
        return myPosition;
    }
}
