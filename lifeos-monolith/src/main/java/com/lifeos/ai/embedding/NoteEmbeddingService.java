package com.lifeos.ai.embedding;

import com.lifeos.note.entity.Note;
import com.lifeos.note.entity.NoteEmbedding;
import com.lifeos.note.repository.NoteEmbeddingRepository;
import com.lifeos.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Note Embedding Service
 * Handles automatic vectorization of notes for RAG
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteEmbeddingService {

    private final NoteRepository noteRepository;
    private final NoteEmbeddingRepository noteEmbeddingRepository;
    private final EmbeddingClient embeddingClient;

    /**
     * Generate embedding for a note asynchronously
     * Called after note creation/update
     */
    @Async("aiTaskExecutor")
    @Transactional
    public void generateEmbeddingAsync(Long noteId) {
        try {
            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> new RuntimeException("Note not found: " + noteId));

            // Combine title and content for embedding
            String textToEmbed = note.getTitle() + "\n\n" + (note.getContent() != null ? note.getContent() : "");

            // Truncate if too long (most embedding models have token limits)
            if (textToEmbed.length() > 8000) {
                textToEmbed = textToEmbed.substring(0, 8000);
            }

            log.info("Generating embedding for note {}", noteId);
            float[] embedding = embeddingClient.generateEmbedding(textToEmbed);

            // Save or update embedding
            Optional<NoteEmbedding> existingOpt = noteEmbeddingRepository.findByNoteId(noteId);

            NoteEmbedding noteEmbedding;
            if (existingOpt.isPresent()) {
                noteEmbedding = existingOpt.get();
                noteEmbedding.setEmbedding(embedding);
            } else {
                noteEmbedding = new NoteEmbedding();
                noteEmbedding.setNoteId(noteId);
                noteEmbedding.setUserId(note.getUserId());
                noteEmbedding.setEmbedding(embedding);
            }

            noteEmbeddingRepository.save(noteEmbedding);
            log.info("Successfully generated embedding for note {}", noteId);

        } catch (Exception e) {
            log.error("Failed to generate embedding for note {}: {}", noteId, e.getMessage(), e);
        }
    }

    /**
     * Delete embedding when note is deleted
     */
    @Transactional
    public void deleteEmbedding(Long noteId) {
        noteEmbeddingRepository.deleteByNoteId(noteId);
        log.info("Deleted embedding for note {}", noteId);
    }

    /**
     * Regenerate embeddings for all notes of a user
     * Useful for model upgrades or data migration
     */
    @Async("aiTaskExecutor")
    @Transactional
    public void regenerateAllEmbeddings(Long userId) {
        log.info("Starting embedding regeneration for user {}", userId);

        noteRepository.findByUserId(userId).forEach(note -> {
            try {
                generateEmbeddingAsync(note.getId());
            } catch (Exception e) {
                log.error("Failed to regenerate embedding for note {}: {}", note.getId(), e.getMessage());
            }
        });

        log.info("Completed embedding regeneration for user {}", userId);
    }
}
