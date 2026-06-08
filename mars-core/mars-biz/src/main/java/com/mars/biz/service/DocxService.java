package com.mars.biz.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * DOCX文件处理 Service
 */
public interface DocxService {

    /**
     * 合并多个docx文件
     *
     * @param files 待合并的文件列表（最多3个）
     * @return 合并后的文件字节数组
     */
    byte[] mergeDocx(List<MultipartFile> files) throws IOException;

    /**
     * 根据分隔符拆分docx文件
     *
     * @param file      待拆分的文件
     * @param separator 分隔符（为空时默认20个"-"）
     * @return 拆分后的文件字节数组列表
     */
    List<byte[]> splitDocx(MultipartFile file, String separator) throws IOException;
}
