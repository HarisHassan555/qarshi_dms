import Swal from 'sweetalert2';
import { firstValueFrom } from 'rxjs';
import {
    PdfSignatureCheckResult,
    TemplateWorkflowService,
} from 'src/app/services/template-workflow/template-workflow.service';

/** Returns true when the user chose to proceed (or no signatures are missing). */
export async function confirmTemplatePdfDownloadIfNeeded(
    templateWorkflowService: TemplateWorkflowService,
    applicationId: string | number
): Promise<boolean> {
    let check: PdfSignatureCheckResult;
    try {
        check = await firstValueFrom(
            templateWorkflowService.checkApplicationPdfSignatures(applicationId)
        );
    } catch {
        return true;
    }

    if (check?.allPresent || !check?.missing?.length) {
        return true;
    }

    const names = check.missing
        .map((item) => item.userName || `User ${item.userId}`)
        .join(', ');

    const result = await Swal.fire({
        title: 'Signature files missing',
        html: `<p class="workflow-confirm-message">Signature image file(s) are not present in the upload folder for: <strong>${names}</strong>. This can leave signature blocks empty or incomplete on the downloaded PDF.</p>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Download anyway',
        cancelButtonText: 'Cancel',
        reverseButtons: true,
        focusCancel: true,
        customClass: {
            popup: 'sweet-alerts workflow-confirm-swal',
            title: 'workflow-confirm-title',
            htmlContainer: 'workflow-confirm-body',
            actions: 'workflow-confirm-actions',
            confirmButton: 'workflow-confirm-primary',
            cancelButton: 'workflow-confirm-cancel',
        },
    });

    return result.isConfirmed === true;
}
