package io.github.xxvw.mcnativechunkengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.xxvw.mcnativechunkengine.brand.UpstreamProvenance;
import io.github.xxvw.mcnativechunkengine.config.McneConfiguration;
import io.github.xxvw.mcnativechunkengine.nativebridge.EngineMode;
import io.github.xxvw.mcnativechunkengine.server.McneRuntime;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class McneCommandTest {
    private CommandSourceStack source(CommandSender sender) {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);
        return source;
    }

    private CommandDispatcher<CommandSourceStack> dispatcher(AtomicLong clock) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(McneCommand.create(() -> new McneRuntime.Status(
            new UpstreamProvenance("fixture", "1", "0000000000000000000000000000000000000000"),
            new McneConfiguration(false, "native.enabled=false"), EngineMode.JAVA_BASELINE), clock::get));
        return dispatcher;
    }

    @Test
    void rejectsUnauthorizedRootAndAdminOnlySubcommands() throws Exception {
        CommandSender sender = mock(CommandSender.class);
        CommandSourceStack source = source(sender);
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(new AtomicLong());
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("mcne status", source));
        when(sender.hasPermission("mcne.metrics")).thenReturn(true);
        assertEquals(1, dispatcher.execute("mcne help", source));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("mcne status", source));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("mcne native info", source));
    }

    @Test
    void rateLimitIsPerSenderAndUsesMonotonicWindow() throws Exception {
        AtomicLong clock = new AtomicLong();
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(clock);
        CommandSender first = mock(CommandSender.class);
        CommandSender second = mock(CommandSender.class);
        when(first.hasPermission("mcne.admin")).thenReturn(true);
        when(second.hasPermission("mcne.admin")).thenReturn(true);
        CommandSourceStack source = source(first);
        assertEquals(1, dispatcher.execute("mcne status", source));
        assertEquals(0, dispatcher.execute("mcne native info", source));
        assertEquals(1, dispatcher.execute("mcne native info", source(second)));
        clock.set(1_000_000_000L);
        assertEquals(1, dispatcher.execute("mcne native info", source));
    }

    @Test
    void completionHidesAdminCommandsFromMetricsOnlySender() throws Exception {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("mcne.metrics")).thenReturn(true);
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(new AtomicLong());
        var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("mcne ", source(sender))).get();
        assertTrue(suggestions.getList().stream().anyMatch(item -> item.getText().equals("help")));
        assertFalse(suggestions.getList().stream().anyMatch(item -> item.getText().equals("status")));
        assertFalse(suggestions.getList().stream().anyMatch(item -> item.getText().equals("native")));
    }
}
