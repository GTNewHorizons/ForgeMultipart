package codechicken.microblock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import codechicken.microblock.handler.MicroblockProxy;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.mutable.HashMap;
import scala.collection.mutable.Map;

public final class ConfigContent$ {

    public static final ConfigContent$ MODULE$ = new ConfigContent$();

    private final Map<String, Seq<Object>> codechicken$microblock$ConfigContent$$nameMap = new HashMap<>();

    private ConfigContent$() {}

    public Map<String, Seq<Object>> codechicken$microblock$ConfigContent$$nameMap() {
        return codechicken$microblock$ConfigContent$$nameMap;
    }

    public void parse(File cfgDir) {
        File cfgFile = new File(cfgDir, "microblocks.cfg");
        try {
            if (cfgFile.exists()) {
                loadLinesIO(cfgFile);
            } else {
                generateDefaultIO(cfgFile);
            }
        } catch (IOException exception) {
            MicroblockProxy.logger().error("Error parsing config", exception);
        }
    }

    public void generateDefault(File cfgFile) {
        try {
            generateDefaultIO(cfgFile);
        } catch (IOException exception) {
            throw ConfigContent$.<RuntimeException>rethrow(exception);
        }
    }

    public void loadLine(String line) {
        if (line.startsWith("#") || line.length() < 3) {
            return;
        }

        if (line.charAt(0) != '"') {
            throw new IllegalArgumentException("Line must begin with a quote");
        }
        int secondQuote = line.indexOf('"', 1);
        if (secondQuote < 0) {
            throw new IllegalArgumentException("Unmatched quotes");
        }

        String name = line.substring(1, secondQuote);
        if (!name.contains(".") && !name.contains(":")) {
            name = "minecraft:" + name;
        }

        List<Object> metadata = new ArrayList<>();
        metadata.add(0);
        if (line.length() > secondQuote + 1) {
            if (line.charAt(secondQuote + 1) != ':') {
                throw new IllegalArgumentException("Name must be followed by a colon separator");
            }

            metadata.clear();
            for (String value : line.substring(secondQuote + 2).split(",")) {
                if (value.contains("-")) {
                    String[] bounds = value.split("-");
                    if (bounds.length != 2) {
                        throw new IllegalArgumentException("Invalid - separated range");
                    }
                    int start = Integer.parseInt(bounds[0]);
                    int end = Integer.parseInt(bounds[1]);
                    if (start <= end) {
                        for (int current = start;; current++) {
                            metadata.add(current);
                            if (current == end) {
                                break;
                            }
                        }
                    }
                } else {
                    metadata.add(Integer.parseInt(value));
                }
            }
        }

        codechicken$microblock$ConfigContent$$nameMap.put(name, JavaConversions.asScalaBuffer(metadata).toList());
    }

    public void loadLines(File cfgFile) {
        try {
            loadLinesIO(cfgFile);
        } catch (IOException exception) {
            throw ConfigContent$.<RuntimeException>rethrow(exception);
        }
    }

    public void load() {
        java.util.Map<String, Seq<Object>> names = JavaConversions
                .mutableMapAsJavaMap(codechicken$microblock$ConfigContent$$nameMap);
        for (Object value : Block.blockRegistry) {
            Block block = (Block) value;
            List<Object> metadata = new ArrayList<>();
            addAll(metadata, names.remove(block.getUnlocalizedName()));
            addAll(metadata, names.remove((String) Block.blockRegistry.getNameForObject(block)));
            for (Object metaValue : metadata) {
                int meta = (Integer) metaValue;
                try {
                    BlockMicroMaterial.createAndRegister(block, meta);
                } catch (IllegalStateException exception) {
                    MicroblockProxy.logger().error(
                            "Unable to register micro material: " + BlockMicroMaterial.materialKey(block, meta)
                                    + "\n\t"
                                    + exception.getMessage());
                } catch (Exception exception) {
                    MicroblockProxy.logger().error(
                            "Unable to register micro material: " + BlockMicroMaterial.materialKey(block, meta),
                            exception);
                }
            }
        }

        for (Entry<String, Seq<Object>> entry : names.entrySet()) {
            MicroblockProxy.logger().warn(
                    "Unable to add micro material for block with unlocalised name " + entry.getKey()
                            + " as it doesn't exist");
        }
    }

    public void handleIMC(Seq<IMCMessage> messages) {
        Iterator<IMCMessage> iterator = messages.iterator();
        while (iterator.hasNext()) {
            IMCMessage message = iterator.next();
            if (!"microMaterial".equals(message.key)) {
                continue;
            }

            if (message.getMessageType() != ItemStack.class) {
                imcError(message, "value is not an instanceof ItemStack");
                continue;
            }

            ItemStack stack = message.getItemStackValue();
            if (!Block.blockRegistry.containsId(Item.getIdFromItem(stack.getItem()))) {
                imcError(message, "Invalid Block: " + stack.getItem());
            } else if (stack.getItemDamage() < 0 || stack.getItemDamage() >= 16) {
                imcError(message, "Invalid metadata: " + stack.getItemDamage());
            } else {
                try {
                    BlockMicroMaterial
                            .createAndRegister(Block.getBlockFromItem(stack.getItem()), stack.getItemDamage());
                } catch (IllegalStateException exception) {
                    imcError(
                            message,
                            "Unable to register micro material: "
                                    + BlockMicroMaterial
                                            .materialKey(Block.getBlockFromItem(stack.getItem()), stack.getItemDamage())
                                    + "\n\t"
                                    + exception.getMessage());
                }
            }
        }
    }

    private static void generateDefaultIO(File cfgFile) throws IOException {
        PrintWriter writer = new PrintWriter(cfgFile);
        writer.println("#Configuration file for adding microblock materials for aesthetic blocks added by mods");
        writer.println("#Each line needs to be of the form <name>:<meta>");
        writer.println(
                "#<name> is the unlocalised name or registry key of the block/item enclosed in quotes. NEI can help you find these");
        writer.println(
                "#<meta> may be ommitted, in which case it defaults to 0, otherwise it can be a number, a comma separated list of numbers, or a dash separated range");
        writer.println("#Ex. \"dirt\" \"minecraft:planks\":3 \"iron_ore\":1,2,3,5 \"ThermalFoundation:Storage\":0-15");
        writer.close();
    }

    private void loadLinesIO(File cfgFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(cfgFile));
        String line;
        do {
            line = reader.readLine();
            if (line != null) {
                try {
                    loadLine(line);
                } catch (Exception exception) {
                    MicroblockProxy.logger().error("Invalid line in microblocks.cfg: " + line);
                    MicroblockProxy.logger().error(exception.getMessage());
                }
            }
        } while (line != null);
        reader.close();
    }

    private static void addAll(List<Object> destination, Seq<Object> source) {
        if (source != null) {
            destination.addAll(JavaConversions.seqAsJavaList(source));
        }
    }

    private static void imcError(IMCMessage message, String error) {
        MicroblockProxy.logger().error("Invalid microblock IMC message from " + message.getSender() + ": " + error);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException rethrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
