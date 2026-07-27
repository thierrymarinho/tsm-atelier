package com.tm.tsm_atelier.common.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
	String uploadImage(MultipartFile file, String folder);
	void deleteImage(String publicIdOrUrl);
}
