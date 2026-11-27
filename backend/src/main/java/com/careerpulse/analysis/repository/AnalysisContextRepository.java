package com.careerpulse.analysis.repository;

import com.careerpulse.analysis.dto.JobPostingSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AnalysisContextRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnalysisContextRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<JobPostingSnapshot> findJob(Long jobId) {
        return jdbcTemplate.query(
                """
                SELECT job_id, title, company_name, job_category, region,
                       description, deadline, status
                  FROM job_postings
                 WHERE job_id = ?
                """,
                (rs, rowNum) -> new JobPostingSnapshot(
                        rs.getLong("job_id"),
                        rs.getString("title"),
                        rs.getString("company_name"),
                        rs.getString("job_category"),
                        rs.getString("region"),
                        rs.getString("description"),
                        rs.getObject("deadline", java.time.LocalDate.class),
                        rs.getString("status")
                ),
                jobId
        ).stream().findFirst();
    }

    public List<Long> findCompletedDocumentIds(Long userId) {
        return jdbcTemplate.queryForList(
                """
                SELECT document_id
                  FROM user_documents
                 WHERE user_id = ?
                   AND processing_status = 'COMPLETED'
                 ORDER BY document_id
                """,
                Long.class,
                userId
        );
    }

    public boolean hasAnalysisConsent(Long userId) {
        Boolean consented = jdbcTemplate.queryForObject(
                "SELECT consented FROM users WHERE user_id = ?",
                Boolean.class,
                userId
        );
        return Boolean.TRUE.equals(consented);
    }

    public boolean isOwnedCompletedChunk(Long userId, Long chunkId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM document_chunks c
                  JOIN user_documents d ON d.document_id = c.document_id
                 WHERE c.chunk_id = ?
                   AND d.user_id = ?
                   AND d.processing_status = 'COMPLETED'
                """,
                Integer.class,
                chunkId,
                userId
        );
        return count != null && count == 1;
    }

    public boolean isOwnedCompletedDocument(Long userId, Long documentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM user_documents
                 WHERE document_id = ?
                   AND user_id = ?
                   AND processing_status = 'COMPLETED'
                """,
                Integer.class,
                documentId,
                userId
        );
        return count != null && count == 1;
    }

    public Optional<Long> findOwnedChunkIdByEvidenceRef(
            Long userId,
            Long documentId,
            String evidenceRef
    ) {
        if (documentId == null || evidenceRef == null || evidenceRef.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                """
                SELECT c.chunk_id
                  FROM document_chunks c
                  JOIN user_documents d ON d.document_id = c.document_id
                 WHERE d.user_id = ?
                   AND d.document_id = ?
                   AND d.processing_status = 'COMPLETED'
                   AND c.embedding_ref = ?
                 LIMIT 1
                """,
                (rs, rowNum) -> rs.getLong("chunk_id"),
                userId,
                documentId,
                evidenceRef
        ).stream().findFirst();
    }

    public Optional<EvidenceSnapshot> findOwnedEvidence(Long userId, Long chunkId) {
        if (chunkId == null) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                """
                SELECT c.chunk_id, d.document_id, d.document_type,
                       c.chunk_text, c.source_location
                  FROM document_chunks c
                  JOIN user_documents d ON d.document_id = c.document_id
                 WHERE c.chunk_id = ?
                   AND d.user_id = ?
                   AND d.processing_status = 'COMPLETED'
                """,
                (rs, rowNum) -> new EvidenceSnapshot(
                        rs.getLong("chunk_id"),
                        rs.getLong("document_id"),
                        rs.getString("document_type"),
                        rs.getString("chunk_text"),
                        rs.getString("source_location")
                ),
                chunkId,
                userId
        ).stream().findFirst();
    }

    public record EvidenceSnapshot(
            Long chunkId,
            Long documentId,
            String documentType,
            String text,
            String sourceLocation
    ) {}
}
