package de.simplepvparena.border;

public enum BorderConnectivity {

    ISOLATED,
    NORMAL,
    WELL_CONNECTED;

    public static final int WELL_CONNECTED_NEIGHBOUR_COUNT = 2;

    public static BorderConnectivity ofConnectedNeighbourCount(int connectedNeighbourCount) {
        if (connectedNeighbourCount < 0) {
            throw new IllegalArgumentException("connectedNeighbourCount must not be negative");
        }
        if (connectedNeighbourCount == 0) {
            return ISOLATED;
        }
        return connectedNeighbourCount >= WELL_CONNECTED_NEIGHBOUR_COUNT ? WELL_CONNECTED : NORMAL;
    }
}
