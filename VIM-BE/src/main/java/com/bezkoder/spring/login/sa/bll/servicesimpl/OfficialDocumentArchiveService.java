package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.utility.common.OfficialDocumentStorageUtil;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblOfficialDocumentArchive;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblOfficialDocumentArchiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Service
public class OfficialDocumentArchiveService {

    @Autowired
    private CfgTblOfficialDocumentArchiveRepository repository;

    @Autowired
    private GeminiDocumentAnalysisService geminiDocumentAnalysisService;

    public Map<String, Object> list(String q, String label, int page, int pageSize) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        Page<CfgTblOfficialDocumentArchive> result = repository.search(
                q == null ? "" : q.trim(),
                label == null ? "" : label.trim(),
                PageRequest.of(safePage, safeSize));

        Map<String, Object> response = new HashMap<>();
        response.put("items", result.getContent());
        response.put("total", result.getTotalElements());
        response.put("page", safePage);
        response.put("pageSize", safeSize);
        response.put("labels", GeminiDocumentAnalysisService.LABEL_PRESETS);
        response.put("geminiConfigured", geminiDocumentAnalysisService.isConfigured());
        return response;
    }

    @Transactional
    public Map<String, Object> upload(MultipartFile file, int userId, boolean runAi) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        CfgTblOfficialDocumentArchive row = new CfgTblOfficialDocumentArchive();
        row.setBlIsDeleted(false);
        row.setDteCreatedDate(new Timestamp(System.currentTimeMillis()));
        row.setSerUploadedByUserId(userId > 0 ? userId : null);
        row.setTxtOriginalFileName(file.getOriginalFilename());
        row.setTxtMimeType(file.getContentType());
        row = repository.save(row);

        GeminiDocumentAnalysisService.AnalysisResult analysis = runAi
                ? geminiDocumentAnalysisService.analyze(file, file.getOriginalFilename())
                : defaultAnalysis(file);

        try {
            row.setTxtStoredPath(OfficialDocumentStorageUtil.save(row.getSerOfficialDocumentId(), file));
        } catch (Exception ex) {
            row.setBlIsDeleted(true);
            repository.save(row);
            throw new IllegalStateException("Could not store file: " + ex.getMessage(), ex);
        }

        row.setTxtTitle(analysis.title);
        row.setTxtSummary(analysis.summary);
        row.setTxtLabel(analysis.label);
        row.setIntPageCount(analysis.pageCount);
        row.setBlAiAnalyzed(analysis.analyzedByAi);
        row = repository.save(row);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", analysis.message);
        response.put("document", row);
        return response;
    }

    private GeminiDocumentAnalysisService.AnalysisResult defaultAnalysis(MultipartFile file) {
        GeminiDocumentAnalysisService.AnalysisResult r = new GeminiDocumentAnalysisService.AnalysisResult();
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        r.title = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        r.summary = "";
        r.label = "GENERAL";
        r.analyzedByAi = false;
        r.pageCount = 1;
        r.message = "Stored without AI analysis.";
        return r;
    }

    public File resolveFile(int id) {
        CfgTblOfficialDocumentArchive row = getActive(id);
        return OfficialDocumentStorageUtil.resolve(row.getTxtStoredPath());
    }

    public CfgTblOfficialDocumentArchive getActive(int id) {
        CfgTblOfficialDocumentArchive row = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        if (Boolean.TRUE.equals(row.getBlIsDeleted())) {
            throw new IllegalArgumentException("Document not found");
        }
        return row;
    }

    @Transactional
    public Map<String, Object> softDelete(int id) {
        CfgTblOfficialDocumentArchive row = getActive(id);
        row.setBlIsDeleted(true);
        repository.save(row);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        return response;
    }

    @Transactional
    public Map<String, Object> reanalyze(int id) {
        CfgTblOfficialDocumentArchive row = getActive(id);
        File file = OfficialDocumentStorageUtil.resolve(row.getTxtStoredPath());
        if (file == null) {
            throw new IllegalArgumentException("Stored file is missing");
        }
        MultipartFileAdapter adapter;
        try {
            adapter = MultipartFileAdapter.fromFile(file, row.getTxtOriginalFileName(), row.getTxtMimeType());
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Could not read stored file", ex);
        }
        GeminiDocumentAnalysisService.AnalysisResult analysis =
                geminiDocumentAnalysisService.analyze(adapter, row.getTxtOriginalFileName());
        row.setTxtTitle(analysis.title);
        row.setTxtSummary(analysis.summary);
        row.setTxtLabel(analysis.label);
        row.setIntPageCount(analysis.pageCount);
        row.setBlAiAnalyzed(analysis.analyzedByAi);
        repository.save(row);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", analysis.message);
        response.put("document", row);
        return response;
    }
}
