package com.example.defensivewalls;

import com.example.defensivewalls.commands.WallCommand;
import com.example.defensivewalls.configuration.Messages;
import com.example.defensivewalls.configuration.Settings;
import com.example.defensivewalls.durability.DurabilityManager;
import com.example.defensivewalls.gui.GuiService;
import com.example.defensivewalls.holograms.HologramManager;
import com.example.defensivewalls.listeners.GuiListener;
import com.example.defensivewalls.listeners.ItemPlacementListener;
import com.example.defensivewalls.listeners.PlayerListener;
import com.example.defensivewalls.listeners.ProtectionListener;
import com.example.defensivewalls.listeners.StructureInteractListener;
import com.example.defensivewalls.listeners.ToolListener;
import com.example.defensivewalls.maintenance.CabinetService;
import com.example.defensivewalls.maintenance.MaintenanceManager;
import com.example.defensivewalls.maintenance.MaintenanceReport;
import com.example.defensivewalls.particles.PreviewManager;
import com.example.defensivewalls.structures.builder.PlacementValidator;
import com.example.defensivewalls.structures.builder.ProtectionBridge;
import com.example.defensivewalls.structures.builder.StructureBuilder;
import com.example.defensivewalls.structures.manager.StructureManager;
import com.example.defensivewalls.structures.storage.StructureStorage;
import com.example.defensivewalls.structures.types.ShapeRegistry;
import com.example.defensivewalls.tools.BuildTargeting;
import com.example.defensivewalls.tools.PlayerDataManager;
import com.example.defensivewalls.tools.ToolActions;
import com.example.defensivewalls.utils.CostService;
import com.example.defensivewalls.utils.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Plugin principal: solo construye y conecta los sistemas (cada uno tiene su paquete y responsabilidad). */
public final class DefensiveWallsPlugin extends JavaPlugin {

    private Settings settings;
    private Messages messages;
    private ItemFactory items;
    private CostService costs;
    private ShapeRegistry shapes;
    private ProtectionBridge protection;
    private StructureManager structures;
    private StructureStorage storage;
    private DurabilityManager durability;
    private CabinetService cabinets;
    private MaintenanceReport report;
    private PlacementValidator validator;
    private StructureBuilder builder;
    private PlayerDataManager players;
    private BuildTargeting targeting;
    private ToolActions tools;
    private HologramManager holograms;
    private PreviewManager previews;
    private MaintenanceManager maintenance;
    private GuiService guis;
    private BukkitTask flushTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings(this);
        messages = new Messages(this);
        items = new ItemFactory(this);
        costs = new CostService(items);
        shapes = new ShapeRegistry(settings);
        protection = new ProtectionBridge();
        structures = new StructureManager(this);
        storage = new StructureStorage(this);
        durability = new DurabilityManager(this);
        cabinets = new CabinetService(this);
        report = new MaintenanceReport(this);
        validator = new PlacementValidator(this);
        builder = new StructureBuilder(this);
        players = new PlayerDataManager(this);
        targeting = new BuildTargeting(this);
        tools = new ToolActions(this);
        holograms = new HologramManager(this);
        previews = new PreviewManager(this);
        maintenance = new MaintenanceManager(this);
        guis = new GuiService(this);

        structures.addObserver(holograms);
        structures.addObserver(guis);

        storage.load(structures);
        players.load();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new ToolListener(this), this);
        pm.registerEvents(new ProtectionListener(this), this);
        pm.registerEvents(new StructureInteractListener(this), this);
        pm.registerEvents(new ItemPlacementListener(this), this);
        pm.registerEvents(new GuiListener(), this);
        pm.registerEvents(new PlayerListener(this), this);

        PluginCommand command = getCommand("wall");
        if (command != null) {
            WallCommand handler = new WallCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        startTasks();
        getLogger().info("AdvancedDefensiveWalls 2.0 activado. Estructuras: " + structures.count());
    }

    private void startTasks() {
        holograms.start();
        previews.start();
        maintenance.start();
        if (flushTask != null) {
            flushTask.cancel();
        }
        // Guardado diferido de acumuladores/durabilidad (cada 5 s si hubo cambios).
        flushTask = Bukkit.getScheduler().runTaskTimer(this, () -> structures.flushIfDirty(), 100L, 100L);
    }

    @Override
    public void onDisable() {
        if (flushTask != null) {
            flushTask.cancel();
        }
        if (maintenance != null) maintenance.stop();
        if (previews != null) previews.stop();
        if (holograms != null) holograms.stop();
        if (structures != null) structures.saveSync();
        if (players != null) players.saveSync();
    }

    /** Recarga config.yml, reinicia tareas y repara bloques faltantes. Devuelve bloques reparados. */
    public int reloadAll() {
        reloadConfig();
        holograms.stop();
        previews.stop();
        maintenance.stop();
        startTasks();
        return structures.verifyAll();
    }

    public Settings settings() { return settings; }
    public Messages messages() { return messages; }
    public ItemFactory items() { return items; }
    public CostService costs() { return costs; }
    public ShapeRegistry shapes() { return shapes; }
    public ProtectionBridge protection() { return protection; }
    public StructureManager structures() { return structures; }
    public StructureStorage storage() { return storage; }
    public DurabilityManager durability() { return durability; }
    public CabinetService cabinets() { return cabinets; }
    public MaintenanceReport report() { return report; }
    public PlacementValidator validator() { return validator; }
    public StructureBuilder builder() { return builder; }
    public PlayerDataManager players() { return players; }
    public BuildTargeting targeting() { return targeting; }
    public ToolActions tools() { return tools; }
    public HologramManager holograms() { return holograms; }
    public PreviewManager previews() { return previews; }
    public GuiService guis() { return guis; }
}
