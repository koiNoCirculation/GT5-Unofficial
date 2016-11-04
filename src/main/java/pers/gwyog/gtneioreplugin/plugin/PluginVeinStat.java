package pers.gwyog.gtneioreplugin.plugin;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.conn.tsccm.ConnPoolByRoute;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler.RecipeTransferRect;
import gregtech.api.GregTech_API;
import gregtech.api.util.GT_LanguageManager;
import gregtech.common.GT_Worldgen_GT_Ore_Layer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import pers.gwyog.gtneioreplugin.util.GTOreLayerHelper;
import pers.gwyog.gtneioreplugin.util.GTOreLayerHelper.OreLayerWrapper;

public class PluginVeinStat extends PluginBase {

	public class CachedVeinStatRecipe extends CachedRecipe {
		public String veinName;
		public PositionedStack positionedStackPrimary;
		public PositionedStack positionedStackSecondary;
		public PositionedStack positionedStackBetween;
		public PositionedStack positionedStackSporadic;
			
		public CachedVeinStatRecipe(String veinName, List<ItemStack> stackListPrimary, List<ItemStack> stackListSecondary,
				List<ItemStack> stackListBetween, List<ItemStack> stackListSporadic) {
			this.veinName = veinName;
			positionedStackPrimary = new PositionedStack(stackListPrimary, 2, 0);
			positionedStackSecondary = new PositionedStack(stackListSecondary, 22, 0);
			positionedStackBetween = new PositionedStack(stackListBetween, 42, 0);
			positionedStackSporadic = new PositionedStack(stackListSporadic, 62, 0);
		}
		
		@Override
		public List<PositionedStack> getIngredients() {
			List<PositionedStack> ingredientsList = new ArrayList<PositionedStack>();
			positionedStackPrimary.setPermutationToRender((cycleticks / 20) % positionedStackPrimary.items.length);;
			positionedStackSecondary.setPermutationToRender((3 + cycleticks / 20) % positionedStackPrimary.items.length);;
			positionedStackBetween.setPermutationToRender((2 + cycleticks / 20) % positionedStackPrimary.items.length);;
			positionedStackSporadic.setPermutationToRender((1 + cycleticks / 20) % positionedStackPrimary.items.length);;
			ingredientsList.add(positionedStackPrimary);
			ingredientsList.add(positionedStackSecondary);
			ingredientsList.add(positionedStackBetween);
			ingredientsList.add(positionedStackSporadic);
			return ingredientsList;
		}
		
		@Override
		public PositionedStack getResult() {
			return null;
		}
		
	}
	
	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		if (outputId.equals(getOutputId())) {
			OreLayerWrapper oreLayerWrapper;
			for (String veinName: GTOreLayerHelper.mapOreLayerWrapper.keySet()) {
				oreLayerWrapper = GTOreLayerHelper.mapOreLayerWrapper.get(veinName);
				List<ItemStack> stackListPrimary = new ArrayList<ItemStack>();
				List<ItemStack> stackListSecondary = new ArrayList<ItemStack>();
				List<ItemStack> stackListBetween = new ArrayList<ItemStack>();
				List<ItemStack> stackListSporadic = new ArrayList<ItemStack>();
				for (int i=0;i<7;i++) {
					stackListPrimary.add(new ItemStack(GregTech_API.sBlockOres1, 1, oreLayerWrapper.primaryMeta+i*1000));
					stackListSecondary.add(new ItemStack(GregTech_API.sBlockOres1, 1, oreLayerWrapper.secondaryMeta+i*1000));
					stackListBetween.add(new ItemStack(GregTech_API.sBlockOres1, 1, oreLayerWrapper.betweenMeta+i*1000));
					stackListSporadic.add(new ItemStack(GregTech_API.sBlockOres1, 1, oreLayerWrapper.sporadicMeta+i*1000));
				}	
				this.arecipes.add(new CachedVeinStatRecipe(veinName, stackListPrimary, stackListSecondary, stackListBetween, stackListSporadic));
			}
		}
		else
			super.loadCraftingRecipes(outputId, results);
	}
	
	@Override
	public void loadCraftingRecipes(ItemStack stack) {
		if (stack.getUnlocalizedName().startsWith("gt.blockores")) {
			if (stack.getItemDamage()>16000) {
				super.loadCraftingRecipes(stack);
				return;
			}
			short baseMeta = (short)(stack.getItemDamage() % 1000);
			for (OreLayerWrapper worldGen: GTOreLayerHelper.mapOreLayerWrapper.values()) {
				System.out.println(worldGen.primaryMeta + "," + worldGen.secondaryMeta + "," + worldGen.betweenMeta + "," + worldGen.sporadicMeta);
				if (worldGen.primaryMeta == baseMeta || worldGen.secondaryMeta == baseMeta || worldGen.betweenMeta == baseMeta || worldGen.sporadicMeta == baseMeta) {
					List<ItemStack> stackListPrimary = new ArrayList<ItemStack>();
					List<ItemStack> stackListSecondary = new ArrayList<ItemStack>();
					List<ItemStack> stackListBetween = new ArrayList<ItemStack>();
					List<ItemStack> stackListSporadic = new ArrayList<ItemStack>();
					for (int i=0;i<7;i++) {
						stackListPrimary.add(new ItemStack(GregTech_API.sBlockOres1, 1, worldGen.primaryMeta+i*1000));
						stackListSecondary.add(new ItemStack(GregTech_API.sBlockOres1, 1, worldGen.secondaryMeta+i*1000));
						stackListBetween.add(new ItemStack(GregTech_API.sBlockOres1, 1, worldGen.betweenMeta+i*1000));
						stackListSporadic.add(new ItemStack(GregTech_API.sBlockOres1, 1, worldGen.sporadicMeta+i*1000));
					}
					this.arecipes.add(new CachedVeinStatRecipe(worldGen.veinName, stackListPrimary, stackListSecondary, stackListBetween, stackListSporadic));
				}
			}
		}
		else
			super.loadCraftingRecipes(stack);
	}
	
	@Override
	public void drawExtras(int recipe) {
		CachedVeinStatRecipe crecipe = (CachedVeinStatRecipe) this.arecipes.get(recipe);
		OreLayerWrapper oreLayer = GTOreLayerHelper.mapOreLayerWrapper.get(crecipe.veinName);
		GuiDraw.drawString(I18n.format("gui.nei.veinName") + ": " + I18n.format(oreLayer.veinName), 2, 18, 0x404040, false);
		GuiDraw.drawString(I18n.format("gui.nei.primaryOre") + ": " + GT_LanguageManager.getTranslation(getGTOreUnlocalizedName(oreLayer.primaryMeta)), 2, 31, 0x404040, false);
		GuiDraw.drawString(I18n.format("gui.nei.secondaryOre") + ": " + GT_LanguageManager.getTranslation(getGTOreUnlocalizedName(oreLayer.secondaryMeta)), 2, 44, 0x404040, false);
		GuiDraw.drawString(I18n.format("gui.nei.betweenOre") + ": " + GT_LanguageManager.getTranslation(getGTOreUnlocalizedName(oreLayer.betweenMeta)), 2, 57, 0x404040, false);
		GuiDraw.drawString(I18n.format("gui.nei.sporadicOre") + ": " + GT_LanguageManager.getTranslation(getGTOreUnlocalizedName(oreLayer.sporadicMeta)), 2, 70, 0x404040, false);
		GuiDraw.drawString(I18n.format("gui.nei.genHeight") + ": " + oreLayer.worldGenHeightRange, 2, 83, 0x404040, false);
		GuiDraw.drawString(I18n.format("gui.nei.weightedChance") + ": " + oreLayer.weightedChance, 2, 96, 0x404040, false);
		GuiDraw.drawString(I18n.format("gui.nei.worldNames") + ": " + getWorldNameTranslated(oreLayer.genOverworld, oreLayer.genNether, oreLayer.genEnd, oreLayer.genMoon, oreLayer.genMars), 2, 109, 0x404040, false);
		if (GTOreLayerHelper.restrictBiomeSupport) GuiDraw.drawString(I18n.format("gui.nei.restrictBiome") + ": " + getBiomeTranslated(oreLayer.restrictBiome), 2, 122, 0x404040, false);
		GuiDraw.drawStringR(EnumChatFormatting.BOLD + I18n.format("gui.nei.seeAll"), getGuiWidth()-3, 5, 0x404040, false);
	}
	
	public String getBiomeTranslated(String unlocalizedBiome) {
		return unlocalizedBiome.equals("None")? I18n.format("biome.none.name"): unlocalizedBiome;
	}
	
	public String getWorldNameTranslated(boolean genOverworld, boolean genNether, boolean genEnd, boolean genMoon, boolean genMars) {
		String worldNameTranslated = "";
    	if (genOverworld) {
    		if (!worldNameTranslated.isEmpty())
    			worldNameTranslated += ", ";
    		worldNameTranslated += I18n.format("world.overworld.name");
    	}
    	if (genNether) {
    		if (!worldNameTranslated.isEmpty())
    			worldNameTranslated += ", ";
    		worldNameTranslated += I18n.format("world.nether.name");
    	}
    	if (genEnd) {
    		if (!worldNameTranslated.isEmpty())
    			worldNameTranslated += ", ";
    		worldNameTranslated += I18n.format("world.end.name");
    	}
    	if (genMoon) {
    		if (!worldNameTranslated.isEmpty())
    			worldNameTranslated += ", ";
    		worldNameTranslated += I18n.format("world.moon.name");
    	}
    	if (genMars) {
    		if (!worldNameTranslated.isEmpty())
    			worldNameTranslated += ", ";
    		worldNameTranslated += I18n.format("world.mars.name");
    	}
		return worldNameTranslated;
	}
	
	@Override
	public String getOutputId() {
		return "GTOrePluginVein";
	}
	
	@Override
	public String getRecipeName() {
		return I18n.format("gui.veinStat.name");
	}
	
}
