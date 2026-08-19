package net.funkpla.emi_discovery;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Constants.MOD_ID)
public class EmiDiscoveryConfig implements ConfigData {

    // General
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enabled = true;

    // Index & Sidebar
    @ConfigEntry.Category("index")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean filterIndex = true;

    @ConfigEntry.Category("index")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean displayCraftableInIndex = false;

    @ConfigEntry.Category("index")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean requireWorkstationForCraftable = true;

    // Recipe Screen & Filtering
    @ConfigEntry.Category("recipes")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean displayWithUnknownWorkstation = true;

    @ConfigEntry.Category("recipes")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean requireCatalystsKnown = true;

    @ConfigEntry.Category("recipes")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean allowRecipeLookupForUndiscovered = true;

    @ConfigEntry.Category("recipes")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean allowUsageLookupForUndiscovered = false;

    // Blackout & Related Styles
    @ConfigEntry.Category("blackout")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean blackoutUnknownInRecipes = false;

    @ConfigEntry.Category("blackout")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean obscureTooltips = true;

    @ConfigEntry.Category("blackout")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean showQuestionMarkOverlay = true;

    // Advancements & Progression
    @ConfigEntry.Category("advancements")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enableAdvancementDiscovery = true;
}