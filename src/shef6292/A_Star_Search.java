/**
 * 
 */
package shef6292;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * 
 *
 */
public class A_Star_Search {
	// The amount of vertices that the priority queue can hold.
	//private final int PRIORITY_QUEUE_SIZE = 10000;
	
	private AStarTreeNode root = null;
	private PriorityQueue<AStarTreeNode> fringe = null;
	private Set<GraphNode> nodesAlreadyVisited = null;
	private GridGraph _graph = null;
	private GraphNode _initialNode  = null;
	private AStarTreeNode _previousNode = null;
	private AStarTreeNode _currentNode = null;
	private GraphNode _goalNode = null;
	private double _current_gn = 0.0;
	private double total_fn = 0.0;
	
	public A_Star_Search(GridGraph graph, GraphNode initialNode, GraphNode goalNode) {
		
		nodesAlreadyVisited = new HashSet<GraphNode>();
		
		_graph = graph;
		_initialNode = initialNode;
		_goalNode = goalNode;
		
		root = new AStarTreeNode(_initialNode, 0.0);
		
		fringe = new PriorityQueue<AStarTreeNode>(graph.HEIGHT*graph.WIDTH, new GraphNodeComparator(this, _graph));
		
	}
	
	public AStarTreeNode[] search() {
            _currentNode = root;

            // Add all children from initialNode(_currentNode) to the fringe.
            for(GraphNode child : _graph.getChildren(_currentNode.getMyNode())) {
                //Create a node for the child
                AStarTreeNode childNode = new AStarTreeNode(child);
                
                //Add the child to the tree
                _currentNode.add(childNode);
                
                //Calculate the path cost to the child
                childNode.setPathCost(_graph.getEdgeLength(child, _currentNode.getMyNode()));
                
                //Add the child to the fringe
                fringe.add(childNode);
            }

            // May need to change while condition since there are always adjacent nodes.
            while (fringe.size() != 0) {
                    if (fringe.size() == 0) return null;
                    // if maxsteps return failure

                    // Get the previous node for g(n) calculation.
                    _previousNode = _currentNode;
                    if (!nodesAlreadyVisited.contains(_currentNode)) {
                            nodesAlreadyVisited.add(_currentNode.getMyNode());
                    }
                    // Look at and remove the first child of the priority queue.
                    _currentNode = fringe.poll();

                    // If we've already visited this node, grab the next one.
                    while (nodesAlreadyVisited.contains(_currentNode.getMyNode()) && !fringe.isEmpty()) {
                            _currentNode = fringe.poll();
                    }

                    // Check if currentNode is the goalNode.
                    if (_currentNode.getMyNode().equals(_goalNode)){

                            //Return the path from the root to the current node
                            TreeNode[] tmpPath = _currentNode.getPath();
                            AStarTreeNode[] path = new AStarTreeNode[tmpPath.length];
                            for(int i = 0; i < path.length; i++) {
                                path[i] = (AStarTreeNode) tmpPath[i];
                            }
                            return path;
                    }

                    // Add current child to the visited set since we are finished looking at it.
                    if (!nodesAlreadyVisited.contains(_currentNode.getMyNode())) {
                            nodesAlreadyVisited.add(_currentNode.getMyNode());
                    }

                    // Get the children of the current child.
                    for (GraphNode child : _graph.getChildren(_currentNode.getMyNode())) {
                            // If we have already visited this child, ignore it.
                            if (nodesAlreadyVisited.contains(child)) {
                                    continue;
                            } else {		
                                    // Else add it to the priority queue
                                    //Create a node for the child
                                    AStarTreeNode childNode = new AStarTreeNode(child);

                                    //Add the child to the tree
                                    _currentNode.add(childNode);

                                    //Calculate the path cost to the child
                                    childNode.setPathCost(_graph.getEdgeLength(child, _currentNode.getMyNode()));

                                    //Add the child to the fringe
                                    fringe.add(childNode);
                            }	
                    }	
            }

            // No solution was found.
            return null;
	}
	
	public double get_running_gn() {
		return _current_gn;
	}
	
	// Always call after search has been called.
	public double get_fn() {
		return total_fn;
	}

	// This is mainly for the GraphNodeComparator class so it can update its comparison operand.
	public AStarTreeNode getCurrentNode() {
		return _currentNode;
	}
	
}

class AStarTreeNode extends DefaultMutableTreeNode {
	private GraphNode myNode;
	private double pathCost;
	
	public AStarTreeNode(GraphNode node) {
		super();
		myNode = node;
	}
        
        public AStarTreeNode(GraphNode node, double edgeLength) {
		super();
		setPathCost(edgeLength);
		myNode = node;
	}
	
	public GraphNode getMyNode() {
		return myNode;
	}
	
	public double getPathCost() {
		return pathCost;
	}
	
	public void setPathCost(double edgeLength) {
            if(getParent() != null) {
                //If the node has a parent
                //Set add the edge length from the parent to this node to the parent's path cost
		pathCost = ((AStarTreeNode)getParent()).getPathCost() + edgeLength;
            } else {
                //Otherwise, we are at the start
                //And the path cost must be 0
                pathCost = 0;
            }
	}	
}

