package io.imiocode.persistence;

import io.imiocode.conversation.SystemReminder;
import io.imiocode.instruction.FileInstructionLoader;
import io.imiocode.instruction.GitProjectLocator;
import io.imiocode.instruction.InstructionLoadRequest;
import io.imiocode.instruction.InstructionLoader;
import io.imiocode.instruction.InstructionReminderFormatter;
import io.imiocode.instruction.InstructionSnapshot;
import io.imiocode.memory.MemoryDocument;
import io.imiocode.memory.MemoryManager;
import io.imiocode.memory.MemoryReminderFormatter;
import io.imiocode.memory.MemoryScope;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 每轮只检查文件元数据，发生变化时才重读正文。 */
public final class DefaultPersistentContextProvider implements PersistentContextProvider {
    private final InstructionLoader instructionLoader;
    private final InstructionLoadRequest request;
    private final InstructionReminderFormatter instructionFormatter;
    private final MemoryManager memoryManager;
    private final MemoryReminderFormatter memoryFormatter;
    private final PersistenceEventListener listener;
    private final Set<Path> topInstructionCandidates;
    private final Set<Path> memoryPaths;
    private InstructionSnapshot instructions = InstructionSnapshot.empty();
    private List<MemoryDocument> memories = List.of();
    private Map<Path, FileFingerprint> instructionFingerprints = Map.of();
    private Map<Path, FileFingerprint> memoryFingerprints = Map.of();

    public DefaultPersistentContextProvider(
            InstructionLoader instructionLoader,
            InstructionLoadRequest request,
            InstructionReminderFormatter instructionFormatter,
            MemoryManager memoryManager,
            MemoryReminderFormatter memoryFormatter,
            Path userMemoryPath,
            Path projectMemoryPath,
            PersistenceEventListener listener) {
        this.instructionLoader = instructionLoader;
        this.request = request;
        this.instructionFormatter = instructionFormatter;
        this.memoryManager = memoryManager;
        this.memoryFormatter = memoryFormatter;
        this.listener = listener == null ? PersistenceEventListener.noop() : listener;
        this.topInstructionCandidates = discoverCandidates(request);
        this.memoryPaths = Set.of(userMemoryPath.toAbsolutePath().normalize(), projectMemoryPath.toAbsolutePath().normalize());
        reloadInstructions();
        reloadMemories();
    }

    @Override
    public synchronized List<SystemReminder> currentReminders() {
        refreshIfChanged();
        List<SystemReminder> result = new ArrayList<>();
        result.addAll(instructionFormatter.format(instructions, request.workspace(), request.userHome()));
        result.addAll(memoryFormatter.format(memories));
        return List.copyOf(result);
    }

    @Override
    public synchronized void reloadInstructions() {
        try {
            InstructionSnapshot loaded = instructionLoader.load(request);
            instructions = loaded;
            LinkedHashSet<Path> tracked = new LinkedHashSet<>(topInstructionCandidates);
            tracked.addAll(loaded.dependencies());
            instructionFingerprints = fingerprints(tracked);
            loaded.problems().forEach(problem -> listener.onEvent(new PersistenceEvent.Warning(
                    "[指令] " + problem.source().getFileName() + "：" + problem.safeMessage())));
        } catch (RuntimeException exception) {
            listener.onEvent(new PersistenceEvent.Warning("[指令] 刷新失败，继续使用上一份有效上下文"));
        }
    }

    @Override
    public synchronized void reloadMemories() {
        try {
            memories = memoryManager.loadEnabledScopes();
            memoryFingerprints = fingerprints(memoryPaths);
        } catch (RuntimeException exception) {
            listener.onEvent(new PersistenceEvent.Warning("[记忆] 刷新失败，继续使用上一份有效上下文"));
        }
    }

    private void refreshIfChanged() {
        LinkedHashSet<Path> instructionPaths = new LinkedHashSet<>(topInstructionCandidates);
        instructionPaths.addAll(instructions.dependencies());
        if (!fingerprints(instructionPaths).equals(instructionFingerprints)) reloadInstructions();
        if (!fingerprints(memoryPaths).equals(memoryFingerprints)) reloadMemories();
    }

    private static Map<Path, FileFingerprint> fingerprints(Set<Path> paths) {
        LinkedHashMap<Path, FileFingerprint> result = new LinkedHashMap<>();
        paths.stream().sorted().forEach(path -> result.put(path, FileFingerprint.capture(path)));
        return Map.copyOf(result);
    }

    private static Set<Path> discoverCandidates(InstructionLoadRequest request) {
        LinkedHashSet<Path> paths = new LinkedHashSet<>();
        paths.add(request.userHome().resolve(".imiocode").resolve(FileInstructionLoader.FILE_NAME));
        Path root = new GitProjectLocator().locate(request.workspace());
        Path relative = root.relativize(request.workspace());
        Path current = root;
        paths.add(current.resolve(FileInstructionLoader.FILE_NAME));
        for (Path part : relative) {
            current = current.resolve(part);
            paths.add(current.resolve(FileInstructionLoader.FILE_NAME));
        }
        return Set.copyOf(paths);
    }
}
