package DataTypes;

import Dijkstra.Node;

import java.util.HashMap;

/**
 * Created by gs on 11/16/14.
 */
public class GlobalVariables {

    public static HashMap<Integer, Node> graph = new HashMap<Integer, Node>();
    public static HashMap<Integer, Node> opticalGraph = new HashMap<Integer, Node>();
    public static HashMap<OpticalLabel, Boolean> opticalLabelUsed = new HashMap<OpticalLabel, Boolean>() {{
        put(OpticalLabel.red, false);
        put(OpticalLabel.blue, false);
        put(OpticalLabel.yellow, false);
        put(OpticalLabel.green, false);
    }};
}
