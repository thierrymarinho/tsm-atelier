package com.tm.tsm_atelier.common.controller.v1;

import com.tm.tsm_atelier.common.dto.UploadResponseDTO;
import com.tm.tsm_atelier.domain.common.port.StoragePort;
import java.util.ArrayList;
import java.util.List;
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

	@PostMapping
	public ResponseEntity<UploadResponseDTO> uploadImages(@RequestParam("files") List<MultipartFile> files,
			@RequestParam(value = "folder", defaultValue = "general") String folder) {

		List<String> urls = new ArrayList<>();
		for (MultipartFile file : files) {
			if (!file.isEmpty()) {
				String url = storagePort.uploadImage(file, folder);
				urls.add(url);
			}
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponseDTO(urls));
	}
}
