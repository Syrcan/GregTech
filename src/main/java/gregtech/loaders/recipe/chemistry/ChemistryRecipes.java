package gregtech.loaders.recipe.chemistry;

import gregtech.api.recipes.ingredients.IntCircuitIngredient;
import net.minecraft.init.Items;

import static gregtech.api.GTValues.*;
import static gregtech.api.recipes.RecipeMaps.*;
import static gregtech.api.unification.material.Materials.*;
import static gregtech.api.unification.ore.OrePrefix.dust;
import static gregtech.api.unification.ore.OrePrefix.ingot;

public class ChemistryRecipes {

    public static void init() {

        CrackingRecipes.init();
        DistillationRecipes.init();
        SeparationRecipes.init();
        MixerRecipes.init();
        BrewingRecipes.init();
        ChemicalBathRecipes.init();
        ReactorRecipes.init();
        PolymerRecipes.init();
        LCRCombined.init();
        GrowthMediumRecipes.init();
        NuclearRecipes.init();
        FuelRecipeChains.init();
        GemSlurryRecipes.init();
        PlatGroupMetalsRecipes.init();
        NaquadahRecipes.init();


        // A Few Random Recipes
        FLUID_HEATER_RECIPES.recipeBuilder()
                .circuitMeta(1)
                .fluidInputs(Acetone.getFluid(100))
                .fluidOutputs(Ethenone.getFluid(100))
                .duration(16).EUt(VA[LV]).buildAndRegister();

        FLUID_HEATER_RECIPES.recipeBuilder()
                .circuitMeta(1)
                .fluidInputs(DissolvedCalciumAcetate.getFluid(200))
                .fluidOutputs(Acetone.getFluid(200))
                .duration(16).EUt(VA[LV]).buildAndRegister();

        VACUUM_RECIPES.recipeBuilder()
                .fluidInputs(Water.getFluid(1000))
                .fluidOutputs(Ice.getFluid(1000))
                .duration(50).EUt(VA[LV]).buildAndRegister();

        VACUUM_RECIPES.recipeBuilder()
                .fluidInputs(Air.getFluid(4000))
                .fluidOutputs(LiquidAir.getFluid(4000))
                .duration(80).EUt(VA[HV]).buildAndRegister();

        VACUUM_RECIPES.recipeBuilder()
                .fluidInputs(NetherAir.getFluid(4000))
                .fluidOutputs(LiquidNetherAir.getFluid(4000))
                .duration(80).EUt(VA[EV]).buildAndRegister();

        VACUUM_RECIPES.recipeBuilder()
                .fluidInputs(EnderAir.getFluid(4000))
                .fluidOutputs(LiquidEnderAir.getFluid(4000))
                .duration(80).EUt(VA[IV]).buildAndRegister();

        BLAST_RECIPES.recipeBuilder()
                .input(dust, FerriteMixture)
                .fluidInputs(Oxygen.getFluid(2000))
                .output(ingot, NickelZincFerrite)
                .blastFurnaceTemp(1500)
                .duration(400).EUt(VA[MV]).buildAndRegister();

        FERMENTING_RECIPES.recipeBuilder()
                .fluidInputs(Biomass.getFluid(100))
                .fluidOutputs(FermentedBiomass.getFluid(100))
                .duration(150).EUt(2).buildAndRegister();

        WIREMILL_RECIPES.recipeBuilder()
                .input(ingot, Polycaprolactam)
                .output(Items.STRING, 32)
                .duration(80).EUt(48).buildAndRegister();

        BLAST_RECIPES.recipeBuilder()
                .input(Items.REDSTONE)
                .input(ingot, Copper)
                .output(ingot, RedAlloy, 2)
                .blastFurnaceTemp(1200)
                .duration(884).EUt(VA[MV]).buildAndRegister();

        BLAST_RECIPES.recipeBuilder()
                .input(Items.REDSTONE)
                .input(dust, Copper)
                .output(ingot, RedAlloy, 2)
                .blastFurnaceTemp(1200)
                .duration(884).EUt(VA[MV]).buildAndRegister();

        GAS_COLLECTOR_RECIPES.recipeBuilder()
                .notConsumable(new IntCircuitIngredient(1))
                .fluidOutputs(Air.getFluid(10000))
                .dimension(0)
                .duration(200).EUt(16).buildAndRegister();

        GAS_COLLECTOR_RECIPES.recipeBuilder()
                .notConsumable(new IntCircuitIngredient(2))
                .fluidOutputs(NetherAir.getFluid(10000))
                .dimension(-1)
                .duration(200).EUt(64).buildAndRegister();

        GAS_COLLECTOR_RECIPES.recipeBuilder()
                .notConsumable(new IntCircuitIngredient(3))
                .fluidOutputs(EnderAir.getFluid(10000))
                .dimension(1)
                .duration(200).EUt(256).buildAndRegister();
    }
}
