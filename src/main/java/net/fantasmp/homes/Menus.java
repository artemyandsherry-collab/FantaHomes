package net.fantasmp.homes;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Every dialog screen the plugin shows. */
public final class Menus {

    private static final int GRID_COLUMNS = 6;
    private static final int ICONS_PER_PAGE = 60;

    private final FantaHomes plugin;

    public Menus(FantaHomes plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- main grid

    /** The homes grid: one button per home, then a New Home button per free slot. */
    public void openHomes(Player player) {
        List<Home> homes = plugin.store().get(player.getUniqueId());
        int limit = plugin.limitFor(player);

        List<ActionButton> buttons = new ArrayList<>();

        for (Home home : homes) {
            buttons.add(ActionButton.builder(
                            Component.text(home.getName(), NamedTextColor.WHITE))
                    .tooltip(Component.text("Click to manage", NamedTextColor.GRAY))
                    .width(98)
                    .action(DialogAction.customClick((view, audience) ->
                            openManage(player, home), ClickCallback.Options.builder().build()))
                    .build());
        }

        int free = Math.max(0, limit - homes.size());
        for (int i = 0; i < free; i++) {
            buttons.add(ActionButton.builder(
                            Component.text("New Home", NamedTextColor.GRAY))
                    .tooltip(Component.text("Save your current position", NamedTextColor.DARK_GRAY))
                    .width(98)
                    .action(DialogAction.customClick((view, audience) ->
                            openCreate(player), ClickCallback.Options.builder().build()))
                    .build());
        }

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.item(new ItemStack(Material.WHITE_BED))
                .description(DialogBody.plainMessage(
                        Component.text(homes.size() + " / " + limit + " homes",
                                NamedTextColor.GRAY)))
                .build());

        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text("Homes", NamedTextColor.GOLD))
                        .body(body)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons)
                        .columns(GRID_COLUMNS)
                        .build()));

        player.showDialog(dialog);
    }

    // ---------------------------------------------------------------- create

    private void openCreate(Player player) {
        int limit = plugin.limitFor(player);
        if (plugin.store().get(player.getUniqueId()).size() >= limit) {
            player.sendMessage(Component.text("You have reached your home limit ("
                    + limit + ").", NamedTextColor.RED));
            return;
        }

        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text("New Home", NamedTextColor.GREEN))
                        .body(List.of(DialogBody.item(new ItemStack(Material.WHITE_BED)).build()))
                        .inputs(List.of(DialogInput.text("name",
                                        Component.text("Name", NamedTextColor.WHITE))
                                .maxLength(24)
                                .width(200)
                                .build()))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(List.of(
                                ActionButton.builder(Component.text("Create", NamedTextColor.GREEN))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) -> {
                                            String name = view.getText("name");
                                            createHome(player, name);
                                        }, ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(Component.text("Back", NamedTextColor.GRAY))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                openHomes(player), ClickCallback.Options.builder().build()))
                                        .build()))
                        .columns(2)
                        .build()));

        player.showDialog(dialog);
    }

    private void createHome(Player player, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) name = "Home " + (plugin.store().get(player.getUniqueId()).size() + 1);

        if (name.length() > 24) name = name.substring(0, 24);

        int limit = plugin.limitFor(player);
        if (plugin.store().get(player.getUniqueId()).size() >= limit) {
            player.sendMessage(Component.text("You have reached your home limit ("
                    + limit + ").", NamedTextColor.RED));
            return;
        }

        if (!plugin.store().add(player.getUniqueId(), name, player.getLocation())) {
            player.sendMessage(Component.text("You already have a home called '"
                    + name + "'.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Home '" + name + "' set.", NamedTextColor.GREEN));
        openHomes(player);
    }

    // ---------------------------------------------------------------- manage

    /** Teleport / Change Icon / Rename / Delete / Back for one home. */
    public void openManage(Player player, Home home) {
        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text(home.getName(), NamedTextColor.GOLD))
                        .body(List.of(DialogBody.item(new ItemStack(home.getIcon())).build()))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(List.of(
                                ActionButton.builder(Component.text("Teleport", NamedTextColor.GREEN))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                teleport(player, home), ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(Component.text("Change Icon", NamedTextColor.AQUA))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                openIconPicker(player, home, "", 0), ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(Component.text("Rename", NamedTextColor.YELLOW))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                openRename(player, home), ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(Component.text("Delete", NamedTextColor.RED))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                openDelete(player, home), ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(Component.text("Back", NamedTextColor.GRAY))
                                        .width(200)
                                        .action(DialogAction.customClick((view, audience) ->
                                                openHomes(player), ClickCallback.Options.builder().build()))
                                        .build()))
                        .columns(2)
                        .build()));

        player.showDialog(dialog);
    }

    private void teleport(Player player, Home home) {
        Location loc = home.toLocation();
        if (loc == null) {
            player.sendMessage(Component.text("That home's world is not loaded.",
                    NamedTextColor.RED));
            return;
        }
        player.teleportAsync(loc).thenAccept(ok -> {
            if (ok) {
                player.sendMessage(Component.text("Teleported to " + home.getName() + ".",
                        NamedTextColor.GREEN));
            }
        });
    }

    // ---------------------------------------------------------------- rename

    private void openRename(Player player, Home home) {
        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text("Rename Home", NamedTextColor.YELLOW))
                        .body(List.of(DialogBody.item(new ItemStack(home.getIcon())).build()))
                        .inputs(List.of(DialogInput.text("name",
                                        Component.text("New name", NamedTextColor.WHITE))
                                .initial(home.getName())
                                .maxLength(24)
                                .width(200)
                                .build()))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(List.of(
                                ActionButton.builder(Component.text("Rename", NamedTextColor.GREEN))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) -> {
                                            String name = view.getText("name");
                                            renameHome(player, home, name);
                                        }, ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(Component.text("Back", NamedTextColor.GRAY))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                openManage(player, home), ClickCallback.Options.builder().build()))
                                        .build()))
                        .columns(2)
                        .build()));

        player.showDialog(dialog);
    }

    private void renameHome(Player player, Home home, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            player.sendMessage(Component.text("That name is empty.", NamedTextColor.RED));
            openManage(player, home);
            return;
        }
        if (name.length() > 24) name = name.substring(0, 24);

        Home clash = plugin.store().byName(player.getUniqueId(), name);
        if (clash != null && clash != home) {
            player.sendMessage(Component.text("You already have a home called '"
                    + name + "'.", NamedTextColor.RED));
            openManage(player, home);
            return;
        }

        home.setName(name);
        plugin.store().save(player.getUniqueId());
        player.sendMessage(Component.text("Renamed to '" + name + "'.", NamedTextColor.GREEN));
        openManage(player, home);
    }

    // ---------------------------------------------------------------- delete

    private void openDelete(Player player, Home home) {
        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text("Delete Home", NamedTextColor.RED))
                        .body(List.of(
                                DialogBody.item(new ItemStack(Material.BARRIER)).build(),
                                DialogBody.plainMessage(Component.text(
                                        "Delete '" + home.getName() + "'? This cannot be undone.",
                                        NamedTextColor.GRAY))))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(List.of(
                                ActionButton.builder(Component.text("Delete", NamedTextColor.RED))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) -> {
                                            plugin.store().remove(player.getUniqueId(), home);
                                            player.sendMessage(Component.text(
                                                    "Deleted '" + home.getName() + "'.",
                                                    NamedTextColor.YELLOW));
                                            openHomes(player);
                                        }, ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(Component.text("Cancel", NamedTextColor.GRAY))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                openManage(player, home), ClickCallback.Options.builder().build()))
                                        .build()))
                        .columns(2)
                        .build()));

        player.showDialog(dialog);
    }

    // ---------------------------------------------------------------- icon picker

    /** Searchable icon browser, paged. */
    public void openIconPicker(Player player, Home home, String search, int page) {
        List<Material> matches = plugin.icons().search(search);

        int totalPages = Math.max(1, (int) Math.ceil(matches.size() / (double) ICONS_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * ICONS_PER_PAGE;
        int to   = Math.min(matches.size(), from + ICONS_PER_PAGE);

        List<ActionButton> buttons = new ArrayList<>();

        for (Material m : matches.subList(from, to)) {
            buttons.add(ActionButton.builder(Component.text(pretty(m), NamedTextColor.WHITE))
                    .tooltip(Component.text("Use this icon", NamedTextColor.DARK_GRAY))
                    .width(98)
                    .action(DialogAction.customClick((view, audience) -> {
                        home.setIcon(m);
                        plugin.store().save(player.getUniqueId());
                        player.sendMessage(Component.text("Icon set to " + pretty(m) + ".",
                                NamedTextColor.GREEN));
                        openManage(player, home);
                    }, ClickCallback.Options.builder().build()))
                    .build());
        }

        // Navigation row.
        buttons.add(ActionButton.builder(Component.text("Search", NamedTextColor.AQUA))
                .width(98)
                .action(DialogAction.customClick((view, audience) -> {
                    String q = view.getText("search");
                    openIconPicker(player, home, q == null ? "" : q, 0);
                }, ClickCallback.Options.builder().build()))
                .build());

        buttons.add(ActionButton.builder(Component.text("Default", NamedTextColor.YELLOW))
                .width(98)
                .action(DialogAction.customClick((view, audience) -> {
                    home.setIcon(Material.WHITE_BED);
                    plugin.store().save(player.getUniqueId());
                    player.sendMessage(Component.text("Icon reset to White Bed.",
                            NamedTextColor.GREEN));
                    openManage(player, home);
                }, ClickCallback.Options.builder().build()))
                .build());

        if (safePage > 0) {
            final int prev = safePage - 1;
            buttons.add(ActionButton.builder(Component.text("Previous", NamedTextColor.GRAY))
                    .width(98)
                    .action(DialogAction.customClick((view, audience) ->
                            openIconPicker(player, home, search, prev), ClickCallback.Options.builder().build()))
                    .build());
        }
        if (safePage < totalPages - 1) {
            final int next = safePage + 1;
            buttons.add(ActionButton.builder(Component.text("Show More", NamedTextColor.GRAY))
                    .width(98)
                    .action(DialogAction.customClick((view, audience) ->
                            openIconPicker(player, home, search, next), ClickCallback.Options.builder().build()))
                    .build());
        }

        buttons.add(ActionButton.builder(Component.text("Back", NamedTextColor.GRAY))
                .width(98)
                .action(DialogAction.customClick((view, audience) ->
                        openManage(player, home), ClickCallback.Options.builder().build()))
                .build());

        final int shownPage = safePage;
        final int shownTotal = totalPages;

        Dialog dialog = Dialog.create(b -> b.empty()
                .base(DialogBase.builder(Component.text("Choose Icon", NamedTextColor.GOLD))
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "Page " + (shownPage + 1) + " / " + shownTotal
                                        + "  •  " + matches.size() + " items",
                                NamedTextColor.GRAY))))
                        .inputs(List.of(DialogInput.text("search",
                                        Component.text("Search", NamedTextColor.WHITE))
                                .initial(search == null ? "" : search)
                                .maxLength(32)
                                .width(200)
                                .build()))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(buttons)
                        .columns(GRID_COLUMNS)
                        .build()));

        player.showDialog(dialog);
    }

    /** STONE_BRICKS -> Stone Bricks */
    static String pretty(Material m) {
        String[] parts = m.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
