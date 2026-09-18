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

    private static final int GRID_COLUMNS = 1;
    private static final int PAGE_SIZE = 5;
    private static final int ICONS_PER_PAGE = 60;

    private final FantaHomes plugin;

    public Menus(FantaHomes plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- icons

    /** "<sprite> Name" for a button label, or plain text when the item has no sprite. */
    private Component iconLabel(Material material, String text, NamedTextColor color) {
        Component name = Component.text(text, color);
        Component icon = plugin.sprites().icon(material);
        return icon == null ? name : icon.append(Component.text(" ")).append(name);
    }

    // ---------------------------------------------------------------- main grid

    /** The homes grid: one button per home, then a New Home button per free slot. */
    public void openHomes(Player player) {
        openHomes(player, 0);
    }

    /** The homes grid, paged: shows PAGE_SIZE entries plus a "More homes" button. */
    public void openHomes(Player player, int page) {
        List<Home> homes = plugin.store().get(player.getUniqueId());
        int limit = plugin.limitFor(player);

        // One trailing "New Home" slot while the player is under their limit.
        int newSlots = homes.size() < limit ? 1 : 0;
        int totalEntries = homes.size() + newSlots;

        int pages = Math.max(1, (int) Math.ceil(totalEntries / (double) PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= pages) page = pages - 1;

        int from = page * PAGE_SIZE;
        int to = Math.min(totalEntries, from + PAGE_SIZE);

        List<ActionButton> buttons = new ArrayList<>();

        for (int i = from; i < to; i++) {
            if (i < homes.size()) {
                final Home home = homes.get(i);
                buttons.add(ActionButton.builder(
                                iconLabel(home.getIcon(), home.getName(), NamedTextColor.WHITE))
                        .tooltip(Component.text("Click to manage", NamedTextColor.GRAY))
                        .width(150)
                        .action(DialogAction.customClick((view, audience) ->
                                openManage(player, home), ClickCallback.Options.builder().build()))
                        .build());
            } else {
                buttons.add(ActionButton.builder(
                                iconLabel(Material.WHITE_BED, "New Home", NamedTextColor.GREEN))
                        .tooltip(Component.text("Save your current position", NamedTextColor.DARK_GRAY))
                        .width(150)
                        .action(DialogAction.customClick((view, audience) ->
                                createHere(player), ClickCallback.Options.builder().build()))
                        .build());
            }
        }

        final int cur = page;
        if (page > 0) {
            buttons.add(ActionButton.builder(Component.text("< Back", NamedTextColor.GRAY))
                    .width(150)
                    .action(DialogAction.customClick((view, audience) ->
                            openHomes(player, cur - 1), ClickCallback.Options.builder().build()))
                    .build());
        }
        if (page < pages - 1) {
            buttons.add(ActionButton.builder(Component.text("More homes >", NamedTextColor.YELLOW))
                    .width(150)
                    .action(DialogAction.customClick((view, audience) ->
                            openHomes(player, cur + 1), ClickCallback.Options.builder().build()))
                    .build());
        }

        // Buttons carry their own sprites now, so the body is just the counter.
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.item(new ItemStack(Material.WHITE_BED))
                .description(DialogBody.plainMessage(
                        Component.text(homes.size() + " / " + limit + " homes"
                                + (pages > 1 ? "   (page " + (page + 1) + " of " + pages + ")" : ""),
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

    /** One click = home saved right here, auto-named. Rename later from manage. */
    private void createHere(Player player) {
        int limit = plugin.limitFor(player);
        List<Home> homes = plugin.store().get(player.getUniqueId());
        if (homes.size() >= limit) {
            player.sendMessage(Component.text("You have reached your home limit ("
                    + limit + ").", NamedTextColor.RED));
            return;
        }

        String name = nextFreeName(player);
        if (!plugin.store().add(player.getUniqueId(), name, player.getLocation())) {
            player.sendMessage(Component.text("Could not create that home.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Home '" + name
                + "' created. Click it to rename or change its icon.", NamedTextColor.GREEN));
        openHomes(player);
    }

    /** First unused "Home N". */
    private String nextFreeName(Player player) {
        for (int i = 1; i <= 1001; i++) {
            String candidate = "Home " + i;
            if (plugin.store().byName(player.getUniqueId(), candidate) == null) return candidate;
        }
        return "Home " + System.currentTimeMillis();
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
                                ActionButton.builder(iconLabel(Material.ENDER_PEARL, "Teleport", NamedTextColor.GREEN))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                teleport(player, home), ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(iconLabel(Material.ITEM_FRAME, "Change Icon", NamedTextColor.AQUA))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                openIconPicker(player, home, "", 0), ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(iconLabel(Material.NAME_TAG, "Rename", NamedTextColor.YELLOW))
                                        .width(98)
                                        .action(DialogAction.customClick((view, audience) ->
                                                openRename(player, home), ClickCallback.Options.builder().build()))
                                        .build(),
                                ActionButton.builder(iconLabel(Material.BARRIER, "Delete", NamedTextColor.RED))
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
                                ActionButton.builder(iconLabel(Material.BARRIER, "Delete", NamedTextColor.RED))
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
            buttons.add(ActionButton.builder(iconLabel(m, pretty(m), NamedTextColor.WHITE))
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
        buttons.add(ActionButton.builder(iconLabel(Material.SPYGLASS, "Search", NamedTextColor.AQUA))
                .width(98)
                .action(DialogAction.customClick((view, audience) -> {
                    String q = view.getText("search");
                    openIconPicker(player, home, q == null ? "" : q, 0);
                }, ClickCallback.Options.builder().build()))
                .build());

        buttons.add(ActionButton.builder(iconLabel(Material.WHITE_BED, "Default", NamedTextColor.YELLOW))
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
