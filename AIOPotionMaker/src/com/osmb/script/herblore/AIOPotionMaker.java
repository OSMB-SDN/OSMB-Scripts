package com.osmb.script.herblore;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.script.herblore.bank.BankHandler;
import com.osmb.script.herblore.javafx.ScriptOptions;
import com.osmb.script.herblore.mixing.PotionMixer;

import static com.osmb.script.herblore.Config.selectedPotion;
import static com.osmb.script.herblore.State.inventorySnapshot;
import static com.osmb.script.herblore.utils.Utilities.hasIngredients;

@ScriptDefinition(name = "AIO Potion maker", author = "Joe", version = 1.0, description = "Makes potions and unfinished potions", skillCategory = SkillCategory.HERBLORE)
public class AIOPotionMaker extends Script {

    private final PotionMixer potionMixer;
    private final BankHandler bankHandler;

    public AIOPotionMaker(Object o) {
        super(o);
        this.potionMixer = new PotionMixer(this);
        this.bankHandler = new BankHandler(this);
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public void onStart() {
        // show UI
        ScriptOptions scriptOptions = ScriptOptions.show(this);
        // get potion to make
        Config.selectedPotion = scriptOptions.getSelectedProduct();
        // safety check
        if (Config.selectedPotion == null) {
            throw new IllegalArgumentException("Selected potion cannot be null!");
        }
    }

    @Override
    public int poll() {
        // update inventory snapshot, if failed to update return and try again next loop
        if ((inventorySnapshot = getWidgetManager().getInventory().search(selectedPotion.getIngredientIds())) == null) {
            log(AIOPotionMaker.class, "Failed to update inventory snapshot!");
            return 0;
        }
        if (getWidgetManager().getBank().isVisible()) {
            bankHandler.handleBankInterface();
        } else {
            if (!hasIngredients(this, inventorySnapshot)) {
                bankHandler.openBank();
            } else {
                potionMixer.poll();
            }
        }
        return 0;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12598};
    }
}
