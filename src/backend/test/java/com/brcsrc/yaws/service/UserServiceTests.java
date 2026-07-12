package com.brcsrc.yaws.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class UserServiceTests {
    // the UserService should get coverage from UserControllerTests, but since password validation
    // has more cases where things can go wrong, I am using this test class here to validate

    @Test
    public void passwordMeetsComplexityRequirementsReturnsTrueOnValidPassword() {
        String testPassword = "gH1@#oKl2ff1";
        boolean meetsComplexityRequirements = UserService.passwordMeetsComplexityRequirements(testPassword);
        assertTrue(meetsComplexityRequirements);
    }

    @Test
    public void passwordMeetsComplexityRequirementsReturnsFalseOnInvalidPassword() {
        String noLowerCaseChars = "ABCDEFG1234%$";
        String noUpperCaseChars = "abcdefg1234%$";
        String noNumbers = "abcdefgABCDEFG$#";
        String noSpecialChars = "ABCDEFGabcdefg1234";
        String tooShort = "ABCabc123%!";

        var invalidPasswords = List.of(
               noLowerCaseChars,
               noUpperCaseChars,
               noNumbers,
               noSpecialChars,
                tooShort
        );
        for (String password : invalidPasswords) {
            assertFalse(UserService.passwordMeetsComplexityRequirements(password));
        }
    }
}
