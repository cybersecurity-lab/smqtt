package it.unipr.netsec.smqtt.gkd.method.keytree;


/**
 * A node of a binary tree, characterized by its depth (root has depth=0) and position among the other nodes at the same depth.
 */
public class Node {
	
	/** Node depth (0,1,2,...) */
	public final int dep;
	
	/** Node position (0,i,..,2^(i-1)) */
	public final int pos;
	
	/**
	 * @param dep
	 * @param pos
	 */
	public Node(int dep, int pos) {
		this.dep= dep;
		this.pos= pos;
		int max= (1<<dep)-1;
		if (dep>max) throw new RuntimeException("Invalid node "+toString());
	}
	
	
	/**
	 * @param n node
	 */
	public Node(Node n) {
		this(n.dep,n.pos);
	}

	
	/** 
	 * @return the parent node
	 */
	public Node parent() {
		if (dep==0) throw new RuntimeException("Node (0,0) doesn't have a parent node.");
		return new Node(dep-1,pos/2);
	}
	
	
	/** 
	 * @return left child node
	 */
	public Node left() {
		return new Node(dep+1,2*pos);
	}
	
	
	/** 
	 * @return right child node
	 */
	public Node right() {
		return new Node(dep+1,2*pos+1);
	}

	
	/**
	 * @param h tree depth
	 * @return the indexes of the leaves
	 */
	public IntRange leaves(int h) {
		if (h<dep) throw new RuntimeException("Invalid tree depth.");
		return new IntRange(pos*(1<<h-dep),(pos+1)*(1<<h-dep)-1);
	}
	
	
	@Override
	public boolean equals(Object o) {
		var node= (Node)o;
		return dep==node.dep && pos==node.pos;
	}

	
	@Override
	public String toString() { return "("+dep+","+pos+")"; }
}