package com.osmb.script.crafting.chartercrafting;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.script.crafting.chartercrafting.handles.BankHandler;
import com.osmb.script.crafting.chartercrafting.handles.CraftHandler;
import com.osmb.script.crafting.chartercrafting.handles.FurnaceHandler;
import com.osmb.script.crafting.chartercrafting.handles.ShopHandler;
import com.osmb.script.crafting.chartercrafting.javafx.UI;

import java.util.Set;

import static com.osmb.script.crafting.chartercrafting.Config.*;
import static com.osmb.script.crafting.chartercrafting.Constants.ITEM_IDS_TO_RECOGNISE;
import static com.osmb.script.crafting.chartercrafting.State.*;

@ScriptDefinition(name = "Charter crafter", author = "Joe", version = 1.0, description = "", skillCategory = SkillCategory.CRAFTING)
public class CharterCrafting extends Script {

    private ShopHandler shopHandler;
    private FurnaceHandler furnaceHandler;
    private BankHandler bankHandler;
    private CraftHandler craftHandler;

    public CharterCrafting(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        this.shopHandler = new ShopHandler(this);
        this.furnaceHandler = new FurnaceHandler(this);
        this.bankHandler = new BankHandler(this);
        this.craftHandler = new CraftHandler(this);

        UI ui = UI.show(this);
        selectedDock = ui.getSelectedDock();
        // workaround as highlights aren't working for charter crew members
        npcs = NPC.getNpcsForDock(selectedDock);
        selectedMethod = ui.getSelectedMethod();
        selectedGlassBlowingItem = ui.getSelectedGlassBlowingItem();

        ITEM_IDS_TO_RECOGNISE.add(selectedGlassBlowingItem.getItemId());
        combinationItemID = selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH;
    }


    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            bankHandler.handleInterface();
        } else if (shopHandler.interfaceVisible()) {
            shopHandler.handleInterface();
        } else {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                // inventory not visible - re-poll
                log(CharterCrafting.class, "Unable to snapshot inventory...");
                return 0;
            }

            if (selectedMethod != Method.BUY_AND_BANK) {
                if (craftAmount == -1) {
                    nextCraftAmount(inventorySnapshot);
                }
                if (!inventorySnapshot.contains(ItemID.GLASSBLOWING_PIPE)) {
                    log(CharterCrafting.class, "No glassblowing pipe found.");
                    stop();
                    return 0;
                }
                if (inventorySnapshot.contains(ItemID.MOLTEN_GLASS)) {
                    log(CharterCrafting.class, "Crafting molten glass...");
                    craftHandler.craft(inventorySnapshot.getItem(ItemID.GLASSBLOWING_PIPE), inventorySnapshot.getRandomItem(ItemID.MOLTEN_GLASS));
                    return 0;
                }
            }

            if (shouldOpenShop(inventorySnapshot)) {
                int itemIDToDrop = getItemIDToDrop(inventorySnapshot);
                if (itemIDToDrop != -1) {
                    log(CharterCrafting.class, "Dropping excess item: " + itemIDToDrop);
                    getWidgetManager().getInventory().dropItems(itemIDToDrop);
                    return 0;
                }

                if (hopFlag) {
                    log(CharterCrafting.class, "Hop flag is set, hopping worlds...");
                    hopWorlds();
                } else {
                    log(CharterCrafting.class, "Need to open shop interface...");
                    shopHandler.open();
                }
                return 0;
            }

            log(CharterCrafting.class, "Handling selected method: " + selectedMethod);
            switch (selectedMethod) {
                case SUPER_GLASS_MAKE -> craftHandler.superGlassMake();
                case BUY_AND_BANK -> bankHandler.open();
                case BUY_AND_FURNACE_CRAFT -> furnaceHandler.poll();
            }
        }
        return 0;
    }

    private int getItemIDToDrop(ItemGroupResult inventorySnapshot) {
        // check for excess items, drop instead of selling to avoid issues
        int freeSlotsExclBuyItems = inventorySnapshot.getFreeSlots(Set.of(ItemID.BUCKET_OF_SAND, combinationItemID));
        int moltenGlassToMake = freeSlotsExclBuyItems / 2;
        if (inventorySnapshot.getAmount(ItemID.BUCKET_OF_SAND) > moltenGlassToMake) {
            return ItemID.BUCKET_OF_SAND;
        } else if (inventorySnapshot.getAmount(combinationItemID) > moltenGlassToMake) {
            return combinationItemID;
        }
        return -1;
    }

    private boolean shouldOpenShop(ItemGroupResult inventorySnapshot) {
        int bucketsOfSand = inventorySnapshot.getAmount(ItemID.BUCKET_OF_SAND);
        int combinationItems = inventorySnapshot.getAmount(combinationItemID);
        return switch (selectedMethod) {
            case BUY_AND_BANK, BUY_AND_FURNACE_CRAFT -> inventorySnapshot.getFreeSlots() >= 2;
            case SUPER_GLASS_MAKE -> bucketsOfSand < craftAmount || combinationItems < craftAmount;
        };
    }

    private void hopWorlds() {
        if (!getProfileManager().hasHopProfile()) {
            log(CharterCrafting.class, "No hop profile set, please make sure to select a hop profile when running this script.");
            stop();
            return;
        }
        getProfileManager().forceHop();
        hopFlag = false;
    }

    @Override
    public boolean canHopWorlds() {
        // only hop when we force
        return false;
    }

    @Override
    public int[] regionsToPrioritise() {
        log(CharterCrafting.class, "Prioritised region:" + selectedDock.getRegionID());
        return new int[]{selectedDock.getRegionID()};
    }

}



