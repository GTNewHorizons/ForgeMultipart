package codechicken.multipart.test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener;

import codechicken.microblock.MicroblockGenerator;
import codechicken.multipart.MultipartGenerator;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;

@Mod(
        modid = "ForgeMultipartTests",
        name = "ForgeMultipart Functional Tests",
        version = "1.0",
        dependencies = "required-after:ForgeMultipart",
        acceptableRemoteVersions = "*")
public final class ForgeMultipartFunctionalTestMod {

    static boolean preInitialized;
    static boolean initialized;
    static boolean postInitialized;
    static boolean serverAboutToStart;
    static boolean serverStarted;
    static int externalScalaMicroblockTraitId;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        preInitialized = true;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MultipartGenerator.registerPassThroughInterface(GeneratorPassThroughFixture.class.getName(), false, true);
        externalScalaMicroblockTraitId = MicroblockGenerator
                .registerTrait("codechicken.multipart.test.ExternalScalaMicroblockFixture");
        initialized = true;
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        postInitialized = true;
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        serverAboutToStart = true;
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        serverStarted = true;
        MinecraftServer server = MinecraftServer.getServer();
        server.addChatMessage(new ChatComponentText("Running ForgeMultipart functional tests..."));

        TestExecutionSummary summary = runTests();
        if (summary.getTestsFoundCount() == 0) {
            throw new IllegalStateException("No ForgeMultipart functional tests were discovered");
        }
        if (summary.getTotalFailureCount() > 0) {
            throw new IllegalStateException(
                    "ForgeMultipart functional tests failed; check junit-out and the server log");
        }

        server.addChatMessage(new ChatComponentText("ForgeMultipart functional tests passed"));
        server.initiateShutdown();
    }

    private static TestExecutionSummary runTests() {
        System.setProperty("junit.platform.reporting.open.xml.enabled", "false");
        Path reportDirectory = FileSystems.getDefault().getPath("./junit-out/").toAbsolutePath();
        File reportDirectoryFile = reportDirectory.toFile();
        reportDirectoryFile.mkdirs();
        File[] oldReports = reportDirectoryFile.listFiles((directory, name) -> name.endsWith(".xml"));
        if (oldReports != null) {
            for (File oldReport : oldReports) {
                oldReport.delete();
            }
        }

        LauncherDiscoveryRequest discovery = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("codechicken.multipart.test")).build();
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        PrintWriter stderr = new PrintWriter(System.err, true);
        LegacyXmlReportGeneratingListener xmlListener = new LegacyXmlReportGeneratingListener(reportDirectory, stderr);

        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            TestPlan plan = launcher.discover(discovery);
            launcher.registerTestExecutionListeners(summaryListener, xmlListener);
            launcher.execute(plan);
        }

        TestExecutionSummary summary = summaryListener.getSummary();
        summary.printFailuresTo(stderr, 32);
        summary.printTo(stderr);
        stderr.flush();
        return summary;
    }
}
