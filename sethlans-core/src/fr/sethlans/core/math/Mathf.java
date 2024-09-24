package fr.sethlans.core.math;

import fr.alchemy.utilities.Validator;

public class Mathf {

    public static int log2(int value) {
        Validator.positive(value, "The value must be strictly positive!");
        var result = 31 - Integer.numberOfLeadingZeros(value);
        return result;
    }
}
