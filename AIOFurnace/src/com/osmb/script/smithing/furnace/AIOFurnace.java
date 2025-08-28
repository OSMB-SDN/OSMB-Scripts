package com.osmb.script.smithing.furnace;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.ChannelThresholdComparator;
import com.osmb.script.smithing.furnace.component.CraftingInterface;
import com.osmb.script.smithing.furnace.component.GoldCraftingInterface;
import com.osmb.script.smithing.furnace.component.SilverCraftingInterface;
import com.osmb.script.smithing.furnace.data.Bar;
import com.osmb.script.smithing.furnace.data.Jewellery;
import com.osmb.script.smithing.furnace.data.Misc;
import com.osmb.script.smithing.furnace.javafx.ScriptOptions;
import javafx.scene.Scene;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ScriptDefinition(
        name = "AIO Furnace",
        author = "Joe",
        version = 1.0,
        description = "Smelts and crafts using furnaces.",
        skillCategory = SkillCategory.SMITHING
)
public class AIOFurnace extends Script {
    public static final String[] BANK_NAMES = {"Bank chest", "Bank booth", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    public static final Predicate<RSObject> BANK_QUERY = gameObject -> {
        // if object has no name
        if (gameObject.getName() == null) {
            return false;
        }
        // has no interact options (eg. bank, open etc.)
        if (gameObject.getActions() == null) {
            return false;
        }

        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) {
            return false;
        }

        // if no actions contain bank or open
        if (Arrays.stream(gameObject.getActions()).noneMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
            return false;
        }
        // final check is if the object is reachable
        return gameObject.canReach();
    };
    public static final Set<Integer> ORE_IDS = new HashSet<>(Set.of(ItemID.COAL, ItemID.IRON_ORE, ItemID.GOLD_ORE, ItemID.SILVER_ORE, ItemID.COPPER_ORE, ItemID.TIN_ORE, ItemID.MITHRIL_ORE, ItemID.ADAMANTITE_ORE, ItemID.RUNITE_ORE));
    // we add the other items in the onStart method
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>();
    private CraftingInterface craftingInterface;
    private Product selectedProduct = Jewellery.SAPPHIRE_RING;
    private int amountChangeTimeout;


    public AIOFurnace(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        ScriptOptions scriptOptions = new ScriptOptions(this);
        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Furnace crafter/smelter configuration", false);
        selectedProduct = scriptOptions.getSelectedProduct();

        for (Resource resource : selectedProduct.getResources()) {
            ITEM_IDS_TO_RECOGNISE.add(resource.getItemID());
        }
        ITEM_IDS_TO_RECOGNISE.add(selectedProduct.getItemID());
        List<Integer> mouldIDs = selectedProduct.getMouldIDs();
        if (mouldIDs != null) {
            // if the product requires a mould, add it to the list of items to recognise
            ITEM_IDS_TO_RECOGNISE.addAll(mouldIDs);
        }
        boolean containsSilver = false;
        for (Resource resource : selectedProduct.getResources()) {
            if (resource.getItemID() == ItemID.SILVER_BAR) {
                containsSilver = true;
                break;
            }
        }
        if (containsSilver) {
            // if we're crafting silver jewellery, we need to add coal to the list of items to recognise as we need it for smelting silver bars
            this.craftingInterface = new SilverCraftingInterface(this);
        } else {
            this.craftingInterface = new GoldCraftingInterface(this);
        }
        this.amountChangeTimeout = selectedProduct == Misc.CANNONBALLS ? RandomUtils.uniformRandom(5800, 9000) : RandomUtils.uniformRandom(3500, 6000);

        // tolerance overrides
        for (int itemID : ORE_IDS) {
            getItemManager().overrideDefaultColorModel(itemID, ColorModel.HSL);
            getItemManager().overrideDefaultComparator(itemID, new ChannelThresholdComparator(9, 7, 7));
        }
        for (Bar bar : Bar.values()) {
            getItemManager().overrideDefaultColorModel(bar.getItemID(), ColorModel.HSL);
            getItemManager().overrideDefaultComparator(bar.getItemID(), new ChannelThresholdComparator(9, 7, 7));
        }
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12342};
    }

    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            handleBank();
            return 0;
        }

        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(AIOFurnace.class, "Failed to get inventory snapshot.");
            return 0;
        }

        if (hasSuppliesForProduct(inventorySnapshot)) {
            produce();
        } else {
            openBank();
        }
        return 0;
    }

    private void produce() {
        // handle dialogue
        if (getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION || craftingInterface.isVisible()) {
            handleDialogue();
            return;
        }

        // if no dialogue interact
        RSObject furnace = getObjectManager().getClosestObject("Furnace", "clay forge");
        if (furnace == null) {
            log(AIOFurnace.class, "No furnace found nearby.");
            return;
        }
        if (!furnace.interact("Smelt")) {
            log(AIOFurnace.class, "Failed to interact with furnace.");
        }
        // wait for the dialogue to appear
        pollFramesHuman(() -> getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION || craftingInterface.isVisible(), random(4000, 10000));
    }

    private void handleDialogue() {
        if (selectedProduct instanceof Jewellery) {
            // handle jewellery crafting
            if (!craftingInterface.isVisible()) {
                log(AIOFurnace.class, "Crafting interface is not visible...");
                return;
            }
            // check all quantity is selected
            CraftingInterface.ProductionQuantity selectedQuantity = craftingInterface.getSelectedProductionQuantity();
            if (selectedQuantity == null) {
                log(AIOFurnace.class, "Production quantity is null");
                return;
            }
            // select all if not already selected
            if (selectedQuantity != CraftingInterface.ProductionQuantity.ALL) {
                log(AIOFurnace.class, "Production quantity is " + selectedQuantity + " Selecting 'All' instead.");
                if (!craftingInterface.selectProductionQuantity(CraftingInterface.ProductionQuantity.ALL)) {
                    log(AIOFurnace.class, "Failed to select 'All' production quantity.");
                    return;
                }
            }
            // select the product to craft
            if (!craftingInterface.selectItem(selectedProduct.getItemID())) {
                log(AIOFurnace.class, "Failed to select item in interface for " + selectedProduct.getProductName());
                return;
            }
        } else {
            if (getWidgetManager().getDialogue().getDialogueType() != DialogueType.ITEM_OPTION) {
                log(AIOFurnace.class, " Dialogue type is not ITEM_OPTION, but " + getWidgetManager().getDialogue().getDialogueType());
                return;
            }
            if (!getWidgetManager().getDialogue().selectItem(selectedProduct.getItemID())) {
                log(AIOFurnace.class, "Failed to select item in dialogue for " + selectedProduct.getProductName());
                return;
            }
        }
        // wait until finished producing
        waitUntilFinishedProducing(selectedProduct.getResources());
    }

    public void waitUntilFinishedProducing(List<Resource> resources) {
        AtomicReference<Map<Integer, Integer>> previousAmounts = new AtomicReference<>(new HashMap<>());
        for (Resource resource : resources) {
            previousAmounts.get().put(resource.getItemID(), -1);
        }
        com.osmb.api.utils.timing.Timer amountChangeTimer = new Timer();
        pollFramesUntil(() -> {
            DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
            // look out for level up dialogue etc.
            if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                log(AIOFurnace.class, "Dialogue detected. Continuing...");
                // sleep for a random time so we're not instantly reacting to the dialogue
                // we do this in the task to continue updating the screen
                pollFramesUntil(() -> false, random(1000, 4000));
                // random chance to interact with the dialogue, if not just re-interact with the furnace
                if (random(2) == 0) {
                    getWidgetManager().getDialogue().continueChatDialogue();
                }
                return true;
            }
            // If the amount of items in the inventory hasn't changed in the timeout amount, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                log(AIOFurnace.class, "No item amount change detected in " + amountChangeTimer.timeElapsed() + "ms. Breaking out of the sleep method.");
                amountChangeTimeout = selectedProduct == Misc.CANNONBALLS ? RandomUtils.uniformRandom(5800, 9000) : RandomUtils.uniformRandom(3500, 6000);
                return true;
            }

            ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                // inv not visible
                return false;
            }

            // update item amounts
            for (Resource resource : resources) {
                int amount = inventorySnapshot.getAmount(resource.getItemID());
                if (amount < resource.getAmount()) {
                    return true;
                }
                int previousAmount = previousAmounts.get().get(resource.getItemID());
                if (amount < previousAmount || previousAmount == -1) {
                    previousAmounts.get().put(resource.getItemID(), amount);
                    amountChangeTimer.reset();
                }
            }
            return false;
        }, 60000, false, true);

        // random delay
        int randomDelay = RandomUtils.gaussianRandom(300, 5000, 500, 1500);
        log(AIOFurnace.class, "⏳ - Executing humanised delay: " + randomDelay + "ms");
        pollFramesUntil(() -> false, randomDelay);
    }

    private boolean hasSuppliesForProduct(ItemGroupResult inventorySnapshot) {
        if (selectedProduct == null) {
            throw new IllegalStateException("No product selected for smelting.");
        }
        List<Integer> mouldIDs = selectedProduct.getMouldIDs();
        if (!mouldIDs.isEmpty()) {
            // if the product requires a mould, check if we have it in the inventory
            if (!inventorySnapshot.containsAny(new HashSet<>(mouldIDs))) {
                log(AIOFurnace.class, "No mould found for " + selectedProduct.getProductName() + ". Required mould ID: " + mouldIDs);
                stop();
                return false;
            }
        }
        // loop through all resources required for the product and check if we have enough in the inventory to craft at least one product
        for (Resource resource : selectedProduct.getResources()) {
            if (inventorySnapshot.getAmount(resource.getItemID()) < resource.getAmount()) {
                String itemName = getItemManager().getItemName(resource.getItemID());
                if (itemName == null) {
                    itemName = "Unknown Item name - ID: " + resource.getItemID();
                }
                log(AIOFurnace.class, "Not enough supplies for " + itemName + ". Need at least " + resource.getAmount() + " of " + resource.getItemID() + ", but have " + inventorySnapshot.getAmount(resource.getItemID()));
                return false;
            }
        }
        return true;
    }

    private void openBank() {
        List<RSObject> banksFound = getObjectManager().getObjects(BANK_QUERY);

        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }

        RSObject object = (RSObject) getUtils().getClosest(banksFound);

        if (!object.interact(BANK_ACTIONS)) {
            log(AIOFurnace.class, "Failed to interact with bank.");
            return;
        }
        submitHumanTask(() -> getWidgetManager().getBank().isVisible(), random(10000, 15000));
    }

    private void handleBank() {
        log(AIOFurnace.class, "Handling bank...");
        Set<Integer> itemsToIgnore = selectedProduct.getResources().stream()
                .map(Resource::getItemID)
                .collect(Collectors.toSet());
        itemsToIgnore.addAll(selectedProduct.getMouldIDs());
        if (!getWidgetManager().getBank().depositAll(itemsToIgnore)) {
            // failed to deposit all except resources
            return;
        }
        // get our item group snapshots
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        ItemGroupResult bankSnapshot = getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        List<BankEntry> bankEntries = new ArrayList<>();

        if (inventorySnapshot == null || bankSnapshot == null) {
            return;
        }
        // work out items to withdraw with some mathematics
        int slotsToWorkWith = inventorySnapshot.getFreeSlots();
        // calculate how many slots it takes to make a single product
        List<Resource> resources = selectedProduct.getResources();
        int slotsPerProduct = 0;
        int stackableIngredients = 0;
        for (Resource resource : resources) {
            if (getItemManager().isStackable(resource.getItemID())) {
                stackableIngredients++;
                if (inventorySnapshot.contains(resource.getItemID())) {
                    slotsToWorkWith++;
                }
            } else {
                slotsPerProduct += resource.getAmount();
                slotsToWorkWith += inventorySnapshot.getAmount(resource.getItemID());
            }
        }
        // commented out as the user should already have the mould in their inventory
//        if(selectedProduct.getMouldID() != null) {
//            // if the product requires a mould, we negate one slot for the mould
//            slotsToWorkWith--;
//        }
        // work out how many products we can make
        int amountOfProducts = (slotsToWorkWith - stackableIngredients) / slotsPerProduct;


        // now we need to work out how many of each resource we need to withdraw
        int resourceCount = resources.size();
        int stackableCount = 0;
        for (Resource resource : resources) {
            int inventoryAmount = inventorySnapshot.getAmount(resource.getItemID());
            log(AIOFurnace.class, "Resource required - Item ID: " + resource.getItemID() + " Amount in inventory: " + inventoryAmount);
            if (getItemManager().isStackable(resource.getItemID())) {
                stackableCount++;
                log(AIOFurnace.class, "Resource is stackable - Withdrawing all if not enough in inventory.");
                // if the item is stackable withdraw all
                ItemSearchResult stackableIngredient = inventorySnapshot.getItem(resource.getItemID());
                if (stackableIngredient == null || inventorySnapshot.getAmount(resource.getItemID()) < resource.getAmount() * amountOfProducts) {
                    bankEntries.add(new BankEntry(resource.getItemID()));
                }
            } else {
                int amountNeeded = (resource.getAmount() * amountOfProducts) - inventoryAmount;
                log(AIOFurnace.class, "Amount needed for Item ID: " + resource.getItemID() + " is " + amountNeeded);
                if (amountNeeded == 0) {
                    continue;
                }
                bankEntries.add(new BankEntry(resource.getItemID(), amountNeeded));
            }
        }

        if (bankEntries.isEmpty()) {
            // close if no entries
            getWidgetManager().getBank().close();
        } else {
            // if we have bank entries, we need to process them
            BankEntry bankEntry = bankEntries.get(random(bankEntries.size()));
            String itemName = getItemManager().getItemName(bankEntry.itemID);
            if (itemName == null) {
                itemName = "Unknown Item name - ID: " + bankEntry.itemID;
            }
            log(AIOFurnace.class, "Processing bank entry: " + itemName + " Amount: " + bankEntry.amount + " Stackable: " + bankEntry.stackable);
            if (bankEntry.getAmount() > 0) {
                // withdraw
                boolean stackable = getItemManager().isStackable(bankEntry.itemID);
                int amountInBank = bankSnapshot.getAmount(bankEntry.itemID);
                boolean notEnough = amountInBank < bankEntry.amount;
                if (notEnough) {
                    log(AIOFurnace.class, "Insufficient supplies - Item: " + itemName + " Amount required: " + bankEntry.amount + " Amount in bank: " + amountInBank);
                    if (hasSuppliesForProduct(inventorySnapshot)) {
                        getWidgetManager().getBank().close();
                        return;
                    }
                    stop();
                    return;
                }
                int nonStackableResources = resourceCount - stackableCount;
                if (!stackable && nonStackableResources >= 2) {
                    getWidgetManager().getBank().withdraw(bankEntry.itemID, amountOfProducts);
                } else {
                    getWidgetManager().getBank().withdraw(bankEntry.itemID, stackable ? Integer.MAX_VALUE : bankEntry.amount);
                }
            } else {
                // deposit
                int depositAmount = Math.abs(bankEntry.amount);
                log(AIOFurnace.class, "Depositing item: " + itemName + " Amount: " + depositAmount);
                getWidgetManager().getBank().deposit(bankEntry.itemID, depositAmount);
            }
        }
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    private static class BankEntry {
        private final int itemID;
        private final int amount;
        private final boolean stackable;

        public BankEntry(int itemID, int amount) {
            this.itemID = itemID;
            this.amount = amount;
            this.stackable = false;
        }

        public BankEntry(int itemID) {
            this.itemID = itemID;
            this.amount = Integer.MAX_VALUE;
            this.stackable = true;
        }

        public boolean isStackable() {
            return stackable;
        }

        public int getItemID() {
            return itemID;
        }

        public int getAmount() {
            return amount;
        }
    }
}