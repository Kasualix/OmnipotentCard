package love.marblegate.omnicard.card;

import love.marblegate.omnicard.block.tileentity.SpecialCardBlockTileEntity;

import java.util.Objects;


public class BlockCard extends AbstractCard {
    private final CardFunc.ISpecialCardBlockServerTick serverTickHandler;
    private final int lifetime;
    private final boolean canRetrieveByBreak;

    public int getLifetime() {
        return lifetime;
    }

    public boolean canRetrieve() {
        return canRetrieveByBreak;
    }


    public BlockCard(Builder builder) {
        super(builder);
        serverTickHandler = builder.serverTickHandler;
        lifetime = builder.lifetime;
        canRetrieveByBreak = builder.canRetrieveByBreak;

    }

    public void handlerServerTick(SpecialCardBlockTileEntity specialCardBlockTile) {
        if (specialCardBlockTile != null) {
            serverTickHandler.handle(specialCardBlockTile);
        }
    }

    public static class Builder extends AbstractCard.Builder<Builder> {
        private CardFunc.ISpecialCardBlockServerTick serverTickHandler;
        private final int lifetime;
        private boolean canRetrieveByBreak = false;

        public Builder(String name, String category, int lifetime) {
            super(name, category);
            this.lifetime = lifetime;
        }

        public Builder isRetrievableWhenBreak() {
            canRetrieveByBreak = true;
            return this;
        }

        @Override
        public BlockCard build() {
            return new BlockCard(this);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder setFlyCardHitHandler(CardFunc.ISpecialCardBlockServerTick serverTickHandler) {
            this.serverTickHandler = Objects.requireNonNull(serverTickHandler);
            return this;
        }


    }
}
