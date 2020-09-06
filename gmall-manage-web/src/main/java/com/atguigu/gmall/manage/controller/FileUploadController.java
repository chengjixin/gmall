package com.atguigu.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;

@RestController
@CrossOrigin        //跨域
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileUrl;

    //http://localhost:8082/fileUpload  spu图片上传
    @RequestMapping("fileUpload")
    public String fileUpload(@RequestParam MultipartFile file) throws IOException, MyException {
        //用于做最后的图片全路径，返回值
        String ImgUrl = fileUrl;
        if (file != null){
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            //创建一个tracker的客户端对象，用于获取连接
            TrackerClient trackerClient=new TrackerClient();
            //获取连接
            TrackerServer trackerServer=trackerClient.getConnection();
            //获取存储的一个客户端对象
            StorageClient storageClient=new StorageClient(trackerServer,null);
            //通过file获取文件名
            String filename= file.getOriginalFilename();
            //获取文件名中的后缀名
            String subffixStr = StringUtils.substringAfterLast(filename, ".");
            //进行保存操作，返回一个包含路径中的图片组名和图片部分路径 的字符串集合
            String[] upload_file = storageClient.upload_file(file.getBytes(), subffixStr, null);
            //返回的字符串数组包含两部分：组名   和     图片路径名
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                System.out.println("path = " + path);
                //字符串拼接，并最终返回
                //http://192.168.92.226/group1/M00/00/00/wKhc4l4I4OWAer1OAAASiLLpJJE575.jpg
                ImgUrl +="/" + path;
            }
        }
        return ImgUrl;
    }

}
