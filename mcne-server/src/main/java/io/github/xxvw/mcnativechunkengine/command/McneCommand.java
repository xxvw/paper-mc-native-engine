package io.github.xxvw.mcnativechunkengine.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.xxvw.mcnativechunkengine.brand.UpstreamProvenance;
import io.github.xxvw.mcnativechunkengine.server.McneRuntime;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Built-in, permission-gated diagnostics. All executions occur on the server thread. */
public final class McneCommand {
    private static final long DIAGNOSTIC_COOLDOWN_NS = 1_000_000_000L;
    private final Map<CommandSender, Long> lastExecution = new WeakHashMap<>();
    private final Supplier<McneRuntime.Status> status;
    private final LongSupplier clock;

    private McneCommand(Supplier<McneRuntime.Status> status, LongSupplier clock) {
        this.status = status;
        this.clock = clock;
    }

    public static LiteralCommandNode<CommandSourceStack> create() {
        return create(McneRuntime::status, System::nanoTime);
    }

    static LiteralCommandNode<CommandSourceStack> create(Supplier<McneRuntime.Status> status, LongSupplier clock) {
        McneCommand command = new McneCommand(status, clock);
        return Commands.literal("mcne")
            .requires(source -> authorized(source.getSender()))
            .executes(context -> command.execute(context, "help"))
            .then(Commands.literal("help").executes(context -> command.execute(context, "help")))
            .then(Commands.literal("status").requires(source -> source.getSender().hasPermission("mcne.admin"))
                .executes(context -> command.execute(context, "status")))
            .then(Commands.literal("native").requires(source -> source.getSender().hasPermission("mcne.admin"))
                .then(Commands.literal("info").executes(context -> command.execute(context, "native"))))
            .build();
    }

    private static boolean authorized(CommandSender sender) {
        return sender.hasPermission("mcne.admin") || sender.hasPermission("mcne.benchmark") || sender.hasPermission("mcne.metrics");
    }

    private int execute(CommandContext<CommandSourceStack> context, String action) {
        CommandSender sender = context.getSource().getSender();
        long now = this.clock.getAsLong();
        Long previous = this.lastExecution.get(sender);
        if (previous != null && now - previous < DIAGNOSTIC_COOLDOWN_NS) {
            sender.sendMessage(Component.text("MCNE_RATE_LIMIT: wait one second before another diagnostic command", NamedTextColor.RED));
            return 0;
        }
        this.lastExecution.put(sender, now);
        if ("help".equals(action)) {
            sender.sendMessage(Component.text("/mcne help | status | native info. Benchmarks and metrics are pending Phase 2. Dedicated disposable worlds only."));
        } else {
            McneRuntime.Status current = this.status.get();
            sender.sendMessage(Component.text(UpstreamProvenance.BRAND + " | mode=" + current.mode()));
            sender.sendMessage(Component.text(current.provenance().display()));
            sender.sendMessage(Component.text("Native ABI/SIMD: unavailable; " + current.configuration().diagnostic()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
