package elocindev.tierify.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class CommandInit {

    private static final List<String> TIER_LIST = List.of("common", "uncommon", "rare", "epic", "legendary", "mythic");

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            dispatcher.register((Commands.literal("tiered").requires((serverCommandSource) -> {
                return serverCommandSource.hasPermission(3);
            })).then(Commands.literal("tier").then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("common").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 0);
            })).then(Commands.literal("uncommon").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 1);
            })).then(Commands.literal("rare").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 2);
            })).then(Commands.literal("epic").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 3);
            })).then(Commands.literal("legendary").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 4);
            })).then(Commands.literal("mythic").executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), 5);
            })))).then(Commands.literal("untier").then(Commands.argument("targets", EntityArgument.players()).executes((commandContext) -> {
                return executeCommand(commandContext.getSource(), EntityArgument.getPlayers(commandContext, "targets"), -1);
            }))));
        });
    }

    // 0: common; 1: uncommon; 2: rare; 3: epic; 4: legendary; 5: mythic
    private static int executeCommand(CommandSourceStack source, Collection<ServerPlayer> targets, int tier) {
        Iterator<ServerPlayer> var3 = targets.iterator();
        // loop over players
        while (var3.hasNext()) {
            ServerPlayer serverPlayerEntity = var3.next();
            ItemStack itemStack = serverPlayerEntity.getMainHandItem();

            if (itemStack.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.tiered.failed", serverPlayerEntity.getDisplayName()), true);
                continue;
            }

            if (tier == -1) {
                CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
                if (customData != null && customData.copyTag().contains(Tierify.NBT_SUBTAG_KEY)) {
                    ModifierUtils.removeItemStackAttribute(itemStack);

                    source.sendSuccess(() -> Component.translatable("commands.tiered.untier", itemStack.getHoverName().getString(), serverPlayerEntity.getDisplayName()), true);
                } else {
                    source.sendSuccess(() -> Component.translatable("commands.tiered.untier_failed", itemStack.getHoverName().getString(), serverPlayerEntity.getDisplayName()), true);
                }
            } else {
                ArrayList<ResourceLocation> potentialAttributes = new ArrayList<ResourceLocation>();
                Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
                    if (attribute.isValid(BuiltInRegistries.ITEM.getKey(itemStack.getItem()))) {
                        potentialAttributes.add(ResourceLocation.parse(attribute.getID()));
                    }
                });
                if (potentialAttributes.size() <= 0) {
                    source.sendSuccess(() -> Component.translatable("commands.tiered.tiering_failed", itemStack.getHoverName().getString(), serverPlayerEntity.getDisplayName()), true);
                    continue;
                } else {

                    List<ResourceLocation> potentialTier = new ArrayList<ResourceLocation>();
                    for (int i = 0; i < potentialAttributes.size(); i++) {
                        if (potentialAttributes.get(i).getPath().contains(TIER_LIST.get(tier))) {
                            if (TIER_LIST.get(tier).equals("common") && potentialAttributes.get(i).getPath().contains("uncommon")) {
                                continue;
                            }
                            potentialTier.add(potentialAttributes.get(i));
                        }
                    }

                    if (potentialTier.size() <= 0) {
                        source.sendSuccess(() -> Component.translatable("commands.tiered.tiering_failed", itemStack.getHoverName().getString(), serverPlayerEntity.getDisplayName()), true);
                        continue;
                    } else {

                        ModifierUtils.removeItemStackAttribute(itemStack);

                        ResourceLocation attribute = potentialTier.get(serverPlayerEntity.level().getRandom().nextInt(potentialTier.size()));
                        if (attribute != null) {
                            CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
                            CompoundTag root = customData != null ? customData.copyTag() : new CompoundTag();
                            root.put(Tierify.NBT_SUBTAG_KEY, new CompoundTag());
                            root.getCompound(Tierify.NBT_SUBTAG_KEY).putString(Tierify.NBT_SUBTAG_DATA_KEY, attribute.toString());
                            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

                            HashMap<String, Object> nbtMap = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(ResourceLocation.parse(attribute.toString())).getNbtValues();

                            // add durability nbt
                            List<AttributeTemplate> attributeList = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(ResourceLocation.parse(attribute.toString())).getAttributes();
                            for (int i = 0; i < attributeList.size(); i++)
                                if (attributeList.get(i).getAttributeTypeID().equals("tiered:generic.durable")) {
                                    if (nbtMap == null)
                                        nbtMap = new HashMap<String, Object>();
                                    nbtMap.put("durable", (double) Math.round(attributeList.get(i).getEntityAttributeModifier().amount() * 100.0) / 100.0);
                                    break;
                                }
                            // add nbtMap
                            if (nbtMap != null) {
                                CustomData data = itemStack.get(DataComponents.CUSTOM_DATA);
                                CompoundTag nbtCompound = data != null ? data.copyTag() : new CompoundTag();
                                for (HashMap.Entry<String, Object> entry : nbtMap.entrySet()) {
                                    String key = entry.getKey();
                                    Object value = entry.getValue();

                                    // json list will get read as ArrayList class
                                    // json map will get read as linkedtreemap
                                    // json integer is read by gson -> always double
                                    if (value instanceof String)
                                        nbtCompound.putString(key, (String) value);
                                    else if (value instanceof Boolean)
                                        nbtCompound.putBoolean(key, (boolean) value);
                                    else if (value instanceof Double) {
                                        if ((double) value % 1.0 < 0.0001D)
                                            nbtCompound.putInt(key, (int) Math.round((double) value));
                                        else
                                            nbtCompound.putDouble(key, Math.round((double) value * 100.0) / 100.0);
                                    }
                                }
                                itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbtCompound));
                            }
                            source.sendSuccess(() -> Component.translatable("commands.tiered.tier", itemStack.getHoverName().getString(), serverPlayerEntity.getDisplayName()), true);
                        }
                    }
                }
            }
        }
        return 1;
    }
}
