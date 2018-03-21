package io.anuke.mindustry.world.blocks.types.generation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class Generator extends PowerBlock{
	public static boolean drawRangeOverlay = false;
	public static final float thicknessScl = 0.85f * 0.7f;
	public static final float laserMinValue = 0f;
	public static final Color laserFrom = Color.valueOf("d0bdd2");
	public static final Color laserTo = Color.valueOf("ffe7a8");

	protected Translator t1 = new Translator();
	protected Translator t2 = new Translator();

	public int laserRange = 6;
	public int laserDirections = 4;
	public float powerSpeed = 0.5f;
	public boolean explosive = true;
	public boolean hasLasers = true;
	public boolean outputOnly = false;

	public Generator(String name){
		super(name);
		expanded = true;
		layer = Layer.power;
	}

	@Override
	public void setStats(){
		super.setStats();

		if(hasLasers){
			stats.add("lasertilerange", laserRange);
			stats.add("maxpowertransfersecond", Strings.toFixed(powerSpeed * 60, 2));
		}

		//TODO fix this
		if(explosive){
			stats.add("explosive", "!!! //TODO");
		}
	}

	@Override
	public void drawSelect(Tile tile){
		super.drawSelect(tile);

		if(drawRangeOverlay){
			int rotation = tile.getRotation();
			if(hasLasers){
				Draw.color(Color.YELLOW);
				Lines.stroke(2f);

				for(int i = 0; i < laserDirections; i++){
					int dir = Mathf.mod(i + rotation - laserDirections / 2, 4);
					float lx = Geometry.d4[dir].x, ly = Geometry.d4[dir].y;
					float dx = lx * laserRange * tilesize;
					float dy = ly * laserRange * tilesize;
					
					Lines.dashLine(
							tile.worldx() + lx * tilesize / 2,
							tile.worldy() + ly * tilesize / 2,
							tile.worldx() + dx - lx * tilesize,
							tile.worldy() + dy - ly * tilesize, 9);
				}

				Draw.reset();
			}
		}
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		if(hasLasers){
			Draw.color(Color.PURPLE);
			Lines.stroke(2f);

			for(int i = 0; i < laserDirections; i++){
				int dir = Mathf.mod(i + rotation - laserDirections / 2, 4);
				float lx = Geometry.d4[dir].x, ly = Geometry.d4[dir].y;
				float dx = lx * laserRange * tilesize;
				float dy = ly * laserRange * tilesize;
				Lines.dashLine(
						x * tilesize + lx * tilesize / 2,
						y * tilesize + ly * tilesize / 2,
						x * tilesize + dx - lx * tilesize,
						y * tilesize + dy - ly * tilesize, 9);
			}

			Draw.reset();
		}
	}

	@Override
	public void onDestroyed(Tile tile){
		if(explosive){
			float x = tile.worldx(), y = tile.worldy();

			Effects.effect(Fx.shellsmoke, x, y);
			Effects.effect(Fx.blastsmoke, x, y);

			Timers.run(Mathf.random(8f + Mathf.random(6f)), () -> {
				Effects.shake(6f, 8f, x, y);
				Effects.effect(Fx.generatorexplosion, x, y);
				Effects.effect(Fx.shockwave, x, y);

				//TODO better explosion effect!

				Effects.sound(explosionSound, x, y);
			});

		}else{
			super.onDestroyed(tile);
		}
	}

	@Override
	public void drawLayer(Tile tile){
		if(!Settings.getBool("lasers")) return;

		GeneratorEntity entity = tile.entity();

		if(entity.power.amount > powerSpeed){
			entity.laserThickness = Mathf.lerpDelta(entity.laserThickness, 1f, 0.05f);
		}else{
			entity.laserThickness = Mathf.lerpDelta(entity.laserThickness, laserMinValue, 0.05f);
		}

		for(int i = 0; i < laserDirections; i++){
			drawLaserTo(tile, (tile.getRotation() + i) - laserDirections / 2);
		}

		Draw.color();
	}

	//@Override
	//public boolean acceptPower(Tile tile, Tile source, float amount){
	//	return false;
	//}

	@Override
	public TileEntity getEntity() {
		return new GeneratorEntity();
	}

	public static class GeneratorEntity extends PowerEntity{
		float laserThickness = laserMinValue;
	}

	protected void distributeLaserPower(Tile tile){
		PowerEntity entity = tile.entity();

		for(int i = 0; i < laserDirections; i++){
			int rot = (tile.getRotation() + i) - laserDirections / 2;
			Tile target = laserTarget(tile, rot);

			if(target == null)
				continue;

			if(isInterfering(target, rot)){
				float fract = tile.entity.power.amount / powerCapacity;
				float ofract = target.entity.power.amount / target.block().powerCapacity;
				if(ofract > fract) continue;
			}

			float transmit = Math.min(powerSpeed * Timers.delta(), entity.power.amount);
			if(target.block().acceptPower(target, tile, transmit)){
				float accepted = target.block().addPower(target, transmit);
				entity.power.amount -= accepted;
			}

		}
	}

	protected void drawLaserTo(Tile tile, int rotation){

		Tile target = laserTarget(tile, rotation);

		GeneratorEntity entity = tile.entity();

		float scale = thicknessScl;

		if(target != null){
			boolean interfering = isInterfering(target, rotation);

			t1.trns(rotation * 90, tilesize / 2 + 2f +
					(interfering ? Vector2.dst(tile.worldx(), tile.worldy(), target.worldx(),
							target.worldy()) / 2f - tilesize / 2f * 1 : 0));

			t2.trns(rotation * 90, size * tilesize / 2 + 2f);

			Draw.tint(Hue.mix(laserFrom, laserTo, entity.laserThickness * 0.93f + Mathf.sin(Timers.time(), 1.7f, 0.07f)));
			
			int relative = tile.sizedRelativeTo(target.x, target.y);
			
			if(relative == -1){
				Shapes.laser("laser", "laserend", tile.worldx() + t2.x, tile.worldy() + t2.y,
						target.worldx() - t1.x,
						target.worldy() - t1.y, scale);
			}else{
				float lf = 1f;
				float s = interfering ? 12f : 18f;
				float sclx = (relative == 1 || relative == 3) ? lf : 1f;
				float scly = (relative == 1 || relative == 3) ? 1f : lf;
				Draw.rect("laserfull", 
						tile.worldx() + Geometry.d4[relative].x * size * tilesize / 2f,
						tile.worldy() + Geometry.d4[relative].y * size * tilesize / 2f , s * sclx, s * scly);
			}
			
			Draw.color();
		}
	}

	protected boolean isInterfering(Tile target, int rotation){
		if(target.block() instanceof Generator){
			Generator other = (Generator) target.block();
			int relrot = (rotation + 2) % 4;
			if(other.hasLasers){
				for(int i = 0; i < other.laserDirections; i ++){
					if(Mathf.mod(target.getRotation() + i - other.laserDirections/2, 4) == relrot){
						return true;
					}
				}
			}
		}
		return false;
	}

	protected Tile laserTarget(Tile tile, int rotation){
		rotation = Mathf.mod(rotation, 4);
		GridPoint2 point = Geometry.d4[rotation];

		for(int i = 1; i < laserRange; i++){
			Tile other = world.tile(tile.x + i * point.x, tile.y + i * point.y);

			if(other != null && other.block().hasPower){
				Tile linked = other.getLinked();
				if((linked == null || linked.block().hasPower) && linked != tile){
					return other;
				}
			}
		}
		return null;
	}

}