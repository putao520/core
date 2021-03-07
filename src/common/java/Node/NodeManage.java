package common.java.Node;

import common.java.Config.nConfig;
import common.java.MasterService.MasterConnect;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.util.List;

public class NodeManage {
    private static final String nodePath = "/nodes";

    /**
     * 新增节点
     *
     * @return
     */
    public static boolean addNode() {
        return MasterConnect.getMasterClient().createEphemeralNode(nodePath + "/" + nConfig.nodeID, "true") != null;
    }

    /**
     * 检查目标服务器是否有效
     */
    public static boolean checkNode(String ip, int port) {
        return MasterConnect.getMasterClient().checkExists(nodePath + "/" + nConfig.createNodeID(ip, port));
    }

    /**
     * 获得全部有效节点
     */
    public static JSONArray getNodes() {
        List<String> nodes = MasterConnect.getMasterClient().getChildren(nodePath);
        JSONArray rArray = new JSONArray();
        for (String _key : nodes) {
            String[] key = _key.split("_");
            rArray.add(JSONObject.putx("ip", key[0]).puts("port", Integer.valueOf(key[1])));
        }
        return rArray;
    }
}
