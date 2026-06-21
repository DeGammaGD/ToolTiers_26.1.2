package elocindev.tierify.screen;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.LevelEvent;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.TieredItemTags;
import elocindev.tierify.Tierify;
import elocindev.tierify.network.TieredServerPacket;

public class ReforgeScreenHandler extends AbstractContainerMenu {

    private final Container inventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ReforgeScreenHandler.this.slotsChanged(this);
        }
    };

    private final ContainerLevelAccess context;
    private final Player player;
    private boolean reforgeReady;
    private BlockPos pos;

    public ReforgeScreenHandler(int syncId, Inventory playerInventory, ContainerLevelAccess context) {
        super(Tierify.REFORGE_SCREEN_HANDLER_TYPE, syncId);

        this.context = context;
        this.player = playerInventory.player;
        this.addSlot(new Slot(this.inventory, 0, 45, 47));
        this.addSlot(new Slot(this.inventory, 1, 80, 34));
        this.addSlot(new Slot(this.inventory, 2, 115, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidAddition(stack);
            }
        });

        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        this.context.execute((world, pos) -> {
            ReforgeScreenHandler.this.setPos(pos);
        });
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (!player.level().isClientSide() && inventory == this.inventory) {
            this.updateResult();
        }

    }

    private void updateResult() {
        if (this.getSlot(0).hasItem() && this.getSlot(1).hasItem() && this.getSlot(2).hasItem()) {
            Item item = this.getSlot(1).getItem().getItem();
            if (ModifierUtils.getRandomAttributeIDFor(null, item, false) != null && !this.getSlot(1).getItem().isDamaged()) {

                List<Item> items = Tierify.REFORGE_DATA_LOADER.getReforgeBaseItems(item);
                ItemStack baseItem = this.getSlot(0).getItem();
                Tierify.LOGGER.info("[TierifyDebug][Reforge] input={} available_reforges={}", BuiltInRegistries.ITEM.getKey(item), items.stream().map(it -> BuiltInRegistries.ITEM.getKey(it).toString()).toList());
                if (!items.isEmpty()) {
                    this.reforgeReady = items.stream().anyMatch(it -> it == baseItem.getItem());
                } else if (item instanceof TieredItem toolItem) {
                    this.reforgeReady = toolItem.getTier().getRepairIngredient().test(baseItem);
                } else if (item instanceof ArmorItem armorItem) {
                    this.reforgeReady = armorItem.getMaterial().value().repairIngredient().get().test(baseItem);
                } else {
                    this.reforgeReady = baseItem.is(TieredItemTags.REFORGE_BASE_ITEM);
                }
                Tierify.LOGGER.info("Reforge check for {} with base {} -> {}", BuiltInRegistries.ITEM.getKey(item), BuiltInRegistries.ITEM.getKey(baseItem.getItem()), this.reforgeReady);
            } else {
                this.reforgeReady = false;
            }
        } else {
            this.reforgeReady = false;
        }
        // if (this.reforgeReady && !Tierify.CONFIG.uniqueReforge && ModifierUtils.getAttributeID(this.getSlot(1).getStack()) != null
        //         && ModifierUtils.getAttributeID(this.getSlot(1).getStack()).getPath().contains("unique")) {
        //     this.reforgeReady = false;
        // }
        TieredServerPacket.writeS2CReforgeReadyPacket((ServerPlayer) player, !this.reforgeReady);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.context.execute((world, pos) -> this.clearContainer(player, this.inventory));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.context.evaluate((world, pos) -> {
            return player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot) this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index == 1) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index == 0 || index == 2) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 3 && index < 39) {
                if (isValidAddition(itemStack) && !this.moveItemStackTo(itemStack2, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }
                if (this.getSlot(1).hasItem()) {
                    Item item = this.getSlot(1).getItem().getItem();
                    if (item instanceof TieredItem toolItem && toolItem.getTier().getRepairIngredient().test(itemStack) && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    if (item instanceof ArmorItem armorItem && armorItem.getMaterial().value().repairIngredient().get().test(itemStack)
                            && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    if (itemStack.is(TieredItemTags.REFORGE_BASE_ITEM) && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    List<Item> items = Tierify.REFORGE_DATA_LOADER.getReforgeBaseItems(item);
                    if (items.stream().anyMatch(it -> it == itemStack2.copy().getItem()) && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                if (ModifierUtils.getRandomAttributeIDFor(null, itemStack.getItem(), false) != null && !this.moveItemStackTo(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemStack2);
        }
        return itemStack;
    }

    public void reforge() {
        ItemStack itemStack = this.getSlot(1).getItem();
        ResourceLocation beforeTier = ModifierUtils.getAttributeID(itemStack);
        Tierify.LOGGER.info("Applying reforge to {} using {}", BuiltInRegistries.ITEM.getKey(itemStack.getItem()), BuiltInRegistries.ITEM.getKey(this.getSlot(2).getItem().getItem()));
        Tierify.LOGGER.info("Reforge before data -> {}", itemStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA));
        Tierify.LOGGER.info("[TierifyDebug][Reforge] input_item={} input_tier={} reforge_material={}", BuiltInRegistries.ITEM.getKey(itemStack.getItem()), beforeTier, BuiltInRegistries.ITEM.getKey(this.getSlot(2).getItem().getItem()));
        ModifierUtils.removeItemStackAttribute(itemStack);
        ModifierUtils.setItemStackAttribute(player, itemStack, true, this.getSlot(2).getItem());
        ResourceLocation afterTier = ModifierUtils.getAttributeID(itemStack);
        Tierify.LOGGER.info("Reforge after data -> {}", itemStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA));
        Tierify.LOGGER.info("[TierifyDebug][Reforge] result_item={} result_tier={}", BuiltInRegistries.ITEM.getKey(itemStack.getItem()), afterTier);

        if (BuiltInRegistries.SOUND_EVENT.get(getReforgeSound(ModifierUtils.getAttributeID(itemStack))) !=null) {
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(getReforgeSound(ModifierUtils.getAttributeID(itemStack)));
            this.context.execute((world, pos) -> {
                if (!world.isClientSide) {
                    world.playSound(null, pos, soundEvent, SoundSource.BLOCKS, 1f, 1f);
                }
            });
        }

        this.decrementStack(0);
        this.decrementStack(2);
        this.context.execute((world, pos) -> world.levelEvent(LevelEvent.SOUND_ANVIL_USED, (BlockPos) pos, 0));
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    private void decrementStack(int slot) {
        ItemStack itemStack = this.inventory.getItem(slot);
        itemStack.shrink(1);
        this.inventory.setItem(slot, itemStack);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.inventory && super.canTakeItemForPickAll(stack, slot);
    }

    public static boolean isValidAddition(ItemStack stack) {
        return stack.is(TieredItemTags.TIER_1_ITEM) || stack.is(TieredItemTags.TIER_2_ITEM) || stack.is(TieredItemTags.TIER_3_ITEM);
    }

    public static ResourceLocation getReforgeSound(ResourceLocation identifier) {
        // plays the corresponding upgrade sound effect for the item tier
        String tier = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(identifier).getID();
        if (tier == null) return null;
        String soundId = null;

        String tierName = Component.translatable(tier + ".label").getString().toLowerCase();
        soundId = "reforge_sound_"+tierName;

        return ResourceLocation.fromNamespaceAndPath("tiered",soundId);
    }


}
