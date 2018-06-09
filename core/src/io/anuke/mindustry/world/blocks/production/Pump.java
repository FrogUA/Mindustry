package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class Pump extends LiquidBlock{
	protected float pumpAmount = 1f;

	public Pump(String name) {
		super(name);
		layer = Layer.overlay;
		liquidFlowFactor = 3f;
		group = BlockGroup.liquids;
		liquidRegion = "pump-liquid";
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add("liquidsecond", Strings.toFixed(60f*pumpAmount, 1));
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return false;
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.drawx(), tile.drawy());
		
		Draw.color(tile.entity.liquids.liquid.color);
		Draw.alpha(tile.entity.liquids.amount / liquidCapacity);
		Draw.rect(liquidRegion, tile.drawx(), tile.drawy());
		Draw.color();
	}

	@Override
	public TextureRegion[] getIcon(){
		return new TextureRegion[]{Draw.region(name)};
	}

	@Override
	public boolean isLayer(Tile tile) {
		return tile.floor().liquidDrop == null;
	}
	
	@Override
	public void drawLayer(Tile tile){
		Draw.colorl(0.85f + Mathf.absin(Timers.time(), 6f, 0.15f));
		Draw.rect("cross-"+size, tile.drawx(), tile.drawy());
		Draw.color();
	}
	
	@Override
	public void update(Tile tile){
		
		if(tile.floor().liquidDrop != null){
			float maxPump = Math.min(liquidCapacity - tile.entity.liquids.amount, pumpAmount * Timers.delta());
			tile.entity.liquids.liquid = tile.floor().liquidDrop;
			tile.entity.liquids.amount += maxPump;
		}

		tryDumpLiquid(tile);
	}

}