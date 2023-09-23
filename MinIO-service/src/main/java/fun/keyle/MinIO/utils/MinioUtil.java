package fun.keyle.MinIO.utils;

import fun.keyle.MinIO.entry.BucketVO;
import fun.keyle.MinIO.entry.Fileinfo;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class MinioUtil {
    //必须使用注入的方式否则会出现空指针
    @Autowired
    MinioClient minioClient;

    /**
     * 查看存储bucket是否存在
     * bucketName 需要传入桶名
     *
     * @return boolean
     */
    public Boolean bucketExists(String bucketName) {
        Boolean found;
        try {
            found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return found;
    }

    /**
     * 创建存储bucket
     * bucketName 需要传入桶名
     *
     * @return Boolean
     */
    public Boolean makeBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 删除存储bucket
     * bucketName 需要传入桶名
     *
     * @return Boolean
     */
    public Boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 获取全部bucket
     */
    @SneakyThrows
    public List<BucketVO> getAllBuckets() {
        List<Bucket> bucketList = minioClient.listBuckets();
        List<BucketVO> bucketVOList = new ArrayList<>(bucketList.size());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        bucketList.forEach(bucket -> {
            System.out.printf("存储桶名：%s, 创建时间：%s%n", bucket.name(), bucket.creationDate().format(formatter));
            BucketVO bucketVO = new BucketVO();
            bucketVO.setName(bucket.name());
            bucketVO.setCreationDate(bucket.creationDate().format(formatter));
            bucketVOList.add(bucketVO);
        });

        return bucketVOList;
    }

    /**
     * 列出一个桶中的所有文件和目录
     *
     * @param bucket 桶名称
     * @return
     * @throws Exception
     */
    @SneakyThrows
    public List<Fileinfo> listFiles(String bucket) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucket).recursive(true).build());

        List<Fileinfo> infos = new ArrayList<>();
        results.forEach(r -> {
            Fileinfo info = new Fileinfo();
            try {
                Item item = r.get();
                info.setFilename(item.objectName());
                info.setDirectory(item.isDir());
                infos.add(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return infos;
    }

    /**
     * 文件上传
     *
     * @param bucketName 桶名称
     * @param filePath   上传路径
     * @param file       文件
     * @return 文件所在路径
     */
    @SneakyThrows
    public String upload(String bucketName, String filePath, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new RuntimeException("文件名无效");
        }
        BucketExistsArgs bucket = BucketExistsArgs.builder().bucket(bucketName).build();
        boolean isExist = false;
        isExist = minioClient.bucketExists(bucket);
        if (!isExist) {
            makeBucket(bucketName);
        }
        String fileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = filePath + "/" + fileName;
        try {
            PutObjectArgs objectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            // 文件名称相同会覆盖
            minioClient.putObject(objectArgs);
        } catch (Exception e) {
            log.info("上传文件失败：",e.getMessage());
            return null;
        }
        return objectName;
    }

    /***
     * 生成一个GET请求的分享链接。
     * * 失效时间默认是7天。
     * @param bucketName 存储桶名称
     * @param objectName 存储桶里的对象名称
     * @param expires 失效时间（以秒为单位），默认是7天，不得大于七天*
     * @return
     */
    @SneakyThrows
    public String preview(String bucketName, String objectName, Integer expires) {
        BucketExistsArgs bucketArgs = BucketExistsArgs.builder().bucket(bucketName).build();
        boolean bucketExists = minioClient.bucketExists(bucketArgs);
        boolean flag = bucketExists(bucketName);
        String url = "";
        if (bucketExists) {
            try {
                if (expires == null) {
                    expires = 604800;
                }
                GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder().method(Method.GET).bucket(bucketName).object(objectName).expiry(expires).build();
                url = minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
            } catch (Exception e) {
                log.info("预签名获取对象失败：{}", e);
            }
        }
        return url;
    }

    /**
     * 文件下载
     *
     * @param fileName 文件名称
     *                 BucketName 需要传入桶名
     * @param res      response
     * @return Boolean
     */
    public void download(String bucketName, String fileName, HttpServletResponse res) {
        GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(bucketName)
                .object(fileName).build();
        try (GetObjectResponse response = minioClient.getObject(objectArgs)) {
            byte[] buf = new byte[1024];
            int len;
            try (FastByteArrayOutputStream os = new FastByteArrayOutputStream()) {
                while ((len = response.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                os.flush();
                byte[] bytes = os.toByteArray();
                res.setCharacterEncoding("utf-8");
                // 设置强制下载不打开
                // res.setContentType("application/force-download");
                res.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
                try (ServletOutputStream stream = res.getOutputStream()) {
                    stream.write(bytes);
                    stream.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查看文件对象
     * bucketName 需要传入桶名
     *
     * @return 存储bucket内文件对象信息
     */
    public List<Item> listObjects(String bucketName) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).build());
        List<Item> items = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                items.add(result.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return items;
    }

    /**
     * 删除
     *
     * @param fileName
     * @param bucketName 需要传入桶名
     * @return
     * @throws Exception
     */
    public boolean remove(String bucketName, String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(fileName).build());
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
