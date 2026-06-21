package elocindev.tierify.screen.client;

import java.util.*;

import com.mojang.blaze3d.systems.RenderSystem;

import draylar.tiered.api.TieredItemTags;
import elocindev.tierify.Tierify;
import elocindev.tierify.network.TieredClientPacket;
import elocindev.tierify.screen.ReforgeScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.libz.api.Tab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;

@Environment(EnvType.CLIENT)
public class ReforgeScreen extends AbstractContainerScreen<ReforgeScreenHandler> implements ContainerListener, Tab {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("tiered", "textures/gui/reforging_screen.png");
    public ReforgeScreen.ReforgeButton reforgeButton;
    private ItemStack last;
    private List<Item> baseItems;

    public ReforgeScreen(ReforgeScreenHandler handler, Inventory playerInventory, Component title) {
        super(handler, playerInventory, title);
        this.titleLabelX = 60;
    }

    @Override
    protected void init() {
        super.init();
        ((ReforgeScreenHandler) this.menu).addSlotListener(this);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.reforgeButton = (ReforgeScreen.ReforgeButton) this.addRenderableWidget(new ReforgeScreen.ReforgeButton(i + 79, j + 56, (button) -> {
            if (button instanceof ReforgeScreen.ReforgeButton && !((ReforgeScreen.ReforgeButton) button).disabled)
                TieredClientPacket.writeC2SReforgePacket();
        }));
    }

    @Override
    public void removed() {
        super.removed();
        ((ReforgeScreenHandler) this.menu).removeSlotListener(this);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        RenderSystem.disableBlend();

        this.renderTooltip(context, mouseX, mouseY);

        if (this.isHovering(79, 56, 18, 18, (double) mouseX, (double) mouseY)) {
            ItemStack itemStack = this.getMenu().getSlot(1).getItem();
            if (itemStack == null || itemStack.isEmpty()) {
                baseItems = Collections.emptyList();
            } else {
                if (itemStack != last) {
                    last = itemStack;
                    baseItems = new ArrayList<Item>();
                    List<Item> items = Tierify.REFORGE_DATA_LOADER.getReforgeBaseItems(itemStack.getItem());
                    if (!items.isEmpty()) {
                        baseItems.addAll(items);
                    } else if (itemStack.getItem() instanceof TieredItem toolItem) {
                        toolItem.getTier().getRepairIngredient().getItems();
                        for (int i = 0; i < toolItem.getTier().getRepairIngredient().getItems().length; i++) {
                            baseItems.add(toolItem.getTier().getRepairIngredient().getItems()[i].getItem());
                        }
                    } else if (itemStack.getItem() instanceof ArmorItem armorItem) {
                        var repairIngredient = armorItem.getMaterial().value().repairIngredient().get();
                        if (repairIngredient != null) {
                            for (int i = 0; i < repairIngredient.getItems().length; i++) {
                                baseItems.add(repairIngredient.getItems()[i].getItem());
                            }
                        }
                    } else {
                        for (Holder<Item> itemRegistryEntry : BuiltInRegistries.ITEM.getOrCreateTag(TieredItemTags.REFORGE_BASE_ITEM)) {
                            baseItems.add(itemRegistryEntry.value());
                        }
                    }
                }
            }
            List<Component> tooltip = new ArrayList<Component>();
            if (!baseItems.isEmpty()) {
                ItemStack ingredient = this.getMenu().getSlot(0).getItem();
                if (ingredient != null && !ingredient.isEmpty() && baseItems.contains(ingredient.getItem())) {
                } else {
                    tooltip.add(Component.translatable("screen.tiered.reforge_ingredient"));
                    for (Item item : baseItems) {
                        tooltip.add(item.getDescription());
                    }
                }
            }
            if (itemStack != null && itemStack.isDamageableItem() && itemStack.isDamaged()) {
                tooltip.add(Component.translatable("screen.tiered.reforge_damaged"));
            }
            if (!tooltip.isEmpty()) {
                context.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }
        // if (!Tierify.CONFIG.mythicReforge && !this.getScreenHandler().getSlot(1).getStack().isEmpty() && ModifierUtils.getAttributeID(this.getScreenHandler().getSlot(1).getStack()) != null
        //         && ModifierUtils.getAttributeID(this.getScreenHandler().getSlot(1).getStack()).getPath().contains("mythic")) {
        //     context.drawTexture(TEXTURE, this.x + 74, this.y + 29, 0, 166, 28, 26);
        // }
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        context.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void dataChanged(AbstractContainerMenu handler, int property, int value) {
    }

    @Override
    public void slotChanged(AbstractContainerMenu handler, int slotId, ItemStack stack) {
    }

    @Override
    public Class<?> getParentScreenClass() {
        return AnvilScreen.class;
    }

    public class ReforgeButton extends Button {
        private boolean disabled;

        public ReforgeButton(int x, int y, Button.OnPress onPress) {
            super(x, y, 18, 18, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.disabled = true;
        }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            int j = 176;
            if (this.disabled) {
                j += this.width * 2;
            } else if (this.isHovered()) {
                j += this.width;
            }
            context.blit(TEXTURE, this.getX(), this.getY(), j, 0, this.width, this.height);
        }

        public void setDisabled(boolean disable) {
            this.disabled = disable;
        }

    }

}
