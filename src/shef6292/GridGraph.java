package shef6292;
import java.util.HashSet;
import java.util.Set;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class GridGraph {
    GraphNode[] myNodes; //The nodes in the graph
    int WIDTH; //The number of nodes wide that space is
    int HEIGHT; //How high the space is, in nodes
    int SCALE; //The seperation between adjacent nodes
    static final double ROOT2 = 1.4142136; //The square root of 2, to avoid recalculation
    double[][] adjacency; //The adjacency matrix of the graph
    
    // Scale should divide both the width and the height of the space
    public GridGraph (Position start, Position goal, Toroidal2DPhysics space, int scale) {
        Set<AbstractObject> allObjects = space.getAllObjects(); //So we can path around them
        int width = space.getWidth();   //The width of the environment
        int height = space.getHeight(); //The height of the environment
        WIDTH = (width/scale) - 1; // The number of graph nodes wide we want the environment to be
        HEIGHT = (height/scale) - 1; // The number of graph nodes high we want the environment to be
        // We want to subtract 1 to allow for the wraparound
        SCALE = scale;
        myNodes = new GraphNode[WIDTH*HEIGHT+2]; //Create an array to hold the nodes
        for(int i = 0; i < myNodes.length - 2; i++) {
            //Systematically fill the array
            //This proceeds by columns
            int x = (i/HEIGHT)*SCALE;
            int y = (i%HEIGHT)*SCALE;
            myNodes[i] = new GraphNode(new Position(x,y));
        }
        myNodes[myNodes.length-2] = new GraphNode(start); //Add the start and goal to the end of the array
        myNodes[myNodes.length-1] = new GraphNode(goal);
        //Assign adjaceny values
        for(int i = 0; i < myNodes.length -2; i++) {
            int x = getCol(myNodes[i]);
            int y = getRow(myNodes[i]);
            for(int j = 0; j < myNodes.length - 2; j++) {
                if(i == j) {
                    //A node is adjacent to itself
                    adjacency[i][j] = 0;
                    continue;
                }
                if(offByOne(x, y, getCol(myNodes[j]), getRow(myNodes[j]))) {
                    if(space.isPathClearOfObstructions(myNodes[i].getPosition(), myNodes[j].getPosition(), allObjects, Ship.SHIP_RADIUS)) {
                        if(x == getCol(myNodes[j]) || y == getRow(myNodes[j])) {
                            //The nodes are adjacent orthogonally
                            adjacency[i][j] = SCALE;
                        } else {
                            //The nodes are adjacent diagonally
                            adjacency[i][j] = SCALE*ROOT2;
                        }
                    } else {
                        //There is an obstruction
                        adjacency[i][j] = -1; // Meaning that there is no connection
                    }
                } else {
                    //They are not adjacent
                    adjacency[i][j] = -1; // To signify no connection
                }
            }
            //If the start node is in a square with myNodes[i] as a corner
            //Add an edge between them
            if(inBox(myNodes[i], getStart()) && space.isPathClearOfObstructions(myNodes[i].getPosition(), start, allObjects, Ship.SHIP_RADIUS)) {
                //The start  node is adjacent to myNodes[i], so mark a connection in the adjacency matrix
                adjacency[i][myNodes.length - 2] = space.findShortestDistance(myNodes[i].getPosition(), start);
                adjacency[myNodes.length - 2][i] = space.findShortestDistance(myNodes[i].getPosition(), start);
            } else {
                //The start node is not adjacent, mark the distance as a negative number so we can tell
                adjacency[i][myNodes.length - 2] = -1;
                adjacency[myNodes.length - 2][i] = -1;
            }
            if(inBox(myNodes[i], getGoal()) && space.isPathClearOfObstructions(myNodes[i].getPosition(), goal, allObjects, Ship.SHIP_RADIUS)) {
                adjacency[i][myNodes.length - 1] = space.findShortestDistance(myNodes[i].getPosition(), goal);
                adjacency[myNodes.length - 1][i] = space.findShortestDistance(myNodes[i].getPosition(), goal);
            } else {
                adjacency[i][myNodes.length - 1] = -1;
                adjacency[myNodes.length - 1][i] = -1;
            }
            //Initialize the heuristics
            myNodes[i].setDistToGoal(space.findShortestDistance(myNodes[i].getPosition(), goal));
        }
        adjacency[myNodes.length-2][myNodes.length-2] = 0; //The start is adjacent to itself
        adjacency[myNodes.length-1][myNodes.length-1] = 0; //The goal is adjacent to itself
        if(space.findShortestDistance(start, goal) < 2*SCALE && space.isPathClearOfObstructions(start, goal, allObjects, Ship.SHIP_RADIUS)) {
            //If the start and goal are adjacent
            //Mark them as such
            adjacency[myNodes.length-1][myNodes.length-2] = space.findShortestDistance(start, goal);
            adjacency[myNodes.length-2][myNodes.length-1] = space.findShortestDistance(start, goal);
        } else {
            //Otherwise, mark that they aren't adjacent
            adjacency[myNodes.length-1][myNodes.length-2] = -1;
            adjacency[myNodes.length-2][myNodes.length-1] = -1;
        }
        //
        getGoal().setDistToGoal(0);
        getStart().setDistToGoal(space.findShortestDistance(start, goal));
    }
    
    private int getCol(GraphNode n) {
        //We convert the x position to a column
        //We add an arbitrary small number to avoid round-off error
        return ((int)(n.getPosition().getX() + 0.0002))/SCALE;
    }
    
    private int getRow(GraphNode n) {
        //We convert the y position to the row
        //We add an arbitrary small number to avoid round-off error
        return ((int)(n.getPosition().getY() + 0.0002))/SCALE;
    }
    
    private boolean inBox(GraphNode center, GraphNode queryNode) {
        //Return true if queryNode is in a 2*SCALE by 2*SCALE box centered on center
        //Return false otherwise
        boolean xMatch;
        boolean yMatch;
        if(getCol(center) == 0) {
            //We are on the left edge of the screen
            //We need to look at positions on the right edge too
            xMatch = queryNode.getPosition().getX() <= SCALE || WIDTH*SCALE - SCALE <= queryNode.getPosition().getX();
        } else {
            //We are somewhere in the middle
            //We can just look at a vertical bar
            xMatch = center.getPosition().getX() - SCALE <= queryNode.getPosition().getX() &&
                    center.getPosition().getX() + SCALE >= queryNode.getPosition().getX();
        }
        if(getRow(center) == 0) {
            //We are on the top edge of the screen
            //Check the bottom edge too
            yMatch = queryNode.getPosition().getY() <= SCALE || HEIGHT*SCALE - SCALE <= queryNode.getPosition().getY();
        } else {
            //Check a horizontal bar about center to see if it contains queryNode
            yMatch = center.getPosition().getY() - SCALE <= queryNode.getPosition().getY() &&
                    center.getPosition().getY() + SCALE >= queryNode.getPosition().getY();
        }
        //If a horizontal bar about center and a vertical bar about center both contain queryNode, return True
        //Otherwise, return false
        return xMatch && yMatch;
    }
    
    //Are these integer points adjacent on our toroidal surface?
    private boolean offByOne(int x1, int y1, int x2, int y2) {
        boolean xMatch; //Whether the x value is valid for adjacency
        boolean yMatch; //Whether the y value is appropriate for adjacency
        if((x1 == 0 && x2 == WIDTH -1) || (x1 == WIDTH -1 && x2 == 0)) {
            //The two places are on the edge of a screen wrap
            //The columns are adjacent
            xMatch = true;
        } else if (Math.abs(x1 - x2) <= 1) {
            //The x value is only off by one
            xMatch = true;
        } else {
            //The x value is off by more than one
            //The nodes are not adjacent
            xMatch = false;
        }
        if((y1 == 0 && y2 == HEIGHT - 1) || (y1 == HEIGHT - 1 && y2 == 0)) {
            //The rows are adjacent
            yMatch = true;
        } else if (Math.abs(y1 - y2) <= 1) {
            //The y value is only off by one
            yMatch = true;
        } else {
            //The y value is off by more than one
            //The nodes are not adjacent
            yMatch = false;
        }
        //If the rows and columns are both adjacent and identical
        // The squares are adjacent or identical
        // Return true
        //Otherwise, return false
        return xMatch && yMatch;
    }
    //Return the x-th node from the left, in the y-th row
    public GraphNode getNode(int x, int y) {
        int myIndex = y + x*HEIGHT;
        return myNodes[myIndex];
    }
    
    //Return the start
    public GraphNode getStart() {
        return myNodes[myNodes.length-2];
    }
    
    //Return the goal
    public GraphNode getGoal() {
        return myNodes[myNodes.length-1];
    }
    
    private int getIndex(GraphNode n) {
        double x = n.getPosition().getX();
        double y = n.getPosition().getY();
        //If x is close to where being in the nearest column would put it
        //And if y is close to being where its nearest row would put it
        //The we treat it as though it were the grid node at that point
        if(Math.abs(x - SCALE*getCol(n)) <= 0.000001) {
            //x is an integer, on the lattice
            if(Math.abs(y - SCALE*getRow(n)) <= 0.000001) {
                //y is an integer, on the lattice
                return getRow(n) + getCol(n)*HEIGHT;
            }
        }
        //It isn't one of our grid nodes
        //Thus, it must be the start or goal node
        if(Math.abs(x - getStart().getPosition().getX()) <= 0.000001) {
            if(Math.abs(y - getStart().getPosition().getY()) <= 0.000001) {
                //It is at the start node, so it must be the start
                return myNodes.length - 2;
            }
        }
        //If it made it down here, it must be at the goal node
        //So return the goal node's index
        return myNodes.length - 1;
    }
    
    //Return a set containing all children of the given GraphNode
    public Set<GraphNode> getChildren(GraphNode parentNode) {
        //Get the GraphNode's index in the array
        int index = getIndex(parentNode);
        Set<GraphNode> myChildren = new HashSet<GraphNode>();
        for(int i = 0; i < myNodes.length; i++) {
            //Loop through all nodes
            //If it is adjacent, add it to the set
            if(adjacency[index][i] >= 0) {
                myChildren.add(myNodes[i]);
            }
        }
        return myChildren;
    }
    
    //Return the edge length between any two graph nodes
    //This is negative if there is no connection
    //Or 0 if they are the same node
    //If either node does not exist in the graph, the distance from the remaining node to the goal node will be returned
    //If both nodes do not exist in the graph, 0 will be returned, the distance from the goal node to the goal node
    public double getEdgeLength(GraphNode node1, GraphNode node2) {
        int index1 = getIndex(node1);
        int index2 = getIndex(node2);
        return adjacency[index1][index2];
    }
}