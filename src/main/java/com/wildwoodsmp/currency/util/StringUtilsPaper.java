package com.wildwoodsmp.currency.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class StringUtilsPaper {

    private final static LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private final static MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private StringUtilsPaper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Expected input format:
     * <pre>
     * {@code
     * <message-id>:
     *   actionbar: <actionbar string>
     *   geyser-actionbar: <actionbar string for bedrock players only>
     *   message: <message string or string list>
     *   geyser: <message string or string list for bedrock players only>
     *   title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   geyser-title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   sound:
     *    id: <sound string id> (default: minecraft:block.amethyst_block.hit)
     *    volume: <sound volume> (default: 1.0)
     *    pitch: <sound pitch> (default: 1.0)
     *    offset: <sound offset> (default: 0)
     * }
     * </pre>
     *
     * @param configuration       The configuration to get the message from
     * @param path                The path to the message
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */
    public static void broadcast(YamlConfiguration configuration, String path, Placeholders placeholderReplacer) {
        if (!configuration.contains(path)) return;
        if (!configuration.contains(path + ".message")) return;

        if (configuration.isList(path + ".message")) {
            List<String> strings = configuration.getStringList(path + ".message");
            broadcast(strings, null, placeholderReplacer);
            return;
        }

        if (configuration.contains(path + ".message")) {
            String s = configuration.getString(path + ".message");
            broadcast(s, null, placeholderReplacer);
        }
    }

    /**
     * @param message             The message to broadcast
     * @param geyser              The geyser alternative message to broadcast
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */
    public static void broadcast(@Nullable List<String> message, @Nullable List<String> geyser, Placeholders placeholderReplacer) {
        if (message == null && geyser == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessage(message, geyser, player, placeholderReplacer);
        }
        for (String s : message) {
            Bukkit.getConsoleSender().sendMessage(parse(s, null, placeholderReplacer));
        }
    }

    public static void broadcast(ConfigurationSection section, Placeholders replacer) {
        if (section == null) return;
        if (!section.contains("message")) return;
        if (section.isList("message")) {
            broadcast(section.getStringList("message"), null, replacer);
            return;
        }
        broadcast(section.getString("message"), null, replacer);
    }

    /**
     * @param message             The message to broadcast
     * @param geyser              The geyser alternative message to broadcast
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */
    public static void broadcast(@Nullable String message, @Nullable String geyser, Placeholders placeholderReplacer) {
        if (message == null && geyser == null) return;
        if (message == null) {
            broadcast(null, List.of(geyser), placeholderReplacer);
            return;
        }
        if (geyser == null) {
            broadcast(List.of(message), null, placeholderReplacer);
            return;
        }
        broadcast(List.of(message), List.of(geyser), placeholderReplacer);
    }

    /**
     * Expected input format:
     * <pre>
     * {@code
     * <message-id>:
     *   actionbar: <actionbar string>
     *   geyser-actionbar: <actionbar string for bedrock players only>
     *   message: <message string or string list>
     *   geyser: <message string or string list for bedrock players only>
     *   title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   geyser-title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   sound:
     *    id: <sound string id> (default: minecraft:block.amethyst_block.hit)
     *    volume: <sound volume> (default: 1.0)
     *    pitch: <sound pitch> (default: 1.0)
     *    offset: <sound offset> (default: 0)
     * }
     * </pre>
     *
     * @param configuration       The configuration to get the message from
     * @param player              The player to send the message to
     * @param path                The path to the message
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */
    public static void sendMessage(YamlConfiguration configuration, CommandSender player, String path, Placeholders placeholderReplacer) {
        if (!configuration.contains(path)) return;
        if (!configuration.contains(path + ".message")) return;

        if (configuration.isList(path + ".message")) {
            List<String> strings = configuration.getStringList(path + ".message");
            sendMessage(strings, null, player, placeholderReplacer);
            return;
        }
        String s = configuration.getString(path + ".message");
        sendMessage(s, null, player, placeholderReplacer);
    }

    /**
     * Expected input format:
     * <pre>
     * {@code
     * <message-id>:
     *   actionbar: <actionbar string>
     *   geyser-actionbar: <actionbar string for bedrock players only>
     *   message: <message string or string list>
     *   geyser: <message string or string list for bedrock players only>
     *   title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   geyser-title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   sound:
     *    id: <sound string id> (default: minecraft:block.amethyst_block.hit)
     *    volume: <sound volume> (default: 1.0)
     *    pitch: <sound pitch> (default: 1.0)
     *    offset: <sound offset> (default: 0)
     * }
     * </pre>
     *
     * @param section             The section that is being parsed for message data
     * @param player              The player to send the message to
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */

    public static void sendMessage(ConfigurationSection section, CommandSender player, Placeholders placeholderReplacer) {
        if (section == null) return;
        if (!section.contains("message")) return;

        if (section.isList("message")) {
            sendMessage(section.getStringList("message"), null, player, placeholderReplacer);
            return;
        }
        sendMessage(section.getString("message"), null, player, placeholderReplacer);
    }


    /**
     * Will not parse placeholderapi placeholders at all
     *
     * @param message             List of strings to parse & send to command sender
     * @param geyser              The list of geyser alternative messages to parse & send to command sender, if null it will default to java edition messsages
     * @param sender              The command sender to send the message to
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */
    public static void sendMessage(@Nullable String message, @Nullable String geyser, CommandSender sender, Placeholders placeholderReplacer) {
        if (message == null && geyser == null) return;

        if (message == null) {
            sendMessage(null, List.of(geyser), sender, placeholderReplacer);
            return;
        }
        if (geyser == null) {
            sendMessage(List.of(message), null, sender, placeholderReplacer);
            return;
        }
        sendMessage(List.of(message), List.of(geyser), sender, placeholderReplacer);
    }

    /**
     * Will not parse placeholderapi placeholders at all
     *
     * @param message             List of strings to parse & send to command sender
     * @param geyser              The list of geyser alternative messages to parse & send to command sender, if null it will default to java edition messsages
     * @param sender              The command sender to send the message to
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */
    public static void sendMessage(@Nullable List<String> message, @Nullable List<String> geyser, CommandSender sender, Placeholders placeholderReplacer) {
        if (message == null && geyser == null) return;

        if (geyser != null) {
            for (String s : geyser) {
                sender.sendMessage(parse(s, null, placeholderReplacer));
            }
            return;
        }

        for (String s : message) {
            sender.sendMessage(parse(s, null, placeholderReplacer));
        }
    }

    /**
     * @param message             String to parse & send to player
     * @param geyser              String of geyser alternative message to parse & send to player, if null it will default to java edition messsage
     * @param player              The player to send the message to
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */
    public static void sendMessage(@Nullable String message, @Nullable String geyser, Player player, Placeholders placeholderReplacer) {
        if (message == null && geyser == null) return;

        if (message == null) {
            sendMessage(null, List.of(geyser), player, placeholderReplacer);
            return;
        }
        if (geyser == null) {
            sendMessage(List.of(message), null, player, placeholderReplacer);
            return;
        }

        sendMessage(List.of(message), List.of(geyser), player, placeholderReplacer);
    }

    /**
     * @param message             List of strings to parse & send to player
     * @param geyser              The list of geyser alternative messages to parse & send to player, if null it will default to java edition messsages
     * @param player              The player to send the message to
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */
    public static void sendMessage(@Nullable List<String> message, @Nullable List<String> geyser, Player player, Placeholders placeholderReplacer) {
        if (message == null && geyser == null) return;

        if (geyser != null) {
            for (String s : geyser) {
                player.sendMessage(parse(s, player, placeholderReplacer));
            }
            return;
        }

        for (String s : message) {
            player.sendMessage(parse(s, player, placeholderReplacer));
        }
    }

    /**
     * Expected input format:
     * <pre>
     * {@code
     * <message-id>:
     *   actionbar: <actionbar string>
     *   geyser-actionbar: <actionbar string for bedrock players only>
     *   message: <message string or string list>
     *   geyser: <message string or string list for bedrock players only>
     *   title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   geyser-title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   sound:
     *    id: <sound string id> (default: minecraft:block.amethyst_block.hit)
     *    volume: <sound volume> (default: 1.0)
     *    pitch: <sound pitch> (default: 1.0)
     *    offset: <sound offset> (default: 0)
     * }
     * </pre>
     *
     * @param configuration       The configuration to get the message from
     * @param player              The player to send the message to
     * @param path                The path to the message
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */

    public static void sendMessage(YamlConfiguration configuration, @NotNull Player player, String path, @Nullable Placeholders placeholderReplacer) {
        if (!configuration.contains(path)) return;
        if (!configuration.contains(path + ".message")) return;

        if (configuration.isList(path + ".message")) {
            List<String> strings = configuration.getStringList(path + ".message");
            sendMessage(strings, null, player, placeholderReplacer);
            return;
        }
        String s = configuration.getString(path + ".message");
        sendMessage(s, null, player, placeholderReplacer);
    }

    /**
     * Expected input format:
     * <pre>
     * {@code
     * <message-id>:
     *   actionbar: <actionbar string>
     *   geyser-actionbar: <actionbar string for bedrock players only>
     *   message: <message string or string list>
     *   geyser: <message string or string list for bedrock players only>
     *   title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   geyser-title:
     *     title: <title string> (optional, requires subtitle to be set if not set)
     *     subtitle: <subtitle string> (optional, requires title to be set if not set)
     *     fadeIn: <fade in time> (default: 10)
     *     stay: <stay time> (default: 70)
     *     fadeOut: <fade out time> (default: 20)
     *   sound:
     *    id: <sound string id> (default: minecraft:block.amethyst_block.hit)
     *    volume: <sound volume> (default: 1.0)
     *    pitch: <sound pitch> (default: 1.0)
     *    offset: <sound offset> (default: 0)
     * }
     * </pre>
     *
     * @param sec                 The section that is being parsed for message data
     * @param player              The player to send the message to
     * @param placeholderReplacer The placeholder replacer to use, if null it will not replace any placeholders
     */
    public static void sendMessage(ConfigurationSection sec, @NotNull Player player, @Nullable Placeholders placeholderReplacer) {

        if (sec.contains("sound")) {
            ConfigurationSection section = sec.getConfigurationSection("sound");
            float offset = (float) section.getDouble("offset", 0.0);
            float pitch = (float) section.getDouble("pitch", 1.0);

            if (offset != 0) {
                offset = offset / 2;
                pitch = pitch + MathUtils.randomFloat(-offset, offset);
            }

            player.playSound(
                    player.getLocation(),
                    section.getString("id", "minecraft:block.amethyst_block.hit"),
                    SoundCategory.valueOf(section.getString("category", "master").toUpperCase()),
                    pitch,
                    (float) section.getDouble("volume", 1.0)
            );
        }

        if (sec.contains("title")) {
            Title.Times times = Title.Times.times(
                    Ticks.duration(sec.getInt("fadeIn", 10)),
                    Ticks.duration(sec.getInt("stay", 70)),
                    Ticks.duration(sec.getInt("fadeOut", 20))
            );

            Title title = Title.title(
                    parse(sec.getString("title"), player, placeholderReplacer),
                    parse(sec.getString("subtitle"), player, placeholderReplacer),
                    times
            );

            player.showTitle(title);
        }

        if (sec.contains("geyser-title")) {
            Title.Times times = Title.Times.times(
                    Ticks.duration(sec.getInt("fadeIn", 10)),
                    Ticks.duration(sec.getInt("stay", 70)),
                    Ticks.duration(sec.getInt("fadeOut", 20))
            );

            Title title = Title.title(
                    parse(sec.getString("geyser-title.title"), player, placeholderReplacer),
                    parse(sec.getString("geyser-title.subtitle"), player, placeholderReplacer),
                    times
            );

            player.showTitle(title);
        }

        if (sec.contains("actionbar")) {
            if (sec.isList("actionbar")) {
                List<String> actionbar = sec.getStringList("actionbar");
                sendMessage(actionbar, null, player, placeholderReplacer);
                return;
            }
            player.sendActionBar(parse(sec.getString("actionbar"), player, placeholderReplacer));
        }

        if (sec.contains("geyser-actionbar")) {
            if (GeyserUtils.isFloodgateEnabled() && GeyserUtils.isUserBedrock(player.getUniqueId())) {
                player.sendActionBar(parse(sec.getString("geyser-actionbar"), player, placeholderReplacer));
            }
        }

        if (sec.contains("message")) {
            if (sec.isList("message")) {
                List<String> strings = sec.getStringList("message");
                sendMessage(strings, null, player, placeholderReplacer);
                return;
            }
            String s = sec.getString("message");
            sendMessage(s, null, player, placeholderReplacer);
        }

        if (sec.contains("geyser")) {
            if (GeyserUtils.isFloodgateEnabled() && GeyserUtils.isUserBedrock(player.getUniqueId())) {
                if (sec.isList("geyser")) {
                    List<String> strings = sec.getStringList("geyser");
                    sendMessage(strings, null, player, placeholderReplacer);
                    return;
                }
                String s = sec.getString("geyser");
                sendMessage(s, null, player, placeholderReplacer);
            }
        }
    }

    private static Component parse(String message, Player player, Placeholders placeholderReplacer) {
        if (placeholderReplacer != null) {
            message = placeholderReplacer.parse(message);
        }
        return MINI_MESSAGE.deserialize(message);
    }

    /**
     * @param current           The current progress
     * @param max               The max progress
     * @param totalBars         The total amount of bars
     * @param symbol            The symbol to use
     * @param completedColor    The color of the completed bars
     * @param notCompletedColor The color of the not completed bars
     */
    public static String getProgressBar(int current, int max, int totalBars, String symbol, String completedColor, String notCompletedColor) {
        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);

        return completedColor + symbol.repeat(Math.max(0, progressBars)) + notCompletedColor + symbol.repeat(Math.max(0, totalBars - progressBars));
    }

    /**
     * @param arg         The argument to complete
     * @param completions The list of completions
     * @return The parsed message
     */
    public static List<String> partialCompletion(String arg, List<String> completions) {
        List<String> completion = new ArrayList<>();
        StringUtil.copyPartialMatches(arg, completions, completion);
        Collections.sort(completion);
        return completion;
    }

    /**
     * Just an alias for {@link #partialCompletion(String, List)} because I forget the order of the arguments sometimes
     *
     * @param list The list of completions
     * @param arg  The argument to complete
     * @return The list of completions
     */
    public static List<String> partialCompletion(List<String> list, String arg) {
        return partialCompletion(arg, list);
    }

    /**
     * @param component The component to center
     * @return The centered component
     */
    public static Component centerMessage(Component component) {
        return Component.text(centerMessageSpaces(getContent(component))).append(component);
    }

    /**
     * @param message The message to center
     * @return The centered message
     */
    public static String centerMessage(String message) {
        return centerMessageSpaces(message) + message;
    }

    /**
     * @param component The component to center
     * @return The centered component
     */
    public static Component centerMessageSpaces(Component component) {
        return Component.text(centerMessageSpaces(getContent(component)));
    }

    /**
     * @param message The message to center
     * @return The spaces needed for the message
     */
    public static String centerMessageSpaces(String message) {
        return centerMessageSpaces(message, 0);
    }

    /**
     * @param message      The message to center
     * @param centerOffset The offset to center the message
     * @return The spaces needed for the message
     */
    public static String centerMessageSpaces(String message, int centerOffset) {
        if (message == null || message.isEmpty()) return "";

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        boolean isHex = false;
        int hexCount = 0;

        for (char c : message.toCharArray()) {
            if (c == ChatColor.COLOR_CHAR) {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L'; // &l
                isHex = c == 'x' || c == 'X'; // &x&r&r&g&g&b&b
                hexCount = isHex ? 12 : 0; // 14 char for hex codes but we are skipping the two one
            } else if (previousCode && isBold) {
                isBold = false;
            } else if (previousCode && isHex) {
                hexCount--;
                if (hexCount == 0) {
                    isHex = false;
                }
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int CENTER_PX = 154 + centerOffset;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        return sb.toString();
    }

    /**
     * @param component The component to get the content of (keeps bold intact)
     * @return The content of the component
     */
    public static String getContent(Component component) {
        return LEGACY_COMPONENT_SERIALIZER.serialize(component);
    }

}
