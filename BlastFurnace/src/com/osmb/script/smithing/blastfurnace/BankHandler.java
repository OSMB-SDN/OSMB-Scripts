package com.osmb.script.smithing.blastfurnace;

import com.osmb.api.ScriptCore;
import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.scene.RSObject;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.bank.Bank;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.script.smithing.blastfurnace.component.Overlay;
import com.osmb.script.smithing.blastfurnace.data.Bar;
import com.osmb.script.smithing.blastfurnace.data.Ore;
import com.osmb.script.smithing.blastfurnace.utility.Utils;

import java.util.*;

import static com.osmb.script.smithing.blastfurnace.BlastFurnace.waitForConveyorBeltInteraction;
import static com.osmb.script.smithing.blastfurnace.Constants.*;
import static com.osmb.script.smithing.blastfurnace.Options.*;
import static com.osmb.script.smithing.blastfurnace.Status.*;

public class BankHandler {


    private final ScriptCore core;
    private final Bank bank;

    public BankHandler(ScriptCore core) {
        this.core = core;
        this.bank = core.getWidgetManager().getBank();
    }

    public void handleBank(Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        ItemGroupResult inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        ItemGroupResult bankSnapshot = bank.search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null || bankSnapshot == null) {
            core.log(BlastFurnace.class, "Unable to snapshot inventory or bank");
            return;
        }
        // check if we need to pay the foreman
        if (payForeman && FOREMAN_PAYMENT_TIMER.hasFinished()) {
            // handle withdrawing coins
            handleCoinWithdrawl(inventorySnapshot);
            return;
        }
        // check if we need to drink a stamina potion
        if (drinkStaminas) {
            Integer runEnergy = core.getWidgetManager().getMinimapOrbs().getRunEnergy();
            Boolean hasStaminaEffect = core.getWidgetManager().getMinimapOrbs().hasStaminaEffect();
            if (runEnergy == null || hasStaminaEffect == null) {
                core.log(BlastFurnace.class, "Can't read run energy...");
                return;
            }
            if (needToDrinkStamina(runEnergy, hasStaminaEffect)) {
                // need to drink a stamina potion, handle withdrawing & drinking
                handleStamina(runEnergy, hasStaminaEffect, inventorySnapshot, bankSnapshot);
                return;
            }
        }
        // check if we need to collect bars from dispenser
        if (Utils.hasBarsToCollect(blastFurnaceInfo)) {
            core.log(BlastFurnace.class, "Need to collect bars from dispenser, depositing unwanted items...");
            // deposit items we don't need to make space for bars
            if (!bank.depositAll(ITEMS_TO_NOT_DEPOSIT)) {
                return;
            }
            // if we reach here, it means all unwanted items were deposited
            bank.close();
            return;
        }

        core.log(BlastFurnace.class, "Getting bank entries...");
        List<BankEntry> bankEntries = getBankEntries(inventorySnapshot, blastFurnaceInfo);

        hasCoalBag = inventorySnapshot.getItem(ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG) != null;
        // Randomly decide whether to fill coal bag first or process bank entries first
        // this will sometimes activate before withdrawing ores, but will ALWAYS activate if we're finished withdrawing the ores we need
        boolean handleCoalBag = RandomUtils.uniformRandom(0, 2) == 0 || bankEntries.isEmpty();
        if (hasCoalBag && !coalBagFull && handleCoalBag) {
            core.log(BlastFurnace.class, "Handling coal bag...");
            fillCoalBag(inventorySnapshot);
            return;
        }

        // lower chance of equipping gloves as we want to sometimes equip them on the way to the conveyor belt
        boolean equipGloves = RandomUtils.uniformRandom(0, 5) == 0;
        ItemSearchResult gloves = inventorySnapshot.getItem(ItemID.GOLDSMITH_GAUNTLETS);
        if (selectedBar == Bar.GOLD && gloves != null && equipGloves) {
            if (BlastFurnace.equipGoldsmithGuantlets(core, gloves, true)) {
                return;
            }
        }

        if (bankEntries.isEmpty()) {
            // nothing to deposit/withdraw, banking complete
            int random = RandomUtils.gaussianRandom(0, 4, 2, 0.9);
            if (random != 0) {
                RSObject conveyorBelt = Utils.getConveyorBelt(core);
                if (conveyorBelt == null) {
                    core.log(BlastFurnace.class, "Unable to find conveyor belt... Please start the script in the Blast Furnace minigame area.");
                    bank.close();
                    return;
                }
                // This is to interact with the conveyor belt if it's visible on screen while the bank is open
                Polygon conveyorBeltPoly = getConveyorBeltPolygon(conveyorBelt, core);
                if (conveyorBeltPoly != null) {
                    // interact with conveyor belt instead of closing bank and interacting
                    if (core.getFinger().tap(conveyorBeltPoly, "put-ore-on")) {
                        GOLD_GLOVE_EQUIP_DELAY.reset(RandomUtils.uniformRandom(800, 2000));
                        waitForConveyorBeltInteraction(core, inventorySnapshot, conveyorBelt, blastFurnaceInfo);
                        return;
                    }
                }
                // if the poly is null, it would indicate that its not visible on screen, so we just resort to closing the bank instead
            }
            bank.close();
            return;
        }

        Set<Integer> itemsToIgnore = new HashSet<>(ITEMS_TO_NOT_DEPOSIT);
        for (BankEntry bankEntry : bankEntries) {
            core.log(BlastFurnace.class, "Bank entry: " + bankEntry);
            itemsToIgnore.add(bankEntry.getItemID());
        }
        for(Resource resource : selectedBar.getOres()) {
            itemsToIgnore.add(resource.getItemID());
        }
        if (!inventorySnapshot.getOccupiedSlots(itemsToIgnore).isEmpty()) {
            core.log(BlastFurnace.class, "We have items to deposit");
            // deposit items we don't need
            bank.depositAll(itemsToIgnore);
            return;
        }

        // handle bank entries

        // randomise the order of bank entries to avoid patterns in withdrawals/deposits
        Collections.shuffle(bankEntries);

        BankEntry bankEntry = bankEntries.get(RandomUtils.uniformRandom(bankEntries.size()));
        core.log(BlastFurnace.class, "Processing bank entry: " + bankEntry);
        if (bankEntry.getAmount() > 0) {
            // withdraw
            bank.withdraw(bankEntry.itemID, bankEntry.amount);
        } else {
            // deposit
            bank.deposit(bankEntry.itemID, bankEntry.amount);
        }
    }


    private List<BankEntry> getBankEntries(ItemGroupResult inventorySnapshot, Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        List<BankEntry> bankEntries = new ArrayList<>();
        Set<Integer> itemsToIgnore = new HashSet<>(ORE_IDS);
        itemsToIgnore.addAll(BAR_IDS);

        Status.hasCoalBag = inventorySnapshot.getItem(ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG) != null;
        int oreCapacity = inventorySnapshot.getFreeSlots(itemsToIgnore);
        core.log(BlastFurnace.class, "Free slots excluding ores: " + oreCapacity);
        int coalGoal = selectedBar.getCoalAmount() * oreCapacity;
        int coalDeposited = blastFurnaceInfo.getOreAmount(Ore.COAL);
        core.log(BlastFurnace.class, "Coal goal: " + coalGoal + ", Coal deposited: " + coalDeposited + ", Ore capacity: " + oreCapacity);
        if (coalGoal == 0 || coalDeposited >= coalGoal) {
            // check if we have enough primary ores
            List<Resource> resourceList = selectedBar.getOres();
            core.log("Resource list: " + resourceList.size() + " Ore capacity: " + oreCapacity);
            int amount = oreCapacity / resourceList.size();
            for (Resource resource : resourceList) {
                int resourceAmount = inventorySnapshot.getAmount(resource.getItemID());
                int amountNeeded = amount - resourceAmount;
                if (resourceAmount < amount) {
                    core.log(BlastFurnace.class, "Not enough " + resource.getItemID() + " in inventory. Required: " + amount + ", Found: " + resourceAmount + ", " + amountNeeded + " needed");
                    bankEntries.add(new BankEntry(resource.getItemID(), amountNeeded));
                } else if (resourceList.size() > 1 && resourceAmount > amount) {
                    core.log(BlastFurnace.class, "Too much " + resource.getItemID() + " in inventory. Required: " + amount + ", Found: " + resourceAmount + ", " + amountNeeded + " needed");
                    bankEntries.add(new BankEntry(resource.getItemID(), amountNeeded));
                }
            }
        } else {
            // fill inventory with coal
            int coalNeeded = oreCapacity - inventorySnapshot.getAmount(ItemID.COAL);
            if (coalNeeded != 0) {
                bankEntries.add(new BankEntry(ItemID.COAL, coalNeeded));
            }
        }
        return bankEntries;
    }

    private Polygon getConveyorBeltPolygon(RSObject conveyorBelt, ScriptCore core) {
        // get the convex hull of the object
        Polygon conveyorBeltPoly = conveyorBelt.getConvexHull();
        // 1. check the poly is not null (we can see it on screen)
        // 2. resize the poly to make tapping more accurate while moving
        // 3. check the poly is not obstructed by any UI elements > 20% visibility on the 3d screen (we ignore chatbox as you can tap through it)
        if (conveyorBeltPoly != null && (conveyorBeltPoly = conveyorBeltPoly.getResized(0.9)) != null && core.getWidgetManager().insideGameScreenFactor(conveyorBeltPoly, List.of(ChatboxComponent.class)) > 0.2) {
            return conveyorBeltPoly;
        }
        return null;
    }

    private void fillCoalBag(ItemGroupResult inventorySnapshot) {
        ItemSearchResult coalBag = inventorySnapshot.getItem(ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG);
        if (coalBag == null) {
            core.log(BlastFurnace.class, "Unable to find coal bag in snapshot");
            return;
        }
        MenuHook coalBagHook = menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                if (entry.getRawText().startsWith("fill")) {
                    return entry;
                } else if (entry.getRawText().startsWith("empty")) {
                    // we set the flag to true if we find an empty option
                    core.log(BlastFurnace.class, "Coal bag is already full.");
                    coalBagFull = true;
                    return null;
                }
            }
            return null;
        };
        if (core.getFinger().tap(true, coalBag, coalBagHook)) {
            coalBagFull = true;
        }
    }

    private boolean needToDrinkStamina(Integer runEnergy, Boolean hasStaminaEffect) {
        if (runEnergy == null || hasStaminaEffect == null) {
            core.log(BlastFurnace.class, "Can't read run energy...");
            return false;
        }
        return runEnergy < 80 && !hasStaminaEffect;
    }

    private void handleStamina(Integer runEnergy, Boolean hasStaminaEffect, ItemGroupResult inventorySnapshot, ItemGroupResult bankSnapshot) {
        core.log(BlastFurnace.class, "Need to drink stamina potion, run energy: " + runEnergy + " has stamina effect: " + hasStaminaEffect);
        // need to drink a stamina potion
        ItemSearchResult staminaPotion = inventorySnapshot.getRandomItem(STAMINA_POTION_IDS);
        if (staminaPotion == null) {
            if (inventorySnapshot.isFull()) {
                // deposit some items to make space
                if (!bank.depositAll(ITEMS_TO_NOT_DEPOSIT)) {
                    core.log(BlastFurnace.class, "Failed to deposit items..");
                }
                return;
            }
            // withdraw stamina potion from bank
            if (!bankSnapshot.containsAny(STAMINA_POTION_IDS)) {
                core.log(BlastFurnace.class, "No stamina potions in bank! Stopping script...");
                core.stop();
                return;
            } else {
                // get the lowest dose visible
                int lowestIndex = Integer.MAX_VALUE;
                for (int i = 0; i < STAMINA_POTION_IDS.size(); i++) {
                    int stamina = STAMINA_POTION_IDS.get(i);
                    if (!bankSnapshot.contains(stamina)) {
                        continue;
                    }
                    if (i < lowestIndex) {
                        lowestIndex = i;
                    }
                }
                bank.withdraw(STAMINA_POTION_IDS.get(lowestIndex), 1);
            }
        } else {
            // drink potion if in the inventory
            if (staminaPotion.interact("drink")) {
                // wait until we have the stamina effect
                core.pollFramesHuman(() -> {
                    Boolean hasStaminaEffect_ = core.getWidgetManager().getMinimapOrbs().hasStaminaEffect();
                    if (hasStaminaEffect_ == null) {
                        core.log(BlastFurnace.class, "Can't read stamina effect...");
                        return false;
                    }
                    return hasStaminaEffect_;
                }, RandomUtils.uniformRandom(3000, 5000));
            }
        }
    }

    private void handleCoinWithdrawl(ItemGroupResult inventorySnapshot) {
        core.log(BlastFurnace.class, "Need to pay foreman.");
        // deposit items we don't need
        if (inventorySnapshot.isFull()) {
            core.log(BlastFurnace.class, "Making space in inventory for coins...");
            Set<Integer> itemsToIgnore = new HashSet<>(ITEMS_TO_NOT_DEPOSIT);
            itemsToIgnore.add(ItemID.COINS_995);
            if (!bank.depositAll(itemsToIgnore)) {
                core.log(BlastFurnace.class, "Failed to deposit items..");
            }
            return;
        }
        // pay the foreman if due
        int coins = inventorySnapshot.getAmount(ItemID.COINS_995);
        core.log(BlastFurnace.class, "Coins in inventory: " + coins);
        if (coins <= 2500) {
            core.log(BlastFurnace.class, "Open bank to withdraw coins for foreman payment.");
            if (!bank.depositAll(ITEMS_TO_NOT_DEPOSIT)) {
                core.log(BlastFurnace.class, "Failed to deposit items..");
                return;
            }
            if (!bank.withdraw(ItemID.COINS_995, Integer.MAX_VALUE)) {
                core.log(BlastFurnace.class, "Failed to withdraw coins for foreman payment.");
            }
        }
        bank.close();
        return;
    }


    static class BankEntry {
        private final int itemID;
        private final int amount;

        public BankEntry(int itemID, int amount) {
            this.itemID = itemID;
            this.amount = amount;
        }

        public int getItemID() {
            return itemID;
        }

        public int getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return "BankEntry{" +
                    "itemID=" + itemID +
                    ", amount=" + amount +
                    '}';
        }
    }
}
