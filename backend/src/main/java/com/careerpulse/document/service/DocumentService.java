package com.careerpulse.document.service;

import com.careerpulse.document.dto.DocumentResponse;
import com.careerpulse.document.entity.Document;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.entity.FileFormat;
import com.careerpulse.document.repository.DocumentRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final Set<FileFormat> SUPPORTED_FORMATS = Set.of(
            FileFormat.PDF,
            FileFormat.DOCX
    );

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final Path storageRoot;

    public DocumentService(
            UserRepository userRepository,
            DocumentRepository documentRepository,
            @Value("${document.storage-dir:uploads/documents}") String storageDirectory
    ) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public DocumentResponse store(
            String email,
            DocumentType documentType,
            MultipartFile file
    ) {
        ValidatedFile validated = validate(file);
        User user = findUser(email);
        Path storedPath = buildStoredPath(
                user.getUserId(),
                documentType,
                validated.fileFormat()
        );

        storeFile(file, storedPath);

        Document document = documentRepository
                .findByUser_UserIdAndDocumentType(user.getUserId(), documentType)
                .orElse(null);
        String previousStoragePath = document != null ? document.getStoragePath() : null;

        if (document == null) {
            document = new Document(
                    user,
                    documentType,
                    validated.originalFilename(),
                    storedPath.toString(),
                    validated.fileFormat(),
                    validated.size()
            );
        } else {
            document.replaceFile(
                    validated.originalFilename(),
                    storedPath.toString(),
                    validated.fileFormat(),
                    validated.size()
            );
        }

        try {
            Document saved = documentRepository.saveAndFlush(document);
            deletePreviousFile(previousStoragePath, storedPath);
            return toResponse(saved);
        } catch (RuntimeException exception) {
            deleteStoredFile(storedPath);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(
            String email,
            DocumentType documentType
    ) {
        User user = findUser(email);
        return toResponse(findOwnedDocument(user.getUserId(), documentType));
    }

    @Transactional
    public void deleteDocument(
            String email,
            DocumentType documentType
    ) {
        User user = findUser(email);
        Document document = findOwnedDocument(user.getUserId(), documentType);
        Path storedPath = Path.of(document.getStoragePath()).toAbsolutePath().normalize();

        documentRepository.delete(document);
        documentRepository.flush();
        deleteStoredFile(storedPath);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));
    }

    private Document findOwnedDocument(
            Long userId,
            DocumentType documentType
    ) {
        return documentRepository
                .findByUser_UserIdAndDocumentType(userId, documentType)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "문서를 찾을 수 없습니다."
                ));
    }

    private ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "업로드할 문서 파일이 필요합니다."
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일 이름을 확인할 수 없습니다."
            );
        }

        FileFormat fileFormat = fileFormatOf(originalFilename);
        if (fileFormat == null || !SUPPORTED_FORMATS.contains(fileFormat)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PDF 또는 DOCX 파일만 업로드할 수 있습니다."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "문서 파일은 10MB 이하여야 합니다."
            );
        }

        return new ValidatedFile(
                Path.of(originalFilename).getFileName().toString(),
                fileFormat,
                file.getSize()
        );
    }

    private FileFormat fileFormatOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }

        try {
            return FileFormat.valueOf(
                    filename.substring(dotIndex + 1).toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Path buildStoredPath(
            Long userId,
            DocumentType documentType,
            FileFormat fileFormat
    ) {
        String storedFilename = "%d-%s-%s.%s".formatted(
                userId,
                documentType.name().toLowerCase(Locale.ROOT),
                UUID.randomUUID(),
                fileFormat.name().toLowerCase(Locale.ROOT)
        );
        Path storedPath = storageRoot.resolve(storedFilename).normalize();
        if (!storedPath.startsWith(storageRoot)) {
            throw new IllegalStateException("문서 저장 경로가 올바르지 않습니다.");
        }
        return storedPath;
    }

    private void storeFile(MultipartFile file, Path storedPath) {
        try {
            Files.createDirectories(storageRoot);
            Files.copy(
                    file.getInputStream(),
                    storedPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "문서 파일을 저장하지 못했습니다.",
                    exception
            );
        }
    }

    private void deletePreviousFile(String previousStoragePath, Path currentPath) {
        if (previousStoragePath == null || previousStoragePath.isBlank()) {
            return;
        }

        Path previousPath = Path.of(previousStoragePath).toAbsolutePath().normalize();
        if (!previousPath.equals(currentPath)) {
            deleteStoredFile(previousPath);
        }
    }

    private void deleteStoredFile(Path path) {
        try {
            if (path.startsWith(storageRoot)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // DB 처리 성공을 파일 정리 실패로 되돌리지 않는다.
        }
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getDocumentId(),
                document.getDocumentType(),
                document.getOriginalFilename(),
                document.getFileFormat(),
                document.getFileSize(),
                document.getProcessingStatus(),
                document.getUploadedAt(),
                document.getAnalyzedAt()
        );
    }

    private record ValidatedFile(
            String originalFilename,
            FileFormat fileFormat,
            long size
    ) {
    }
}
