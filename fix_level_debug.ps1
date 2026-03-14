$filePath = "VIM-BE\src\main\java\com\bezkoder\spring\login\sa\dal\daoimpl\CfgTblCustomFormApplicationDAO.java"
$content = Get-Content $filePath -Raw

# Fix the sendBackEmailNotification to use correct level logic
# The issue is: when sending back from level 4, the app goes to level 3
# We need to send email to level 3, which is at index (3-1) = 2 in the pipelines array

$oldMethod = @'
    /**
     * Send email notification when an application is sent back to previous department
     * Uses the same format as approval emails with approve/reject buttons
     */
    private void sendBackEmailNotification(CfgTblCustomFormApplication application, Integer previousLevel, 
            List<java.util.Map<String, Object>> pipelines) {
        if (application == null || previousLevel == null || previousLevel <= 0) {
            log.info("Skipping send-back email: appId={}, previousLevel={}", 
                application != null ? application.getSerApplicationId() : "null", previousLevel);
            return;
        }
        
        if (pipelines == null || pipelines.isEmpty()) {
            log.info("No pipelines to notify for send-back, appId={}", application.getSerApplicationId());
            return;
        }
        
        EntityManager emailEntityManager = getEntityManager();
        try {
            // Get the form
            CfgTblCustomForm form = emailEntityManager.find(CfgTblCustomForm.class, application.getSerFormId());
            String formName = form != null ? form.getTxtFormName() : "Application";
            
            // Find the previous department (the one at previousLevel - after send back, app goes to this level)
            int prevLevelIndex = previousLevel - 1;
            if (prevLevelIndex < 0 || prevLevelIndex >= pipelines.size()) {
                log.info("Previous level index out of bounds for send-back, appId={}, level={}, pipelineSize={}", 
                    application.getSerApplicationId(), previousLevel, pipelines.size());
                return;
            }
            
            java.util.Map<String, Object> prevPipeline = pipelines.get(prevLevelIndex);
            if (prevPipeline == null) {
                log.warn("Previous pipeline is null for send-back, appId={}, level={}", 
                    application.getSerApplicationId(), previousLevel);
                return;
            }
            
            // Get department ID - try multiple key names
            Integer prevDeptId = safeInt(prevPipeline.get("serDepartmentId"), 
                safeInt(prevPipeline.get("departmentId"), null));
            
            if (prevDeptId == null) {
                log.warn("Could not find previous department ID for send-back email, appId={}", application.getSerApplicationId());
                return;
            }
            
            log.info("Sending send-back email for appId={}, prevDeptId={}, previousLevel={}", 
                application.getSerApplicationId(), prevDeptId, previousLevel);
'@

$newMethod = @'
    /**
     * Send email notification when an application is sent back to previous department
     * Uses the same format as approval emails with approve/reject buttons
     */
    private void sendBackEmailNotification(CfgTblCustomFormApplication application, Integer newLevelAfterSendBack, 
            List<java.util.Map<String, Object>> pipelines) {
        if (application == null || newLevelAfterSendBack == null || newLevelAfterSendBack <= 0) {
            log.info("Skipping send-back email: appId={}, newLevelAfterSendBack={}", 
                application != null ? application.getSerApplicationId() : "null", newLevelAfterSendBack);
            return;
        }
        
        if (pipelines == null || pipelines.isEmpty()) {
            log.info("No pipelines to notify for send-back, appId={}", application.getSerApplicationId());
            return;
        }
        
        // Log for debugging
        log.info("=== SEND BACK EMAIL DEBUG ===");
        log.info("appId={}, newLevelAfterSendBack={}, pipelines.size={}", 
            application.getSerApplicationId(), newLevelAfterSendBack, pipelines.size());
        
        EntityManager emailEntityManager = getEntityManager();
        try {
            // Get the form
            CfgTblCustomForm form = emailEntityManager.find(CfgTblCustomForm.class, application.getSerFormId());
            String formName = form != null ? form.getTxtFormName() : "Application";
            
            // Find the department at the NEW level (where the app is going after send back)
            // newLevelAfterSendBack is 1-based (e.g., 3 means level 3)
            // pipelines array is 0-based, so level 3 is at index 2
            int targetLevelIndex = newLevelAfterSendBack - 1;
            
            log.info("Looking for department at index {} (level {})", targetLevelIndex, newLevelAfterSendBack);
            
            if (targetLevelIndex < 0 || targetLevelIndex >= pipelines.size()) {
                log.error("Target level index out of bounds for send-back email, appId={}, newLevel={}, index={}, pipelineSize={}", 
                    application.getSerApplicationId(), newLevelAfterSendBack, targetLevelIndex, pipelines.size());
                return;
            }
            
            java.util.Map<String, Object> targetPipeline = pipelines.get(targetLevelIndex);
            if (targetPipeline == null) {
                log.error("Target pipeline is null for send-back, appId={}, level={}, index={}", 
                    application.getSerApplicationId(), newLevelAfterSendBack, targetLevelIndex);
                return;
            }
            
            // Log the target pipeline for debugging
            log.info("Target pipeline for send-back: index={}, data={}", targetLevelIndex, targetPipeline);
            
            // Get department ID - try multiple key names
            Integer targetDeptId = safeInt(targetPipeline.get("serDepartmentId"), 
                safeInt(targetPipeline.get("departmentId"), null));
            
            if (targetDeptId == null) {
                log.error("Could not find target department ID for send-back email, appId={}, pipeline={}", 
                    application.getSerApplicationId(), targetPipeline);
                return;
            }
            
            log.info("Sending send-back email to department ID {} for appId={}, level={}", 
                targetDeptId, application.getSerApplicationId(), newLevelAfterSendBack);
'@

$content = $content -replace [regex]::Escape($oldMethod), $newMethod
Write-Host "Fixed level logic in sendBackEmailNotification"

# Also update the variable name in the rest of the method
$oldVars = @'
            String prevDeptName = resolveDepartmentName(emailEntityManager, prevDeptId, prevPipeline);
            
            // Get department head from HrTblDepartment table (like sendApprovalEmails does)
            emailEntityManager.getTransaction().begin();
            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment prevDept = 
                emailEntityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, prevDeptId);
            
            java.util.List<Integer> headIds = new java.util.ArrayList<>();
            if (prevDept != null && prevDept.getSerDepartmentHeadId() != null) {
                String headIdsStr = prevDept.getSerDepartmentHeadId();
                for (String id : headIdsStr.split(",")) {
                    try {
                        headIds.add(Integer.parseInt(id.trim()));
                    } catch (Exception e) {
                    }
                }
            }
            
            // Fallback if no head IDs found
            if (headIds.isEmpty()) {
                Integer fallbackHeadId = findDepartmentHeadUserId(emailEntityManager, prevDeptId);
                if (fallbackHeadId != null) {
                    headIds.add(fallbackHeadId);
                }
            }
            
            if (headIds.isEmpty()) {
                emailEntityManager.getTransaction().rollback();
                log.warn("No department head found for previous department {} in send-back email", prevDeptId);
                return;
            }
'@

$newVars = @'
            String targetDeptName = resolveDepartmentName(emailEntityManager, targetDeptId, targetPipeline);
            
            // Get department head from HrTblDepartment table (like sendApprovalEmails does)
            emailEntityManager.getTransaction().begin();
            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment targetDept = 
                emailEntityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, targetDeptId);
            
            java.util.List<Integer> headIds = new java.util.ArrayList<>();
            if (targetDept != null && targetDept.getSerDepartmentHeadId() != null) {
                String headIdsStr = targetDept.getSerDepartmentHeadId();
                for (String id : headIdsStr.split(",")) {
                    try {
                        headIds.add(Integer.parseInt(id.trim()));
                    } catch (Exception e) {
                    }
                }
            }
            
            // Fallback if no head IDs found
            if (headIds.isEmpty()) {
                Integer fallbackHeadId = findDepartmentHeadUserId(emailEntityManager, targetDeptId);
                if (fallbackHeadId != null) {
                    headIds.add(fallbackHeadId);
                }
            }
            
            if (headIds.isEmpty()) {
                emailEntityManager.getTransaction().rollback();
                log.error("No department head found for target department {} in send-back email", targetDeptId);
                return;
            }
'@

$content = $content -replace [regex]::Escape($oldVars), $newVars
Write-Host "Updated variable names part 1"

# Update more references
$oldVars2 = @'
                // Use the same email format as approval emails
                String subject = formName + " Sent Back - Requires Your Approval - " + 
                    (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                
                String html = generateApprovalEmailHtml(
                    deptHead.getTxtUserName() != null ? deptHead.getTxtUserName() : "User",
                    previousLevel, // level
'@

$newVars2 = @'
                // Use the same email format as approval emails
                String subject = formName + " Sent Back - Requires Your Approval - " + 
                    (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                
                String html = generateApprovalEmailHtml(
                    deptHead.getTxtUserName() != null ? deptHead.getTxtUserName() : "User",
                    newLevelAfterSendBack, // level (the level they're receiving at)
'@

$content = $content -replace [regex]::Escape($oldVars2), $newVars2
Write-Host "Updated variable names part 2"

Set-Content -Path $filePath -Value $content -NoNewline
Write-Host "All debug logging added"
