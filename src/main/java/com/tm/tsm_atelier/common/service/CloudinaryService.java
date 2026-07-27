package com.tm.tsm_atelier.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tm.tsm_atelier.common.exception.custom.FileUploadException;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CloudinaryService implements ImageStorageService {

	private final Cloudinary cloudinary;

	@Override
	public String uploadImage(MultipartFile file, String folder) {
		try {
			Map<String, Object> uploadParams = ObjectUtils.asMap("folder", "tsm_atelier/" + folder, "public_id",
					UUID.randomUUID().toString(), "resource_type", "image");

			Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
			return uploadResult.get("secure_url").toString();

		} catch (IOException e) {
			throw new FileUploadException("Error uploading image to Cloudinary", e);
		}
	}

	@Override
	public void deleteImage(String publicId) {
		try {
			cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
		} catch (IOException e) {
			throw new FileUploadException("Error deleting image from Cloudinary: " + publicId, e);
		}
	}
}
