import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { urls } from 'src/app/utils/urls';
import { WorkflowPipelineThresholdRule } from 'src/app/components/workflow-pipelines/workflow-pipeline-thresholds.demo-data';

export type WorkflowThresholdRuleSavePayload = Omit<WorkflowPipelineThresholdRule, 'id'> & {
    id?: number;
    serFormId: number;
    serTargetId: number;
};

@Injectable({
    providedIn: 'root',
})
export class WorkflowThresholdRuleService {
    constructor(private http: HttpClient) {}

    getAll(serFormId?: number): Observable<WorkflowPipelineThresholdRule[]> {
        const params: Record<string, string> = {};
        if (serFormId != null && serFormId > 0) {
            params['serFormId'] = String(serFormId);
        }
        return this.http.get<WorkflowPipelineThresholdRule[]>(urls.API_URL + 'getAllWorkflowThresholdRules', {
            params,
        });
    }

    getById(id: number): Observable<WorkflowPipelineThresholdRule> {
        return this.http.get<WorkflowPipelineThresholdRule>(urls.API_URL + 'getWorkflowThresholdRule', {
            params: { id: String(id) },
        });
    }

    create(payload: WorkflowThresholdRuleSavePayload): Observable<WorkflowPipelineThresholdRule> {
        return this.http.post<WorkflowPipelineThresholdRule>(urls.API_URL + 'addWorkflowThresholdRule', payload);
    }

    update(payload: WorkflowThresholdRuleSavePayload): Observable<WorkflowPipelineThresholdRule> {
        return this.http.post<WorkflowPipelineThresholdRule>(urls.API_URL + 'updateWorkflowThresholdRule', payload);
    }

    delete(ids: number[]): Observable<string> {
        return this.http.post(urls.API_URL + 'deleteWorkflowThresholdRule', ids.join(','), {
            responseType: 'text',
        });
    }
}
