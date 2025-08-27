package com.osmb.script.woodcutting.powerchopburn.data;

import com.osmb.api.item.ItemID;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.script.woodcutting.powerchopburn.PixelProvider;

public enum Tree {
    NORMAL("Tree", ItemID.LOGS, PixelProvider.TREE_CLUSTER_1, 1),
    OAK("Oak tree", ItemID.OAK_LOGS, PixelProvider.TREE_CLUSTER_1, 15),
    WILLOW("Willow tree", ItemID.WILLOW_LOGS, PixelProvider.TREE_CLUSTER_2, 30);

    private final int logID;
    private final SearchablePixel[] cluster;
    private final String objectName;
    private final int requiredLevel;

    Tree(String objectName, int logID, SearchablePixel[] cluster, int requiredLevel) {
        this.objectName = objectName;
        this.logID = logID;
        this.cluster = cluster;
        this.requiredLevel = requiredLevel;
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
