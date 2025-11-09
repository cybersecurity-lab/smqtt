package assist.smqtt.gkd.method.keytree;

import java.util.ArrayList;


public class IntRange {
	public int first;
	public int last;
	
	public IntRange(int first, int last) {
		this.first= first;
		this.last= last;	
		if (last<first || first<0) throw new RuntimeException("Invalid interval "+toString());
	}
	
	
	public int size() {
		return last-first+1;
	}

	
	public boolean contains(IntRange it) {
		return it.first>=first && it.last<=last;
	}
	
	
	public boolean contains(int val) {
		return val>=first && val<=last;
	}

	
	/** Gets a list of nodes in a binary tree that are parents of all leaves whose indices are in the interval.
	 * No other leaves are generated.
	 * @param height tree height
	 * @return
	 */
	public ArrayList<Node> generators(int height) {
		var list= new ArrayList<Node>();
		addGenerators(this,new Node(height,first),height,list);
		return list;
	}
	
	
	private static void addGenerators(IntRange range, Node node, int height, ArrayList<Node> list) {
		//System.out.println("DEBUG: IntRange: addGenerators(): v="+v+", n="+node);
		if (node.leaves(height).equals(range)) {
			list.add(node);
			return;
		}
		// else
		var parent= node.parent();
		var parentLeaves= parent.leaves(height);
		if (range.contains(parentLeaves)) addGenerators(range,parent,height,list);
		else {
			list.add(node);
			var nodeLeaves= node.leaves(height);
			if (nodeLeaves.last<range.last) {
				range= new IntRange(nodeLeaves.last+1,range.last);
				addGenerators(range,new Node(height,range.first),height,list);
			}
		}
		
	}
	
	
	@Override
	public boolean equals(Object o) {
		var it= (IntRange)o;
		return first==it.first && last==it.last;
	}


	@Override
	public String toString() { return "["+first+","+last+"]"; }
}
