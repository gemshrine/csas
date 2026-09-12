package com.dev.csas.control;

public record ControlResult(Integer numericScore, PassFail passFail) {

    public static ControlResult numeric(int score) {
        return new ControlResult(score, null);
    }

    public static ControlResult passFail(PassFail result) {
        return new ControlResult(null, result);
    }
}
