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
import static com.osmb.script.crafting.chartercrafting.utils.Utilities.getExcessItemsToDrop;

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
        if (hasNoHopProfile()) {
            log(CharterCrafting.class, "No hop profile selected, make sure to select one before running the script.");
            stop();
        }
        decideTask();
        return 0;
    }



    private boolean shouldOpenShop(ItemGroupResult inventorySnapshot) {
        int bucketsOfSand = inventorySnapshot.getAmount(ItemID.BUCKET_OF_SAND);
        int combinationItems = inventorySnapshot.getAmount(combinationItemID);
        return switch (selectedMethod) {
            case BUY_AND_BANK -> inventorySnapshot.getFreeSlots() >= 2;
            case BUY_AND_FURNACE_CRAFT ->
                    inventorySnapshot.getFreeSlots() >= 2 && !smelt || (bucketsOfSand == 0 || combinationItems == 0);
            case SUPER_GLASS_MAKE -> bucketsOfSand < craftAmount || combinationItems < craftAmount;
        };
    }

    private void hopWorlds() {
        if (hasNoHopProfile()) {
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

    private boolean canCraft(ItemGroupResult inventorySnapshot) {
        return inventorySnapshot.contains(ItemID.MOLTEN_GLASS) && (selectedMethod == Method.BUY_AND_FURNACE_CRAFT && !smelt || selectedMethod == Method.SUPER_GLASS_MAKE &&  !inventorySnapshot.containsAll(Set.of(ItemID.BUCKET_OF_SAND, combinationItemID)));
    }

    private void decideTask() {
        if (hasNoHopProfile()) return;
        if (getWidgetManager().getBank().isVisible()) {
            bankHandler.handleInterface();
            return;
        } else if (shopHandler.interfaceVisible()) {
            shopHandler.handleInterface();
            return;
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            // inventory not visible - re-poll
            log(CharterCrafting.class, "Unable to snapshot inventory...");
            return;
        }

        if (selectedMethod != Method.BUY_AND_BANK) {
            if (smelt) {
                if (!inventorySnapshot.contains(ItemID.BUCKET_OF_SAND) || !inventorySnapshot.contains(combinationItemID)) {
                    smelt = false;
                } else {
                    furnaceHandler.poll();
                }
                return;
            }
            if (craftAmount == -1) {
                // reset craft amount if we don't have one
                nextCraftAmount(inventorySnapshot);
            }
            if (!inventorySnapshot.contains(ItemID.GLASSBLOWING_PIPE)) {
                log(CharterCrafting.class, "No glassblowing pipe found.");
                stop();
                return;
            }
            if (canCraft(inventorySnapshot)) {
                log(CharterCrafting.class, "Crafting molten glass...");
                craftHandler.craft(inventorySnapshot.getItem(ItemID.GLASSBLOWING_PIPE), inventorySnapshot.getRandomItem(ItemID.MOLTEN_GLASS));
                return;
            }
        }

        if (shouldOpenShop(inventorySnapshot)) {
            DropResult itemToDrop = getExcessItemsToDrop(inventorySnapshot);
            if (itemToDrop != null) {
                log(CharterCrafting.class, "Dropping excess item: " + itemToDrop);
                getWidgetManager().getInventory().dropItem(itemToDrop.itemId, itemToDrop.amount);
                return;
            }

            if (hopFlag) {
                log(CharterCrafting.class, "Hop flag is set, hopping worlds...");
                hopWorlds();
            } else {
                log(CharterCrafting.class, "Need to open shop interface...");
                shopHandler.open();
            }
            return;
        }

        log(CharterCrafting.class, "Handling selected method: " + selectedMethod);
        switch (selectedMethod) {
            case SUPER_GLASS_MAKE -> craftHandler.superGlassMake();
            case BUY_AND_BANK -> bankHandler.open();
            case BUY_AND_FURNACE_CRAFT -> furnaceHandler.poll();
        }
    }

    private boolean hasNoHopProfile() {
        if (!getProfileManager().hasHopProfile()) {
            log(CharterCrafting.class, "No hop profile set, please make sure to select a hop profile when running this script.");
            stop();
            return true;
        }
        return false;
    }

    public static class DropResult {
        public final int amount;
        public final int itemId;

        public DropResult(int itemId, int amount) {
            this.amount = amount;
            this.itemId = itemId;
        }
    }

    enum Task {
        OPEN_SHOP,
        BUY_ITEMS,
        OPEN_BANK,
        BANK_ITEMS,
        HANDLE_BANK_INTERFACE,
        HANDLE_SHOP_INTERFACE,
        WALK_TO_FURNACE,
        SMELT,
        CRAFT
    }

}





