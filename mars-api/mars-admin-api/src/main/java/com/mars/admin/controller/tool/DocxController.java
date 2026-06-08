package com.mars.admin.controller.tool;

import com.mars.biz.service.DocxService;
import com.mars.system.annotation.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * DOCX文件工具控制器
 */
@RestController
@RequestMapping("/tool/docx")
@RequiredArgsConstructor
public class DocxController {

    private final DocxService docxService;

    /**
     * 合并多个docx文件（最多3个）
     */
    @PostMapping("/merge")
    @Log(title = "合并DOCX文件", businessType = Log.BusinessType.OTHER)
    public ResponseEntity<byte[]> mergeDocx(@RequestParam("files") List<MultipartFile> files) throws IOException {
        byte[] mergedData = docxService.mergeDocx(files);

        String fileName = URLEncoder.encode("合并文档.docx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(mergedData);
    }

    /**
     * 根据分隔符拆分docx文件
     *
     * @param file      待拆分的docx文件
     * @param separator 分隔符（非必填，默认20个"-"）
     */
    @PostMapping("/split")
    @Log(title = "拆分DOCX文件", businessType = Log.BusinessType.OTHER)
    public ResponseEntity<byte[]> splitDocx(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "separator", required = false) String separator) throws IOException {

        List<byte[]> splitParts = docxService.splitDocx(file, separator);

        ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipBaos)) {
            for (int i = 0; i < splitParts.size(); i++) {
                String entryName = "拆分文档_" + (i + 1) + ".docx";
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(splitParts.get(i));
                zos.closeEntry();
            }
        }

        String originalName = file.getOriginalFilename();
        String baseName = "拆分文档";
        if (originalName != null && originalName.toLowerCase().endsWith(".docx")) {
            baseName = originalName.substring(0, originalName.length() - 5);
        }
        String zipFileName = URLEncoder.encode(baseName + "_拆分结果.zip", StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + zipFileName)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBaos.toByteArray());
    }
}
