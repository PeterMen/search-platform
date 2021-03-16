package com.peter.search.util;

import com.aliyun.oss.OSSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    /** 阿里云oss配置*/
    private static final String endpoint = "";
    private static final String accessKeyId = "";
    private static final String accessKeySecret = "";
    private static final String bucketName = "";
    public static final String COM = "";


    public static String saveMultipartFile(MultipartFile file, String env) {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("文件为空");
        }

        try{

            String fileName = file.getOriginalFilename();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(file.getBytes());
            OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);

            String objectName = uploadPath(env) + "/" + fileName;
            ossClient.putObject(bucketName, objectName, inputStream);
            ossClient.shutdown();
            return COM + objectName;
        }catch (IOException e){
            logger.error("文件上传失败：", e);
        }

        return "上传失败";
    }

    public static String deleteFile(String filePath, String env) {

        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);

        String objectName = filePath.substring(filePath.indexOf(uploadPath(env)));
        ossClient.deleteObject(bucketName, objectName);
        ossClient.shutdown();
        return "";
    }

    private static String uploadPath(String env){
        return "search-es/" + env;
    }

}
