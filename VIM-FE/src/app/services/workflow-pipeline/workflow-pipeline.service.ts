import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { urls } from 'src/app/utils/urls';
import { WorkflowPipelineListItem } from 'src/app/components/workflow-pipelines/workflow-pipelines.demo-data';
import { WorkflowPipelineStep } from 'src/app/components/workflow-pipelines/workflow-pipeline-step.util';

export interface WorkflowPipelineDetail extends WorkflowPipelineListItem {
    pipeline?: WorkflowPipelineStep[];
}

export interface WorkflowPipelineSavePayload {
    serWorkflowPipelineId?: number;
    name: string;
    code?: string | null;
    description?: string | null;
    pipelineType?: string;
    active?: boolean;
    pipeline: WorkflowPipelineStep[];
}

@Injectable({
    providedIn: 'root',
})
export class WorkflowPipelineService {
    constructor(private http: HttpClient) {}

    getAll(): Observable<WorkflowPipelineListItem[]> {
        return this.http.get<WorkflowPipelineListItem[]>(urls.API_URL + 'getAllWorkflowPipelines');
    }

    getById(id: number): Observable<WorkflowPipelineDetail> {
        return this.http.get<WorkflowPipelineDetail>(urls.API_URL + 'getWorkflowPipeline', {
            params: { id: String(id) },
        });
    }

    getNew(): Observable<WorkflowPipelineDetail> {
        return this.http.get<WorkflowPipelineDetail>(urls.API_URL + 'getNewWorkflowPipeline');
    }

    create(payload: WorkflowPipelineSavePayload): Observable<WorkflowPipelineDetail> {
        return this.http.post<WorkflowPipelineDetail>(urls.API_URL + 'addWorkflowPipeline', payload);
    }

    update(payload: WorkflowPipelineSavePayload): Observable<WorkflowPipelineDetail> {
        return this.http.post<WorkflowPipelineDetail>(urls.API_URL + 'updateWorkflowPipeline', payload);
    }

    delete(ids: number[]): Observable<string> {
        return this.http.post(urls.API_URL + 'deleteWorkflowPipeline', ids.join(','), {
            responseType: 'text',
        });
    }

    duplicate(id: number): Observable<WorkflowPipelineDetail> {
        return this.http.post<WorkflowPipelineDetail>(urls.API_URL + 'duplicateWorkflowPipeline', null, {
            params: { id: String(id) },
        });
    }

    codeExists(code: string, excludeId?: number): Observable<{ exists: boolean }> {
        const params: Record<string, string> = { code };
        if (excludeId != null && excludeId > 0) {
            params['excludeId'] = String(excludeId);
        }
        return this.http.get<{ exists: boolean }>(urls.API_URL + 'workflowPipelineExistByCode', { params });
    }
}
