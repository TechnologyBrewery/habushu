package org.technologybrewery.habushu.migration.poetryv2migrations;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.technologybrewery.habushu.PoetryCommandHelperTestWrapper;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared context for BDD steps to verify whether the Poetry version
 * is at least 2.0.0 or not.
 */
public class PoetryV2MigrationContext {
    public static boolean isPoetryAtLeast2;

    public static void setPoetryAtLeast2(boolean mockIsPoetryVersionAtLeast2){
        isPoetryAtLeast2 = mockIsPoetryVersionAtLeast2;
    }

    public static boolean getIsPoetryAtLeast2(){
        return isPoetryAtLeast2;
    }

    @Before("@poetry-v2-migration")
    public void resetPoetryContext() {
        setPoetryAtLeast2(false);
    }

    @Given("the Poetry version is at least \"2.0.0\"")
    public void poetry_version_is_at_least_2_0_0() {
        boolean mockIsPoetryAtLeast2 = new PoetryCommandHelperTestWrapper(new File("."), "2.0.0").isPoetryVersionAtLeastMinimumVersion();
        assertTrue(mockIsPoetryAtLeast2, "Unexpected Poetry version found.");
        setPoetryAtLeast2(mockIsPoetryAtLeast2);
    }

    @Given("the Poetry version is less than \"2.0.0\"")
    public void poetry_version_is_less_than_2_0_0(){
        boolean mockIsPoetryAtLeast2 = new PoetryCommandHelperTestWrapper(new File("."), "1.6.1").isPoetryVersionAtLeastMinimumVersion();
        assertFalse(mockIsPoetryAtLeast2, "Unexpected Poetry version found.");
        setPoetryAtLeast2(mockIsPoetryAtLeast2);
    }
}
