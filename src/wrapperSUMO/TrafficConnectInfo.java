package wrapperSUMO;

public class TrafficConnectInfo
{
    private String fromEdge;
    private String toEdge;
    private String direction;
    private int linkIndex;
    private String fromLane;
    private String toLane;

    public TrafficConnectInfo(String fromEdge, String toEdge, String direction,
                          int linkIndex, String fromLane, String toLane) {
        this.fromEdge = fromEdge;
        this.toEdge = toEdge;
        this.direction = direction;
        this.linkIndex = linkIndex;
        this.fromLane = fromLane;
        this.toLane = toLane;
    }

    public String getFromEdge() { return fromEdge; }
    public String getToEdge() { return toEdge; }
    public String getDirection() { return direction; }
    public int getLinkIndex() { return linkIndex; }
    public String getFromLane() { return fromLane; }
    public String getTolane() { return toLane; }
}

