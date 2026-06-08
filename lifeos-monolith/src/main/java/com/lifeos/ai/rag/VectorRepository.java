package com.lifeos.ai.rag;

import com.lifeos.ai.embedding.EmbeddingClient;
import com.lifeos.note.entity.Note;
import com.lifeos.note.repository.NoteEmbeddingRepository;
import com.lifeos.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Vector Repository
 * Handles vector similarity search using pgvector
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VectorRepository {

    private final NoteEmbeddingRepository noteEmbeddingRepository;
    private final NoteRepository noteRepository;
    private final EmbeddingClient embeddingClient;

    /**
     * Search for similar notes using vector similarity
     * @param userId User ID
     * @param queryText Query text
     * @param topK Number of results to return
     * @return List of similar notes with similarity scores
     */
    public List<SimilarNote> searchSimilarNotes(Long userId, String queryText, int topK) {
        try {
            // Generate embedding for query
            float[] queryEmbedding = embeddingClient.generateEmbedding(queryText);

            // Convert to PostgreSQL vector format: '[0.1,0.2,0.3,...]'
            String vectorString = floatArrayToVectorString(queryEmbedding);

            // Query similar notes using pgvector
            List<Object[]> results = noteEmbeddingRepository.findSimilarNotes(userId, vectorString, topK);

            // Convert results to SimilarNote objects
            List<SimilarNote> similarNotes = new ArrayList<>();
            for (Object[] row : results) {
                Long noteId = ((BigInteger) row[0]).longValue();
                Double similarity = ((Number) row[2]).doubleValue();

                // Fetch note details
                Note note = noteRepository.findById(noteId).orElse(null);
                if (note != null) {
                    SimilarNote similarNote = new SimilarNote();
                    similarNote.setNoteId(noteId);
                    similarNote.setUserId(userId);
                    similarNote.setSimilarity(similarity);
                    similarNote.setTitle(note.getTitle());
                    similarNote.setContent(note.getContent());
                    similarNote.setTags(note.getTags());
                    similarNotes.add(similarNote);
                }
            }

            log.info("Found {} similar notes for query (topK={})", similarNotes.size(), topK);
            return similarNotes;

        } catch (Exception e) {
            log.error("Failed to search similar notes: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert float array to PostgreSQL vector string format
     */
    private String floatArrayToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Check if a note has an embedding
     */
    public boolean hasEmbedding(Long noteId) {
        return noteEmbeddingRepository.findByNoteId(noteId).isPresent();
    }

    /**
     * Get embedding coverage for a user
     */
    public double getEmbeddingCoverage(Long userId) {
        long totalNotes = noteRepository.countByUserId(userId);
        long embeddedNotes = noteEmbeddingRepository.countByUserId(userId);

        if (totalNotes == 0) return 0.0;
        return (double) embeddedNotes / totalNotes;
    }
}
