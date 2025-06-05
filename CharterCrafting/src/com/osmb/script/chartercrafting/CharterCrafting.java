package com.osmb.script.chartercrafting;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.spellbook.LunarSpellbook;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.chartercrafting.component.ShopInterface;
import com.osmb.script.chartercrafting.javafx.UI;
import javafx.scene.Scene;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@ScriptDefinition(name = "Charter crafter", author = "Joe", version = 1.0, description = "", skillCategory = SkillCategory.CRAFTING)
public class CharterCrafting extends Script {

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    private static final int[] SELL_OPTION_AMOUNTS = new int[]{1, 5, 10, 50};


    private static final ToleranceComparator TOLERANCE_COMPARATOR_2 = new SingleThresholdComparator(5);
    private static final SearchablePixel SELECTED_HIGHLIGHT_COLOR = new SearchablePixel(-2171877, TOLERANCE_COMPARATOR_2, ColorModel.RGB);
    private static final ToleranceComparator TOLERANCE_COMPARATOR = new SingleThresholdComparator(3);
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.GLASSBLOWING_PIPE, ItemID.MOLTEN_GLASS, ItemID.BUCKET_OF_SAND, ItemID.SODA_ASH, ItemID.SEAWEED, ItemID.BUCKET));
    private static Dock selectedDock;
    // Find bank and open it
    private static final Predicate<RSObject> BANK_QUERY = gameObject -> {
        // if object has no name
        String name = gameObject.getName();

        if (name == null) {
            return false;
        }


        if (selectedDock == Dock.CORSAIR_COVE) {
            // handle closed bank
            if (gameObject.getWorldX() != 2569 || gameObject.getWorldY() != 2865) {
                return false;
            }
            if (!name.equalsIgnoreCase("Closed booth")) {
                return false;
            }

        } else {
            // has no interact options (eg. bank, open etc.)
            if (gameObject.getActions() == null) {
                return false;
            }

            if (!Arrays.stream(BANK_NAMES).anyMatch(bankName -> bankName.equalsIgnoreCase(name))) {
                return false;
            }

            // if no actions contain bank or open
            if (!Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
                return false;
            }
        }
        // final check is if the object is reachable
        return gameObject.canReach();
    };
    private ShopInterface shopInterface;
    private GlassBlowingItem selectedGlassBlowingItem;
    private Method selectedMethod;
    private int amountChangeTimeout;
    // not used, npc highlights are bugged in osrs and reset when relogging. saving for when they are fixed.
    private SearchablePixel highlightColor;
    private boolean hopFlag = false;
    private List<NPC> npcs;
    private ItemGroupResult inventorySnapshot;
    private int combinationItemID;

    public CharterCrafting(Object scriptCore) {
        super(scriptCore);
    }

    public static int roundDownToNearestOption(int amount) {
        if (amount < SELL_OPTION_AMOUNTS[0]) {
            return 0;
        }

        for (int i = SELL_OPTION_AMOUNTS.length - 1; i >= 0; i--) {
            if (SELL_OPTION_AMOUNTS[i] <= amount) {
                return SELL_OPTION_AMOUNTS[i];
            }
        }

        // This line should theoretically never be reached because of the first check
        return SELL_OPTION_AMOUNTS[0];
    }

    @Override
    public void onStart() {
        shopInterface = new ShopInterface(this);
        // inventory will be seen as visible when shop interface is visible
        getWidgetManager().getInventory().registerInventoryComponent(shopInterface);
        UI ui = new UI(this);
        Scene scene = new Scene(ui);
        //  osmb style sheet
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Settings", false);

        this.amountChangeTimeout = random(4500, 7000);
        selectedDock = ui.getSelectedDock();
        // workaround as highlights aren't working for charter crew members
        this.npcs = NPC.getNpcsForDock(selectedDock);
        this.selectedMethod = ui.getSelectedMethod();
        this.selectedGlassBlowingItem = ui.getSelectedGlassBlowingItem();
        this.highlightColor = new SearchablePixel(-14221313, TOLERANCE_COMPARATOR, ColorModel.RGB);

        ITEM_IDS_TO_RECOGNISE.add(selectedGlassBlowingItem.getItemId());
        combinationItemID = selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH;
    }

    @Override
    public int[] regionsToPrioritise() {
        log(getClass().getSimpleName(), "Prioritised region:" + selectedDock.getRegionID());
        return new int[]{selectedDock.getRegionID()};
    }

    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            log(CharterCrafting.class, "Bank is visible");
            handleBank();
            return 0;
        } else if (shopInterface.isVisible()) {
            log(CharterCrafting.class, "Shop is visible");
            handleShopInterface();
            return 0;
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            // inventory not visible - re-poll
            return 0;
        }
        if (selectedMethod != Method.BUY_AND_BANK) {
            if (!inventorySnapshot.contains(ItemID.GLASSBLOWING_PIPE)) {
                log(CharterCrafting.class, "No glassblowing pipe found.");
                stop();
                return 0;
            }
            if (inventorySnapshot.contains(ItemID.MOLTEN_GLASS)) {
                log(CharterCrafting.class, "Crafting molten glass...");
                craftMoltenGlass(inventorySnapshot.getItem(ItemID.GLASSBLOWING_PIPE), inventorySnapshot.getRandomItem(ItemID.MOLTEN_GLASS));
                return 0;
            }
        }

        if (shouldOpenShop(inventorySnapshot)) {
            if (hopFlag) {
                log(CharterCrafting.class, "Hopping worlds");
                hopWorlds();
            }
            log(CharterCrafting.class, "Need to open shop interface...");
            openShop();
            return 0;
        }

        switch (selectedMethod) {
            case SUPER_GLASS_MAKE -> superGlassMake();
            case BUY_AND_BANK -> bankSupplies();
            case BUY_AND_FURNACE_CRAFT -> smeltSupplies();
        }
        return 0;
    }

    private void hopWorlds() {
        forceHop();
        hopFlag = false;
    }

    private void openShop() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(CharterCrafting.class, "Position is null!");
            return;
        }

        if (!selectedDock.getWanderArea().contains(myPosition)) {
            // walk to area
            walkToNPCWanderArea();
            return;
        }

        UIResultList<WorldPosition> npcPositionsResult = getWidgetManager().getMinimap().getNPCPositions();
        if (npcPositionsResult.isNotFound()) {
            log(getClass().getSimpleName(), "No NPC's found nearby...");
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
        Polygon cubePoly = getSceneProjector().getTileCube(closestPosition, 130);
        if (cubePoly == null) {
            return;
        }
        //shrink the poly towards the center, this will make it more accurate to the npc - you can check this with the tile picking in the debug tool (scale).
        cubePoly = cubePoly.getResized(0.5);

        // tap inside the poly
        if (!getFinger().tap(cubePoly, "trade trader crewmember")) {
            return;
        }
        // wait for shop interface + human reaction time after
        submitHumanTask(() -> shopInterface.isVisible(), random(6000, 9000));
    }

    private void walkToFurthestNPC(WorldPosition myPosition, List<WorldPosition> npcPositions) {
        WorldPosition furthestNPCPosition = getFurthestNPC(myPosition, npcPositions);
        if (furthestNPCPosition == null) {
            log(CharterCrafting.class, "Furthest npc position is null");
            return;
        }
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            // break out when they are on screen, so we're not breaking out when we reach the specific tile... looks a lot more fluent
            RSTile tile = getSceneManager().getTile(furthestNPCPosition);
            if (tile == null) {
                return false;
            }
            return tile.isOnGameScreen();
        });
        getWalker().walkTo(furthestNPCPosition, walkConfig.build());
    }

    private void walkToNPCWanderArea() {
        log(CharterCrafting.class, "Walking to npc area...");
        WorldPosition randomPos = selectedDock.getWanderArea().getRandomPosition();
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            WorldPosition myPosition2 = getWorldPosition();
            if (myPosition2 == null) {
                log(CharterCrafting.class, "Position is null!");
                return false;
            }
            return selectedDock.getWanderArea().contains(myPosition2);
        });
        getWalker().walkTo(randomPos, walkConfig.build());
    }

    private WorldPosition getFurthestNPC(WorldPosition myPosition, List<WorldPosition> npcPositions) {
        // get furthest npc
        return npcPositions.stream().max(Comparator.comparingDouble(npc -> npc.distanceTo(myPosition))).orElse(null);
    }

    private boolean shouldOpenShop(ItemGroupResult inventorySnapshot) {
        return switch (selectedMethod) {
            case BUY_AND_BANK -> inventorySnapshot.getFreeSlots() >= 2;
            case BUY_AND_FURNACE_CRAFT ->
                    !inventorySnapshot.contains(ItemID.BUCKET_OF_SAND) || !inventorySnapshot.contains(combinationItemID) || inventorySnapshot.getFreeSlots() >= 2;
            case SUPER_GLASS_MAKE ->
                    !inventorySnapshot.contains(ItemID.BUCKET_OF_SAND) || !inventorySnapshot.contains(combinationItemID);
        };
    }

    private void craftMoltenGlass(ItemSearchResult glassblowingPipe, ItemSearchResult moltenGlass) {
        log(CharterCrafting.class, "Crafting Molten glass...");
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return;
        }
        Area wanderArea = selectedDock.getWanderArea();
        if (wanderArea.contains(myPosition)) {
            craft(glassblowingPipe, moltenGlass, Integer.MAX_VALUE);
        } else {
            // walk to wander area and craft
            WalkConfig.Builder walkConfig = new WalkConfig.Builder().disableWalkScreen(true).tileRandomisationRadius(2);
            walkConfig.doWhileWalking(() -> {
                log(CharterCrafting.class, "Crafting while walking...");
                inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    // inventory not visible
                    return null;
                }
                if (!inventorySnapshot.contains(ItemID.MOLTEN_GLASS)) {
                    // no molten glass to craft
                    return null;
                }
                if (!inventorySnapshot.contains(ItemID.GLASSBLOWING_PIPE)) {
                    log(CharterCrafting.class, "No glassblowing pipe found.");
                    stop();
                    return null;
                }
                craft(inventorySnapshot.getItem(ItemID.GLASSBLOWING_PIPE), inventorySnapshot.getRandomItem(ItemID.MOLTEN_GLASS), random(4000, 12000));
                return null;
            });
            getWalker().walkTo(wanderArea.getRandomPosition(), walkConfig.build());
        }
    }

    private void craft(ItemSearchResult glassblowingPipe, ItemSearchResult moltenGlass, int timeout) {
        if (!getWidgetManager().getInventory().unSelectItemIfSelected()) {
            log(CharterCrafting.class, "Failed to unselect item.");
            return;
        }
        if (validDialogue()) {
            waitUntilFinishedProducing(timeout, ItemID.MOLTEN_GLASS);
            return;
        }
        log(CharterCrafting.class, "Interacting...");
        interactAndWaitForDialogue(glassblowingPipe, moltenGlass);

        // only double call due to the walking method
        if (validDialogue()) {
            waitUntilFinishedProducing(timeout, ItemID.MOLTEN_GLASS);
        }
    }

    public boolean validDialogue() {
        DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selectedOption = getWidgetManager().getDialogue().selectItem(selectedGlassBlowingItem.getItemId());
            if (!selectedOption) {
                log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
                return false;
            }
            return true;
        }
        return false;
    }

    public void waitUntilFinishedProducing(int timeout, int... resources) {
        AtomicReference<Stopwatch> stopwatch = new AtomicReference<>(new Stopwatch(timeout));
        AtomicReference<Map<Integer, Integer>> previousAmounts = new AtomicReference<>(new HashMap<>());
        for (int resource : resources) {
            previousAmounts.get().put(resource, -1);
        }
        Timer amountChangeTimer = new Timer();
        submitHumanTask(() -> {
            DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType != null) {
                // look out for level up dialogue etc.
                // we can check the dialogue text specifically if it is a level up dialogue,
                // no point though as if we're interrupted we want to break out the loop anyway
                if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    // sleep for a random time so we're not instantly reacting to the dialogue
                    // we do this in the task to continue updating the screen
                    log(CharterCrafting.class, "Tap here to continue dialogue interrupted us, generating extra random time to react...");
                    // submitHumanTask already gives a delay afterward on completetion,
                    // but we want a bit of extra time on top as the user won't always be expecting the dialogue
                    submitTask(() -> false, random(1000, 6000));
                    // return true and execute the shorter generated human delay by submitHumanTask
                    return true;
                }
            }

            WorldPosition myPosition = getWorldPosition();
            if (myPosition != null) {
                if (!selectedDock.getWanderArea().contains(myPosition) && stopwatch.get().hasFinished()) {
                    return true;
                }

            }
            // If the amount of resources in the inventory hasn't changed and the timeout is exceeded, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                this.amountChangeTimeout = random(4500, 7000);
                return true;
            }

            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }

            for (int resource : resources) {
                int amount = inventorySnapshot.getAmount(resource);
                if (amount == 0) {
                    return true;
                }
                int previousAmount = previousAmounts.get().get(resource);
                if (amount < previousAmount || previousAmount == -1) {
                    previousAmounts.get().put(resource, amount);
                    amountChangeTimer.reset();
                }
            }
            return false;
        }, 90000, false, true);
    }

    public boolean interactAndWaitForDialogue(ItemSearchResult item1, ItemSearchResult item2) {
        // use chisel on gems and wait for dialogue
        int random = random(2);
        ItemSearchResult interact1 = random == 0 ? item1 : item2;
        ItemSearchResult interact2 = random == 0 ? item2 : item1;
        if (interact1.interact() && interact2.interact()) {
            return submitHumanTask(() -> {
                DialogueType dialogueType1 = getWidgetManager().getDialogue().getDialogueType();
                if (dialogueType1 == null) return false;
                return dialogueType1 == DialogueType.ITEM_OPTION;
            }, 3000);
        }
        return false;
    }

    private void bankSupplies() {
        log(CharterCrafting.class, "Banking supplies");
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return;
        }
        Area bankArea = selectedDock.getBankArea();
        if (bankArea.contains(myPosition)) {
            // find bank
            openBank();
            return;
        }

        // walk to bank
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            return bankArea.contains(myPosition_);
        });
        getWalker().walkTo(bankArea.getRandomPosition(), walkConfig.build());
    }

    private void handleBank() {
        if (!getWidgetManager().getBank().depositAll(Set.of(ItemID.COINS_995))) {
            return;
        }
        getWidgetManager().getBank().close();
    }

    private void superGlassMake() {
        try {
            if (getWidgetManager().getSpellbook().selectSpell(LunarSpellbook.SUPERGLASS_MAKE, null)) {
                // generate human response after selecting spell
                submitHumanTask(() -> true, 100);
                // check inventory
                submitHumanTask(() -> {
                    inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                    if (inventorySnapshot == null) {
                        return false;
                    }
                    int combinationItem = selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH;
                    return !inventorySnapshot.contains(combinationItem) || !inventorySnapshot.contains(ItemID.BUCKET_OF_SAND);
                }, 5000);
            }
        } catch (SpellNotFoundException e) {
            log(CharterCrafting.class, "Spell sprite not found, stopping script...");
            stop();
        }
    }

    private void smeltSupplies() {
        Area furnaceArea = selectedDock.getFurnaceArea();
        if (furnaceArea == null) {
            throw new RuntimeException("No furnace area for selected dock.");
        }

        RSObject furnace = getObjectManager().getClosestObject("Furnace");
        if (furnace == null) {
            // walk to furnace area if no furnace in our loaded scene
            walkToFurnace();
            return;
        }
        WorldPosition position = getWorldPosition();
        if (position == null) {
            return;
        }
        if (furnaceArea.contains(position)) {
            // check for dialogue
            DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType == DialogueType.ITEM_OPTION) {
                boolean selectedOption = getWidgetManager().getDialogue().selectItem(ItemID.MOLTEN_GLASS);
                if (!selectedOption) {
                    log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
                    return;
                }
                waitUntilFinishedProducing(Integer.MAX_VALUE, new int[]{selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH, ItemID.BUCKET_OF_SAND});
                return;
            }
        }

        if (furnace.interact("Smelt")) {
            // sleep until dialogue is visible
            submitHumanTask(() -> getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION, random(2000, 7000));
        }
    }

    private void walkToFurnace() {
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            WorldPosition myPosition = getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            return selectedDock.getFurnaceArea().contains(myPosition);
        });
        getWalker().walkTo(selectedDock.getFurnaceArea().getRandomPosition(), walkConfig.build());
    }

    private void openBank() {
        log(getClass().getSimpleName(), "Searching for a bank...");

        List<RSObject> banksFound = getObjectManager().getObjects(BANK_QUERY);
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (object.getName().equals("Closed booth")) {
            if (!object.interact("Bank booth", new String[]{"Bank"})) {
                return;
            }
        } else {
            if (!object.interact(BANK_ACTIONS)) return;
        }
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
        // sleep until bank is open or not moving (failsafe for dud actions)
        submitHumanTask(() -> {
            WorldPosition position = getWorldPosition();
            if (position == null) {
                return false;
            }
            if (pos.get() == null || !position.equals(pos.get())) {
                positionChangeTimer.get().reset();
                pos.set(position);
            }

            return getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, 15000);
    }

    private void handleShopInterface() {
        log(CharterCrafting.class, "Shop interface is visible.");
        // sell crafted items
        ItemGroupResult shopSnapshot = shopInterface.search(ITEM_IDS_TO_RECOGNISE);
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (shopSnapshot == null || inventorySnapshot == null) {
            return;
        }
        if (inventorySnapshot.contains(selectedGlassBlowingItem.getItemId())) {
            log(CharterCrafting.class, "Selling crafted items");
            sellItems(new SellEntry(inventorySnapshot.getRandomItem(selectedGlassBlowingItem.getItemId()), 999), inventorySnapshot.getFreeSlots());
            return;
        }
        if (inventorySnapshot.contains(ItemID.BUCKET)) {
            log(CharterCrafting.class, "Selling crafted items");
            sellItems(new SellEntry(inventorySnapshot.getRandomItem(ItemID.BUCKET), 999), inventorySnapshot.getFreeSlots());
            return;
        }


        int bucketOfSandInventory = inventorySnapshot.getAmount(ItemID.BUCKET_OF_SAND);
        int combinationItemInventory = inventorySnapshot.getAmount(combinationItemID);

        int freeSlotsExclBuyItems = inventorySnapshot.getFreeSlots() + combinationItemInventory + bucketOfSandInventory;
        int moltenGlassToMake = freeSlotsExclBuyItems / 2;
        int excessSlots = freeSlotsExclBuyItems - (moltenGlassToMake * 2);

        ItemSearchResult bucketOfSandShop = shopSnapshot.getItem(ItemID.BUCKET_OF_SAND);
        ItemSearchResult combinationItemShop = shopSnapshot.getItem(combinationItemID);
        // cache shop stock
        int bucketOfSandStock = bucketOfSandShop != null ? bucketOfSandShop.getStackAmount() : 0;
        int combinationItemStock = combinationItemShop != null ? combinationItemShop.getStackAmount() : 0;
        log(CharterCrafting.class, "Bucket of sand stock: " + bucketOfSandStock + " Combination stock: " + combinationItemStock);

        // calculate amount to buy
        int bucketOfSandToBuy = moltenGlassToMake - bucketOfSandInventory;
        int combinationToBuy = moltenGlassToMake - combinationItemInventory;
        log(CharterCrafting.class, "Need to buy Bucket of sand: " + bucketOfSandToBuy + " Combination: " + combinationToBuy);


        List<SellEntry> sellEntries = new ArrayList<>();

        // if the number is negative, that means we have too many in our inventory and need to sell
        if (hasTooMany(bucketOfSandToBuy)) {
            sellEntries.add(new SellEntry(inventorySnapshot.getRandomItem(ItemID.BUCKET_OF_SAND), Math.abs(bucketOfSandToBuy)));
        }
        if (hasTooMany(combinationToBuy)) {
            sellEntries.add(new SellEntry(inventorySnapshot.getRandomItem(combinationItemID), Math.abs(combinationToBuy)));
        }
        if (!sellEntries.isEmpty()) {
            boolean shouldSkip = sellEntries.size() == 1 &&
                    sellEntries.get(0).amount == 1 &&
                    excessSlots == 1;
            if (shouldSkip) {
                combinationToBuy = Math.max(0, combinationToBuy);
                bucketOfSandToBuy = Math.max(0, bucketOfSandToBuy);
            } else {
                SellEntry sellEntry = sellEntries.get(random(sellEntries.size()));
                sellItems(sellEntry, inventorySnapshot.getFreeSlots());
                return;
            }
        }

        if (bucketOfSandToBuy > 0) {
            bucketOfSandToBuy += excessSlots;
            if (bucketOfSandToBuy >= bucketOfSandStock || bucketOfSandToBuy == inventorySnapshot.getFreeSlots()) {
                bucketOfSandToBuy = 999;
            }
        }
        if (combinationToBuy > 0) {
            combinationToBuy += excessSlots;
            if (combinationToBuy >= combinationItemStock || combinationToBuy == inventorySnapshot.getFreeSlots()) {
                combinationToBuy = 999;
            }
        }
        if (bucketOfSandStock == 0) {
            bucketOfSandToBuy = 0;
        }
        if (combinationItemStock == 0) {
            combinationToBuy = 0;
        }

        log(CharterCrafting.class, "BucketOfSandToBuy: " + bucketOfSandToBuy + " CombinationToBuy: " + combinationToBuy);
        if (bucketOfSandToBuy == 0 && combinationToBuy == 0) {
            // complete
            if (combinationItemStock == 0 || bucketOfSandStock == 0) {
                hopFlag = true;
            }
            shopInterface.close();
        }

        List<BuyEntry> buyEntries = new ArrayList<>();
        if (bucketOfSandToBuy > 0) {
            buyEntries.add(new BuyEntry(bucketOfSandShop, bucketOfSandToBuy));
        }
        if (combinationToBuy > 0) {
            buyEntries.add(new BuyEntry(combinationItemShop, combinationToBuy));
        }
        if (buyEntries.isEmpty()) {
            // should never happen
            return;
        }
        BuyEntry randomEntry = buyEntries.get(random(buyEntries.size()));
        buyItem(randomEntry, inventorySnapshot.getFreeSlots());
    }

    private boolean hasTooMany(int amount) {
        return amount < 0;
    }

    private boolean buyItem(BuyEntry buyEntry, int initialFreeSlots) {
        log(CharterCrafting.class, "Buying item. Entry: " + buyEntry);
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
            return submitTask(() -> {
                        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                        if (inventorySnapshot == null) {
                            return false;
                        }
                        return inventorySnapshot.getFreeSlots() != initialFreeSlots;
                    },
                    5000);
        }
        return false;
    }

    private boolean sellItems(SellEntry sellEntry, int initialFreeSlots) {
        log(CharterCrafting.class, "Selling item. Entry: " + sellEntry);
        int amount = sellEntry.amount;
        boolean all = amount == 999;
        if (all) {
            amount = (random(2) == 1 ? 10 : 50);
        } else {
            amount = roundDownToNearestOption(amount);
        }
        UIResult<Integer> selectedAmount = shopInterface.getSelectedAmount();
        if (selectedAmount.isNotVisible()) {
            return false;
        }
        Integer amountSelected = selectedAmount.get();
        log("Amount selected: " + amountSelected);

        if (amountSelected == null || amountSelected != amount) {
            log("All? " + all);
            if (!all || (amountSelected == null || amountSelected < 10)) {
                if (!shopInterface.setSelectedAmount(amount)) {
                    return false;
                }
            }
        }
        log(CharterCrafting.class, "Selling items...");
        ItemSearchResult item = sellEntry.item;
        if (item.interact()) {
            submitTask(() -> false, 300);
            // wait for inv slots to change
            return submitTask(() -> {
                        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                        if (inventorySnapshot == null) {
                            return false;
                        }
                        return inventorySnapshot.getFreeSlots() != initialFreeSlots;
                    },
                    5000);
        }
        return false;
    }

    private List<WorldPosition> getValidNPCPositions(List<WorldPosition> npcPositions) {
        List<WorldPosition> validPositions = new ArrayList<>();
        npcPositions.forEach(position -> {
            // check if npc is in wander area
            if (!selectedDock.getWanderArea().contains(position)) {
                return;
            }
            // create a tile cube, we will analyse this for the npc's pixels
            Polygon poly = getSceneProjector().getTileCube(position, 150);
            if (poly == null) {
                return;
            }
            // check for highlight pixel
            for (NPC npc : npcs) {

//  -------------- highlights aren't working for charter npc's idk why. but this is how you would do it if not & works sooo much better.
//                Rectangle highlightBounds = getUtils().getHighlightBounds(poly, highlightColor, SELECTED_HIGHLIGHT_COLOR);
//                if (highlightBounds == null) {
//                    log(CharterCrafting.class, "No highlight bounds!");
//                    return;
//                }
//                if (!getFinger().tap(highlightBounds, "Trade")) {
//                    return;
//                }

                if (getPixelAnalyzer().findPixel(poly, npc.getSearchablePixels()) != null) {
                    // add to our separate list if we find the npc's pixels inside the tile cube
                    validPositions.add(position);
                    getScreen().getDrawableCanvas().drawPolygon(poly.getXPoints(), poly.getYPoints(), poly.numVertices(), Color.GREEN.getRGB(), 1);
                    break;
                }
            }
        });
        return validPositions;
    }

    @Override
    public boolean canHopWorlds() {
        return false;
    }

    public static class BuyEntry {
        ItemSearchResult item;
        int amount;

        public BuyEntry(ItemSearchResult item, int amount) {
            this.item = item;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "BuyEntry{" +
                    "item=" + item +
                    ", amount=" + amount +
                    '}';
        }
    }

    public static class SellEntry {
        ItemSearchResult item;
        int amount;

        public SellEntry(ItemSearchResult item, int amount) {
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



