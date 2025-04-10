package com.osmb.script.fletching.method;

public enum MethodType {
    STRING_BOWS("String bows"), CUT_LOGS("Cut logs"), ARROW_TIPS("Arrow tips"), HEADLESS_ARROWS("Headless arrows"),;

    private final String name;

    MethodType(String name) {
        this.name = name;
    }
}
