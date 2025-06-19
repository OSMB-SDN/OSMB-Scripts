package com.osmb.script.smithing;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.smithing.component.AnvilInterface;
import com.osmb.script.smithing.data.Product;
import com.osmb.script.smithing.javafx.ScriptOptions;
import javafx.scene.Scene;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(name = "AIO Anvil", skillCategory = SkillCategory.SMITHING, version = 1, author = "Joe", description = "Uses bars at anvils to create weapons and armour.")
public class AIOAnvil extends Script {

    private static final RectangleArea VARROCK_AREA = new RectangleArea(3131, 3391, 109, 75, 0);
    private static final WorldPosition VARROCK_BANK_BOOTH_POSITION = new WorldPosition(3186, 3436, 0);
    private int selectedBarID;
    private int selectedProductID;
    private Product selectedProduct;
    private AnvilInterface anvilInterface;
    private int amountChangeTimeout = Utils.random(4000,6500);
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.HAMMER));
    private ItemGroupResult inventorySnapshot;

    public AIOAnvil(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        anvilInterface = new AnvilInterface(this);
        ScriptOptions scriptOptions = new ScriptOptions(this);
        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Settings", false);
        selectedProduct = scriptOptions.getSelectedProduct();
        selectedProductID = scriptOptions.getSelectedProductID();
        selectedBarID = scriptOptions.getSelectedBar();
        ITEM_IDS_TO_RECOGNISE.add(selectedBarID);
        getWidgetManager().addComponent(anvilInterface);
    }

    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            handleBankInterface();
            return 0;
        }

        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if(inventorySnapshot == null) {
            return 0;
        }
        if(!inventorySnapshot.contains(ItemID.HAMMER)) {
            log(AIOAnvil.class, "Hammer not found in inventory, stopping script...");
            stop();
            return 0;
        }
        if (anvilInterface.isVisible()) {
            log(AIOAnvil.class,"Anvil interface is visible");
            if (handleAnvilInterface()) {
                waitUntilFinishedSmithing();
            }
            return 0;
        }
        // bank
        if (inventorySnapshot.getAmount(selectedBarID) < selectedProduct.getBarsNeeded()) {
            openBank();
            return 0;
        }
        RSObject anvil = getObjectManager().getClosestObject("Anvil");
        if (anvil == null) {
            log(getClass().getSimpleName(), "Can't find Anvil...");
            return 0;
        }
        if (!anvil.interact("Smith")) {
            // if fail to interact (we don't necessarily need to do anything here as we need to try again, so return to the top of the loop)
            return 0;
        }
        submitHumanTask(() -> anvilInterface.isVisible(), 15000);
        return 0;
    }

    private void waitUntilFinishedSmithing() {
        // sleep until finished smithing items
        Timer amountChangeTimer = new Timer();
        AtomicReference<Integer> previousAmount = new AtomicReference<>(-1);
        submitHumanTask(() -> {
            DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType != null) {
                // look out for level up dialogue etc.
                if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    // sleep for a random time so we're not instantly reacting to the dialogue
                    // we do this in the task to continue updating the screen
                    submitTask(() -> false, random(1000, 4000));
                    return true;
                }
            }
            inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.HAMMER, selectedBarID));
            if (inventorySnapshot == null) {
                return false;
            }
            // If the amount of gems in the inventory hasn't changed and the timeout is exceeded, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                return true;
            }

            // if no bars left break out
            int amountOfBars = inventorySnapshot.getAmount(selectedBarID);
            if (amountOfBars < selectedProduct.getBarsNeeded()) {
                return true;
            }
            // check if bars have decremented
            if (amountOfBars < previousAmount.get() || previousAmount.get() == -1) {
                previousAmount.set(amountOfBars);
                amountChangeTimer.reset();
            }

            return false;
        }, 60000, false, true);
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    private boolean handleAnvilInterface() {
        return anvilInterface.selectItem(selectedProductID);
    }

    private void handleBankInterface() {
        if (!getWidgetManager().getBank().depositAll(ITEM_IDS_TO_RECOGNISE)) {
            return;
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        ItemGroupResult bankSnapshot = getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        if(inventorySnapshot == null || bankSnapshot == null) {
            log(AIOAnvil.class, "Inventory or bank snapshot is null");
            return;
        }
        // we have bars in inventory and no free slots, close bank
        if (inventorySnapshot.isFull() && inventorySnapshot.contains(selectedBarID)) {
            getWidgetManager().getBank().close();
            return;
        }
        if (!bankSnapshot.contains(selectedBarID)) {
            log(getClass().getSimpleName(), "Can't find bars in bank, stopping script...");
            stop();
            return;
        }
        // withdraw bars
        getWidgetManager().getBank().withdraw(selectedBarID, Integer.MAX_VALUE);
    }

    //TODO prioritise other anvil areas
    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12597};
    }

    private void openBank() {
        log(AIOAnvil.class, "Opening bank...");
        WorldPosition position = getWorldPosition();
        // the bank booth closest to the anvil has no name in object def (will be some anti-botting thing where its info is loaded on login)
        // to combat this we just get the object from the tile
        if (VARROCK_AREA.contains(position)) {
            RSTile bankTile = getSceneManager().getTile(VARROCK_BANK_BOOTH_POSITION);
            if (bankTile == null) {
                log(getClass().getSimpleName(), "Bank tile is null.");
                return;
            }
            RSObject bank = bankTile.getObjects().get(0);
            if (bank != null) {
                if (bank.interact("Bank booth", new String[] {"Bank"})) {
                    log(AIOAnvil.class, "Interacted successfully, waiting for bank to be visible.");
                    if (submitTask(() -> getWidgetManager().getBank().isVisible(), 12000)) {
                        log(AIOAnvil.class, "Bank is visible.");
                    } else {
                        log(AIOAnvil.class, "Timed out waiting for bank to open");
                    }
                }
            }
        } else {
            RSObject bank = getObjectManager().getClosestObject("Bank booth");
            if (bank == null) {
                log(getClass().getSimpleName(), "Can't find Bank booth...");
                return;
            }
            if (bank.interact("Bank")) {
                // wait for bank to be visible
                submitTask(() -> getWidgetManager().getBank().isVisible(), 10000);
            }
        }
    }
}
