package com.osmb.script.chartercrafting;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.LocalPosition;
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

@ScriptDefinition(name = "Charter crafter", author = "Joe", version = 1.0, description = "", skillCategory = SkillCategory.CRAFTING)
public class CharterCrafting extends Script {

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth"};
    public static final String[] BANK_ACTIONS = {"bank", "open"};
    private static final int[] SELL_OPTION_AMOUNTS = new int[]{1, 5, 10, 50};


    private static final ToleranceComparator TOLERANCE_COMPARATOR_2 = new SingleThresholdComparator(5);
    private static final SearchablePixel SELECTED_HIGHLIGHT_COLOR = new SearchablePixel(-2171877, TOLERANCE_COMPARATOR_2, ColorModel.RGB);
    private static final ToleranceComparator TOLERANCE_COMPARATOR = new SingleThresholdComparator(3);
    private Dock selectedDock;
    private ShopInterface shopInterface;
    private GlassBlowingItem selectedGlassBlowingItem;
    private Method selectedMethod;
    private int amountChangeTimeout;
    // not used, npc highlights are bugged in osrs and reset when relogging. saving for when they are fixed.
    private SearchablePixel highlightColor;
    private boolean hopFlag = false;
    private List<NPC> npcs;

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
        this.selectedDock = ui.getSelectedDock();
        // workaround as highlights aren't working for charter crew members
        this.npcs = NPC.getNpcsForDock(selectedDock);
        this.selectedMethod = ui.getSelectedMethod();
        this.selectedGlassBlowingItem = ui.getSelectedGlassBlowingItem();
        this.highlightColor = new SearchablePixel(-14221313, TOLERANCE_COMPARATOR, ColorModel.RGB);
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
        if (selectedMethod != Method.BUY_AND_BANK) {
            UIResultList<ItemSearchResult> moltenGlass = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.MOLTEN_GLASS);
            UIResult<ItemSearchResult> glassblowingPipe = getItemManager().findItem(getWidgetManager().getInventory(), ItemID.GLASSBLOWING_PIPE);
            if (moltenGlass.isNotVisible() || glassblowingPipe.isNotVisible()) {
                log(CharterCrafting.class, "Inventory not visible...");
                return 0;
            }
            if (glassblowingPipe.isNotFound()) {
                log(CharterCrafting.class, "No glassblowing pipe found.");
                stop();
                return 0;
            }
            if (!moltenGlass.isEmpty() && glassblowingPipe.isFound()) {
                log(CharterCrafting.class, "Crafting molten glass...");
                craftMoltenGlass(glassblowingPipe, moltenGlass);
                return 0;
            }
        }

        UIResultList<ItemSearchResult> combinationItem = getItemManager().findAllOfItem(getWidgetManager().getInventory(), selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH);
        UIResultList<ItemSearchResult> bucketOfSand = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.BUCKET_OF_SAND);

        Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());

        if (combinationItem.isNotVisible() || bucketOfSand.isNotVisible() || freeSlots.isEmpty()) {
            log(CharterCrafting.class, "Inventory not visible...");
            return 0;
        }
        if (shouldOpenShop(bucketOfSand, combinationItem, freeSlots.get())) {
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
            return;
        }

        UIResultList<WorldPosition> npcPositions = getWidgetManager().getMinimap().getNPCPositions();
        if (npcPositions.isNotVisible()) {
            return;
        }
        if (npcPositions.isNotFound()) {
            log(getClass().getSimpleName(), "No NPC's found nearby...");
            return;
        }
        List<WorldPosition> validNPCPositions = getValidNPCPositions(npcPositions);
        if (validNPCPositions.isEmpty()) {
            // walk to furthest if none are visible on screen
            WorldPosition furthestNPCPosition = getFurthestNPC(myPosition, npcPositions);
            if (furthestNPCPosition == null) {
                log(CharterCrafting.class, "Furthest npc position is null");
                return;
            }
            WalkConfig.Builder walkConfig = new WalkConfig.Builder();
            walkConfig.breakCondition(() -> {
                RSTile tile = getSceneManager().getTile(furthestNPCPosition);
                if (tile == null) {
                    return false;
                }
                return tile.isOnGameScreen();
            });
            getWalker().walkTo(furthestNPCPosition, walkConfig.build());
            return;
        }
        // interact
        WorldPosition closestPosition = (WorldPosition) Utils.getClosestPosition(myPosition, validNPCPositions.toArray(new WorldPosition[0]));
        Polygon cubePoly = getSceneProjector().getTileCube(closestPosition, 130);
        if (cubePoly == null) {
            return;
        }
        // highlights aren't working for charter npc's idk why
//        Rectangle highlightBounds = getUtils().getHighlightBounds(cubePoly, highlightColor, SELECTED_HIGHLIGHT_COLOR);
//        if (highlightBounds == null) {
//            log(CharterCrafting.class, "No highlight bounds!");
//            return;
//        }
        // Search for npc's pixels instead of highlight pixels
        List<Point> pixels = List.of();
        for (NPC npc : npcs) {
            pixels = getPixelAnalyzer().findPixels(cubePoly, npc.getSearchablePixels());
            if (!pixels.isEmpty()) {
                break;
            }
        }
        if (pixels.isEmpty()) {
            return;
        }
        if (!getFinger().tap(pixels.get(random(pixels.size())), "trade trader crewmember")) {
            return;
        }

        submitHumanTask(() -> shopInterface.isVisible(), random(6000, 9000));
    }

    private WorldPosition getFurthestNPC(WorldPosition myPosition, UIResultList<WorldPosition> npcPositions) {
        // instantiate an arraylist as asList returns an unmodifiable list.
        List<WorldPosition> positionList = new ArrayList<>(npcPositions.asList());
        positionList.removeIf(position -> !selectedDock.getWanderArea().contains(position));
        // get furthest npc
        return positionList.stream().max(Comparator.comparingDouble(npc -> npc.distanceTo(myPosition))).orElse(null);
    }

    private boolean shouldOpenShop(UIResultList<ItemSearchResult> bucketOfSand, UIResultList<ItemSearchResult> combinationItem, int freeSlots) {
        return switch (selectedMethod) {
            case BUY_AND_BANK -> freeSlots >= 2;
            case BUY_AND_FURNACE_CRAFT -> bucketOfSand.isEmpty() || combinationItem.isEmpty() || freeSlots >= 2;
            case SUPER_GLASS_MAKE -> bucketOfSand.isEmpty() || combinationItem.isEmpty();
        };
    }

    private void craftMoltenGlass(UIResult<ItemSearchResult> glassblowingPipe, UIResultList<ItemSearchResult> moltenGlass) {
        log(CharterCrafting.class, "Crafting Molten glass...");
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return;
        }
        Area wanderArea = selectedDock.getWanderArea();
        if (wanderArea.contains(myPosition)) {
            craft(glassblowingPipe, moltenGlass, Integer.MAX_VALUE);
        } else {
            WalkConfig.Builder walkConfig = new WalkConfig.Builder().disableWalkScreen(true).tileRandomisationRadius(2);
            walkConfig.doWhileWalking(() -> {
                log(CharterCrafting.class, "Crafting while walking...");
                // find items
                UIResultList<ItemSearchResult> moltenGlass_ = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.MOLTEN_GLASS);
                UIResult<ItemSearchResult> glassblowingPipe_ = getItemManager().findItem(getWidgetManager().getInventory(), ItemID.GLASSBLOWING_PIPE);
                if (moltenGlass_.isNotVisible() || glassblowingPipe_.isNotVisible()) {
                    log(CharterCrafting.class, "Inventory not visible...");
                    return null;
                }
                if (moltenGlass_.isEmpty()) {
                    // no molten glass to craft
                    return null;
                }
                if (glassblowingPipe_.isNotFound()) {
                    log(CharterCrafting.class, "No glassblowing pipe found.");
                    stop();
                    return null;
                }

                craft(glassblowingPipe_, moltenGlass_, random(4000, 12000));
                return null;
            });

            getWalker().walkTo(wanderArea.getRandomPosition(), walkConfig.build());
        }
    }

    private void craft(UIResult<ItemSearchResult> glassblowingPipe, UIResultList<ItemSearchResult> moltenGlass, int timeout) {
        if (validDialogue()) {
            waitUntilFinishedProducing(timeout, ItemID.MOLTEN_GLASS);
            return;
        }
        log(CharterCrafting.class, "Interacting...");
        interactAndWaitForDialogue(glassblowingPipe.get(), moltenGlass.getRandom());

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
            // If the amount of gems in the inventory hasn't changed and the timeout is exceeded, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                this.amountChangeTimeout = random(4500, 7000);
                return true;
            }
            if (!getWidgetManager().getInventory().open()) {
                return false;
            }

            for (int resource : resources) {
                int amount;
                if (!getItemManager().isStackable(resource)) {
                    UIResultList<ItemSearchResult> resourceResult = getItemManager().findAllOfItem(getWidgetManager().getInventory(), resource);
                    if (resourceResult.isNotVisible()) {
                        return false;
                    }
                    amount = resourceResult.size();
                } else {
                    UIResult<ItemSearchResult> resourceResult = getItemManager().findItem(getWidgetManager().getInventory(), resource);
                    if (resourceResult.isNotVisible()) {
                        return false;
                    }
                    amount = resourceResult.get().getStackAmount();
                }
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
        }, 90000, true, false, true);
    }

    public boolean interactAndWaitForDialogue(ItemSearchResult item1, ItemSearchResult item2) {
        int random = random(1);
        ItemSearchResult interact1 = random == 0 ? item1 : item2;
        ItemSearchResult interact2 = random == 0 ? item2 : item1;

        interact1.interact();
        sleep(Utils.random(300, 1200));
        interact2.interact();
        // sleep until dialogue is visible
        return submitTask(() -> {
            DialogueType dialogueType1 = getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType1 == null) return false;
            return dialogueType1 == DialogueType.ITEM_OPTION;
        }, 3000);
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
        if (!getWidgetManager().getBank().depositAll(new int[]{ItemID.COINS_995})) {
            return;
        }
        getWidgetManager().getBank().close();
    }

    private void superGlassMake() {
        try {
            if (getWidgetManager().getSpellbook().selectSpell(LunarSpellbook.SUPERGLASS_MAKE, null)) {
                submitHumanTask(() -> {
                    UIResultList<ItemSearchResult> combinationItem = getItemManager().findAllOfItem(getWidgetManager().getInventory(), selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH);
                    UIResultList<ItemSearchResult> bucketOfSand = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.BUCKET_OF_SAND);
                    if (bucketOfSand.isNotVisible() || combinationItem.isNotVisible()) {
                        return false;
                    }
                    return combinationItem.isEmpty() || bucketOfSand.isEmpty();
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

    private void openBank() {
        log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it
        List<RSObject> banksFound = getObjectManager().getObjects(gameObject -> {
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
        });
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (object.getName().equals("Closed booth")) {
            if (!object.interact(1, "Bank booth", null, "Bank")) {
                return;
            }
        } else {
            if (!object.interact(BANK_ACTIONS)) return;
        }
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
        submitTask(() -> {
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
        return;
    }

    private void handleShopInterface() {
        log(CharterCrafting.class, "Shop interface is visible.");
        // sell crafted items
        UIResultList<ItemSearchResult> craftedItems = getItemManager().findAllOfItem(getWidgetManager().getInventory(),
                selectedGlassBlowingItem.getItemId(), ItemID.BUCKET);
        Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());

        if (craftedItems.isNotVisible() || !freeSlots.isPresent()) {
            // inventory not visible
            return;
        }
        if (craftedItems.isFound()) {
            log(CharterCrafting.class, "Selling crafted items");
            sellItems(new SellEntry(craftedItems, 999), freeSlots.get());
            return;
        }

        // find items in inventory
        UIResultList<ItemSearchResult> combinationItem = getItemManager().findAllOfItem(getWidgetManager().getInventory(), selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH);
        UIResultList<ItemSearchResult> bucketOfSand = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.BUCKET_OF_SAND);
        // find items in shop
        UIResult<ItemSearchResult> bucketOfSandShop = getItemManager().findItem(shopInterface, ItemID.BUCKET_OF_SAND);
        UIResult<ItemSearchResult> combinationItemShop = selectedMethod == Method.SUPER_GLASS_MAKE
                ? getItemManager().findItem(shopInterface, ItemID.SEAWEED)
                : getItemManager().findItem(shopInterface, ItemID.SODA_ASH);

        if (bucketOfSandShop.isNotVisible() || combinationItemShop.isNotVisible()) {
            return;
        }
        Integer[] itemsToIgnore = new Integer[]{ItemID.BUCKET_OF_SAND,
                selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH};

        int freeSlotsExclBuyItems = freeSlots.get() + combinationItem.size() + bucketOfSand.size();
        int moltenGlassToMake = freeSlotsExclBuyItems / 2;
        int excessSlots = freeSlotsExclBuyItems - (moltenGlassToMake * 2);

        // cache shop stock
        int bucketOfSandStock = bucketOfSandShop.get().getStackAmount();
        int combinationItemStock = combinationItemShop.get().getStackAmount();
        log(CharterCrafting.class, "Bucket of sand stock: " + bucketOfSandStock + " Combination stock: " + combinationItemStock);

        // calculate amount to buy
        int bucketOfSandToBuy = moltenGlassToMake - bucketOfSand.size();
        int combinationToBuy = moltenGlassToMake - combinationItem.size();
        log(CharterCrafting.class, "Need to buy Bucket of sand: " + bucketOfSandToBuy + " Combination: " + combinationToBuy);


        List<SellEntry> sellEntries = new ArrayList<>();

        // if the number is negative, that means we have too many in our inventory and need to sell
        if (hasTooMany(bucketOfSandToBuy)) {
            sellEntries.add(new SellEntry(bucketOfSand, Math.abs(bucketOfSandToBuy)));
        }
        if (hasTooMany(combinationToBuy)) {
            sellEntries.add(new SellEntry(combinationItem, Math.abs(combinationToBuy)));
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
                sellItems(sellEntry, freeSlots.get());
                return;
            }
        }

        if (bucketOfSandToBuy > 0) {
            bucketOfSandToBuy += excessSlots;
            if (bucketOfSandToBuy >= bucketOfSandStock || bucketOfSandToBuy == freeSlots.get()) {
                bucketOfSandToBuy = 999;
            }
        }
        if (combinationToBuy > 0) {
            combinationToBuy += excessSlots;
            if (combinationToBuy >= combinationItemStock || combinationToBuy == freeSlots.get()) {
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
        buyItem(randomEntry, freeSlots.get());
    }

    private boolean hasTooMany(int amount) {
        return amount < 0;
    }

    private boolean buyItem(BuyEntry buyEntry, int freeSlots) {
        log(CharterCrafting.class, "Buying item. Entry: " + buyEntry);
        int amount = buyEntry.amount;
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
        if (amountSelected == null || amountSelected != amount) {
            if (!all || amountSelected == null || amountSelected < amount) {

                if (!shopInterface.setSelectedAmount(amount)) {
                    return false;
                }
            }
        }
        UIResult<ItemSearchResult> item = buyEntry.item;

        if (item.get().interact()) {
            return submitTask(() -> {
                        Optional<Integer> freeSlots_ = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
                        if (!freeSlots_.isPresent()) {
                            return false;
                        }
                        return freeSlots_.get() < freeSlots;
                    },
                    5000);
        }
        return false;
    }

    private boolean sellItems(SellEntry sellEntry, int freeSlots) {
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
        ItemSearchResult randomItem = sellEntry.items.getRandom();
        if (randomItem.interact()) {
            sleep(300);
            // wait for items to decrement
            return submitTask(() -> {
                        Optional<Integer> freeSlots_ = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
                        if (!freeSlots_.isPresent()) {
                            return false;
                        }
                        return freeSlots_.get() > freeSlots;
                    },
                    5000);
        }
        return false;
    }

    private List<WorldPosition> getValidNPCPositions(UIResultList<WorldPosition> npcPositions) {
        List<WorldPosition> validPositions = new ArrayList<>();
        npcPositions.forEach(position -> {
            // check if npc is in wander area
            if (!selectedDock.getWanderArea().contains(position)) {
                return;
            }
            // convert to local
            LocalPosition localPosition = position.toLocalPosition(this);
            // get poly for position
            Polygon poly = getSceneProjector().getTileCube(localPosition.getX(), localPosition.getY(), localPosition.getPlane(), 150, localPosition.getRemainderX(), localPosition.getRemainderY());
            if (poly == null) {
                return;
            }
            poly = poly.getResized(1.5);
            // check for highlight pixel
            for (NPC npc : npcs) {
                if (getPixelAnalyzer().findPixel(poly, npc.getSearchablePixels()) != null) {
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
        UIResult<ItemSearchResult> item;
        int amount;

        public BuyEntry(UIResult<ItemSearchResult> item, int amount) {
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
        UIResultList<ItemSearchResult> items;
        int amount;

        public SellEntry(UIResultList<ItemSearchResult> items, int amount) {
            this.items = items;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "SellEntry{" +
                    "items=" + items +
                    ", amount=" + amount +
                    '}';
        }
    }
}



