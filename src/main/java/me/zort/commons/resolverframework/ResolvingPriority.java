package me.zort.commons.resolverframework;

public enum ResolvingPriority {

    ROOT_WORK(5),
    HIGHEST(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private int code;

    ResolvingPriority(int code) {
        this.code = code;
    }

    protected int getCode() {
        return code;
    }

}
