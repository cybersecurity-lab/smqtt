package it.unipr.netsec.smqtt.gkd;


public class IndexKeyPair {
	
	public int index;
	public byte[] key;
	
	public IndexKeyPair(int index, byte[] key) {
		this.index= index;
		this.key= key;
	}
}
