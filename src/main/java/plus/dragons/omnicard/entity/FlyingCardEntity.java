package plus.dragons.omnicard.entity;

import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.jetbrains.annotations.NotNull;
import plus.dragons.omnicard.card.CommonCard;
import plus.dragons.omnicard.card.CommonCards;
import plus.dragons.omnicard.misc.Configuration;
import plus.dragons.omnicard.registry.EntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FlyingCardEntity extends Projectile implements GeoEntity, IEntityAdditionalSpawnData {
    private static final RawAnimation CARD_FLY = RawAnimation.begin().thenLoop("fly");
    public boolean canPickUp = false;
    private int lifetime = 60 * 20;
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private CommonCard card;
    private double xPower;
    private double yPower;
    private double zPower;

    public FlyingCardEntity(EntityType<? extends FlyingCardEntity> entityType, Level level) {
        super(entityType, level);
    }

    public FlyingCardEntity(LivingEntity livingEntity, double xPower, double yPower, double zPower, Level level, CommonCard card) {
        super(EntityRegistry.FLYING_CARD.get(), level);
        this.moveTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), this.getYRot(), this.getXRot());
        this.reapplyPosition();
        double d0 = Math.sqrt(xPower * xPower + yPower * yPower + zPower * zPower);
        if (d0 != 0.0D) {
            this.xPower = xPower / d0 * 0.1D;
            this.yPower = yPower / d0 * 0.1D;
            this.zPower = zPower / d0 * 0.1D;
        }
        this.card = card;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double v) {
        double d0 = this.getBoundingBox().getSize() * 4.0D;
        if (Double.isNaN(d0)) {
            d0 = 4.0D;
        }
        d0 *= 128.0D;
        return v < d0 * d0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "card_controller", 1, state->{
            if (state.getData(DataTickets.ACTIVE)) {
                return state.setAndContinue(CARD_FLY);
            } else {
                return PlayState.STOP;
            }
        }));
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        // Removal Check
        Entity entity = this.getOwner();
        if (!level.isClientSide()) {
            if (blockPosition().getY() >= 384)
                remove(RemovalReason.DISCARDED);
            if (lifetime <= 0) {
                if (card.getRetrievedItem().isPresent())
                    this.spawnAtLocation(card.getRetrievedItem().get().getDefaultInstance(), 0.1F);
                remove(RemovalReason.DISCARDED);
            } else {
                lifetime--;
                // Pickup Card on Ground
                if (canPickUp && qualifiedToBeRetrieved()) {
                    this.spawnAtLocation(card.getRetrievedItem().get().getDefaultInstance(), 0.1F);
                    remove(RemovalReason.DISCARDED);
                }
            }
        }

        //Handle Movement & Hit
        if (this.level.isClientSide || (entity == null || !entity.isRemoved()) && this.level.hasChunkAt(this.blockPosition())) {
            super.tick();
            HitResult hitresult = ProjectileUtil.getHitResult(this, this::canHitEntity);
            if (hitresult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitresult)) {
                this.onHit(hitresult);
            }
            if(!canPickUp){
                card.onFly(this);
            }
            this.checkInsideBlocks();
            Vec3 vec3 = this.getDeltaMovement();
            double d0 = this.getX() + vec3.x;
            double d1 = this.getY() + vec3.y;
            double d2 = this.getZ() + vec3.z;
            float f = this.getInertia();
            if (this.isInWater()) {
                f = 0.8F;
            }
            this.setDeltaMovement(vec3.add(this.xPower, this.yPower, this.zPower).scale(f));
            this.setPos(d0, d1, d2);
        } else {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity entity) {
        return super.canHitEntity(entity) && !entity.noPhysics;
    }

    protected float getInertia() {
        return 0.99F;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    protected void onHit(@NotNull HitResult rayTraceResult) {
        super.onHit(rayTraceResult);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityRayTraceResult) {
        super.onHitEntity(entityRayTraceResult);
        Entity entity = entityRayTraceResult.getEntity();
        if (entity instanceof LivingEntity && !canPickUp) {
            if((!Configuration.HURT_MOUNT.get() && entity.getPassengers().stream().anyMatch(entity1 -> {
                if (getOwner() != null)
                    return entity1.getUUID().equals(getOwner().getUUID());
                else return false;
            })) || (!Configuration.HURT_PET.get() && isPet(entity))){
                this.spawnAtLocation(card.getRetrievedItem().get().getDefaultInstance(), 0.1F);
                remove(RemovalReason.DISCARDED);
            } else {
                card.hitEntity(this, (LivingEntity) entity);
                remove(RemovalReason.DISCARDED);
            }
        }
    }

    private boolean isPet(Entity entity) {
        if (entity instanceof OwnableEntity ownableEntity) {
            return ownableEntity.getOwnerUUID() != null;
        }
        return false;
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult blockRayTraceResult) {
        super.onHitBlock(blockRayTraceResult);
        card.hitBlock(this, blockRayTraceResult.getBlockPos(), blockRayTraceResult.getDirection());
        if (!this.isRemoved())
            stayOnBlock(blockRayTraceResult);
    }

    private boolean qualifiedToBeRetrieved() {
        return !level.getEntities(this, this.getBoundingBox(), (entity -> entity instanceof Player)).isEmpty();
    }

    private void stayOnBlock(BlockHitResult blockRayTraceResult) {
        Vec3 vector3d = blockRayTraceResult.getLocation().subtract(this.getX(), this.getY(), this.getZ());
        this.setDeltaMovement(vector3d);
        Vec3 vector3d1 = vector3d.normalize().scale((double) 0.05F);
        this.setPosRaw(this.getX() - vector3d1.x, this.getY() - vector3d1.y, this.getZ() - vector3d1.z);
        canPickUp = true;
        setAnimData(DataTickets.ACTIVE,false);
    }

    public CommonCard getCardType() {
        return card;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundNBT) {
        super.readAdditionalSaveData(compoundNBT);
        if (compoundNBT.contains("power", 9)) {
            ListTag listtag = compoundNBT.getList("power", 6);
            if (listtag.size() == 3) {
                this.xPower = listtag.getDouble(0);
                this.yPower = listtag.getDouble(1);
                this.zPower = listtag.getDouble(2);
            }
        }
        card = CommonCards.fromByte(compoundNBT.getByte("card_type"));
        lifetime = compoundNBT.getInt("remaining_life_time");
        canPickUp = compoundNBT.getBoolean("can_pickup");
        setAnimData(DataTickets.ACTIVE,!canPickUp);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundNBT) {
        super.addAdditionalSaveData(compoundNBT);
        compoundNBT.put("power", this.newDoubleList(new double[]{this.xPower, this.yPower, this.zPower}));
        compoundNBT.putByte("card_type", CommonCards.toByte(card));
        compoundNBT.putInt("remaining_life_time", lifetime);
        compoundNBT.putBoolean("can_pickup", canPickUp);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeDouble(xPower);
        buffer.writeDouble(yPower);
        buffer.writeDouble(zPower);
        buffer.writeByte(CommonCards.toByte(card));
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        xPower = additionalData.readDouble();
        yPower = additionalData.readDouble();
        zPower = additionalData.readDouble();
        card = CommonCards.fromByte(additionalData.readByte());
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return Configuration.FLYING_CARD_BRIGHTNESS.get().floatValue();
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        return false;
    }

    // For Delay Handling
    // 5 tick lifespan is counted
    public boolean justBeenThrown(){
        return lifetime > 60 * 20 - 5;
    }
}
