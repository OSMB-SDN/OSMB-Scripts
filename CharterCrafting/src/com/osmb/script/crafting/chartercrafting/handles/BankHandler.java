package com.osmb.script.crafting.chartercrafting.handles;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.ui.bank.Bank;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.crafting.chartercrafting.CharterCrafting;

import java.util.List;
import java.util.Set;

import static com.osmb.script.crafting.chartercrafting.Config.*;
import static com.osmb.script.crafting.chartercrafting.Constants.*;

public class BankHandler {

    private final ScriptCore core;
    private final Bank bank;

    public BankHandler(ScriptCore core) {
        this.core = core;
        this.bank = core.getWidgetManager().getBank();
    }

    public void open() {
        core.log(BankHandler.class, "Banking supplies");
        WorldPosition myPosition = core.getWorldPosition();
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
            WorldPosition myPosition_ = core.getWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            return bankArea.contains(myPosition_);
        });
        core.getWalker().walkTo(bankArea.getRandomPosition(), walkConfig.build());
    }

    public void handleInterface() {
        core.log(BankHandler.class, "Handling bank interface");
        if (!bank.depositAll(Set.of(ItemID.COINS_995))) {
            return;
        }
        bank.close();
    }


    private void openBank() {
        core.log(BankHandler.class, "Searching for a bank...");

        List<RSObject> banksFound = core.getObjectManager().getObjects(BANK_QUERY);
        //can't find a bank
        if (banksFound.isEmpty()) {
            core.log(BankHandler.class, "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) core.getUtils().getClosest(banksFound);
        if (object.getName().equals("Closed booth")) {
            if (!object.interact("Bank booth", new String[]{"Bank"})) {
                return;
            }
        } else {
            if (!object.interact(BANK_ACTIONS)) return;
        }
        long positionChangeTimeout = RandomUtils.uniformRandom(1300, 3000);
        // sleep until bank is open or not moving (failsafe for dud actions)
        core.pollFramesHuman(() -> {
            WorldPosition position = core.getWorldPosition();
            if (position == null) {
                return false;
            }
            if (object.getTileDistance(position) > 1 && core.getLastPositionChangeMillis() > positionChangeTimeout) {
                return true;
            }

            return bank.isVisible();
        }, RandomUtils.uniformRandom(9000, 17000));
    }
}
