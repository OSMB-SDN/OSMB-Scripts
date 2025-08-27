package com.osmb.script.mining.gemminer;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class GemMiner extends Script {

    private static final int UNDERGROUND_REGION = 11410;
    private static final Set<Integer> ITEMS_TO_RECOGNISE = Set.of(
            ItemID.UNCUT_OPAL,
            ItemID.UNCUT_JADE,
            ItemID.UNCUT_RED_TOPAZ,
            ItemID.UNCUT_SAPPHIRE,
            ItemID.UNCUT_EMERALD,
            ItemID.UNCUT_RUBY,
            ItemID.UNCUT_DIAMOND
    );
    private static final Area GROUND_MINE_AREA = new PolyArea(List.of(new WorldPosition(2826, 3005, 0), new WorldPosition(2827, 3005, 0), new WorldPosition(2827, 2996, 0), new WorldPosition(2818, 2996, 0), new WorldPosition(2818, 3000, 0), new WorldPosition(2820, 3002, 0), new WorldPosition(2822, 3004, 0)));
    private static final SearchablePixel[] GEM_ROCK_PIXELS = new SearchablePixel[]{
            new SearchablePixel(-6351459, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-7728504, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-5236561, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-9366928, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-7072365, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-10809510, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-8580484, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-9891992, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12383936, new SingleThresholdComparator(2), ColorModel.HSL),
    };
    private ItemGroupResult inventorySnapshot;
    private Method selectedMethod = Method.UNDERGROUND;

    public GemMiner(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int poll() {
        if (getWidgetManager().getDepositBox().isVisible()) {
            handleDepositBox();
            return 0;
        }
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return 0;
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEMS_TO_RECOGNISE);
        if (inventorySnapshot.isFull()) {
            openDepositBox();
        } else {
            if (selectedMethod == Method.GROUND && !GROUND_MINE_AREA.contains(myPosition)) {
                // walk to mining area if not already there
                walkToMineArea();
                return 0;
            }
            mine(myPosition);
        }
        return 0;
    }

    private void walkToMineArea() {
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> {
            WorldPosition worldPosition = getWorldPosition();
            if (worldPosition == null) {
                return false;
            }
            return GROUND_MINE_AREA.contains(worldPosition);
        });
        getWalker().walkTo(GROUND_MINE_AREA.getRandomPosition(), builder.build());
    }

    private void handleDepositBox() {
    }

    private void openDepositBox() {

    }

    private void mine(WorldPosition myPosition) {
        List<RSObject> rocksOnScreen = getObjectManager().getObjects(rsObject -> rsObject.getName() != null && rsObject.getName().equalsIgnoreCase("gem rocks") && rsObject.isInteractableOnScreen());
        // loop through and remove inactive rocks
        List<RSObject> activeRocksOnScreen = new ArrayList<>();
        for (RSObject rock : rocksOnScreen) {
            Polygon rockPolygon = rock.getConvexHull();
            if (rockPolygon == null || (rockPolygon = rockPolygon.getResized(0.8)) == null) {
                continue;
            }
            if (getPixelAnalyzer().findPixel(rockPolygon, GEM_ROCK_PIXELS) != null) {
                activeRocksOnScreen.add(rock);
            }
        }
        if (activeRocksOnScreen.isEmpty()) {
            log(GemMiner.class, "No gem rocks found on screen.");
            return;
        }
        // sort list of rocks by distance to player position
        activeRocksOnScreen.sort(Comparator.comparingDouble(rock -> rock.getWorldPosition().distanceTo(myPosition)));
        // interact with the closest rock
        RSObject closestRock = activeRocksOnScreen.get(0);
        if (closestRock.interact("Mine")) {
            log(GemMiner.class, "Mining gem rock @ " + closestRock.getWorldPosition());
            waitUntilFinishedMining(closestRock);
        } else {
            log(GemMiner.class, "Failed to interact with gem rock: " + closestRock.getName());
        }
    }

    private void waitUntilFinishedMining(RSObject rock) {
        submitHumanTask(() -> {

            return false;
        }, random(10000, 15000));
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                UNDERGROUND_REGION, 11310
        };
    }

    public enum Method {
        GROUND,
        UNDERGROUND
    }
}
