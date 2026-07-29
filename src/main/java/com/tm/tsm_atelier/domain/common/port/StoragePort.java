package com.tm.tsm_atelier.domain.common.port;

import org.springframework.web.multipart.MultipartFile;

public interface StoragePort {

	String uploadImage(MultipartFile file, String folder);

	void deleteImage(String publicIdOrUrl);
}
