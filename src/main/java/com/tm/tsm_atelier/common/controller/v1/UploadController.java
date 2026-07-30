package com.tm.tsm_atelier.common.controller.v1;

import com.tm.tsm_atelier.common.dto.UploadResponseDTO;
import com.tm.tsm_atelier.domain.common.port.StoragePort;
import com.tm.tsm_atelier.common.exception.custom.InvalidFileTypeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/uploads")
@RequiredArgsConstructor
public class UploadController {

	private final StoragePort storagePort;

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	@PostMapping
	public ResponseEntity<UploadResponseDTO> uploadImages(@RequestParam("files") List<MultipartFile> files,
			@RequestParam(value = "folder", defaultValue = "general") String folder) {

		List<String> urls = new ArrayList<>();
		for (MultipartFile file : files) {
			validateFile(file);
			String url = storagePort.uploadImage(file, folder);
			urls.add(url);
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponseDTO(urls));
	}

	private void validateFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new InvalidFileTypeException("O arquivo está vazio.");
		}

		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new InvalidFileTypeException("Formato não suportado: " + contentType);
		}

		try {
			byte[] header = new byte[12];
			int read = file.getInputStream().read(header);
			if (read == -1) {
				throw new InvalidFileTypeException("O arquivo está vazio.");
			}

			if (isJPEG(header) || isPNG(header) || isWEBP(header)) {
				return;
			}
			throw new InvalidFileTypeException("Assinatura do arquivo inválida. Formato real não suportado.");
		} catch (IOException e) {
			throw new RuntimeException("Erro ao ler o arquivo para validação", e);
		}
	}

	private boolean isJPEG(byte[] header) {
		return header.length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8
				&& (header[2] & 0xFF) == 0xFF;
	}

	private boolean isPNG(byte[] header) {
		return header.length >= 4 && (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50
				&& (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47;
	}

	private boolean isWEBP(byte[] header) {
		return header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
				&& header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
	}
}
