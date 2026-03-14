$filePath = "VIM-BE\src\main\java\com\bezkoder\spring\login\sa\dal\daoimpl\CfgTblCustomFormApplicationDAO.java"
$content = Get-Content $filePath -Raw

# Remove incorrect email notification from rejectApplication method
$search = @'
                log.warn("Error updating approval history for rejection: " + e.getMessage());
            }

            entityManager.merge(application);
            entityManager.getTransaction().commit();
            
            // Send email notification to previous department after successful send back
            try {
                sendBackEmailNotification(application, currentLevel, pipelines);
            } catch (Exception emailEx) {
                log.error("Error sending send-back email notification: " + emailEx.getMessage(), emailEx);
                // Don't fail the send back if email fails
            }
            
            return "Success";
        } catch (

        Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error rejecting application: " + e.getMessage(), e);
'@

$replace = @'
                log.warn("Error updating approval history for rejection: " + e.getMessage());
            }

            entityManager.merge(application);
            entityManager.getTransaction().commit();
            return "Success";
        } catch (

        Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("Error rejecting application: " + e.getMessage(), e);
'@

$content = $content -replace [regex]::Escape($search), $replace
Set-Content -Path $filePath -Value $content -NoNewline
Write-Host "Removed incorrect email from reject method"
