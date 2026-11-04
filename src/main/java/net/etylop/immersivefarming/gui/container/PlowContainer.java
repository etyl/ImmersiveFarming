package net.etylop.immersivefarming.gui.container;

import net.etylop.immersivefarming.entity.AbstractDrawnInventoryEntity;
import net.etylop.immersivefarming.gui.IFMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;

public final class PlowContainer extends CartContainer {
    public PlowContainer(final int id, final Inventory playerInv, final FriendlyByteBuf buf) {
        this(id, playerInv, (AbstractDrawnInventoryEntity) playerInv.player.level().getEntity(buf.readInt()));
    }

    public PlowContainer(final int id, final Inventory playerInv, final AbstractDrawnInventoryEntity cart) {
        super(IFMenuTypes.PLOW_CART.get(), id, cart);
        this.addSlot(new SlotItemHandler(this.cartInv, 0, 57, 24));
        this.addSlot(new SlotItemHandler(this.cartInv, 1, 80, 24));
        this.addSlot(new SlotItemHandler(this.cartInv, 2, 103, 24));

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInv, k, 8 + k * 18, 142));
        }
    }

}
