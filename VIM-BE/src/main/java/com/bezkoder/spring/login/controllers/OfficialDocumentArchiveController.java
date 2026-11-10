package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.bll.servicesimpl.OfficialDocumentArchiveService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblOfficialDocumentArchive;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class OfficialDocumentArchiveController {

    private static final Logger logger = LogManager.getLogger(OfficialDocumentArchiveController.class);

    @Autowired
    private OfficialDocumentArchiveService officialDocumentArchiveService;

    @Autowired
    private ICommonService commonService;

    @RequestMapping(value = "/listOfficialDocuments", method = RequestMethod.GET)
    public Map<String, Object> listOfficialDocuments(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return officialDocumentArchiveService.list(q, label, page, pageSize);
    }

    @RequestMapping(value = "/uploadOfficialDocument", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadOfficialDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "runAi", defaultValue = "true") boolean runAi) {
        try {
            int userId = commonService.getCurrentLoggedInUser();
            Map<String, Object> result = officialDocumentArchiveService.upload(file, userId, runAi);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("uploadOfficialDocument failed: " + ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "Upload failed"));
        }
    }

    @RequestMapping(value = "/downloadOfficialDocument", method = RequestMethod.GET)
    public ResponseEntity<?> downloadOfficialDocument(@RequestParam("id") int id) {
        try {
            CfgTblOfficialDocumentArchive row = officialDocumentArchiveService.getActive(id);
            File file = officialDocumentArchiveService.resolveFile(id);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            String mime = row.getTxtMimeType() != null ? row.getTxtMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            String fileName = row.getTxtOriginalFileName() != null ? row.getTxtOriginalFileName() : ("document-" + id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName.replace("\"", "") + "\"")
                    .contentType(MediaType.parseMediaType(mime))
                    .body(new FileSystemResource(file));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            logger.error("downloadOfficialDocument failed: " + ex.getMessage(), ex);
            return ResponseEntity.status(500).build();
        }
    }

    @RequestMapping(value = "/deleteOfficialDocument", method = RequestMethod.POST)
    public ResponseEntity<?> deleteOfficialDocument(@RequestParam("id") int id) {
        try {
            return ResponseEntity.ok(officialDocumentArchiveService.softDelete(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("deleteOfficialDocument failed: " + ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "Delete failed"));
        }
    }

    @RequestMapping(value = "/reanalyzeOfficialDocument", method = RequestMethod.POST)
    public ResponseEntity<?> reanalyzeOfficialDocument(@RequestParam("id") int id) {
        try {
            return ResponseEntity.ok(officialDocumentArchiveService.reanalyze(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("reanalyzeOfficialDocument failed: " + ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "Re-analysis failed"));
        }
    }
}
