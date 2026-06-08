package com.mars.biz.service.impl;

import com.mars.biz.service.DocxService;
import com.mars.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DOCX文件处理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocxServiceImpl implements DocxService {

    private static final String DEFAULT_SEPARATOR = "--------------------";

    @Override
    public byte[] mergeDocx(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("请至少上传一个文件");
        }
        if (files.size() > 3) {
            throw new BusinessException("最多支持合并3个文件");
        }

        for (MultipartFile file : files) {
            validateDocxFile(file);
        }

        try (XWPFDocument mergedDoc = new XWPFDocument()) {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                try (XWPFDocument sourceDoc = new XWPFDocument(file.getInputStream())) {
                    copyDocumentContent(sourceDoc, mergedDoc);
                }
                if (i < files.size() - 1) {
                    XWPFParagraph breakPara = mergedDoc.createParagraph();
                    breakPara.setPageBreak(true);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mergedDoc.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public List<byte[]> splitDocx(MultipartFile file, String separator) throws IOException {
        validateDocxFile(file);

        String effectiveSeparator = StringUtils.hasText(separator) ? separator : DEFAULT_SEPARATOR;

        List<byte[]> result = new ArrayList<>();

        try (XWPFDocument sourceDoc = new XWPFDocument(file.getInputStream())) {
            List<BodyPart> bodyParts = extractBodyParts(sourceDoc);

            List<List<BodyPart>> chunks = splitBySeparator(bodyParts, effectiveSeparator);

            for (List<BodyPart> chunk : chunks) {
                if (chunk.isEmpty()) continue;

                try (XWPFDocument newDoc = new XWPFDocument()) {
                    for (BodyPart part : chunk) {
                        if (part.type == BodyPartType.PARAGRAPH) {
                            copyParagraph(part.paragraph, newDoc);
                        } else if (part.type == BodyPartType.TABLE) {
                            copyTable(part.table, newDoc);
                        }
                    }

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    newDoc.write(out);
                    result.add(out.toByteArray());
                }
            }
        }

        if (result.isEmpty()) {
            throw new BusinessException("未找到分隔符，无法拆分文档");
        }

        return result;
    }

    private void validateDocxFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".docx")) {
            throw new BusinessException("仅支持 .docx 格式文件");
        }
    }

    private void copyDocumentContent(XWPFDocument source, XWPFDocument target) {
        List<BodyPart> bodyParts = extractBodyParts(source);

        for (BodyPart part : bodyParts) {
            if (part.type == BodyPartType.PARAGRAPH) {
                copyParagraph(part.paragraph, target);
            } else if (part.type == BodyPartType.TABLE) {
                copyTable(part.table, target);
            }
        }
    }

    private List<BodyPart> extractBodyParts(XWPFDocument doc) {
        List<BodyPart> parts = new ArrayList<>();
        List<IBodyElement> elements = doc.getBodyElements();

        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph) {
                parts.add(new BodyPart(BodyPartType.PARAGRAPH, (XWPFParagraph) element, null));
            } else if (element instanceof XWPFTable) {
                parts.add(new BodyPart(BodyPartType.TABLE, null, (XWPFTable) element));
            }
        }
        return parts;
    }

    private List<List<BodyPart>> splitBySeparator(List<BodyPart> bodyParts, String separator) {
        List<List<BodyPart>> chunks = new ArrayList<>();
        List<BodyPart> currentChunk = new ArrayList<>();

        for (BodyPart part : bodyParts) {
            if (part.type == BodyPartType.PARAGRAPH) {
                String text = part.paragraph.getText();
                if (text != null && text.contains(separator)) {
                    if (!currentChunk.isEmpty()) {
                        chunks.add(currentChunk);
                        currentChunk = new ArrayList<>();
                    }
                } else {
                    currentChunk.add(part);
                }
            } else {
                currentChunk.add(part);
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }

        return chunks;
    }

    private void copyParagraph(XWPFParagraph source, XWPFDocument target) {
        XWPFParagraph newPara = target.createParagraph();
        newPara.setStyle(source.getStyle());
        newPara.setAlignment(source.getAlignment());
        newPara.setVerticalAlignment(source.getVerticalAlignment());
        newPara.setSpacingBefore(source.getSpacingBefore());
        newPara.setSpacingAfter(source.getSpacingAfter());
        newPara.setSpacingLineRule(source.getSpacingLineRule());
        newPara.setIndentationFirstLine(source.getIndentationFirstLine());
        newPara.setIndentationLeft(source.getIndentationLeft());
        newPara.setIndentationRight(source.getIndentationRight());

        for (XWPFRun run : source.getRuns()) {
            XWPFRun newRun = newPara.createRun();
            copyRunProperties(run, newRun);
            newRun.setText(run.getText(0), 0);
        }
    }

    private void copyRunProperties(XWPFRun source, XWPFRun target) {
        target.setFontFamily(source.getFontFamily());
        target.setFontSize(source.getFontSizeAsDouble());
        target.setBold(source.isBold());
        target.setItalic(source.isItalic());
        target.setUnderline(source.getUnderline());
        target.setColor(source.getColor());
        target.setStrikeThrough(source.isStrikeThrough());
        target.setDoubleStrikethrough(source.isDoubleStrikeThrough());
        if (source.getTextPosition() != null) {
            target.setTextPosition(source.getTextPosition());
        }
    }

    private void copyTable(XWPFTable source, XWPFDocument target) {
        XWPFTable newTable = target.createTable();
        newTable.removeRow(0);

        for (XWPFTableRow sourceRow : source.getRows()) {
            XWPFTableRow newRow = newTable.createRow();
            newRow.removeCell(0);

            for (XWPFTableCell sourceCell : sourceRow.getTableCells()) {
                XWPFTableCell newCell = newRow.addNewTableCell();

                for (int i = newCell.getParagraphs().size() - 1; i >= 0; i--) {
                    newCell.removeParagraph(i);
                }

                for (XWPFParagraph para : sourceCell.getParagraphs()) {
                    XWPFParagraph newPara = newCell.addParagraph();
                    newPara.setStyle(para.getStyle());
                    newPara.setAlignment(para.getAlignment());

                    for (XWPFRun run : para.getRuns()) {
                        XWPFRun newRun = newPara.createRun();
                        copyRunProperties(run, newRun);
                        newRun.setText(run.getText(0), 0);
                    }
                }
            }
        }
    }

    private enum BodyPartType {
        PARAGRAPH, TABLE
    }

    private static class BodyPart {
        final BodyPartType type;
        final XWPFParagraph paragraph;
        final XWPFTable table;

        BodyPart(BodyPartType type, XWPFParagraph paragraph, XWPFTable table) {
            this.type = type;
            this.paragraph = paragraph;
            this.table = table;
        }
    }
}
