package committee.nova.mods.magneticraft.content.fluid;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;

/**
 * Applies one catalogue entry to Forge's common and client fluid contracts.
 */
public final class MagneticraftFluidType extends FluidType {
    private static final String DESCRIPTION_PREFIX = "fluid_type." + Magneticraft.MOD_ID + ".";

    private final FluidDefinition definition;

    public MagneticraftFluidType(FluidDefinition definition) {
        super(Properties.create()
                .descriptionId(definition.translationKey())
                .temperature(definition.temperatureKelvin())
                .density(definition.density())
                .viscosity(definition.viscosity()));
        this.definition = definition;
    }

    public boolean isGaseous() {
        return definition.isGaseous();
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        // FluidType invokes this callback from its constructor, before this
        // subclass can assign definition. The description id is already
        // initialized by FluidType at that point, so it is the safe source.
        FluidDefinition clientDefinition = definition(getDescriptionId());
        ResourceLocation still = texture(clientDefinition, "_still");
        ResourceLocation flowing = texture(clientDefinition, "_flow");
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }

            @Override
            public int getTintColor() {
                return clientDefinition.tintColor();
            }
        });
    }

    static ResourceLocation texture(String descriptionId, String suffix) {
        return texture(definition(descriptionId), suffix);
    }

    private static FluidDefinition definition(String descriptionId) {
        if (!descriptionId.startsWith(DESCRIPTION_PREFIX)) {
            throw new IllegalArgumentException("Unexpected Magneticraft fluid description id: " + descriptionId);
        }
        return FluidDefinition.byId(descriptionId.substring(DESCRIPTION_PREFIX.length()));
    }

    private static ResourceLocation texture(FluidDefinition definition, String suffix) {
        return Magneticraft.id("fluid/" + definition.textureId() + suffix);
    }
}
