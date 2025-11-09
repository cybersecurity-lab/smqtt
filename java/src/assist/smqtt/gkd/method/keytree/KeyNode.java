package assist.smqtt.gkd.method.keytree;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.zoolu.util.Bytes;


/**
 * A node of a key tree.
 */
public class KeyNode extends Node {
	
	/** Hash algorithm used to derive the child keys  */
	public static String HASH_ALGO= "SHA256";

	/** Hash function used to derive the child keys  */
	static MessageDigest hash;
		
	static {
		try {
			hash= MessageDigest.getInstance(HASH_ALGO);
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	
	public final byte[] key;

	
	public KeyNode(Node node, byte[] key) {
		super(node);
		this.key= key;
	}

	
	public KeyNode(int dep, int pos, byte[] key) {
		super(dep,pos);
		this.key= key;
	}
	
	
	/** Gets the successor at a given depth and position.
	 * @param node the depth and position
	 * @return the successor key node
	 */
	public KeyNode successor(Node node) {
		return successor(node.dep,node.pos);
	}

	
	/** Gets the successor at a given depth and position.
	 * @param dep successor depth
	 * @param pos successor position
	 * @return the successor key node
	 */
	public KeyNode successor(int dep, int pos) {
		//System.out.println("DEBUG: KeyNode: "+toString()+".successor("+dep+", pos="+pos+")");
		var leaves= leaves(dep);
		if (!leaves.contains(pos)) throw new RuntimeException("Node "+new Node(dep,pos)+" is not a successor of "+this);
		while (this.dep<dep) {
			if (pos<leaves.size()/2+leaves.first) return left().successor(dep,pos);
			else return right().successor(dep,pos);
		}
		return this;
	}

	
	public String toJson() {
		return "["+dep+","+pos+","+Bytes.toHex(key)+"]";
	}

	
	public static KeyNode fromJson(String json) {
		//System.out.println("DEBUG: KeyNode: fromJson(): "+json);
		json= json.trim();
		json= json.substring(1,json.length()-1);
		var obj= json.split(",");
		return new KeyNode(Integer.parseInt(obj[0]),Integer.parseInt(obj[1]),Bytes.fromHex(obj[2]));
	}
		
	
	@Override
	public KeyNode left() {
		return new KeyNode(super.left(),f0(key));
	}
	
	
	@Override
	public KeyNode right() {
		return new KeyNode(super.right(),f1(key));
	}


	@Override
	public boolean equals(Object obj) {
		var knode= (KeyNode)obj;
		return super.equals(knode) && Bytes.compare(key,knode.key)==0;
	}
	
	
	@Override
	public String toString() {
		return toJson();
	}
	
	
	/** f0() function used to derive the left child key.
	 * @param x the node key
	 * @return left child key
	 */
	private static byte[] f0(byte[] x) {
		var y= hash.digest(x);
		if (x.length<y.length) y= Bytes.copy(y,0,x.length);
		return y;
	}
	
	
	/** f1() function used to derive the right child key.
	 * @param x the node key
	 * @return right child key
	 */
	private static byte[] f1(byte[] x) {
		x= Bytes.copy(x);
		x[x.length-1]^= 0x01;
		var y= f0(x);
		return y;
	}

}
