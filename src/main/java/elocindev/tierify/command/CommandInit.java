package elocindev.tierify.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@SuppressWarnings({"null"})
public class CommandInit {

    private static final List<String> TIER_LIST = List.of("common", "uncommon", "rare", "epic", "legendary", "mythic");

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            dispatcher.register((Commands.literal("tiered").requires((serverCommandSource) -> {
                return serverCommandSource.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
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
                ArrayList<Identifier> potentialAttributes = new ArrayList<Identifier>();
                Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
                    if (attribute.isValid(BuiltInRegistries.ITEM.getKey(itemStack.getItem()))) {
                        potentialAttributes.add(Identifier.parse(attribute.getID()));
                    }
                });
                if (potentialAttributes.size() <= 0) {
                    source.sendSuccess(() -> Component.translatable("commands.tiered.tiering_failed", itemStack.getHoverName().getString(), serverPlayerEntity.getDisplayName()), true);
                    continue;
                } else {

                    List<Identifier> potentialTier = new ArrayList<Identifier>();
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

                        Identifier attribute = potentialTier.get(serverPlayerEntity.level().getRandom().nextInt(potentialTier.size()));
                        if (attribute != null) {
                            ModifierUtils.setTier(itemStack, attribute);
                            int appliedCount = ModifierUtils.applyTierAttributes(itemStack);
                            if (appliedCount == 0) {
                                Tierify.LOGGER.warn("Command tier assignment generated zero modifiers for item={} tier={}", BuiltInRegistries.ITEM.getKey(itemStack.getItem()), attribute);
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
