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
	
	private AStarTreeNode root 	= null;
	private PriorityQueue<AStarTreeNode> fringe    = null;
	private Set<GraphNode> nodesAlreadyVisited = null;
	private GridGraph _graph		= null;
	private GraphNode _initialNode  = null;
	private AStarTreeNode _previousNode = null;
	private AStarTreeNode _currentNode  = null;
	private GraphNode _goalNode 	= null;
	private double _current_gn = 0.0;
	private double total_fn = 0.0;
	
	public A_Star_Search(GridGraph graph, GraphNode initialNode, GraphNode goalNode) {
		
		nodesAlreadyVisited = new HashSet<GraphNode>();
		
		_graph		  = graph;
		_initialNode  = initialNode;
		_goalNode	  = goalNode;
		
		root = new AStarTreeNode(_initialNode, 0.0);
		
		fringe = new PriorityQueue<AStarTreeNode>(graph.HEIGHT*graph.WIDTH, new GraphNodeComparator(this, _graph));
		
	}
	
	public AStarTreeNode[] search() {
		_currentNode = root;
		
		// Add all children from initialNode(_currentNode) to the fringe.
		for(GraphNode child : _graph.getChildren(_currentNode.getMyNode())) {
			fringe.add(new AStarTreeNode(child, _graph.getEdgeLength(_currentNode.getMyNode(), child)));
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
				
				// Compute f(n) by iterating through the list of nodes and 
				// adding their h(n) and distance to each other g(n).
				return (AStarTreeNode[]) _currentNode.getPath();
			}
			
			// Add current child to the visited set since we are finished looking at it.
			if (!nodesAlreadyVisited.contains(_currentNode.getMyNode())) {
				nodesAlreadyVisited.add(_currentNode.getMyNode());
			}
			
			// Get the children of the current child.
			for (GraphNode child : _graph.getChildren(_currentNode.getMyNode())) {
				// If we have already visited this child, ignore it.
				if (nodesAlreadyVisited.contains(child) || fringe.contains(child)) {
					continue;
				} else {		
					// Else add it to the priority queue
					AStarTreeNode childNode = new AStarTreeNode(child, _graph.getEdgeLength(child, _currentNode.getMyNode()));
					
					fringe.add(childNode);
					// Since this child will have the lowest f(n) value, add it to the tree.
					_currentNode.add(childNode);
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
		pathCost = ((AStarTreeNode)this.getParent()).getPathCost() + edgeLength;
	}
	
}

