package com.lifeos.note.service;

import com.lifeos.note.dto.NoteRequest;
import com.lifeos.note.dto.NoteResponse;
import com.lifeos.note.entity.Note;
import com.lifeos.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note Service
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    @Transactional
    public NoteResponse createNote(Long userId, NoteRequest request) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setTags(request.getTags());
        note.setPinned(request.getPinned() != null ? request.getPinned() : false);

        note = noteRepository.save(note);

        // TODO: Trigger async embedding generation for RAG
        // embeddingService.generateEmbeddingAsync(note.getId(), note.getContent());

        return toResponse(note);
    }

    @Transactional
    public NoteResponse updateNote(Long userId, Long noteId, NoteRequest request) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setTags(request.getTags());
        if (request.getPinned() != null) {
            note.setPinned(request.getPinned());
        }

        note = noteRepository.save(note);

        // TODO: Update embedding
        // embeddingService.updateEmbeddingAsync(note.getId(), note.getContent());

        return toResponse(note);
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        noteRepository.delete(note);

        // TODO: Delete embedding
        // embeddingService.deleteEmbedding(noteId);
    }

    public NoteResponse getNote(Long userId, Long noteId) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        return toResponse(note);
    }

    public Page<NoteResponse> listNotes(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return noteRepository.findByUserIdOrderByPinnedDescUpdateTimeDesc(userId, pageable)
                .map(this::toResponse);
    }

    public Page<NoteResponse> searchNotes(Long userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return noteRepository.searchByKeyword(userId, keyword, pageable)
                .map(this::toResponse);
    }

    private NoteResponse toResponse(Note note) {
        NoteResponse response = new NoteResponse();
        BeanUtils.copyProperties(note, response);
        return response;
    }
}
