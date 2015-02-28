import NetworkElements.*;

import java.util.*;

public class example {
	// This object will be used to move time forward on all objects
	private int time = 0;
	private ArrayList<LSR> allConsumers = new ArrayList<LSR>();
	/**
	 * Create a network and creates connections
	 * @since 1.0
	 */
	public void go(){
		System.out.println("** SYSTEM SETUP **");
		
//		// Create some new ATM Routers
//		LSR r1 = new LSR(9);
//		LSR r2 = new LSR(3);
//		LSR r3 = new LSR(11);
//		LSR r4 = new LSR(13);
//		LSR r5 = new LSR(15);
//
//		// give the routers interfaces
//		LSRNIC r1n1 = new LSRNIC(r1);
//		LSRNIC r2n1 = new LSRNIC(r2);
//		LSRNIC r2n2 = new LSRNIC(r2);
//		LSRNIC r2n3 = new LSRNIC(r2);
//		LSRNIC r3n1 = new LSRNIC(r3);
//		LSRNIC r3n2 = new LSRNIC(r3);
//		LSRNIC r3n3 = new LSRNIC(r3);
//		LSRNIC r3n4 = new LSRNIC(r3);
//		LSRNIC r4n1 = new LSRNIC(r4);
//		LSRNIC r4n2 = new LSRNIC(r4);
//		LSRNIC r4n3 = new LSRNIC(r4);
//		LSRNIC r5n1 = new LSRNIC(r5);
//
//		// physically connect the router's nics
//		OtoOLink l1 = new OtoOLink(r1n1, r2n1);
//		OtoOLink l2 = new OtoOLink(r2n2, r3n1);
//		OtoOLink l2opt = new OtoOLink(r2n3, r3n2, true); // optical link
//		OtoOLink l3 = new OtoOLink(r3n3, r4n1);
//		OtoOLink l3opt = new OtoOLink(r3n4, r4n2, true); // optical link
//		OtoOLink l4 = new OtoOLink(r4n3, r5n1);
//
//		// Add the objects that need to move in time to an array
//		this.allConsumers.add(r1);
//		this.allConsumers.add(r2);
//		this.allConsumers.add(r3);
//		this.allConsumers.add(r4);
//
//		//send packets from router 1 to the other routers...
//		r1.createPacket(13);
//

        // Create some new ATM Routers
        LSR A = new LSR(1,true,false);
        LSR B = new LSR(2,true,true);
        LSR C = new LSR(3,false,true);
        LSR D = new LSR(4,false,true);
        LSR E = new LSR(5,false,true);
        LSR F = new LSR(6,true,true);
        LSR G = new LSR(7,true,false);

        // give the routers interfaces
        LSRNIC An1 = new LSRNIC(A);
        LSRNIC Bn1 = new LSRNIC(B);
        LSRNIC Bn2 = new LSRNIC(B);
        LSRNIC Bn3 = new LSRNIC(B);
        LSRNIC Cn1 = new LSRNIC(C);
        LSRNIC Cn2 = new LSRNIC(C);
        LSRNIC Cn3 = new LSRNIC(C);
        LSRNIC Cn4 = new LSRNIC(C);
        LSRNIC Dn1 = new LSRNIC(D);
        LSRNIC Dn2 = new LSRNIC(D);
        LSRNIC Dn3 = new LSRNIC(D);
        LSRNIC Dn4 = new LSRNIC(D);
        LSRNIC En1 = new LSRNIC(E);
        LSRNIC En2 = new LSRNIC(E);
        LSRNIC En3 = new LSRNIC(E);
        LSRNIC En4 = new LSRNIC(E);
        LSRNIC Fn1 = new LSRNIC(F);
        LSRNIC Fn2 = new LSRNIC(F);
        LSRNIC Fn3 = new LSRNIC(F);
        LSRNIC Gn1 = new LSRNIC(G);

        // physically connect the router's nics
        OtoOLink l1 = new OtoOLink(An1, Bn1);
        OtoOLink l2 = new OtoOLink(Bn2, Cn1);
        OtoOLink l2opt = new OtoOLink(Bn3, Cn2, true); // optical link
        OtoOLink l3 = new OtoOLink(Cn3, Dn1);
        OtoOLink l3opt = new OtoOLink(Cn4, Dn2, true); // optical link
        OtoOLink l4 = new OtoOLink(Dn3, En1);
        OtoOLink l4opt = new OtoOLink(Dn4, En2, true); // optical link
        OtoOLink l5 = new OtoOLink(En3, Fn1);
        OtoOLink l5opt = new OtoOLink(En4, Fn2, true); // optical link
        OtoOLink l6 = new OtoOLink(Fn3, Gn1);

        // Add the objects that need to move in time to an array
        this.allConsumers.add(A);
        this.allConsumers.add(B);
        this.allConsumers.add(C);
        this.allConsumers.add(D);
        this.allConsumers.add(E);
        this.allConsumers.add(F);
        this.allConsumers.add(G);

        //send packets from router 1 to the other routers...
        A.createPacket(7);

        for(int i =0; i<30; i++)
            tock();
        G.createPacket(1);

        for(int i =0; i<30; i++)
            tock();
	}
	
	public void tock(){
		System.out.println("** TIME = " + time + " **");
		time++;		
		
		// Send packets between routers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).sendPackets();

		// Move packets from input buffers to output buffers
		for(int i=0; i<this.allConsumers.size(); i++)
			allConsumers.get(i).receivePackets();
		
	}
	public static void main(String args[]){
		example go = new example();
		go.go();
	}
}