package com.schoolms.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.schoolms.result.ResultService;
import java.io.ByteArrayOutputStream;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ResultService resultService;

    @GetMapping("/class/{classId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<byte[]> classPdf(@PathVariable Long classId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();
        document.add(new Paragraph("Class Result Sheet"));
        resultService.classResult(classId).forEach(r -> {
            try { document.add(new Paragraph(r.toString())); } catch (Exception ignored) {}
        });
        document.close();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=class-results.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(baos.toByteArray());
    }

    @GetMapping("/class/{classId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<byte[]> classExcel(@PathVariable Long classId) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Class Results");
            int rowNum = 0;
            for (var item : resultService.classResult(classId)) {
                var row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.get("name").toString());
                row.createCell(1).setCellValue(((Number) item.get("total")).doubleValue());
                row.createCell(2).setCellValue(((Number) item.get("average")).doubleValue());
            }
            workbook.write(baos);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=class-results.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(baos.toByteArray());
        }
    }
}
