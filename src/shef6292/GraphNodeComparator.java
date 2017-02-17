package shef6292;

import java.util.Comparator;

public class GraphNodeComparator implements Comparator<AStarTreeNode> {
	A_Star_Search _aStarSearch;
	//GraphNode _nextNode;
	GridGraph _graph;
	
	public GraphNodeComparator(A_Star_Search aStarSearch, GridGraph graph) {
		_aStarSearch = aStarSearch;
		_graph = graph;
	}

	@Override
	public int compare(AStarTreeNode treeNode1, AStarTreeNode treeNode2) {
		
		GraphNode currentNode = _aStarSearch.getCurrentNode().getMyNode();
		double gn = _aStarSearch.get_running_gn();
		
		GraphNode child1 = treeNode1.getMyNode();
		GraphNode child2 = treeNode2.getMyNode();
		
		// If child1's f(n) value [f(n)=g(n)+h(n)] is less than child2's, child1 will
		// move up in priority.
		if (child1.getDistToGoal() + treeNode1.getPathCost() <
				child2.getDistToGoal() + treeNode2.getPathCost()) {
			return -1;
		// If child2's f(n) value [f(n)=g(n)+h(n)] is less than child1's, child2 will
		// move up in priority.
		} else if (child1.getDistToGoal() + treeNode1.getPathCost() >
				child2.getDistToGoal() + treeNode2.getPathCost()) {
			return 1;
		}
		// They have the same f(n) value, so do no sorting.
		return 0;
	}
	
	
}
