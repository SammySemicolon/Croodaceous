package coda.croodaceous.registry;

import coda.croodaceous.CroodaceousMod;
import coda.croodaceous.common.world.tree.CoolerTreeFeature;
import coda.croodaceous.common.world.tree.DesertBaobabFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CEFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, CroodaceousMod.MOD_ID);

    //public static final RegistryObject<HoodooFeature> HOODOO = FEATURES.register("hoodoo", HoodooFeature::new);
    public static final RegistryObject<DesertBaobabFeature> DESERT_BAOBAB = FEATURES.register("desert_baobab", DesertBaobabFeature::new);
    public static final RegistryObject<CoolerTreeFeature> THE_OTHER_ONE = FEATURES.register("cooler_tree", CoolerTreeFeature::new);
    //public static final RegistryObject<Feature<NoneFeatureConfiguration>> BONE_PILE = FEATURES.register("bone_pile", BonePileFeature::new);

}