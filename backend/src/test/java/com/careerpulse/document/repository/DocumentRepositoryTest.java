package com.careerpulse.document.repository;

import com.careerpulse.document.entity.Document;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.entity.FileFormat;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(createUser("owner@example.com"));
        otherUser = userRepository.save(createUser("other@example.com"));
    }

    @Test
    void savesAndFindsDocumentsByUser() {
        Document resume = documentRepository.save(createDocument(owner, DocumentType.RESUME));
        Document portfolio = documentRepository.save(createDocument(owner, DocumentType.PORTFOLIO));
        documentRepository.save(createDocument(otherUser, DocumentType.SELF_INTRO));

        List<Document> documents = documentRepository
                .findAllByUser_UserIdOrderByUploadedAtDesc(owner.getUserId());

        assertThat(documents).containsExactlyInAnyOrder(resume, portfolio);
    }

    @Test
    void findsOwnedDocumentByDocumentId() {
        Document document = documentRepository.save(createDocument(owner, DocumentType.RESUME));

        assertThat(documentRepository.findByDocumentIdAndUser_UserId(
                document.getDocumentId(),
                owner.getUserId()
        )).contains(document);
        assertThat(documentRepository.findByDocumentIdAndUser_UserId(
                document.getDocumentId(),
                otherUser.getUserId()
        )).isEmpty();
    }

    @Test
    void checksDocumentExistenceByUserAndDocumentType() {
        documentRepository.save(createDocument(owner, DocumentType.SELF_INTRO));

        assertThat(documentRepository.existsByUser_UserIdAndDocumentType(
                owner.getUserId(),
                DocumentType.SELF_INTRO
        )).isTrue();
        assertThat(documentRepository.existsByUser_UserIdAndDocumentType(
                otherUser.getUserId(),
                DocumentType.SELF_INTRO
        )).isFalse();
    }

    private static User createUser(String email) {
        return new User(
                email,
                "Test User",
                "NEW",
                null,
                "DOCUMENT_ANALYSIS_V1"
        );
    }

    private static Document createDocument(User user, DocumentType documentType) {
        String fileName = documentType.name().toLowerCase() + ".pdf";

        return new Document(
                user,
                documentType,
                fileName,
                user.getEmail() + "/" + fileName,
                FileFormat.PDF,
                1024L
        );
    }
}
