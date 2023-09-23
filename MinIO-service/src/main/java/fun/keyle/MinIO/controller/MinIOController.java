package fun.keyle.MinIO.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONPObject;
import fun.keyle.MinIO.entry.BucketVO;
import fun.keyle.MinIO.entry.Fileinfo;
import fun.keyle.MinIO.utils.MinioUtil;
import io.micrometer.common.util.StringUtils;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/minioService")
@RestController
@SuppressWarnings(value = "all")
public class MinIOController {
    @Resource
    MinioUtil minioUtil;

    /**
     * 判断存储桶的存在
     *
     * @param bucketName 存储桶名称
     * @return 存在/不存在
     */
    @GetMapping("/bucketExists")
    public Boolean bucketExists(String bucketName) {
        return minioUtil.bucketExists(bucketName);
    }

    /**
     * 创建存储bucket
     * bucketName 需要传入桶名
     *
     * @return Boolean
     */
    @PutMapping("/makeBucket")
    public Boolean makeBucket(String bucketName) {
        return minioUtil.makeBucket(bucketName);
    }

    /**
     * 删除存储bucket
     * bucketName 需要传入桶名
     *
     * @return Boolean
     */
    @DeleteMapping("/removeBucket")
    public Boolean removeBucket(String bucketName) {
        return minioUtil.removeBucket(bucketName);
    }

    /**
     * 获取全部bucket
     */
    @GetMapping("/getAllBuckets")
    public List<BucketVO> getAllBuckets() {
        return minioUtil.getAllBuckets();
    }

    /**
     * 列出一个桶中的所有文件和目录
     *
     * @param bucket 桶名称
     * @return
     * @throws Exception
     */
    @GetMapping("/getListFiles")
    public List<Fileinfo> listFiles(String bucket){
        return minioUtil.listFiles(bucket);
    }


    /**
     * 文件上传
     * @param bucketName 桶名称
     * @param filePath 上传路径
     * @param file 文件
     * @return 文件所在路径
     */
    @PostMapping("/upload")
    public String upload(String bucketName, String filePath, MultipartFile file) {
        String upload = minioUtil.upload(bucketName, filePath, file);
        return StringUtils.isEmpty(upload) ? "上传文件不能为空" : upload;
    }

    /**
     * 预览
     * @param bucketName 桶名称
     * @param objectName 文件路径
     * @param expires 过期时间
     * @return 图片
     */
    @GetMapping("/preview")
    public String preview(String bucketName, String objectName,Integer expires) {
        return minioUtil.preview(bucketName, objectName,expires);
    }

    /**
     * 文件下载
     *
     * @param fileName   文件名称
     * @param bucketName 需要传入桶名
     * @param res        response
     * @return Boolean
     */
    @GetMapping("/download")
    public void download(String bucketName, String fileName, HttpServletResponse res) {
        minioUtil.download(bucketName, fileName, res);
    }

    /**
     * 查看文件对象
     *
     * @param bucketName 需要传入桶名
     * @return 存储bucket内文件对象信息
     */
    @GetMapping("/listObjects")
    public List<Item> listObjects(String bucketName) {
        return minioUtil.listObjects(bucketName);
    }

    /**
     * 删除
     *
     * @param bucketName 需要传入桶名
     * @param fileName
     * @return
     * @throws Exception
     */
    @DeleteMapping("/remove")
    public boolean remove(String bucketName, String fileName) {
        return minioUtil.remove(bucketName, fileName);
    }
}
