package com.cloud.navigators.services;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    public String uploadfiletoS3(MultipartFile file) throws IOException {

        // Convert MultipartFile to File
        File convertedFile = convertMultipartFileToFile(file);

        // Generate a unique key for the S3 object
        String key = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build(), convertedFile.toPath());

        // Delete the temporary file
        convertedFile.delete();

        // Generate the public URL for the uploaded file
        URL publicUrl = s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(key));

        return publicUrl.toString() ;

    }

    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        // Use a unique name for the temporary file
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

        File convertedFile = new File(tempDir, uniqueFileName);
        file.transferTo(convertedFile);

        if (!convertedFile.exists()) {
            throw new FileNotFoundException("Temporary file not found: " + convertedFile.getAbsolutePath());
        }

        return convertedFile;
    }




}
