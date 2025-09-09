package com.osmb.script.crafting.chartercrafting.handles;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSTile;
import com.osmb.api.shape.Polygon;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.crafting.chartercrafting.CharterCrafting;
import com.osmb.script.crafting.chartercrafting.NPC;
import com.osmb.script.crafting.chartercrafting.component.ShopInterface;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.osmb.script.crafting.chartercrafting.Config.*;
import static com.osmb.script.crafting.chartercrafting.Constants.ITEM_IDS_TO_RECOGNISE;
import static com.osmb.script.crafting.chartercrafting.State.*;
import static com.osmb.script.crafting.chartercrafting.utils.Utilities.roundDownToNearestOption;

public class ShopHandler {
    private final ScriptCore core;
    private final ShopInterface shopInterface;

    public ShopHandler(ScriptCore core) {
        this.core = core;
        shopInterface = new ShopInterface(core);
        // inventory will be seen as visible when shop interface is visible
        core.getWidgetManager().getInventory().registerInventoryComponent(shopInterface);
    }

    public boolean interfaceVisible() {
        return shopInterface.isVisible();
    }

    public void handleInterface() {
        core.log(ShopHandler.class, "Handling shop interface.");
        // sell crafted items
        ItemGroupResult shopSnapshot = shopInterface.search(ITEM_IDS_TO_RECOGNISE);
        inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (shopSnapshot == null || inventorySnapshot == null) {
            return;
        }
        if (inventorySnapshot.contains(selectedGlassBlowingItem.getItemId())) {
            core.log(ShopHandler.class, "Selling crafted items");
            sellItems(new TransactionEntry(inventorySnapshot.getRandomItem(selectedGlassBlowingItem.getItemId()), 999), inventorySnapshot.getFreeSlots());
            return;
        }
        if (inventorySnapshot.contains(ItemID.BUCKET)) {
            core.log(ShopHandler.class, "Selling buckets");
            sellItems(new TransactionEntry(inventorySnapshot.getRandomItem(ItemID.BUCKET), 999), inventorySnapshot.getFreeSlots());
            return;
        }

        int bucketOfSandInventory = inventorySnapshot.getAmount(ItemID.BUCKET_OF_SAND);
        int combinationItemInventory = inventorySnapshot.getAmount(combinationItemID);

        int freeSlotsExclBuyItems = inventorySnapshot.getFreeSlots() + combinationItemInventory + bucketOfSandInventory;
        int moltenGlassToMake = freeSlotsExclBuyItems / 2;
        int excessSlots = freeSlotsExclBuyItems - (moltenGlassToMake * 2);

        // calculate amount to buy
        int bucketOfSandToBuy = moltenGlassToMake - bucketOfSandInventory;
        int combinationToBuy = moltenGlassToMake - combinationItemInventory;
        core.log(ShopHandler.class, "Need to buy Bucket of sand: " + bucketOfSandToBuy + " Combination: " + combinationToBuy);

        ItemSearchResult bucketOfSandShop = shopSnapshot.getItem(ItemID.BUCKET_OF_SAND);
        ItemSearchResult combinationItemShop = shopSnapshot.getItem(combinationItemID);
        // cache shop stock
        int bucketOfSandStock = bucketOfSandShop != null ? bucketOfSandShop.getStackAmount() : 0;
        int combinationItemStock = combinationItemShop != null ? combinationItemShop.getStackAmount() : 0;
        core.log(ShopHandler.class, "Bucket of sand stock: " + bucketOfSandStock + " Combination stock: " + combinationItemStock);


        // if the number is negative, that means we have too many in our inventory and need to sell
        if (hasTooMany(bucketOfSandToBuy) || hasTooMany(combinationToBuy)) {
            shopInterface.close();
            return;
        }

        if (bucketOfSandToBuy > 0) {
            bucketOfSandToBuy += excessSlots;
            if (bucketOfSandToBuy >= bucketOfSandStock || bucketOfSandToBuy >= inventorySnapshot.getFreeSlots()) {
                bucketOfSandToBuy = 999;
            }
        }
        if (combinationToBuy > 0) {
            combinationToBuy += excessSlots;
            if (combinationToBuy >= combinationItemStock || combinationToBuy >= inventorySnapshot.getFreeSlots()) {
                combinationToBuy = 999;
            }
        }
        if (bucketOfSandStock == 0) {
            bucketOfSandToBuy = 0;
        }
        if (combinationItemStock == 0) {
            combinationToBuy = 0;
        }

        core.log(ShopHandler.class, "BucketOfSandToBuy: " + bucketOfSandToBuy + " CombinationToBuy: " + combinationToBuy);
        if (bucketOfSandToBuy == 0 && combinationToBuy == 0) {
            // complete
            if (combinationItemStock == 0 || bucketOfSandStock == 0) {
                core.log(ShopHandler.class, "One of our required items is out of stock, setting hop flag to true.");
                hopFlag = true;
            }
            shopInterface.close();
            return;
        }
        // handle buy entries
        List<TransactionEntry> buyEntries = new ArrayList<>();
        if (bucketOfSandToBuy > 0) {
            buyEntries.add(new TransactionEntry(bucketOfSandShop, bucketOfSandToBuy));
        }
        if (combinationToBuy > 0) {
            buyEntries.add(new TransactionEntry(combinationItemShop, combinationToBuy));
        }
        if (buyEntries.isEmpty()) {
            // should never happen
            return;
        }
        TransactionEntry randomEntry = buyEntries.get(RandomUtils.uniformRandom(buyEntries.size()));
        buyItem(randomEntry, inventorySnapshot.getFreeSlots());
    }

    private boolean hasTooMany(int amount) {
        return amount < 0;
    }

    private boolean buyItem(TransactionEntry buyEntry, int initialFreeSlots) {
        core.log(ShopHandler.class, "Buying item - Entry: " + buyEntry);
        int amount = buyEntry.amount;
        boolean all = amount == 999;
        if (all) {
            amount = 50;
        } else {
            amount = roundDownToNearestOption(amount);
        }
        UIResult<Integer> selectedAmount = shopInterface.getSelectedAmount();
        if (selectedAmount.isNotVisible()) {
            return false;
        }
        Integer amountSelected = selectedAmount.get();
        if (amountSelected == null || amountSelected != amount) {
            if (!all || amountSelected == null || amountSelected < amount) {
                if (!shopInterface.setSelectedAmount(amount)) {
                    return false;
                }
            }
        }
        ItemSearchResult item = buyEntry.item;

        if (item.interact()) {
            // wait for inv slots to change
            return core.pollFramesUntil(() -> {
                        inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                        if (inventorySnapshot == null) {
                            return false;
                        }
                        return inventorySnapshot.getFreeSlots() != initialFreeSlots;
                    },
                    5000);
        }
        return false;
    }

    private boolean sellItems(TransactionEntry transactionEntry, int initialFreeSlots) {
        core.log(ShopHandler.class, "Selling item. Entry: " + transactionEntry);
        int amount = transactionEntry.amount;
        boolean all = amount == 999;
        if (all) {
            amount = (RandomUtils.uniformRandom(2) == 1 ? 10 : 50);
        } else {
            amount = roundDownToNearestOption(amount);
        }
        UIResult<Integer> selectedAmount = shopInterface.getSelectedAmount();
        if (selectedAmount.isNotVisible()) {
            return false;
        }
        Integer amountSelected = selectedAmount.get();
        core.log(ShopHandler.class,"Amount selected: " + amountSelected);

        if (amountSelected == null || amountSelected != amount) {
            core.log(ShopHandler.class, "All? " + all);
            if (!all || (amountSelected == null || amountSelected < 10)) {
                if (!shopInterface.setSelectedAmount(amount)) {
                    return false;
                }
            }
        }
        core.log(ShopHandler.class, "Selling items...");
        ItemSearchResult item = transactionEntry.item;
        if (!item.interact()) {
            core.log(ShopHandler.class, "Failed to sell item.");
            return false;
        }
        core.pollFramesUntil(() -> false, 600);
        // wait for inv slots to change
        return core.pollFramesUntil(() -> {
                    inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                    if (inventorySnapshot == null) {
                        return false;
                    }
                    return inventorySnapshot.getFreeSlots() != initialFreeSlots;
                },
                5000);
    }

    public void open() {
        WorldPosition myPosition = core.getWorldPosition();
        if (myPosition == null) {
            core.log(ShopHandler.class, "Position is null!");
            return;
        }

        if (!selectedDock.getWanderArea().contains(myPosition)) {
            // walk to area
            walkToNPCWanderArea();
            return;
        }

        UIResultList<WorldPosition> npcPositionsResult = core.getWidgetManager().getMinimap().getNPCPositions();
        if (npcPositionsResult.isNotFound()) {
            core.log(getClass().getSimpleName(), "No NPC's found nearby...");
            return;
        }
        List<WorldPosition> npcPositions = new ArrayList<>(npcPositionsResult.asList());

        // remove positions which aren't in the wander area
        npcPositions.removeIf(worldPosition -> !selectedDock.getWanderArea().contains(worldPosition));

        // get tile cubes for positions & scan for npc pixels
        List<WorldPosition> validNPCPositions = getValidNPCPositions(npcPositions);
        if (validNPCPositions.isEmpty()) {
            // walk to the furthest if there is none visible on screen
            walkToFurthestNPC(myPosition, npcPositions);
            return;
        }

        // interact - get closest position from valid npc's
        WorldPosition closestPosition = (WorldPosition) Utils.getClosestPosition(myPosition, validNPCPositions.toArray(new WorldPosition[0]));

        // create a cube poly
        Polygon cubePoly = core.getSceneProjector().getTileCube(closestPosition, 130);
        if (cubePoly == null) {
            return;
        }
        //shrink the poly towards the center, this will make it more accurate to the npc - you can check this with the tile picking in the debug tool (scale).
        cubePoly = cubePoly.getResized(0.5);

        // tap inside the poly
        if (!core.getFinger().tap(cubePoly, "trade trader crewmember")) {
            return;
        }
        // wait for shop interface + human reaction time after
        core.pollFramesHuman(shopInterface::isVisible, RandomUtils.uniformRandom(6000, 9000));
    }

    private void walkToFurthestNPC(WorldPosition myPosition, List<WorldPosition> npcPositions) {
        WorldPosition furthestNPCPosition = getFurthestNPC(myPosition, npcPositions);
        if (furthestNPCPosition == null) {
            core.log(ShopHandler.class, "Furthest npc position is null");
            return;
        }
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            // break out when they are on screen, so we're not breaking out when we reach the specific tile... looks a lot more fluent
            RSTile tile = core.getSceneManager().getTile(furthestNPCPosition);
            if (tile == null) {
                return false;
            }
            return tile.isOnGameScreen();
        });
        core.getWalker().walkTo(furthestNPCPosition, walkConfig.build());
    }

    private void walkToNPCWanderArea() {
        core.log(ShopHandler.class, "Walking to npc area...");
        WorldPosition randomPos = selectedDock.getWanderArea().getRandomPosition();
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            WorldPosition myPosition2 = core.getWorldPosition();
            if (myPosition2 == null) {
                core.log(ShopHandler.class, "Position is null!");
                return false;
            }
            return selectedDock.getWanderArea().contains(myPosition2);
        });
        core.getWalker().walkTo(randomPos, walkConfig.build());
    }

    private WorldPosition getFurthestNPC(WorldPosition myPosition, List<WorldPosition> npcPositions) {
        // get furthest npc
        return npcPositions.stream().max(Comparator.comparingDouble(npc -> npc.distanceTo(myPosition))).orElse(null);
    }


    private List<WorldPosition> getValidNPCPositions(List<WorldPosition> npcPositions) {
        List<WorldPosition> validPositions = new ArrayList<>();
        npcPositions.forEach(position -> {
            // check if npc is in wander area
            if (!selectedDock.getWanderArea().contains(position)) {
                return;
            }
            // create a tile cube, we will analyse this for the npc's pixels
            Polygon poly = core.getSceneProjector().getTileCube(position, 150);
            if (poly == null) {
                return;
            }
            // check for highlight pixel
            for (NPC npc : npcs) {
                if (core.getPixelAnalyzer().findPixel(poly, npc.getSearchablePixels()) == null) {
                    continue;
                }
                // add to our separate list if we find the npc's pixels inside the tile cube
                validPositions.add(position);
                core.getScreen().getDrawableCanvas().drawPolygon(poly.getXPoints(), poly.getYPoints(), poly.numVertices(), Color.GREEN.getRGB(), 1);
                break;
            }

        });
        return validPositions;
    }


    public static class TransactionEntry {
        ItemSearchResult item;
        int amount;

        public TransactionEntry(ItemSearchResult item, int amount) {
            this.item = item;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "SellEntry{" +
                    "item=" + item +
                    ", amount=" + amount +
                    '}';
        }
    }

}
