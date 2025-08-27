package com.osmb.script.firemaking.wintertodt.utilities;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.firemaking.wintertodt.ui.ScriptOptions;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.osmb.script.firemaking.wintertodt.Config.fletchType;

public class Utils {
    public static int calculateNextMilestone(int currentPoints) {
        if (currentPoints < 500) {
            return 500; // First milestone is always 500
        } else {
            // Calculate the next milestone as the next multiple of 250
            return ((currentPoints / 250) + 1) * 250;
        }
    }

    public static String getMenuOption(int amount) {
        int menuAmount = -1;
        if (amount >= 8) {
            menuAmount = 10;
        } else if (amount >= 3) {
            menuAmount = 5;
        }

        return "take-" + (menuAmount != -1 ? menuAmount + " " : "") + "concoction" + (menuAmount != -1 ? "s" : "");
    }

    public static Supplier<WalkConfig> getFastCombineSupplier(ScriptCore core, List<ItemSearchResult> brumaHerbs, List<ItemSearchResult> unfPotions) {
        return () -> {
            core.pollFramesUntil(() -> {
                if (brumaHerbs.isEmpty() || unfPotions.isEmpty()) {
                    // break out when none left
                    return true;
                }
                ItemSearchResult unfPotion = unfPotions.get(0);
                ItemSearchResult herb = brumaHerbs.get(0);
                if (!core.getFinger().tap(false, unfPotion) || !core.getFinger().tap(false, herb)) {
                    return true;
                }
                // remove from lists if we interacted with both
                brumaHerbs.remove(0);
                unfPotions.remove(0);
                // short sleep
                core.pollFramesUntil(() -> false, RandomUtils.uniformRandom(RandomUtils.weightedRandom(200, 800)));
                return false;
            }, 8000);
            return null;
        };
    }

    public static Supplier<WalkConfig> getCombineSupplier(ScriptCore core) {
        return () -> {
            ItemGroupResult inventorySnapshot_ = core.getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF, ItemID.BRUMA_HERB));
            if (inventorySnapshot_ == null) {
                return null;
            }
            int rand = RandomUtils.uniformRandom(2);
            ItemSearchResult item1 = inventorySnapshot_.getRandomItem((rand == 0 ? ItemID.REJUVENATION_POTION_UNF : ItemID.BRUMA_HERB));
            ItemSearchResult item2 = inventorySnapshot_.getRandomItem((rand == 0 ? ItemID.BRUMA_HERB : ItemID.REJUVENATION_POTION_UNF));
            if (item1 == null || item2 == null) {
                return null;
            }

            // combine the two items in a random order
            if (!item1.interact() && !item2.interact()) {
                // failed to interact with either item
                return null;
            }
            // sleep until potions are made
            core.pollFramesUntil(() -> {
                ItemGroupResult inventorySnapshot = core.getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF));
                if (inventorySnapshot == null) {
                    return false;
                }
                return !inventorySnapshot.contains(ItemID.REJUVENATION_POTION_UNF);
            }, RandomUtils.uniformRandom(3000, 8000));
            return null;
        };
    }

    public static UIResult<Integer> calculateResourcePoints(ItemGroupResult inventorySnapshot, int currentPoints) {
        int kindlingPoints = 25 * inventorySnapshot.getAmount(ItemID.BRUMA_KINDLING);
        boolean reachedFirstMilestone = currentPoints >= 500;

        boolean fletch = fletchType == ScriptOptions.FletchType.YES || !reachedFirstMilestone && fletchType == ScriptOptions.FletchType.UNTIL_MILESTONE;

        int rootXp = fletch ? 25 : 10;
        int rootsPoints = inventorySnapshot.getAmount(ItemID.BRUMA_ROOT) * rootXp;
        return UIResult.of(kindlingPoints + rootsPoints);
    }
}
