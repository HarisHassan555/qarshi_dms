import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  constructor(
    private http: HttpClient
  ) { }

  getUserMenus() {
    return this.http.get(urls.API_URL + 'allMenu');
  }

  getAllMenusRaw() {
    return this.http.get(urls.API_URL + 'getAllMenu');
  }

  getAllSubMenusRaw() {
    return this.http.get(urls.API_URL + 'getAllSubMenu');
  }

  getAllSubMenuRolesRaw() {
    return this.http.get(urls.API_URL + 'getAllSubMenuRole');
  }

  updateMenu(payload: any) {
    return this.http.post(urls.API_URL + 'updateMenu', payload, { responseType: 'text' });
  }

  updateSubMenu(payload: any) {
    return this.http.post(urls.API_URL + 'updateSubMenu', payload, { responseType: 'text' });
  }

  deleteSubMenuRole(ids: (number | string)[]) {
    const csv = (ids || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0).join(',');
    return this.http.post(urls.API_URL + 'deleteSubMenuRole', JSON.stringify(csv), {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
      responseType: 'text'
    });
  }

  deleteSubMenuRoleBySubMenu(ids: (number | string)[]) {
    const csv = (ids || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0).join(',');
    return this.http.post(urls.API_URL + 'deleteSubMenuRoleBySubMenu', JSON.stringify(csv), {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
      responseType: 'text'
    });
  }

  deleteSubMenu(ids: (number | string)[]) {
    const csv = (ids || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0).join(',');
    return this.http.post(urls.API_URL + 'deleteSubMenu', JSON.stringify(csv), {
      headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
      responseType: 'text'
    });
  }


    // @ts-ignore
    getAllSubMenuRoles(roleId: number | undefined, userId: number): Observable<CfgTblSubMenuRole[]> {
        // @ts-ignore
        // @ts-ignore
        // @ts-ignore
        const params = new HttpParams()
            .set('roleId', Number(roleId))
            .set('userId', Number(userId));

        // @ts-ignore
        return this.http.get<CfgTblSubMenuRole[]>(urls.API_URL + 'allSubMenuRole', { params });
    }



    // @ts-ignore
    saveSubMenuRole(userId: string, roleId: string,subMenuRole: any): Observable<CfgTblSubMenuRole> {

      // @ts-ignore
        return this.http.post<CfgTblSubMenuRole>(`${urls.API_URL}addNewSubMenuRoleinList/${userId}/${roleId}`,subMenuRole)
    }

    addSubMenuRole(payload: any): Observable<any> {
        return this.http.post<any>(urls.API_URL + 'addNewSubMenuRole', payload, { responseType: 'text' as 'json' });
    }


    getMenuBySubMenu(id: any) {
        const params = new HttpParams()
            .set('Id', Number(id))
        return this.http.get(urls.API_URL + 'getMenuBySubmenuId',{ params });
    }
}
