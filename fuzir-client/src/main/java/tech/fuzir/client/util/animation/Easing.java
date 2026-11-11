package tech.fuzir.client.util.animation;

import java.util.function.DoubleUnaryOperator;

/**
 * Easing curves operating on a normalized progress value in the range [0, 1].
 */
public enum Easing {
    LINEAR(t -> t),
    OUT_QUAD(t -> 1.0 - (1.0 - t) * (1.0 - t)),
    OUT_CUBIC(t -> 1.0 - Math.pow(1.0 - t, 3.0)),
    IN_OUT_CUBIC(t -> t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0),
    OUT_QUINT(t -> 1.0 - Math.pow(1.0 - t, 5.0)),
    OUT_EXPO(t -> t >= 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t)),
    OUT_BACK(t -> {
        double c1 = 1.70158;
        double c3 = c1 + 1.0;
        return 1.0 + c3 * Math.pow(t - 1.0, 3.0) + c1 * Math.pow(t - 1.0, 2.0);
    });

    private final DoubleUnaryOperator function;

    Easing(DoubleUnaryOperator function) {
        this.function = function;
    }

    public double apply(double progress) {
        if (progress <= 0.0) return 0.0;
        if (progress >= 1.0) return 1.0;
        return function.applyAsDouble(progress);
    }
}
