package com.osmb.script.furnace;

import java.util.List;

public interface Product {

    int getItemID();

    String getProductName();

    // non-primitive to allow null values for no mould
    List<Integer> getMouldIDs();

    List<Resource> getResources();
}
