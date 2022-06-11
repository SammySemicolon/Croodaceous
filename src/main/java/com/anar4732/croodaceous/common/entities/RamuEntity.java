package com.anar4732.croodaceous.common.entities;

import com.anar4732.croodaceous.common.blocks.RamuNestBlock;
import com.anar4732.croodaceous.registry.CEBlocks;
import com.anar4732.croodaceous.registry.CEEntities;
import com.anar4732.croodaceous.registry.CEItems;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;

public class RamuEntity extends Animal implements IAnimatable {
	private static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(RamuEntity.class, EntityDataSerializers.BOOLEAN);
	private final AnimationFactory animationFactory = new AnimationFactory(this);
	private BlockPos nestPos;
	private boolean sitting;
	private boolean wantsSit;
	private int sprintStartTimestemp;
	private boolean willLayEgg;
	
	public RamuEntity(EntityType<? extends RamuEntity> type, Level level) {
		super(type, level);
	}
	
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.0D, true));
		this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isTarget));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}
	
	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
		              .add(Attributes.MAX_HEALTH, 20)
					  .add(Attributes.MOVEMENT_SPEED, 0.25D)
		              .add(Attributes.ATTACK_DAMAGE, 3)
		              .add(Attributes.FOLLOW_RANGE, 16);
	}
	
	private boolean isTarget(LivingEntity livingEntity) {
		if (livingEntity instanceof RamuEntity) {
			return false;
		}
		if (nestPos == null) {
			return livingEntity.getMainHandItem().getItem() == CEItems.RAMU_EGG.get();
		}
		return livingEntity.getPosition(1F).distanceTo(new Vec3(nestPos.getX(), nestPos.getY(), nestPos.getZ())) < 4 || livingEntity.getMainHandItem().getItem() == CEItems.RAMU_EGG.get();
	}
	
	private PlayState animControllerMain(AnimationEvent<?> e) {
		if (e.isMoving()) {
			if (this.isSprinting()) {
				if (this.sprintStartTimestemp + 81 > this.tickCount) {
					e.getController().setAnimation(new AnimationBuilder().addAnimation("animation.ramu.charge_start", true));
				} else {
					e.getController().setAnimation(new AnimationBuilder().addAnimation("animation.ramu.charge", true));
				}
			} else {
				e.getController().setAnimation(new AnimationBuilder().addAnimation("animation.ramu.walk", true));
			}
		} else if (sitting) {
			e.getController().setAnimation(new AnimationBuilder().addAnimation("animation.ramu.sit", true));
		} else {
			e.getController().setAnimation(new AnimationBuilder().addAnimation("animation.ramu.idle", true));
		}
		return PlayState.CONTINUE;
	}

	@Override
	public void registerControllers(AnimationData data) {
		data.addAnimationController(new AnimationController<>(this, "controller", 2F, this::animControllerMain));
	}
	
	@Override
	public AnimationFactory getFactory() {
		return animationFactory;
	}
	
	@Override
	public void tick() {
		super.tick();
		if (!level.isClientSide) {
			if (this.isSitting()) {
				this.getNavigation().stop();
				this.goalSelector.disableControlFlag(Goal.Flag.JUMP);
				this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
				sitting = true;
			} else {
				this.goalSelector.enableControlFlag(Goal.Flag.JUMP);
				this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
				this.sitting = false;
			}
			if (((wantsSit() && !isSitting()) || willLayEgg) && nestPos != null && this.getTarget() == null) {
				this.getNavigation().moveTo(nestPos.getX(), nestPos.getY(), nestPos.getZ(), 1.0D);
			}
			if (this.getTarget() != null && this.getTarget().getMainHandItem().getItem() == CEItems.RAMU_EGG.get() && !sitting) {
				if (sprintStartTimestemp == 0) {
					sprintStartTimestemp = this.tickCount;
				} else if (this.sprintStartTimestemp + 81 > this.tickCount) {
					this.getNavigation().stop();
					this.lookAt(EntityAnchorArgument.Anchor.EYES, this.getTarget().getPosition(1F));
				} else {
					this.setSprinting(true);
				}
			} else  {
				this.setSprinting(false);
				sprintStartTimestemp = 0;
			}
			if (this.tickCount % 1200 == 0) {
				this.wantsSit = this.random.nextBoolean();
			}
			if (willLayEgg && isOnNest()) {
				willLayEgg = false;
				if (this.level.getBlockState(nestPos).getValue(RamuNestBlock.WITH_EGG)) {
					this.spawnAtLocation(CEItems.RAMU_EGG.get());
				} else {
					this.level.setBlock(nestPos, CEBlocks.RAMU_NEST.get().defaultBlockState().setValue(RamuNestBlock.WITH_EGG, true), 3);
				}
			}

			this.entityData.set(DATA_SITTING, this.sitting);
		} else {
			this.sitting = this.entityData.get(DATA_SITTING);
		}
	}
	
	@Override
	public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		ItemStack itemStack = pPlayer.getMainHandItem();
		if (itemStack.getItem() == Items.MELON || itemStack.getItem() == Items.PUMPKIN) {
			if (nestPos != null && !willLayEgg) {
				itemStack.shrink(1);
				willLayEgg = true;
				this.level.broadcastEntityEvent(this, (byte) 18);
				return InteractionResult.CONSUME_PARTIAL;
			}
		}
		return InteractionResult.SUCCESS;
	}
		
		private boolean isSitting() {
		return wantsSit() && isNearNest();
	}

	private boolean wantsSit() {
		return this.getTarget() == null && wantsSit;
	}
	
	@Nullable
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
		this.nestPos = this.getOnPos().above();
		pLevel.setBlock(nestPos, CEBlocks.RAMU_NEST.get().defaultBlockState().setValue(RamuNestBlock.WITH_EGG, this.random.nextBoolean()), 3);
		return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
	}

	@Nullable
	@Override
	public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
		return CEEntities.ENTITY_RAMU.get().create(p_146743_);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag pCompound) {
		super.readAdditionalSaveData(pCompound);
		this.nestPos = new BlockPos(pCompound.getInt("NestPosX"), pCompound.getInt("NestPosY"), pCompound.getInt("NestPosZ"));
		this.sitting = pCompound.getBoolean("Sitting");
	}
	
	@Override
	public void addAdditionalSaveData(CompoundTag pCompound) {
		super.addAdditionalSaveData(pCompound);
		if (nestPos != null) {
			pCompound.putInt("NestPosX", nestPos.getX());
			pCompound.putInt("NestPosY", nestPos.getY());
			pCompound.putInt("NestPosZ", nestPos.getZ());
		}
		pCompound.putBoolean("Sitting", this.sitting);
	}
	
	private boolean isNearNest() {
		if (nestPos == null) {
			return false;
		}
		return this.getOnPos().above().distSqr(this.nestPos) < 256;
	}
	
	private boolean isOnNest() {
		if (nestPos == null) {
			return false;
		}
		return this.getOnPos().above().distSqr(this.nestPos) < 4;
	}
	
	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_SITTING, false);
	}
	
	@Override
	public boolean hurt(DamageSource pSource, float pAmount) {
		this.sitting = false;
		return super.hurt(pSource, pAmount);
	}
	
	@Override
	public void push(Entity pEntity) {
		this.sitting = false;
		this.setLastHurtByMob(null);
		super.push(pEntity);
	}
	
}