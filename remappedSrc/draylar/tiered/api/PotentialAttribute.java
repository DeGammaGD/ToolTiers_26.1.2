package draylar.tiered.api;

import java.util.HashMap;
import java.util.List;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class PotentialAttribute {

    private final String id;
    private final List<ItemVerifier> verifiers;
    private final int weight;
    private final Style style;
    private final List<AttributeTemplate> attributes;
    private final HashMap<String, Object> nbtValues;

    public PotentialAttribute(String id, List<ItemVerifier> verifiers, int weight, Style style, List<AttributeTemplate> attributes, HashMap<String, Object> nbtValues) {
        this.id = id;
        this.verifiers = verifiers;
        this.style = style;
        this.attributes = attributes;
        this.weight = weight;
        this.nbtValues = nbtValues;
    }

    public String getID() {
        return id;
    }

    public List<ItemVerifier> getVerifiers() {
        return verifiers;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isValid(ResourceLocation id) {
        for (ItemVerifier verifier : verifiers) {
            if (verifier.isValid(id))
                return true;
        }

        return false;
    }

    public Style getStyle() {
        return style;
    }

    public List<AttributeTemplate> getAttributes() {
        return attributes;
    }

    @Nullable
    public HashMap<String, Object> getNbtValues() {
        return nbtValues;
    }

}
