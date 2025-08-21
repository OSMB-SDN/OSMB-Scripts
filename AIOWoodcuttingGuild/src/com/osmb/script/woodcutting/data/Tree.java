package com.osmb.script.woodcutting.data;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.visual.SearchablePixel;


public enum Tree {
    NORMAL("Tree", ItemID.LOGS, PixelProvider.TREE_CLUSTER_1, AreaManager.TREE_AREA, 1),
    OAK("Oak tree", ItemID.OAK_LOGS, PixelProvider.TREE_CLUSTER_1, AreaManager.OAK_TREE_AREA, 15),
    WILLOW("Willow tree", ItemID.WILLOW_LOGS, PixelProvider.TREE_CLUSTER_1, AreaManager.OAK_TREE_AREA, 30),
    MAPLE("Maple tree", ItemID.MAPLE_LOGS, PixelProvider.TREE_CLUSTER_2, AreaManager.WILLOW_TREE_AREA, 45),
    YEW("Yew tree", ItemID.YEW_LOGS, PixelProvider.TREE_CLUSTER_1, AreaManager.YEW_TREE_AREA, 60),
    //MAGIC("Magic tree", ItemID.MAGIC_LOGS, PixelProvider.TREE_CLUSTER_1,AreaManager.MAPLE_TREE_AREA, 75),
    REDWOOD("Redwood tree", ItemID.REDWOOD_LOGS, null, AreaManager.REDWOOD_TREE_AREA, 90);

    private final int logID;
    private final SearchablePixel[] cluster;
    private final String objectName;
    private final int requiredLevel;
    private final Area treeArea;

    Tree(String objectName, int logID, SearchablePixel[] cluster, Area treeArea, int requiredLevel) {
        this.objectName = objectName;
        this.logID = logID;
        this.cluster = cluster;
        this.requiredLevel = requiredLevel;
        this.treeArea = treeArea;
    }

    public static Tree getTreeForLevel(int level) {
        Tree[] trees = Tree.values();
        for (int i = trees.length - 1; i >= 0; i--) {
            if (trees[i].getRequiredLevel() <= level) {
                return trees[i];
            }
        }
        return null; // No tree found for the given level
    }

    public Area getTreeArea() {
        return treeArea;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getLogID() {
        return logID;
    }

    public String getObjectName() {
        return objectName;
    }

    public SearchablePixel[] getCluster() {
        return cluster;
    }

}
