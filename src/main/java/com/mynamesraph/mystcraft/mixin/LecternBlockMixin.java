package com.mynamesraph.mystcraft.mixin;

import com.mojang.logging.LogUtils;
import com.mynamesraph.mystcraft.component.LocationDisplayComponent;
import com.mynamesraph.mystcraft.item.LinkingBookItem;
import com.mynamesraph.mystcraft.registry.MystcraftComponents;
import com.mynamesraph.mystcraft.registry.MystcraftItems;
import com.mynamesraph.mystcraft.ui.menu.LinkingBookMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternBlock.class)
public class LecternBlockMixin extends Block {
    @Shadow @Final public static BooleanProperty HAS_BOOK;
    @Unique
    private static final BooleanProperty IS_MYSTCRAFT_BOOK = BooleanProperty.create("is_mystcraft_book");

    public LecternBlockMixin(Properties properties) {
        super(properties);
    }


    @Inject(at = @At("RETURN"), method = "<init>")
    protected  void init(BlockBehaviour.Properties properties, CallbackInfo ci) {
        this.registerDefaultState(
                this.defaultBlockState().setValue(IS_MYSTCRAFT_BOOK,false)
        );
    }

    @Inject(at= @At(value = "HEAD"), method = "createBlockStateDefinition")
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(IS_MYSTCRAFT_BOOK);
    }

    @Inject(at= @At("RETURN"), method = "placeBook")
    private static void placeBook(LivingEntity entity, Level level, BlockPos pos, BlockState state, ItemStack stack, CallbackInfo ci) {
        if(level.getBlockEntity(pos) instanceof LecternBlockEntity blockEntity) {
            boolean isMystcraftBook = blockEntity.getBook().is(MystcraftItems.INSTANCE.getLINKING_BOOK());
            level.setBlockAndUpdate(
                    pos,
                    level.getBlockState(pos).setValue(IS_MYSTCRAFT_BOOK,isMystcraftBook)
            );
        }
    }

    @Inject(at= @At("HEAD"), method = "useWithoutItem", cancellable = true)
    protected void useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (state.getValue(HAS_BOOK) && state.getValue(IS_MYSTCRAFT_BOOK)) {
            if (!level.isClientSide) {
                mystcraft$openScreen(level,pos,player);
            }

            cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
        }
    }


    @Unique
    protected void mystcraft$openScreen(Level level, BlockPos pos, Player serverPlayer) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LecternBlockEntity lecternBE && be.getBlockState().getValue(IS_MYSTCRAFT_BOOK)) {
            var linkingBook = lecternBE.getBook().getItem();

            if (linkingBook instanceof LinkingBookItem) {
                var locationDisplay = lecternBE.getBook().getComponents().get(MystcraftComponents.INSTANCE.getLOCATION_DISPLAY().get());

                if (locationDisplay instanceof LocationDisplayComponent) {
                    serverPlayer.openMenu(
                            new SimpleMenuProvider(
                                ((containerId, playerInventory, player) -> new LinkingBookMenu(containerId,playerInventory,pos)),
                                locationDisplay.getName()
                            ),
                            pos
                    );
                }

                serverPlayer.awardStat(Stats.INTERACT_WITH_LECTERN);
            }
            else {
                LogUtils.getLogger().warn("Lectern has is_mystcraft_book set to true without a mystcraft book inside it!");
            }
        }
    }
}
