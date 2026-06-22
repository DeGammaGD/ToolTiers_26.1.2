package draylar.tiered.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class BorderTemplate {

    private final int index;
    private final String texture;
    private final Identifier identifier;
    private final int startGradient;
    private final int endGradient;
    private final int backgroundGradient;
    private final List<String> decider;
    private List<ItemStack> stacks = new ArrayList<ItemStack>();

    public BorderTemplate(int index, String texture, int startGradient, int endGradient, int backgroundGradient, List<String> decider) {
        this.index = index;
        this.texture = texture;
        this.identifier = resolveTextureIdentifier(texture);
        this.startGradient = startGradient;
        this.endGradient = endGradient;
        this.backgroundGradient = backgroundGradient;
        this.decider = new ArrayList<String>();
        this.decider.addAll(decider);
    }

    public int getIndex() {
        return this.index;
    }

    public String getTexture() {
        return this.texture;
    }

    public Identifier getIdentifier() {
        return this.identifier;
    }

    public int getStartGradient() {
        return this.startGradient;
    }

    public int getEndGradient() {
        return this.endGradient;
    }

    public int getBackgroundGradient() {
        return this.backgroundGradient;
    }

    public List<String> getDecider() {
        return this.decider;
    }

    public boolean containsDecider(String string) {
        if (this.decider.contains(string))
            return true;
        if (this.decider.contains("{Tier:\"" + string + "\"}"))
            return true;

        for (String configuredDecider : this.decider) {
            // Support wildcard-style pack entries, e.g. tiered:common_*.
            if (configuredDecider.endsWith("*") && string.startsWith(configuredDecider.substring(0, configuredDecider.length() - 1))) {
                return true;
            }
        }

        // Generic quality fallback: match tiered:common_<any-category>_<n> to common template
        // without hard-coding tool/armor/elytra categories.
        String incomingQualityKey = getTierQualityKey(string);
        if (incomingQualityKey != null) {
            for (String configuredDecider : this.decider) {
                String configuredQualityKey = getTierQualityKey(configuredDecider);
                if (incomingQualityKey.equals(configuredQualityKey)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addStack(ItemStack itemStack) {
        if (!this.stacks.contains(itemStack))
            this.stacks.add(itemStack);
    }

    public boolean containsStack(ItemStack itemStack) {
        return this.stacks.contains(itemStack);
    }

    private static Identifier resolveTextureIdentifier(String texture) {
        String normalized = texture.trim();
        if (normalized.contains(":")) {
            Identifier parsed = Identifier.parse(normalized);
            String path = parsed.getPath();
            if (!path.startsWith("textures/")) {
                path = "textures/gui/" + path;
            }
            if (!path.endsWith(".png")) {
                path = path + ".png";
            }
            return Identifier.fromNamespaceAndPath(parsed.getNamespace(), path);
        }

        String path = normalized;
        if (!path.startsWith("textures/")) {
            path = "textures/gui/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return Identifier.fromNamespaceAndPath("tiered", path);
    }

    private static String getTierQualityKey(String idLike) {
        if (idLike == null || idLike.isEmpty()) {
            return null;
        }

        int namespaceSplit = idLike.indexOf(':');
        if (namespaceSplit <= 0 || namespaceSplit >= idLike.length() - 1) {
            return null;
        }

        String namespace = idLike.substring(0, namespaceSplit);
        String path = idLike.substring(namespaceSplit + 1);

        int qualitySplit = path.indexOf('_');
        if (qualitySplit <= 0) {
            return null;
        }

        String quality = path.substring(0, qualitySplit);
        return namespace + ":" + quality;
    }

}
