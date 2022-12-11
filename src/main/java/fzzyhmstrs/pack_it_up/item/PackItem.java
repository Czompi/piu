package fzzyhmstrs.pack_it_up.item;

import fzzyhmstrs.pack_it_up.PIU;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.block.OreBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class PackItem extends Item {

    public PackItem(Settings settings, ModuleTier tier, PackItem.StackPredicate stackPredicate) {
        super(settings);
        this.tier = tier;
        this.stackPredicate = stackPredicate;
    }

    private static final String INVENTORY = "pack_inventory";

    private final ModuleTier tier;
    private final PackItem.StackPredicate stackPredicate;

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable(stackPredicate.translationKey).formatted(Formatting.ITALIC));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            world.playSound(user,user.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.MASTER,0.5f,1.0f);
            return TypedActionResult.fail(stack);
        }
        Inventory inventory;
        if (tier == ModuleTier.ENDER){
            inventory = user.getEnderChestInventory();
        } else {
            inventory = getInventory(stack);
        }

        user.openHandledScreen(new PackScreenHandlerFactory(inventory, tier, stack, hand));
        return super.use(world, user, hand);
    }

    public static void saveInventory(ItemStack stack, PackInventory inventory){
        NbtCompound stackNbt = stack.getOrCreateNbt();
        NbtCompound inventoryNbt = new NbtCompound();
        inventory.toNbt(inventoryNbt);
        stackNbt.put(INVENTORY,stackNbt);
    }

    public PackInventory getInventory(ItemStack stack){
        NbtCompound nbt = stack.getNbt();
        if (nbt == null){
            return new PackInventory(tier.slots,stackPredicate);
        }
        if (!nbt.contains(INVENTORY)){
            return new PackInventory(tier.slots,stackPredicate);
        }
        NbtCompound packInv = nbt.getCompound(INVENTORY);
        return PackInventory.fromNbt(tier.slots,packInv);
    }


    public enum StackPredicate implements Predicate<ItemStack>{
        ANY(stack -> true,"pack_it_up.predicate.any"),
        BLOCK(stack -> stack.getItem() instanceof BlockItem,"pack_it_up.predicate.block"),
        FOOD(stack -> stack.isIn(ConventionalItemTags.FOODS),"pack_it_up.predicate.food"),
        PLANTS(stack-> stack.isIn(PIU.PLANT_ITEMS), "pack_it_up.predicate.plants"),
        TOOL(ItemStack::isDamageable,"pack_it_up.predicate.tool"),
        MAGIC(stack -> stack.getItem() instanceof EnchantedBookItem || stack.getItem() instanceof PotionItem || stack.getItem() instanceof TippedArrowItem,"pack_it_up.predicate.magic"),
        ORE(stack-> {
            boolean bl1 = stack.isIn(ConventionalItemTags.ORES);
            boolean bl2 = stack.isIn(PIU.GEMS);
            if (stack.getItem() instanceof BlockItem blockItem){
                return bl1 || bl2 || blockItem.getBlock() instanceof OreBlock;
            } else {
                return bl1 || bl2;
            }
        },"pack_it_up.predicate.ore");

        private static final String PREDICATE = "stack_predicate";
        private final Predicate<ItemStack> predicate;
        public final String translationKey;

        StackPredicate(Predicate<ItemStack> predicate, String key){
            this.predicate = predicate;
            this.translationKey = key;
        }

        @Override
        public boolean test(ItemStack stack) {
            return predicate.test(stack);
        }

        public static StackPredicate fromNbt(NbtCompound nbt){
            if (!nbt.contains(PREDICATE)){
                return ANY;
            } else {
                String predicateId = nbt.getString(PREDICATE);
                StackPredicate predicate;
                try {
                    predicate = StackPredicate.valueOf(predicateId);
                } catch (Exception e){
                    predicate = ANY;
                }
                return predicate;
            }
        }

        public void toNbt(NbtCompound nbt){
            nbt.putString(PREDICATE,this.name());
        }
    }

    public enum ModuleTier{
        PACK(0,2, 18),
        SPECIAL(1,3,27),
        BIG_PACK(2,4,36),
        TOOL(3,6,54),
        ENDER(4,3,27),
        NETHERITE(5,6,54);

        public final int height;
        public final int slots;
        public final int id;

        ModuleTier(int id,int height, int slots){
            this.id = id;
            this.height = height;
            this.slots = slots;
        }
    }

}
