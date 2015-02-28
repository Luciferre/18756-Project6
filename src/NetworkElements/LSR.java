package NetworkElements;

import java.util.*;

import DataTypes.*;
import Dijkstra.Dijkstra;
import javafx.scene.effect.InnerShadow;

public class LSR {
    private int address; // The AS address of this router
    private ArrayList<LSRNIC> nics = new ArrayList<LSRNIC>(); // all of the nics in this router
    private boolean isInitial = true;
    private Dijkstra dj = new Dijkstra();
    private TreeMap<Integer, NICLabelPair> LabeltoLabel = new TreeMap<Integer, NICLabelPair>(); // a map of input Label to output nic and new Label number
    private TreeMap<OpticalLabel, LSRNIC> opticalLabeltoLabel = new TreeMap<OpticalLabel, LSRNIC>();
    private HashMap<Integer, Integer> LabeltoDest = new HashMap<Integer, Integer>();
    private HashMap<OpticalLabel, Integer> opticalLabeltoDest = new HashMap<OpticalLabel, Integer>(); // a map of input label to destination
    private HashMap<Integer, Integer> routingTable = new HashMap<Integer, Integer>();//the next router to get to the destination
    private HashMap<Integer, Integer> routingOpticalTable = new HashMap<Integer, Integer>();//the next router to get to the destination
    private ArrayList<Packet> waitedPackets = new ArrayList<Packet>();
    private ArrayList<Integer> isEstabLSP = new ArrayList<Integer>();
    private boolean psc = false;
    private boolean lsc = false;
    private boolean trace = true;
    private ArrayList<Packet> newwaitedPackets = new ArrayList<Packet>();


    /**
     * The default constructor for an ATM router
     *
     * @param address the address of the router
     * @since 1.0
     */
    public LSR(int address) {
        this.address = address;
    }

    /**
     * The default constructor for an ATM router
     *
     * @param address the address of the router
     * @since 1.0
     */
    public LSR(int address, boolean psc, boolean lsc) {
        this.address = address;
        this.psc = psc;
        this.lsc = lsc;
    }

    /**
     * The return the router's address
     *
     * @since 1.0
     */
    public int getAddress() {
        return this.address;
    }

    /**
     * Adds a nic to this router
     *
     * @param nic the nic to be added
     * @since 1.0
     */
    public void addNIC(LSRNIC nic) {
        this.nics.add(nic);
    }

    /**
     * This method processes data and OAM cells that arrive from any nic with this router as a destination
     *
     * @param currentPacket the packet that arrived at this router
     * @param nic           the nic that the cell arrived on
     * @since 1.0
     */
    public void receivePacket(Packet currentPacket, LSRNIC nic) {
        //System.out.println("packet: " + currentPacket.getSource() + ", " + currentPacket.getDest());
        //System.out.println("\tOAM: " + currentPacket.isOAM());
        if (currentPacket.isOAM()) {
            // System.out.println("\tOAM: " + currentPacket.getOAMMsg() + ", " + currentPacket.getOpticalLabel().toString());
            // What's OAM for? set up LSP
            //setup
            if (currentPacket.getOAMMsg().equals("Path")) {

                receivedPath(currentPacket);
                LSRNIC forwardNIC = getRoutingNic(currentPacket.getDest());
                // IP receive path
                if (this.psc == true && this.lsc == false) {

                    if (forwardNIC != null) {
                        // psr -> psr
                        int i;
                        for (i = 1; i <= LabeltoLabel.size(); i++) {
                            if (!LabeltoLabel.containsKey(i)) {
                                break;
                            }
                        }
                        LabeltoLabel.put(i, new NICLabelPair(nic, currentPacket.getPacketLabel()));
                        // LabeltoDest.put(i, currentPacket.getSource());
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: PSI\\" + routingTable.get(currentPacket.getDest())
                                + "\\" + i + ", " + "Output: PSI\\" + routingTable.get(currentPacket.getSource())
                                + "\\" + currentPacket.getPacketLabel());
                        Packet nextPacket = new Packet(currentPacket.getSource(), currentPacket.getDest(), i);
                        sentPath(nextPacket);
                        nextPacket.setOAM(true, "Path");
                        nextPacket.setDSCP(currentPacket.getDSCP());
                        forwardNIC.sendPacket(nextPacket, this);


                    } else if (currentPacket.getDest() == this.address) {

                        int i;
                        for (i = 1; i <= LabeltoLabel.size(); i++) {
                            if (!LabeltoLabel.containsKey(i)) {
                                break;
                            }
                        }
                        //LabeltoLabel.put(i, new NICLabelPair(nic, currentPacket.getPacketLabel()));
                        LabeltoDest.put(currentPacket.getPacketLabel(), getHashcode(currentPacket.getSource(), currentPacket.getDSCP()));
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: null\\null\\null, " + "Output: PSI\\"
                                + routingTable.get(currentPacket.getSource())
                                + "\\" + currentPacket.getPacketLabel());
                        Packet resvPacket = new Packet(currentPacket.getDest(), currentPacket.getSource(), i);

                        resvPacket.setOAM(true, "Resv");
                        sentResv(resvPacket);
                        nic.sendPacket(resvPacket, this);

                    }
                }
                //OXC receive path
                else if (this.psc == false && this.lsc == true) {
                    //lsr -> lsr
                    if(currentPacket.getOpticalLabel() != null) {
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: LSI\\" +
                                routingTable.get(currentPacket.getDest()) + "\\" + currentPacket.getOpticalLabel() + ", " + "Output: LSI\\" +
                                routingTable.get(currentPacket.getSource()) + "\\" + currentPacket.getOpticalLabel());
                        opticalLabeltoLabel.put(currentPacket.getOpticalLabel(), getOpticalRoutingNic(nextPSR(currentPacket.getSource())));
                        //Packet nextPacket = new Packet(currentPacket.getSource(), currentPacket.getDest(), color);
                        sentPath(currentPacket);
                        // nextPacket.setOAM(true, "Path");
                        // nextPacket.setDSCP(currentPacket.getDSCP());
                        forwardNIC.sendPacket(currentPacket, this);
                    }else{
                        sentPath(currentPacket);
                        forwardNIC.sendPacket(currentPacket, this);
                    }

                }
                //hybrid receive path
                else if (this.psc == true && this.lsc == true) {
                    //hybrid receive lsc path and send reserve to setup lsc
                    if (currentPacket.getOpticalLabel() != null) {

                        OpticalLabel color = null;
                        for (OpticalLabel i : GlobalVariables.opticalLabelUsed.keySet()) {
                            if (GlobalVariables.opticalLabelUsed.get(i) == false) {
                                color = i;
                                break;
                            }
                        }
                        GlobalVariables.opticalLabelUsed.remove(color);
                        GlobalVariables.opticalLabelUsed.put(color, true);
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: PSI\\" + routingTable.get(currentPacket.getDest())
                                        + "\\PSC" + ", " + "Output: LSI\\" + routingTable.get(currentPacket.getSource()) +
                                        "\\" + currentPacket.getOpticalLabel()
                        );
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: LSI\\" +
                                routingTable.get(currentPacket.getSource()) + "\\" + color + ", " + "Output: PSI\\" +
                                routingTable.get(currentPacket.getDest()) + "\\PSC");
                        opticalLabeltoLabel.put(currentPacket.getOpticalLabel(), getOpticalRoutingNic(nextPSR(currentPacket.getSource())));
                        int nextpsr = nextPSR(currentPacket.getSource());
                        opticalLabeltoDest.put(currentPacket.getOpticalLabel(), nextpsr);
                        Packet nextPacket = new Packet(currentPacket.getDest(), currentPacket.getSource(), color);
                        sentResv(nextPacket);
                        nextPacket.setOAM(true, "Resv");
                        nextPacket.setDSCP(currentPacket.getDSCP());
                        nic.sendPacket(nextPacket, this);
                    }

                    //hybrid receive psc path
                    else if (currentPacket.getPacketLabel() != -1) {
                        //hybrid receive psc path and forward ip
                        if (forwardNIC.getLinkDestLSR().psc == true) {
                            int i;
                            for (i = 1; i <= LabeltoLabel.size(); i++) {
                                if (!LabeltoLabel.containsKey(i)) {
                                    break;
                                }
                            }

                            LabeltoLabel.put(i, new NICLabelPair(nic, currentPacket.getPacketLabel()));
                            // LabeltoDest.put(i, currentPacket.getSource());
                            System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: PSI\\" + routingTable.get(currentPacket.getDest())
                                    + "\\" + i + ", " + "Output: PSI\\" + routingTable.get(currentPacket.getSource())
                                    + "\\" + currentPacket.getPacketLabel());
                            Packet nextPacket = new Packet(currentPacket.getSource(), currentPacket.getDest(), i);
                            sentPath(nextPacket);
                            nextPacket.setOAM(true, "Path");
                            nextPacket.setDSCP(currentPacket.getDSCP());
                            forwardNIC.sendPacket(nextPacket, this);
                        }
                        //hybrid receive psc path and convert to lsc path
                        else {

                            OpticalLabel color = null;
                            for (OpticalLabel i : GlobalVariables.opticalLabelUsed.keySet()) {
                                if (GlobalVariables.opticalLabelUsed.get(i) == false) {
                                    color = i;
                                    break;
                                }
                            }
                            GlobalVariables.opticalLabelUsed.remove(color);
                            GlobalVariables.opticalLabelUsed.put(color, true);
                            //opticalLabeltoLabel.put(color, getOpticalRoutingNic(currentPacket.getDest()));
                            LabeltoDest.put(currentPacket.getPacketLabel(), getSDHashcode(currentPacket.getSource(), currentPacket.getDest()));
                            System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: LSI\\" +
                                    routingTable.get(currentPacket.getDest()) + "\\" + color + ", " + "Output: PSI\\" +
                                    routingTable.get(currentPacket.getSource()) + "\\" + currentPacket.getPacketLabel());
                            Packet nextPacket = new Packet(currentPacket.getSource(), currentPacket.getDest(), color);
                            sentPath(nextPacket);
                            nextPacket.setOAM(true, "Path");
                            nextPacket.setDSCP(currentPacket.getDSCP());
                            forwardNIC.sendPacket(nextPacket, this);
                        }
                    }

                }
            }
            //Resv
            else if (currentPacket.getOAMMsg().equals("Resv")) {
                receivedResv(currentPacket);
                //ip receive resv
                if (this.lsc == false && this.psc == true) {
                    int i;
                    // calculate the available input Label
                    for (i = 1; i <= LabeltoLabel.size(); i++) {
                        if (!LabeltoLabel.containsKey(i)) {
                            break;
                        }
                    }

                    NICLabelPair newNicLabelPair;
                    newNicLabelPair = new NICLabelPair(nic, currentPacket.getPacketLabel());
                    LabeltoLabel.put(i, newNicLabelPair);


                    if (this.getAddress() == currentPacket.getDest()) {
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: null\\null\\null, " + "Output: PSI\\"
                                        + routingTable.get(currentPacket.getSource()) + "\\" + currentPacket.getPacketLabel()
                        );
                        Packet ResvConfPacket = new Packet(currentPacket.getDest(), currentPacket.getSource());
                        sentResvConf(ResvConfPacket);
                        ResvConfPacket.setOAM(true, "ResvConf");
                        nic.sendPacket(ResvConfPacket, this);

                        LabeltoDest.put(currentPacket.getPacketLabel(), getHashcode(currentPacket.getSource(), currentPacket.getDSCP()));

                        newwaitedPackets.clear();
                        boolean isSent = false;
                        for (Packet packet : waitedPackets) {
                            isSent = false;
                            for (Integer key : LabeltoDest.keySet()) {
                                if (LabeltoDest.get(key) == getHashcode(packet.getDest(), packet.getDSCP())) {
                                    Packet nextPacket = new Packet(packet.getSource(), packet.getDest(), key);

                                    getRoutingNic(nextPacket.getDest()).sendPacket(nextPacket, this);
                                    // waitedPackets.remove(packet);
                                    isSent = true;
                                }
                            }
                            if (isSent == false) {
                                newwaitedPackets.add(packet);
                                if (!isEstabLSP.contains(getHashcode(packet.getDest(), packet.getDSCP())))
                                    setupLSP(packet);
                            }
                        }
                        waitedPackets.clear();
                        waitedPackets.addAll(newwaitedPackets);
                        for (int j = 0; j < isEstabLSP.size(); j++) {
                            if (isEstabLSP.get(j) == getHashcode(currentPacket.getSource(), currentPacket.getDSCP()))
                                isEstabLSP.remove(j);

                        }
                    } else {
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: PSI\\" +
                                        routingTable.get(currentPacket.getDest()) + "\\" + i
                                        + ", " + "Output: PSI\\" + routingTable.get(currentPacket.getSource()) + "\\" + currentPacket.getPacketLabel()
                        );
                        Packet nextPacket = new Packet(currentPacket.getSource(), currentPacket.getDest(), i);
                        nextPacket.setOAM(true, "Resv");
                        nextPacket.setDSCP(currentPacket.getDSCP());
                        sentResv(nextPacket);
                        getRoutingNic(currentPacket.getDest()).sendPacket(nextPacket, this);

                    }
                }
                //oxc receive resv
                else if (this.psc == false && this.lsc == true) {
                    if(currentPacket.getOpticalLabel() != null) {
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: LSI\\" +
                                        routingTable.get(currentPacket.getDest()) + "\\" + currentPacket.getOpticalLabel()
                                        + ", " + "Output: LSI\\" + routingTable.get(currentPacket.getSource()) + "\\" + currentPacket.getOpticalLabel()
                        );
                        opticalLabeltoLabel.put(currentPacket.getOpticalLabel(), getOpticalRoutingNic(nextPSR(currentPacket.getSource())));
                        sentResv(currentPacket);
                        getRoutingNic(currentPacket.getDest()).sendPacket(currentPacket, this);
                    }else{
                        sentResv(currentPacket);
                        getRoutingNic(currentPacket.getDest()).sendPacket(currentPacket, this);
                    }
                }
                //hybrid receive resv
                else if (this.lsc == true && this.psc == true) {
                    //hybrid receive psc resv and forward it
                    if (currentPacket.getPacketLabel() != -1) {
                        int i;
                        // calculate the available input Label
                        for (i = 1; i <= LabeltoLabel.size(); i++) {
                            if (!LabeltoLabel.containsKey(i)) {
                                break;
                            }
                        }

                        NICLabelPair newNicLabelPair;
                        newNicLabelPair = new NICLabelPair(nic, currentPacket.getPacketLabel());
                        LabeltoLabel.put(i, newNicLabelPair);

                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: PSI\\" +
                                        routingTable.get(currentPacket.getDest()) + "\\" + i
                                        + ", " + "Output: PSI\\" + routingTable.get(currentPacket.getSource()) + "\\" + currentPacket.getPacketLabel()
                        );
                        Packet nextPacket = new Packet(currentPacket.getSource(), currentPacket.getDest(), i);
                        nextPacket.setOAM(true, "Resv");
                        nextPacket.setDSCP(currentPacket.getDSCP());
                        sentResv(nextPacket);
                        getRoutingNic(currentPacket.getDest()).sendPacket(nextPacket, this);

                    }
                    //hybrid receive lsc resv and setup lsc completed and send psc path
                    else if (currentPacket.getOpticalLabel() != null) {
                        int key = 0;
                        for (Integer i : LabeltoDest.keySet()) {
                            if (LabeltoDest.get(i) == getSDHashcode(currentPacket.getDest(), currentPacket.getSource()))
                                key = i;
                        }
                        LabeltoDest.remove(key);
                        opticalLabeltoDest.put(currentPacket.getOpticalLabel(), nextPSR(currentPacket.getSource()));
                        opticalLabeltoLabel.put(currentPacket.getOpticalLabel(), getOpticalRoutingNic(nextPSR(currentPacket.getSource())));

                        int i;
                        // calculate the available input Label
                        for (i = 1; i <= LabeltoLabel.size(); i++) {
                            if (!LabeltoLabel.containsKey(i)) {
                                break;
                            }
                        }
                        NICLabelPair newNicLabelPair;
                        newNicLabelPair = new NICLabelPair(getRoutingNic(currentPacket.getDest()), key);
                        LabeltoLabel.put(i, newNicLabelPair);

                        LSRNIC forwardNIC = getRoutingNic(currentPacket.getSource());
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: PSI\\" +
                                        routingTable.get(currentPacket.getSource()) + "\\PSC" + ", " + "Output: LSI\\"
                                        + routingTable.get(currentPacket.getDest()) + "\\" + currentPacket.getOpticalLabel()
                        );
                        System.out.println("LSR: " + this.getAddress() + ", ROUTE ADD, Input: PSI\\" +
                                routingTable.get(currentPacket.getDest()) + "\\" + i + ", " + "Output: PSI\\" +
                                routingTable.get(currentPacket.getSource()) + "\\PSC");
                        Packet nextPacket = new Packet(currentPacket.getDest(), currentPacket.getSource(), key);
                        sentPath(nextPacket);
                        nextPacket.setOAM(true, "Path");
                        nextPacket.setDSCP(currentPacket.getDSCP());
                        forwardNIC.sendPacket(nextPacket, this);
                    }
                }


            }
            //ResvConf
            else if (currentPacket.getOAMMsg().equals("ResvConf")) {
                receivedResvConf(currentPacket);
                if (this.getAddress() != currentPacket.getDest()) {

                    sentResvConf(currentPacket);
                    getRoutingNic(currentPacket.getDest()).sendPacket(currentPacket, this);
                }

            } else if (currentPacket.getOAMMsg().equals("PathErr")) {
                receivedPathErr(currentPacket);
                if (this.getAddress() != currentPacket.getDest()) {
                    sentPathErr(currentPacket);
                    getRoutingNic(currentPacket.getDest()).sendPacket(currentPacket, this);
                }
            } else if (currentPacket.getOAMMsg().equals("ResvErr")) {
                receivedResvErr(currentPacket);
                if (this.getAddress() != currentPacket.getDest()) {
                    sentResvErr(currentPacket);
                    getRoutingNic(currentPacket.getDest()).sendPacket(currentPacket, this);
                }
            }
        } else {
            // find the nic and new VC number to forward the currentPacket on
            // otherwise the currentPacket has nowhere to go. output to the console and drop the currentPacket
           // System.out.println("packet: " + currentPacket.getSource() + ", " + currentPacket.getDest());
            if (this.address != currentPacket.getDest()) {
                if (this.lsc == false && this.psc == true) {
                    if (LabeltoLabel.containsKey(currentPacket.getPacketLabel())) {

                        NICLabelPair newNicLabelPair = LabeltoLabel.get(currentPacket.getPacketLabel());
                        Packet nextPacket = new Packet(currentPacket.getSource(), currentPacket.getDest(), newNicLabelPair.getlabel());
                        sentPacket(currentPacket,newNicLabelPair.getNIC().getLinkDestLSR().getAddress());
                        newNicLabelPair.getNIC().sendPacket(nextPacket, this);

                    }
                } else if (this.lsc == true && this.psc == false) {
                    if (opticalLabeltoLabel.containsKey(currentPacket.getOpticalLabel())) {

                        LSRNIC newNic = opticalLabeltoLabel.get(currentPacket.getOpticalLabel());
                        sentPacket(currentPacket,newNic.getLinkDestLSR().getAddress());

                        newNic.sendPacket(currentPacket, this);

                    }
                } else if (this.psc == true && this.lsc == true) {

                    if (currentPacket.getOpticalLabel() != null) {
                        NICLabelPair forwardnic = LabeltoLabel.get(currentPacket.getPacketLabel());
                        Packet nextPacket = new Packet(currentPacket.getSource(), currentPacket.getDest(), forwardnic.getlabel());
                        sentPacket(currentPacket,forwardnic.getNIC().getLinkDestLSR().getAddress());
                        forwardnic.getNIC().sendPacket(nextPacket, this);
                    } else {
                        int nextpsr = nextPSR(currentPacket.getDest());
                        OpticalLabel color = null;
                        for (OpticalLabel i : opticalLabeltoDest.keySet()) {
                            if (opticalLabeltoDest.get(i) == nextpsr) {
                                color = i;
                                break;
                            }
                        }
                        NICLabelPair forwardnic = LabeltoLabel.get(currentPacket.getPacketLabel());
                        Packet nextPacket = new Packet(currentPacket.getSource(), currentPacket.getDest(), color);
                        nextPacket.setpacketLabel(LabeltoLabel.get(currentPacket.getPacketLabel()).getlabel());
                        sentPacket(currentPacket,forwardnic.getNIC().getLinkDestLSR().getAddress());
                        forwardnic.getNIC().sendPacket(nextPacket, this);
                    }

                }
            } else {
                if (trace)
                    System.out.println("Trace (Router" + this.address + "): Received a packet ");
            }
        }
    }

    /**
     * This method creates a packet with the specified type of service field and sends it to a destination
     *
     * @param destination the destination router
     * @since 1.0
     */

    public void createPacket(int destination) {
        Packet newPacket = new Packet(this.getAddress(), destination);
        this.sendPacket(newPacket);
    }

    /**
     * This method forwards a packet to the correct nic or drops if at destination router
     *
     * @param newPacket The packet that has just arrived at the router.
     * @since 1.0
     */
    public void sendPacket(Packet newPacket) {

        //This method should send the packet to the correct NIC (and wavelength if LSC router).
        if (isInitial == true) {
            //if (this.psc == true) {
            updateRoutingTable();

            // }
            if (this.lsc == true) {
                updateOpticalRoutingTable();
                //  isInitial = false;
            }
            isInitial = false;

        }

        //This method should send the packet to the correct NIC.
        //no lsp and establish new lsp
        if (!LabeltoDest.containsValue(getHashcode(newPacket.getDest(), newPacket.getDSCP()))) {
            //   waitedPackets.add(newPacket);
            if (isEstabLSP.contains(getHashcode(newPacket.getDest(), newPacket.getDSCP())))
                waitedPackets.add(newPacket);
            else {
                setupLSP(newPacket);
                waitedPackets.add(newPacket);
            }
        }
        //send new packet
        else {
            for (Integer key : LabeltoDest.keySet()) {
                if (LabeltoDest.get(key) == getHashcode(newPacket.getDest(), newPacket.getDSCP())) {
                    Packet nextPacket = new Packet(newPacket.getSource(), newPacket.getDest(), key);
                    getRoutingNic(nextPacket.getDest()).sendPacket(nextPacket, this);

                }
            }

        }

    }

    /**
     * This method forwards a packet to the correct nic or drops if at destination router
     *
     * @param newPacket The packet that has just arrived at the router.
     * @since 1.0
     */
    public void sendKeepAlivePackets() {

        //This method should send the keep alive packets for routes for each the router is an inbound router

    }

    /**
     * Makes each nic move its cells from the output buffer across the link to the next router's nic
     *
     * @since 1.0
     */
    public void sendPackets() {
        if (isInitial == true) {
            //if (this.psc == true) {
            updateRoutingTable();

            // }
            if (this.lsc == true) {
                updateOpticalRoutingTable();
                //  isInitial = false;
            }
            isInitial = false;

        }
        sendKeepAlivePackets();
        for (int i = 0; i < this.nics.size(); i++)
            this.nics.get(i).sendPackets();
    }

    /**
     * Makes each nic move all of its cells from the input buffer to the output buffer
     *
     * @since 1.0
     */
    public void receivePackets() {
        for (int i = 0; i < this.nics.size(); i++)
            this.nics.get(i).receivePackets();
    }

    public void sendKeepAlive(int dest, OpticalLabel label) {
        Packet p = new Packet(this.getAddress(), dest, label);
        p.setOAM(true, "KeepAlive");
        this.sendPacket(p);
    }

    private void updateRoutingTable() {
        dj.DijkstraAlgorithm(this.address);
        routingTable = dj.calRoutingTable(this.address);

    }

    private void updateOpticalRoutingTable() {
        dj.DijkstraOpticalAlgorithm(this.address);
        routingOpticalTable = dj.calRoutingOpticalTable(this.address);

    }

    private LSRNIC getRoutingNic(int des) {
        for (LSRNIC nic : nics) {
            if (routingTable.get(des) != null && nic.getLinkDestAddr() == routingTable.get(des)) {
                return nic;
            }
        }
        return null;
    }

    private LSRNIC getOpticalRoutingNic(int des) {
        for (LSRNIC nic : nics) {
            if (routingOpticalTable.get(des) != null && nic.getLinkDestAddr() == routingOpticalTable.get(des)) {
                return nic;
            }
        }
        return null;
    }

    public void setupLSP(Packet newPacket) {
        //  allocateBandwidth(newPacket.getDest(), GlobalVariables.PHB_BE, 0, 0);
        int i;
        for (i = 1; i <= LabeltoLabel.size(); i++) {
            if (!LabeltoLabel.containsKey(i)) {
                break;
            }
        }
        Packet setupPacket = new Packet(newPacket.getSource(), newPacket.getDest(), i);
        sentPath(setupPacket);
        setupPacket.setOAM(true, "Path");
        setupPacket.setDSCP(newPacket.getDSCP());
        getRoutingNic(newPacket.getDest()).sendPacket(setupPacket, this);
        //  LabeltoLabel.put(i, new NICLabelPair(getRoutingNic(newPacket.getDest()),-1));
        isEstabLSP.add(getHashcode(newPacket.getDest(), newPacket.getDSCP()));

    }

    public int getHashcode(int dest, int dscp) {
        return dest * 100 + dscp;
    }

    public int getSDHashcode(int source, int dest) {
        return source * 100 + dest;
    }

    public int nextPSR(int address) {
        LSR nextlsr = getRoutingNic(address).getLinkDestLSR();
        while (nextlsr.psc != true) {
            nextlsr = nextlsr.getRoutingNic(address).getLinkDestLSR();
        }
        return nextlsr.getAddress();
    }

    public void sentPacket(Packet packet, int address) {
        //  if (this.trace)
        System.out.println("DATA: Router " + this.address + " sent a DATA to Router " + address);
    }

    /**
     * Outputs to the console that a connect message has been sent
     *
     * @since 1.0
     */
    private void sentPath(Packet packet) {
        //if (this.trace)
        System.out.println("PATH: Router " + this.address + " sent a PATH to Router " + routingTable.get(packet.getDest()));
    }

    /**
     * Outputs to the console that a setup message has been sent
     *
     * @since 1.0
     */
    private void receivedPath(Packet packet) {
        //    if (this.trace)
        System.out.println("PATH: Router " + this.address + "  received a PATH from Router " + routingTable.get(packet.getSource()));
    }

    /**
     * Outputs to the console that a connect message has been sent
     *
     * @since 1.0
     */
    private void sentResv(Packet packet) {
        //   if (this.trace)
        System.out.println("RESV: Router " + this.address + " sent a RESV to Router " + routingTable.get(packet.getDest()));
    }

    /**
     * Outputs to the console that a connect message has been sent
     *
     * @since 1.0
     */
    private void receivedResv(Packet packet) {
        // if (this.trace)
        System.out.println("RESV: Router " + this.address + "  received a RESV from Router " + routingTable.get(packet.getSource()));
    }

    /**
     * Outputs to the console that a connect message has been received
     *
     * @since 1.0
     */
    private void sentResvConf(Packet packet) {
        //  if (this.trace)
        System.out.println("RESVCONF: Router " + this.address + " sent a RESVCONF to Router " + routingTable.get(packet.getDest()));
    }


    /**
     * Outputs to the console that a connect ack message has been received
     *
     * @since 1.0
     */
    private void receivedResvConf(Packet packet) {
        // if (this.trace)
        System.out.println("RESVCONF: Router " + this.address + "  received a RESVCONF from Router " + routingTable.get(packet.getSource()));
    }

    /**
     * Outputs to the console that a wait message has been sent
     *
     * @since 1.0
     */
    private void sentPathErr(Packet packet) {
        // if (this.trace)
        System.out.println("PATHERR: Router " + this.address + " sent a PATHERR to Router " + routingTable.get(packet.getDest()));
    }

    /**
     * Outputs to the console that a wait message has been received
     *
     * @since 1.0
     */
    private void receivedPathErr(Packet packet) {
        //if (this.trace)
        System.out.println("PATHERR: Router " + this.address + "  received a PATHERR from Router " + routingTable.get(packet.getSource()));
    }

    /**
     * Outputs to the console that a wait message has been sent
     *
     * @since 1.0
     */
    private void sentResvErr(Packet packet) {
        // if (this.trace)
        System.out.println("RESVERR: Router " + this.address + " sent a RESVERR to Router " + routingTable.get(packet.getDest()));
    }

    /**
     * Outputs to the console that a wait message has been received
     *
     * @since 1.0
     */
    private void receivedResvErr(Packet packet) {
        //if (this.trace)
        System.out.println("RESVERR: Router " + this.address + "  received a RESVERR from Router " + routingTable.get(packet.getSource()));
    }
}
