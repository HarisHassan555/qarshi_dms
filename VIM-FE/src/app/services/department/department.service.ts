import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class DepartmentService {

  constructor(
    private http: HttpClient
  ) { }

  getAll() {
    return this.http.get(urls.API_URL + 'getAllDepartments');
  }

  save(payload: any) {
    if (payload.serDepartmentId) {
      return this.http.post(urls.API_URL + 'updateDepartment', payload, { responseType: 'text' });
    } else {
      return this.http.post(urls.API_URL + 'addNewDepartment', payload, { responseType: 'text' });
    }
  }

  delete(departmentId: string) {
    return this.http.post(urls.API_URL + 'deleteDepartment', departmentId, { responseType: 'text' });
  }

  getUsersByDepartment(departmentId: number) {
    return this.http.get(urls.API_URL + 'getUsersByDepartment?departmentId=' + departmentId);
  }

  assignUsersToDepartment(departmentId: number, userIds: number[], departmentHeadId?: string | null) {
    const payload: any = {
      departmentId: departmentId,
      userIds: userIds
    };
    if (departmentHeadId !== null && departmentHeadId !== undefined) {
      payload.departmentHeadId = departmentHeadId;
    }
    return this.http.post(urls.API_URL + 'assignUsersToDepartment', payload, { responseType: 'text' });
  }
}

