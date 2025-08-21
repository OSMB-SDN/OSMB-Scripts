package com.osmb.script.farming.tithefarm;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.shape.Polygon;
import com.osmb.api.utils.Utils;
import com.osmb.api.visual.drawing.Canvas;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PatchManager {
    private static final int SOUTH_MOST_Y = 3488;
    private static final int NORTH_MOST_Y = 3512;
    private static final int[] X_POSITIONS = new int[]{1810, 1815, 1820, 1825, 1830};
    private static final Map<Integer, TitheFarm.Patch> PATCH_MAP = new HashMap<>();
    private static final Font ARIAL_12 = new Font("Arial", Font.PLAIN, 12);
    private static final Font ARIAL_10 = new Font("Arial", Font.PLAIN, 10);
    private final ScriptCore core;
    private final int plantAmount;
    private final PlantMethod plantMethod;
    private int focusedPatchIndex = 0;

    public PatchManager(ScriptCore core, int plantAmount, PlantMethod plantMethod) {
        this.core = core;
        this.plantAmount = plantAmount;
        this.plantMethod = plantMethod;
    }

    public int getFocusedPatchIndex() {
        return focusedPatchIndex;
    }

    public void reset() {
        PATCH_MAP.clear(); // clear the map if all patches are completed
        focusedPatchIndex = 0; // reset index
    }

    public boolean startedRun() {
        if (PATCH_MAP.isEmpty()) {
            return false;
        }
        for (TitheFarm.Patch patch : PATCH_MAP.values()) {
            if (patch.getPlantTime() != -1) {
                return true; // if any patch is not completed, return true
            }
        }
        return false;
    }

    //TODO add random pattern
    private Map<Integer, TitheFarm.Patch> createPatchMap(WorldPosition worldPosition, int plantAmount) {
        // get the closest starting patch based on the world position
        StartingPatch startingPatch = getClosestStartingPatch(worldPosition);
        PlantDirection plantDirection = startingPatch.getPlantDirection();
        Map<Integer, TitheFarm.Patch> patchMap = new HashMap<>();
        WorldPosition startingPatchPosition = startingPatch.getWorldPosition();
        PatchRow startingPatchRow = startingPatch.getStartingPatchRow();
        // calculate the patches based on the starting patch position and the plant direction
        int x = startingPatchPosition.getX();
        int y = startingPatchPosition.getY();
        PatchRow patchRow = startingPatchRow;
        int indexOffset = 0;
        int i = 0;
        int order = Utils.random(2);
        while (patchMap.size() < plantAmount) {
            WorldPosition leftPatchPosition = new WorldPosition(patchRow.getLeftXPosition(), y, startingPatchPosition.getPlane());
            WorldPosition rightPatchPosition = new WorldPosition(patchRow.getRightXPosition(), y, startingPatchPosition.getPlane());
            WorldPosition patch1;
            WorldPosition patch2;
            // here we determine the order based on the plant method
            // serpentine method we keep the same x axis when incrementing the y axis
            if (plantMethod == PlantMethod.SERPENTINE) {
                patch1 = i % 2 == 0 ? leftPatchPosition : rightPatchPosition;
                patch2 = i % 2 == 0 ? rightPatchPosition : leftPatchPosition;
            } else {
                patch1 = leftPatchPosition;
                patch2 = rightPatchPosition;
            }
            // add patches to the map
            if (patchRow != startingPatchRow) {
                // handle when we move onto the next patch row, make sure we don't add the patch if its on the starting row
                int leftX = startingPatchRow.getLeftXPosition();
                int rightX = startingPatchRow.getRightXPosition();
                if (leftPatchPosition.getX() != leftX && leftPatchPosition.getX() != rightX) {
                    patchMap.put(i + indexOffset, new TitheFarm.Patch(leftPatchPosition, patchRow));
                } else if (rightPatchPosition.getX() != rightX && rightPatchPosition.getX() != leftX) {
                    patchMap.put(i + indexOffset, new TitheFarm.Patch(rightPatchPosition, patchRow));
                }
            } else {
                if (plantMethod == PlantMethod.RANDOM) {
                    order = Utils.random(2);
                }
                patchMap.put(i * 2, new TitheFarm.Patch(order == 0 ? patch1 : patch2, patchRow));
                patchMap.put(i * 2 + 1, new TitheFarm.Patch(order == 0 ? patch2 : patch1, patchRow));
            }
            // handle the next patch position based on the plant direction
            if (patchRow != startingPatchRow) {
                // handle the next column, move in the opposite y direction
                y += (plantDirection == PlantDirection.NORTH ? -3 : 3);
                if (y == 3500) {
                    y += (plantDirection == PlantDirection.NORTH ? -3 : 3);
                }
            } else {
                if (i > 0 && (y <= SOUTH_MOST_Y || y >= NORTH_MOST_Y)) {
                    // move to the next column - if we are at the edge, move the opposite else choose a random direction
                    x += (x == X_POSITIONS[X_POSITIONS.length - 1] ? -5 : x == X_POSITIONS[0] ? 5 : (core.random(2) == 0 ? 5 : -5));
                    PatchRow[] patchRows = PatchRow.values();
                    int currentRowIndex = patchRow.ordinal();
                    if (currentRowIndex == patchRows.length - 1) {
                        // if we are at the last row, go to the row to the left
                        patchRow = patchRows[currentRowIndex - 1];
                    } else if (currentRowIndex == 0) {
                        // otherwise go to row to the right
                        patchRow = patchRows[currentRowIndex + 1];
                    } else {
                        // randomly choose the next row
                        patchRow = core.random(2) == 0 ? patchRows[currentRowIndex - 1] : patchRows[currentRowIndex + 1];
                    }
                    indexOffset = i + 1;
                } else {
                    y += (plantDirection == PlantDirection.NORTH ? 3 : -3);
                    // account for middle row where there is no patch
                    if (y == 3500) {
                        y += (plantDirection == PlantDirection.NORTH ? 3 : -3);
                    }
                }
            }
            i++;
        }
        core.log("Created patch map with " + patchMap.size() + " patches.");
        return patchMap;
    }

    private StartingPatch getClosestStartingPatch(WorldPosition worldPosition) {
        WorldPosition closestPatch = null;
        double closestDistance = Double.MAX_VALUE;
        PatchRow startingPatchRow = PatchRow.getRowByPosition(worldPosition);
        if (startingPatchRow == null) {
            // not stood in a patch row, so we need to find the closest patch row
            PatchRow closestPatchRow = null;
            double closestPatchRowXDistance = Double.MAX_VALUE;
            for (PatchRow row : PatchRow.values()) {
                int middleX = row.getMiddleXPosition();
                double distance = Math.abs(worldPosition.getX() - middleX);
                if (closestPatchRow == null || distance < closestPatchRowXDistance) {
                    closestPatchRowXDistance = distance;
                    closestPatchRow = row;
                }
            }
            startingPatchRow = closestPatchRow;
        }
        int[] yPositions = new int[]{SOUTH_MOST_Y, NORTH_MOST_Y};
        for (int x : X_POSITIONS) {
            for (int y : yPositions) {
                WorldPosition patchPosition = new WorldPosition(x, y, worldPosition.getPlane());
                double distance = worldPosition.distanceTo(patchPosition);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPatch = patchPosition;
                }
            }
        }
        return new StartingPatch(closestPatch, closestPatch.getY() == SOUTH_MOST_Y ? PlantDirection.NORTH : PlantDirection.SOUTH, startingPatchRow);
    }

    /**
     * Gets the next patch to interact with
     *
     * @return
     */
    public TitheFarm.Patch getFocusedPatch() {
        WorldPosition worldPosition = core.getWorldPosition();
        if (worldPosition == null) {
            core.log(PatchManager.class, "Unable to get world position...");
            return null;
        }
        if (!PATCH_MAP.isEmpty()) {
            if (focusedPatchIndex >= PATCH_MAP.size()) {
                core.log(PatchManager.class, "Completed all patches, resetting patch index & clearing patch map...");
                focusedPatchIndex = 0; // reset index if we reached the end
                return null;
            } else if (isCompleted()) {
                core.log(PatchManager.class, "All patches completed, resetting patch map...");
                reset();
                return null;
            }
        }
        if (PATCH_MAP.isEmpty()) {
            core.log(PatchManager.class, "No patches found, creating new patch map...");
            PATCH_MAP.putAll(createPatchMap(worldPosition, plantAmount));
            focusedPatchIndex = 0;
        }
        // return the patch with the lowest growth stage & the lowest key
        return PATCH_MAP.get(focusedPatchIndex);
    }

    public TitheFarm.Patch getNextPatch() {
        if (focusedPatchIndex + 1 >= PATCH_MAP.size()) {
            return PATCH_MAP.get(0);
        }
        return PATCH_MAP.get(focusedPatchIndex + 1);
    }

    public void setNextPatch() {
        focusedPatchIndex++;
        if (!PATCH_MAP.containsKey(focusedPatchIndex)) {
            focusedPatchIndex = 0;
        }
    }

    private boolean isCompleted() {
        for (TitheFarm.Patch patch : PATCH_MAP.values()) {
            if (!patch.isHarvested() && !patch.isDead()) {
                return false; // if any patch is not completed, return false
            }
        }
        return true;
    }

    public RSObject getPatchObject(WorldPosition patchPosition) {
        if (patchPosition == null) {
            core.log(PatchManager.class, "Unable to get patch object, world position is null.");
            return null;
        }
        // skip if name is null
        // skip if position is null
        return core.getObjectManager().getRSObject(rsObject -> {
            String name = rsObject.getName();
            if (name == null || !name.equalsIgnoreCase("tithe patch")) {
                return false; // skip if name is null
            }
            WorldPosition objectPosition = rsObject.getWorldPosition();
            if (objectPosition == null) {
                core.log(PatchManager.class, "Patch position is null for object: " + rsObject);
                return false; // skip if position is null
            }
            return objectPosition.equals(patchPosition);
        });
    }

    public TitheFarm.Patch getPatchByPlayerPosition(WorldPosition worldPosition) {
        for (TitheFarm.Patch patch : PATCH_MAP.values()) {
            RSObject patchObject = getPatchObject(patch.getPosition());
            if (patchObject == null) {
                continue;
            }
            RectangleArea patchArea = patchObject.getObjectArea();
            if (com.osmb.script.farming.tithefarm.Utils.isCardinallyAdjacent(worldPosition, patchArea)) {
                return patch;
            }
        }
        return null; // no patch found at the given position
    }

    public void paintPatches(Canvas canvas) {
        for (Map.Entry<Integer, TitheFarm.Patch> entry : PATCH_MAP.entrySet()) {
            TitheFarm.Patch patch = entry.getValue();
            Integer patchIndex = entry.getKey();
            RSObject patchObject = getPatchObject(patch.getPosition());
            if (patchObject == null) {
                continue;
            }

            String status;
            Color color;
            if (patchIndex == focusedPatchIndex) {
                color = Color.YELLOW; // highlight the focused patch
                status = "Focused";
            } else if (patch.isDead()) {
                status = "Dead";
                color = Color.DARK_GRAY; // dead patches are gray
            } else if (patch.isWatered()) {
                status = "Watered";
                color = Color.BLUE; // harvested patches are blue
            } else if (patch.isHarvested()) {
                status = "Harvested";
                color = Color.GREEN; // harvested patches are green
            } else if (patch.getPlantTime() != -1) {
                status = "Planted";
                color = Color.magenta; // growing patches are orange
            } else {
                status = "Unplanted";
                color = Color.RED; // unplanted patches are red
            }
            Polygon objectPoly = patchObject.getConvexHull();
            if (objectPoly == null) {
                continue;
            }
            FontMetrics fontMetrics10 = canvas.getFontMetrics(ARIAL_10);
            canvas.fillPolygon(objectPoly, color.getRGB(), 0.3);
            canvas.drawPolygon(objectPoly, color.getRGB(), 1);
            Point center = objectPoly.getCenter();
            int y = center.y - 40;

            // Prepare all text lines
            String[] lines = {
                    "Index: " + patchIndex,
                    "Status: " + status,
                    "Current stage: " + patch.getCurrentStage(),
                    "Watered stage: " + patch.getWaterStage()
            };

            // Calculate max width and total height
            int maxWidth = 0, totalHeight = 0;
            for (String line : lines) {
                int w = fontMetrics10.stringWidth(line);
                if (w > maxWidth) maxWidth = w;
                totalHeight += fontMetrics10.getHeight();
            }

            // Border position and size
            int borderX = center.x - maxWidth / 2 - 4;
            int borderY = y + 15 - fontMetrics10.getAscent() - 4;
            int borderWidth = maxWidth + 8;
            int borderHeight = totalHeight + 8;

            // Draw border rectangle
            canvas.drawRect(borderX, borderY, borderWidth, borderHeight, color.getRGB(), 1);
            canvas.fillRect(borderX, borderY, borderWidth, borderHeight, Color.BLACK.getRGB(), 0.3);

            // Draw text lines
            int textY = y + 15;
            for (int i = 0; i < lines.length; i++) {
                canvas.drawText(lines[i], center.x - maxWidth / 2, textY, Color.WHITE.getRGB(), ARIAL_10);
                textY += fontMetrics10.getHeight();
            }
        }
    }

    public enum PlantMethod {
        ZIG_ZAG,
        SERPENTINE,
        RANDOM
    }

    public enum PatchRow {
        FIRST(X_POSITIONS[0], X_POSITIONS[1]),
        SECOND(X_POSITIONS[1], X_POSITIONS[2]),
        THIRD(X_POSITIONS[2], X_POSITIONS[3]),
        FOURTH(X_POSITIONS[3], X_POSITIONS[4]),
        FIFTH(X_POSITIONS[4], X_POSITIONS[0]);

        private final int leftXPosition;
        private final int rightXPosition;

        PatchRow(int leftXPosition, int rightXPosition) {
            this.leftXPosition = leftXPosition;
            this.rightXPosition = rightXPosition;
        }

        public static PatchRow getRowByPosition(WorldPosition worldPosition) {
            for (PatchRow row : PatchRow.values()) {
                if (worldPosition.getX() >= row.leftXPosition && worldPosition.getX() <= row.rightXPosition) {
                    return row;
                }
            }
            return null;
        }

        public int getLeftXPosition() {
            return leftXPosition;
        }

        public int getRightXPosition() {
            return rightXPosition;
        }

        public int getMiddleXPosition() {
            return (leftXPosition + rightXPosition) / 2;
        }
    }

    enum PlantDirection {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    static class StartingPatch {
        private final WorldPosition worldPosition;
        private final PlantDirection plantDirection;
        private final PatchRow startingPatchRow;

        public StartingPatch(WorldPosition worldPosition, PlantDirection plantDirection, PatchRow startingPatchRow) {
            this.worldPosition = worldPosition;
            this.plantDirection = plantDirection;
            this.startingPatchRow = startingPatchRow;
        }

        public PatchRow getStartingPatchRow() {
            return startingPatchRow;
        }

        public PlantDirection getPlantDirection() {
            return plantDirection;
        }

        public WorldPosition getWorldPosition() {
            return worldPosition;
        }

    }
}
